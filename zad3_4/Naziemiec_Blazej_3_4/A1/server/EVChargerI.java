import SmartHome.*;
import com.zeroc.Ice.Current;
import java.util.*;

public class EVChargerI extends DeviceI implements EVCharger {
    private long startChargingTime = 0;
    private ChargingLevel currentLevel = ChargingLevel.Slow;

    public EVChargerI(String id, Floor loc, HomeDirectoryI dir) {
        super(id, loc, "EV Charger Opus 4.7", dir);
    }

    @Override public void turnOn(Current c) {
        boolean wasOn = this.isOn;
        super.turnOn(c);
        if (!wasOn) {
            startChargingTime = System.currentTimeMillis();
        }
    }

    @Override public float getCurrentCostPLN(Current c) {
        if(!isOn) return 0;
        long durationMillis = System.currentTimeMillis() - startChargingTime;
        float hours = durationMillis / 3600000.0f;
        float power = (currentLevel == ChargingLevel.Fast) ? 22.0f : (currentLevel == ChargingLevel.Medium ? 11.0f : 3.7f);
        return hours * power * 1.20f;
    }

    @Override public void setLevel(ChargingLevel l, Current c) {
        if (!checkOn()) return;
        this.currentLevel = l;
        log("Charging mode: " + l);
    }

    @Override public float getEstimatedTimeToFull(Current c) {
        return 3.5f;
    }

    @Override public void startSuperCharge(Current c) {
        if (!checkOn()) return;
        setLevel(ChargingLevel.Fast, c);
        log("SUPERCHARGE START");
    }

    @Override protected Map<String, String> getExtraInfo() {
        return Map.of(
                "cost_pln", String.format("%.2f PLN", getCurrentCostPLN(null)),
                "charging_mode", currentLevel.toString()
        );
    }
}