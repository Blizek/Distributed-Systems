import java.io.IOException;

public class ExternalAppManager {

    private final String[] command;
    private final String appName;
    private Process process;
    private boolean appRunning = false;

    public ExternalAppManager(String[] command) {
        this.command = command;
        if (command.length >= 3 && command[0].equals("open") && command[1].equals("-a")) {
            this.appName = command[2];
        } else {
            this.appName = null;
        }
    }

    public synchronized void startApp() {
        if (appRunning) {
            System.out.println("[AppManager] External app is already running");
            return;
        }
        try {
            System.out.println("[AppManager] Starting external app: " + String.join(" ", command));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            process = pb.start();
            appRunning = true;

            if (appName != null) {
                System.out.println("[AppManager] App '" + appName + "' launched via open command");
            } else {
                System.out.println("[AppManager] External app started (PID: " + process.pid() + ")");
            }
        } catch (IOException e) {
            System.err.println("[AppManager] Failed to start external app: " + e.getMessage());
        }
    }

    public synchronized void stopApp() {
        if (!appRunning) {
            System.out.println("[AppManager] External app is not running");
            return;
        }

        if (appName != null) {
            stopMacApp();
        } else {
            stopDirectProcess();
        }

        appRunning = false;
    }

    private void stopMacApp() {
        try {
            System.out.println("[AppManager] Stopping macOS app '" + appName + "' via osascript...");
            ProcessBuilder pb = new ProcessBuilder(
                    "osascript", "-e", "tell application \"" + appName + "\" to quit"
            );
            pb.inheritIO();
            Process killProcess = pb.start();
            killProcess.waitFor();
            System.out.println("[AppManager] App '" + appName + "' stopped");
        } catch (IOException | InterruptedException e) {
            try {
                System.out.println("[AppManager] osascript failed, trying pkill...");
                new ProcessBuilder("pkill", "-f", appName).start().waitFor();
                System.out.println("[AppManager] App killed via pkill");
            } catch (IOException | InterruptedException ex) {
                System.err.println("[AppManager] Failed to stop app: " + ex.getMessage());
            }
        }
    }

    private void stopDirectProcess() {
        if (process == null || !process.isAlive()) {
            System.out.println("[AppManager] Process already exited");
            return;
        }
        System.out.println("[AppManager] Stopping external app (PID: " + process.pid() + ")...");
        process.destroy();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            process.destroyForcibly();
        }
        process = null;
        System.out.println("[AppManager] External app stopped.");
    }

    public synchronized boolean isRunning() {
        return appRunning;
    }
}
