import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CountDownLatch;

public class ZooKeeperApp {

    private static final int SESSION_TIMEOUT = 30000;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: java -jar zookeeper-watcher.jar <zk_hosts> <external_app_command...>");
            System.out.println();
            System.out.println("<zk_hosts> - ZooKeeper connection string, e.g. localhost:2181,localhost:2182,localhost:2183");
            System.out.println("<external_app_command> - Command to launch external app, e.g. 'open -a Calculator' (macOS)");
            System.out.println();
            System.out.println("Example:");
            System.out.println("java -jar zookeeper-watcher.jar localhost:2181,localhost:2182,localhost:2183 open -a Calculator");
            System.exit(1);
        }

        String zkHosts = args[0];
        String[] appCommand = new String[args.length - 1];
        System.arraycopy(args, 1, appCommand, 0, appCommand.length);

        System.out.println("  ZooKeeper Node Watcher Application\n");
        System.out.println("ZooKeeper hosts: " + zkHosts);
        System.out.println("External app:    " + String.join(" ", appCommand));
        System.out.println();

        CountDownLatch connectedLatch = new CountDownLatch(1);

        ZooKeeper zk = new ZooKeeper(zkHosts, SESSION_TIMEOUT, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("[ZK] Connected to ZooKeeper.");
                    connectedLatch.countDown();
                } else if (event.getState() == Event.KeeperState.Disconnected) {
                    System.out.println("[ZK] Disconnected from ZooKeeper.");
                } else if (event.getState() == Event.KeeperState.Expired) {
                    System.out.println("[ZK] Session expired.");
                }
            }
        });

        System.out.println("[ZK] Connecting to ZooKeeper...");
        connectedLatch.await();

        ExternalAppManager appManager = new ExternalAppManager(appCommand);
        NodeWatcher nodeWatcher = new NodeWatcher(zk, appManager);

        nodeWatcher.startWatching();

        System.out.println();
        System.out.println("Commands:");
        System.out.println("tree - display the full tree structure of /a");
        System.out.println("quit - exit the application");
        System.out.println();

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim().toLowerCase();
            switch (line) {
                case "tree":
                    nodeWatcher.printTree();
                    break;
                case "quit":
                    System.out.println("[App] Shutting down...");
                    appManager.stopApp();
                    zk.close();
                    System.out.println("[App] Goodbye!");
                    System.exit(0);
                    break;
                default:
                    if (!line.isEmpty()) {
                        System.out.println("Unknown command: " + line);
                        System.out.println("Available commands: tree, quit");
                    }
                    break;
            }
        }
    }
}
