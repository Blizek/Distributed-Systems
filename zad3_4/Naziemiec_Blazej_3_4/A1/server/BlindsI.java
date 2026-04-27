import SmartHome.*;
import com.zeroc.Ice.Current;
import java.util.Map;

public class BlindsI extends DeviceI implements Blinds {
    private short position = 0;

    public BlindsI(String id, Floor loc, HomeDirectoryI dir) {
        super(id, loc, "LocalBlinds6767", dir);
    }

    @Override
    public void setPosition(short percentage, Current current) throws ValueOutOfRange {
        if (!checkOn()) return;
        if (percentage < 0 || percentage > 100) {
            throw new ValueOutOfRange(0, 100);
        }
        this.position = percentage;
        log("Blinds set to position: " + percentage + "%");
    }

    @Override
    public void calibrate(Current current) {
        if (!checkOn()) return;
        log("Started blinds calibration...");
        this.position = 100;
        log("Calibration finished. Blinds are at position: 100%");
    }

    @Override
    protected Map<String, String> getExtraInfo() {
        return Map.of("position", position + "%");
    }
}
