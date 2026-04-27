import SmartHome.Floor;
import SmartHome.NoctovisionCamera;
import com.zeroc.Ice.Current;

import java.util.Map;

public class NoctovisionCameraI extends DeviceI implements NoctovisionCamera {
    private boolean isNoctovision = false;

    public NoctovisionCameraI(String id, Floor location, HomeDirectoryI directory) {
        super(id, location, "Noctovision-JEE", directory);
    }

    @Override
    protected Map<String, String> getExtraInfo() {
        return Map.of("noctovision_mode", String.valueOf(this.isNoctovision));
    }

    @Override
    public void turnOnNoctovision(Current current) {
        if (!checkOn())
            return;
        this.isNoctovision = true;
        log("Noctovision On");
    }

    @Override
    public void turnOffNoctovision(Current current) {
        if (!checkOn())
            return;
        this.isNoctovision = false;
        log("Noctovision Off");
    }

    @Override
    public void triggerVideoDump(Current current) {
        if (!checkOn())
            return;
        log("Generated dump to file DUMP_" + System.currentTimeMillis() + ".mp4");
    }
}
