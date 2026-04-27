import SmartHome.*;
import com.zeroc.Ice.Current;
import java.util.*;

public class FridgeI extends DeviceI implements Fridge {
    private float temp = 4.0f;
    private Map<String, Integer> inventory = new HashMap<>();

    public FridgeI(String id, Floor loc, HomeDirectoryI dir) {
        super(id, loc, "Samsung Smart Gemini 3.1", dir);
        inventory.put("Milk", 2);
    }

    @Override public Map<String, Integer> getInventory(Current c) { return inventory; }

    @Override public void addProduct(String name, int qty, Current c) throws InvalidQuantity {
        if (qty <= 0) {
            throw new InvalidQuantity(qty);
        }
        inventory.put(name, inventory.getOrDefault(name, 0) + qty);
        log("Added: " + name + " (" + qty + ")");
    }

    @Override public void removeProduct(String name, int qty, Current c) throws ProductNotFound, InvalidProductQuantity, InvalidQuantity {
        if (qty <= 0) {
            throw new InvalidQuantity(qty);
        }
        if(!inventory.containsKey(name)) throw new ProductNotFound(name);
        if(inventory.get(name) < qty) throw new InvalidProductQuantity(name, inventory.get(name));

        inventory.put(name, inventory.get(name) - qty);
        if(inventory.get(name) == 0) inventory.remove(name);
        log("Removed: " + name + " (" + qty + ")");
    }

    @Override public float getReading(Current c) {
        return temp;
    }

    @Override public void setTemperature(float t, Current c) throws ValueOutOfRange {
        if (!checkOn()) return;
        if (t < -30.0f || t > 20.0f) {
            throw new ValueOutOfRange(-30.0f, 20.0f);
        }
        this.temp = t;
        log("Temp set to: " + t);
    }

    @Override protected Map<String, String> getExtraInfo() {
        return Map.of("temp", temp + "C", "items_count", String.valueOf(inventory.size()));
    }
}