import SmartHome.*;
import com.zeroc.Ice.Current;
import java.util.Map;

public class LightI extends DeviceI implements Light {
    private Color color = new Color((short)255, (short)255, (short)255);
    private short brightness = 100;
    private boolean discoMode = false;

    public LightI(String id, Floor loc, HomeDirectoryI dir) {
        super(id, loc, "Kanye's Flashing Light", dir);
    }

    @Override public void setColor(Color c, Current current) throws ValueOutOfRange {
        if (!checkOn()) return;
        if (c.r < 0 || c.r > 255 || c.g < 0 || c.g > 255 || c.b < 0 || c.b > 255) {
            throw new ValueOutOfRange(0, 255);
        }
        this.color = c;
        this.discoMode = false;
        log(String.format("Changed color to RGB(%d, %d, %d)", c.r, c.g, c.b));
    }

    @Override public void setBrightness(short level, Current current) throws ValueOutOfRange {
        if (!checkOn()) return;
        if (level < 0 || level > 100) {
            throw new ValueOutOfRange(0, 100);
        }
        this.brightness = level;
        log("Changed brightness to " + level + "%");
    }

    @Override public void startDiscoMode(Current current) {
        if (!checkOn()) return;
        this.discoMode = true;
        log("Started DISCO mode!");
    }

    @Override protected Map<String, String> getExtraInfo() {
        return Map.of(
                "color_rgb", String.format("(%d, %d, %d)", color.r, color.g, color.b),
                "brightness", brightness + "%",
                "disco_mode", String.valueOf(discoMode)
        );
    }
}
