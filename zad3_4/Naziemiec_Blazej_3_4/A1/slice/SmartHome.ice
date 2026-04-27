module SmartHome {
    enum Floor { Basement, GroundFloor, FirstFloor, All };
    enum FurnaceMode { WaterOnly, HeatingOnly, Both };
    enum ChargingLevel { Slow, Medium, Fast };

    struct Color {
        short r;
        short g;
        short b;
    };

    struct PTZPosition {
        float pan;
        float tilt;
    };

    sequence<string> LogEntries;
    sequence<string> IdentityList;
    dictionary<string, int> Inventory;
    dictionary<string, string> AdditionalInfo;

    struct DeviceConfig {
        string id;
        Floor location;
        string model;
        bool isOn;
        AdditionalInfo extra;
    };

    exception ValueOutOfRange {
        float minVal;
        float maxVal;
    };

    exception InvalidQuantity {
        int providedQuantity;
    };

    exception ProductNotFound {
        string productName;
    };

    exception InvalidProductQuantity {
        string productName;
        int available;
    };

    interface Device {
        idempotent DeviceConfig getConfig();
        idempotent LogEntries getRecentLogs();
        void turnOn();
        void turnOff();
    };

    interface Thermostat extends Device {
        idempotent void setTemperature(float temp) throws ValueOutOfRange;
        idempotent float getReading();
    };

    interface Fridge extends Thermostat {
        idempotent Inventory getInventory();
        void addProduct(string name, int quantity) throws InvalidQuantity;
        void removeProduct(string name, int quantity) throws ProductNotFound, InvalidProductQuantity, InvalidQuantity;
    };

    interface Light extends Device {
        idempotent void setColor(Color c) throws ValueOutOfRange;
        idempotent void setBrightness(short level) throws ValueOutOfRange;
        void startDiscoMode();
    };

    interface Blinds extends Device {
        idempotent void setPosition(short percentage) throws ValueOutOfRange;
        void calibrate();
    };

    interface EVCharger extends Device {
        idempotent void setLevel(ChargingLevel level);
        idempotent float getEstimatedTimeToFull();
        idempotent float getCurrentCostPLN();
        void startSuperCharge();
    };

    interface Furnace extends Device {
        idempotent void setMode(FurnaceMode mode);
        void boostHotWater();
    };

    interface Camera extends Device {
        void triggerVideoDump();
    };

    interface PTZCamera extends Camera {
        idempotent void setPTZ(PTZPosition pos) throws ValueOutOfRange;
        void zoomByPercentage(float percentage) throws ValueOutOfRange;
        idempotent void resetZoom();
        void startPatrol();
        void stopPatrol();
        idempotent void backToPosition();
        idempotent bool isMoving();
    };

    interface NoctovisionCamera extends Camera {
        idempotent void turnOnNoctovision();
        idempotent void turnOffNoctovision();
    }

    interface HomeDirectory {
        idempotent IdentityList getActiveDevices(Floor location);
        idempotent LogEntries getGlobalLogs();
    };
};