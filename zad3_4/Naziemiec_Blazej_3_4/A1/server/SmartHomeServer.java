import com.zeroc.Ice.*;
import SmartHome.*;
import java.util.Scanner;

public class SmartHomeServer {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Choose floor to run:");
        System.out.println("1 - Basement");
        System.out.println("2 - Ground floor");
        System.out.println("3 - First floor");
        System.out.print("Choice: ");

        String choice = scanner.nextLine();

        Floor floor;
        int port;

        switch (choice) {
            case "1" -> {
                floor = Floor.Basement;
                port = 10001;
            }
            case "2" -> {
                floor = Floor.GroundFloor;
                port = 10002;
            }
            case "3" -> {
                floor = Floor.FirstFloor;
                port = 10003;
            }
            default -> {
                System.out.println("ERROR: Invalid choice (" + choice + "). Closing.");
                return;
            }
        }

        try (Communicator communicator = Util.initialize(args)) {
            ObjectAdapter adapter = communicator.createObjectAdapterWithEndpoints(
                    "SmartHomeAdapter", "tcp -p " + port);

            HomeDirectoryI directory = new HomeDirectoryI();
            adapter.add(directory, Util.stringToIdentity("smart_home"));

            setupDevices(floor, adapter, directory);

            adapter.activate();

            System.out.println("\nServer is running");
            System.out.println("Location: " + floor);
            System.out.println("Port: " + port);
            System.out.println("Press Ctrl+C, to shutdown");

            communicator.waitForShutdown();
        }
    }

    private static void setupDevices(Floor floor, ObjectAdapter adapter, HomeDirectoryI directory) {
        switch (floor) {
            case Basement -> {
                register(adapter, directory, new EVChargerI("ev_charger", floor, directory));
                register(adapter, directory, new FurnaceI("main_furnace", floor, directory));
                register(adapter, directory, new FridgeI("basement_fridge", floor, directory));
                System.out.println("[INFO] Loaded basement devices.");
            }
            case GroundFloor -> {
                register(adapter, directory, new PTZCameraI("ptz_camera", floor, directory));
                register(adapter, directory, new NoctovisionCameraI("noctovision_camera", floor, directory));
                register(adapter, directory, new LightI("kitchen_light", floor, directory));
                register(adapter, directory, new FridgeI("kitchen_fridge", floor, directory));
                register(adapter, directory, new LightI("living_room_light", floor, directory));
                register(adapter, directory, new BlindsI("living_room_blinds", floor, directory));
                register(adapter, directory, new BlindsI("kitchen_blinds", floor, directory));
                register(adapter, directory, new ThermostatI("living_room_thermostat", floor, directory));
                register(adapter, directory, new ThermostatI("kitchen_thermostat", floor, directory));
                System.out.println("[INFO] Loaded ground floor devices.");
            }
            case FirstFloor -> {
                register(adapter, directory, new LightI("bedroom_light", floor, directory));
                register(adapter, directory, new CameraI("normal_camera", floor, directory));
                register(adapter, directory, new BlindsI("bedroom_blinds", floor, directory));
                register(adapter, directory, new ThermostatI("bedroom_thermostat", floor, directory));
                System.out.println("[INFO] Loaded first floor devices.");
            }
        }
    }

    private static void register(ObjectAdapter adapter, HomeDirectoryI dir, DeviceI servant) {
        String identity = servant.location.toString() + "/" + servant.id;
        adapter.add(servant, Util.stringToIdentity(identity));
        dir.registerDevice(identity, servant);
    }
}