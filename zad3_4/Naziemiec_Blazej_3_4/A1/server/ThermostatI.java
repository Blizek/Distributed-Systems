import SmartHome.*;
import com.zeroc.Ice.Current;
import java.util.Map;

public class ThermostatI extends DeviceI implements Thermostat {
    private float temperature = 21.0f;

    public ThermostatI(String id, Floor loc, HomeDirectoryI dir) {
        super(id, loc, "Temperatura47", dir);
    }

    @Override
    public float getReading(Current c) {
        return temperature;
    }

    @Override
    public void setTemperature(float t, Current c) throws ValueOutOfRange {
        if (!checkOn())
            return;
        if (t < 10.0f || t > 35.0f) {
            throw new ValueOutOfRange(10.0f, 35.0f);
        }
        this.temperature = t;
        log("Temperature set to: " + t);
    }

    @Override
    protected Map<String, String> getExtraInfo() {
        return Map.of("temperature", temperature + "C");
    }
}
