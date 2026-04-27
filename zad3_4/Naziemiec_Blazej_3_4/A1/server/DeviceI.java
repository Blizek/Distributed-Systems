import SmartHome.*;
import com.zeroc.Ice.Current;
import java.util.*;

public abstract class DeviceI implements Device {
    protected String id;
    protected Floor location;
    protected String model;
    protected boolean isOn = false;
    protected List<String> logs = new ArrayList<>();
    protected HomeDirectoryI directory;

    public DeviceI(String id, Floor location, String model, HomeDirectoryI directory) {
        this.id = id;
        this.location = location;
        this.model = model;
        this.directory = directory;
    }

    protected void log(String msg) {
        String entry = new Date() + ": [" + id + "] " + msg;
        logs.add(entry);
        directory.addLog(entry);
        System.out.println("[DEVICE LOG] " + entry);
    }

    @Override
    public void turnOn(Current c) {
        if (isOn) {
            log("Rejected: device is already on.");
            return;
        }
        isOn = true;
        log("Turned on");
    }

    @Override
    public void turnOff(Current c) {
        if (!isOn) {
            log("Rejected: device is already off.");
            return;
        }
        isOn = false;
        log("Turned off");
    }

    @Override
    public String[] getRecentLogs(Current c) {
        return logs.toArray(new String[0]);
    }

    @Override
    public DeviceConfig getConfig(Current c) {
        return new DeviceConfig(id, location, model, isOn, getExtraInfo());
    }

    protected abstract Map<String, String> getExtraInfo();

    protected boolean checkOn() {
        if (!isOn) {
            log("Rejected: device is disconnected from power.");
            return false;
        }
        return true;
    }
}