import SmartHome.*;
import com.zeroc.Ice.Current;
import java.util.Map;

public class CameraI extends DeviceI implements Camera {

    public CameraI(String id, Floor loc, HomeDirectoryI dir) {
        super(id, loc, "Camera Obscura", dir);
    }

    @Override public void triggerVideoDump(Current c) {
        if (!checkOn()) return;
        String filename = "DUMP_" + System.currentTimeMillis() + ".mp4";
        log("Generated video dump: " + filename);
    }

    @Override protected Map<String, String> getExtraInfo() {
        return Map.of(
                "resolution", "1080p"
        );
    }
}
