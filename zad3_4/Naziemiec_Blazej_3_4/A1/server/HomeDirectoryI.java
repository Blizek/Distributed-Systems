import SmartHome.*;
import com.zeroc.Ice.Current;
import java.util.*;

public class HomeDirectoryI implements HomeDirectory {
    private final List<String> globalLogs = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Device> devices = new HashMap<>();

    public void registerDevice(String identity, Device servant) {
        devices.put(identity, servant);
    }

    public void addLog(String message) {
        globalLogs.add(message);
    }

    @Override
    public String[] getActiveDevices(Floor location, Current current) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Device> e : devices.entrySet()) {
            if (location == Floor.All || e.getValue().getConfig(current).location == location) {
                result.add(e.getKey());
            }
        }
        return result.toArray(new String[0]);
    }

    @Override
    public String[] getGlobalLogs(Current current) {
        return globalLogs.toArray(new String[0]);
    }
}