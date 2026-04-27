import SmartHome.*;
import com.zeroc.Ice.Current;
import java.util.Map;

public class FurnaceI extends DeviceI implements Furnace {
    private FurnaceMode currentMode = FurnaceMode.Both;
    private boolean isBoostingHotWater = false;

    public FurnaceI(String id, Floor loc, HomeDirectoryI dir) {
        super(id, loc, "Fire GPT 5.5", dir);
    }

    @Override public void setMode(FurnaceMode mode, Current current) {
        if (!checkOn()) return;
        this.currentMode = mode;
        log("Changed furnace mode to: " + mode.toString());
    }

    @Override public void boostHotWater(Current current) {
        if (!checkOn()) return;
        this.isBoostingHotWater = true;
        log("Turned on water heating (BOOST).");
    }

    @Override protected Map<String, String> getExtraInfo() {
        return Map.of(
                "current_mode", currentMode.toString(),
                "is_boosting", String.valueOf(isBoostingHotWater)
        );
    }
}
