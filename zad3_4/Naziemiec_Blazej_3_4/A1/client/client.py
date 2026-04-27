import sys
import os

current_dir = os.path.dirname(__file__)
gen_path = os.path.abspath(os.path.join(current_dir, "..", "gen-python"))
sys.path.append(gen_path)

import Ice
import SmartHome

def print_menu(options):
    for i, opt in enumerate(options, 1):
        print(f"{i}. {opt}")
    while True:
        try:
            res = int(input("Choose option: "))
            if 1 <= res <= len(options): return res
        except ValueError: pass

GENERIC_OPTIONS = ["Get full configuration (Config)", "Turn On", "Turn Off", "Device log history"]

def handle_generic_option(dev, act_offset):
    if act_offset == 1:
        conf = dev.getConfig()
        print(f"\n[CONFIG] ID: {conf.id}, Location: {conf.location}")
        print(f"Model: {conf.model}, Is On: {conf.isOn}")
        print(f"Additional info: {conf.extra}")
    elif act_offset == 2:
        dev.turnOn()
        print("[OK] Command: Turn on power sent.")
    elif act_offset == 3:
        dev.turnOff()
        print("[OK] Command: Turn off power sent.")
    elif act_offset == 4:
        logs = dev.getRecentLogs()
        print("\nDEVICE LOG")
        print("\n".join(logs) if logs else "No logs for this device.")

def handle_fridge(proxy):
    fridge = SmartHome.FridgePrx.checkedCast(proxy)
    opts = ["Live Status", "Add product", "Remove product", "Set Temperature"] + GENERIC_OPTIONS + ["Back"]
    while True:
        act = print_menu(opts)
        if act == 1:
            conf = fridge.getConfig()
            print(f"\n[INFO] Fridge {conf.id} (On: {conf.isOn}): {conf.extra}")
            print(f"Current Reading: {fridge.getReading():.1f}C")
            print(f"[INV] Inventory: {fridge.getInventory()}")
        elif act == 2:
            name = input("Product name: ")
            try:
                qty = int(input("Quantity: "))
                fridge.addProduct(name, qty)
                print(f"[INFO] Add product '{name}' request sent to server.")
            except SmartHome.InvalidQuantity as e:
                print(f"SERVER ERROR: Invalid parameter size - {e.providedQuantity}")
            except ValueError: pass
        elif act == 3:
            name = input("Product name: ")
            try:
                qty = int(input("Quantity: "))
                fridge.removeProduct(name, qty)
                print(f"[INFO] Remove product '{name}' request sent to server.")
            except (SmartHome.ProductNotFound, SmartHome.InvalidProductQuantity) as e:
                print(f"SERVER ERROR: {e}")
            except SmartHome.InvalidQuantity as e:
                print(f"SERVER ERROR: Invalid parameter size - {e.providedQuantity}")
            except ValueError: pass
        elif act == 4:
            try:
                t = float(input("Target temperature: "))
                fridge.setTemperature(t)
                print("[INFO] Temperature set request sent to server.")
            except SmartHome.ValueOutOfRange as e:
                print(f"SERVER ERROR: Temperature out of range ({e.minVal:.2f} - {e.maxVal:.2f})")
            except ValueError: pass
        elif act in [5, 6, 7, 8]:
            handle_generic_option(fridge, act - 4)
        else: break

def handle_basic_camera(proxy):
    cam = SmartHome.CameraPrx.checkedCast(proxy)
    opts = ["Live Status", "Generate video dump (Video Dump)"] + GENERIC_OPTIONS + ["Back"]
    while True:
        act = print_menu(opts)
        if act == 1:
            conf = cam.getConfig()
            print(f"\n[INFO] Basic Camera {conf.id} (On: {conf.isOn}): {conf.extra}")
        elif act == 2:
            cam.triggerVideoDump()
            print("[INFO] Video dump request sent to server.")
        elif act in [3, 4, 5, 6]:
            handle_generic_option(cam, act - 2)
        else: break

def handle_noctovision_camera(proxy):
    cam = SmartHome.NoctovisionCameraPrx.checkedCast(proxy)
    opts = ["Live Status", "Turn On Noctovision", "Turn Off Noctovision", "Generate video dump (Video Dump)"] + GENERIC_OPTIONS + ["Back"]
    while True:
        act = print_menu(opts)
        if act == 1:
            conf = cam.getConfig()
            print(f"\n[INFO] Noctovision Camera {conf.id} (On: {conf.isOn}): {conf.extra}")
        elif act == 2:
            cam.turnOnNoctovision()
            print("[INFO] Turn on noctovision sent to server.")
        elif act == 3:
            cam.turnOffNoctovision()
            print("[INFO] Turn off noctovision sent to server.")
        elif act == 4:
            cam.triggerVideoDump()
            print("[INFO] Video dump request sent to server.")
        elif act in [5, 6, 7, 8]:
            handle_generic_option(cam, act - 4)
        else: break

def handle_camera(proxy):
    cam = SmartHome.PTZCameraPrx.checkedCast(proxy)
    opts = ["Live Status", "Set PTZ (Pan/Tilt)", "Zoom (Zoom In)", "Reset Zoom", "Reset Position", "Check if moving", "Video Dump", "Start Patrol", "Stop Patrol"] + GENERIC_OPTIONS + ["Back"]
    while True:
        act = print_menu(opts)
        if act == 1:
            conf = cam.getConfig()
            print(f"\n[INFO] PTZ Camera {conf.id} (On: {conf.isOn}): {conf.extra}")
            print(f"Currently moving: {cam.isMoving()}")
        elif act == 2:
            try:
                p = float(input("Pan (-45 to 45): "))
                t = float(input("Tilt (-30 to 30): "))
                cam.setPTZ(SmartHome.PTZPosition(pan=p, tilt=t))
                print("[INFO] PTZ set request sent to server.")
            except SmartHome.ValueOutOfRange as e:
                print(f"ERROR: PTZ value out of range ({e.minVal:.2f} - {e.maxVal:.2f})")
            except ValueError: pass
        elif act == 3:
            try:
                z = float(input("Change zoom by % (e.g. 50, -50): "))
                cam.zoomByPercentage(z)
                print("[INFO] Zoom change request sent to server.")
            except SmartHome.ValueOutOfRange as e:
                print(f"SERVER ERROR: Zoom scale would go out of physical bounds: ({e.minVal:.2f}x - {e.maxVal:.2f}x)")
            except ValueError: pass
        elif act == 4:
            cam.resetZoom()
            print("[INFO] Zoom reset request sent to server.")
        elif act == 5:
            cam.backToPosition()
            print("[INFO] Position reset request sent to server.")
        elif act == 6:
            moving = cam.isMoving()
            print(f"\n[STATUS] Camera is currently moving: {'Yes' if moving else 'No'}")
        elif act == 7:
            cam.triggerVideoDump()
            print("[INFO] Video dump request sent to server.")
        elif act == 8:
            cam.startPatrol()
            print("[INFO] Patrol start request sent to server.")
        elif act == 9:
            cam.stopPatrol()
            print("[INFO] Patrol stop request sent to server.")
        elif act in [10, 11, 12, 13]:
            handle_generic_option(cam, act - 9)
        else: break

def handle_light(proxy):
    light = SmartHome.LightPrx.checkedCast(proxy)
    opts = ["Status", "Change color (RGB)", "Set brightness", "Start DISCO mode"] + GENERIC_OPTIONS + ["Back"]
    while True:
        act = print_menu(opts)
        if act == 1:
            conf = light.getConfig()
            print(f"\n[INFO] Smart Light {conf.id} (On: {conf.isOn}): {conf.extra}")
        elif act == 2:
            try:
                r = int(input("Red (0-255): "))
                g = int(input("Green (0-255): "))
                b = int(input("Blue (0-255): "))
                light.setColor(SmartHome.Color(r=r, g=g, b=b))
                print("[INFO] Color change request sent to server.")
            except SmartHome.ValueOutOfRange as e:
                print(f"SERVER ERROR: RGB value must be between {e.minVal:.2f} and {e.maxVal:.2f}")
            except ValueError: pass
        elif act == 3:
            try:
                level = int(input("Brightness (0-100%): "))
                light.setBrightness(level)
                print("[INFO] Brightness change request sent to server.")
            except SmartHome.ValueOutOfRange as e:
                print(f"ERROR: Brightness out of range ({e.minVal:.2f} - {e.maxVal:.2f})")
            except ValueError: pass
        elif act == 4:
            light.startDiscoMode()
            print("[INFO] DISCO mode request sent to server.")
        elif act in [5, 6, 7, 8]:
            handle_generic_option(light, act - 4)
        else: break

def handle_blinds(proxy):
    blinds = SmartHome.BlindsPrx.checkedCast(proxy)
    opts = ["Status", "Set blind extension", "Force calibration"] + GENERIC_OPTIONS + ["Back"]
    while True:
        act = print_menu(opts)
        if act == 1:
            conf = blinds.getConfig()
            print(f"\n[INFO] Blinds/Curtains {conf.id} (Active: {conf.isOn}): {conf.extra}")
        elif act == 2:
            try:
                pos = int(input("Set extension percentage (0=closed to 100=open): "))
                blinds.setPosition(pos)
                print("[INFO] Blinds set request sent to server.")
            except SmartHome.ValueOutOfRange as e:
                print(f"SERVER ERROR: Position out of range ({e.minVal:.2f} - {e.maxVal:.2f})")
            except ValueError: pass
        elif act == 3:
            blinds.calibrate()
            print("[INFO] Calibration request sent to server.")
        elif act in [4, 5, 6, 7]:
            handle_generic_option(blinds, act - 3)
        else: break

def handle_furnace(proxy):
    furnace = SmartHome.FurnacePrx.checkedCast(proxy)
    opts = ["Furnace Status", "Change heating mode", "Start BOOST (hot water)"] + GENERIC_OPTIONS + ["Back"]
    while True:
        act = print_menu(opts)
        if act == 1:
            conf = furnace.getConfig()
            print(f"\n[INFO] Furnace {conf.id} (Power: {conf.isOn}): {conf.extra}")
        elif act == 2:
            modes = [SmartHome.FurnaceMode.WaterOnly, SmartHome.FurnaceMode.HeatingOnly, SmartHome.FurnaceMode.Both]
            try:
                m = int(input("Select mode (1=Water Only, 2=Heating Only, 3=Both modes): "))
                if 1 <= m <= 3:
                    furnace.setMode(modes[m-1])
                    print("[INFO] Furnace mode change request sent to server.")
                else: print("Invalid mode.")
            except ValueError: pass
        elif act == 3:
            furnace.boostHotWater()
            print("[INFO] BOOST request sent to server.")
        elif act in [4, 5, 6, 7]:
            handle_generic_option(furnace, act - 3)
        else: break

def handle_evcharger(proxy):
    ev = SmartHome.EVChargerPrx.checkedCast(proxy)
    opts = ["Charger Station Status", "Change current speed", "Start SuperCharge"] + GENERIC_OPTIONS + ["Back"]
    while True:
        act = print_menu(opts)
        if act == 1:
            conf = ev.getConfig()
            print(f"\n[INFO] EV Station {conf.id} (Power: {conf.isOn}): {conf.extra}")
            print(f"Estimated time to full: {ev.getEstimatedTimeToFull()} hours")
            print(f"Current session cost (PLN): {ev.getCurrentCostPLN():.2f}")
        elif act == 2:
            levels = [SmartHome.ChargingLevel.Slow, SmartHome.ChargingLevel.Medium, SmartHome.ChargingLevel.Fast]
            try:
                m = int(input("Select speed (1=Slow, 2=Medium, 3=Fast): "))
                if 1 <= m <= 3:
                    ev.setLevel(levels[m-1])
                    print("[INFO] Charge level change request sent to server.")
                else: print("Invalid level.")
            except ValueError: pass
        elif act == 3:
            ev.startSuperCharge()
            print("[INFO] SuperCharge request sent to server.")
        elif act in [4, 5, 6, 7]:
            handle_generic_option(ev, act - 3)
        else: break

def handle_thermostat(proxy):
    thermostat = SmartHome.ThermostatPrx.checkedCast(proxy)
    opts = ["Live Status", "Set Temperature"] + GENERIC_OPTIONS + ["Back"]
    while True:
        act = print_menu(opts)
        if act == 1:
            conf = thermostat.getConfig()
            print(f"\n[INFO] Thermostat {conf.id} (On: {conf.isOn}): {conf.extra}")
            print(f"Current Reading: {thermostat.getReading():.1f}C")
        elif act == 2:
            try:
                t = float(input("Target temperature: "))
                thermostat.setTemperature(t)
                print("[INFO] Temperature set request sent to server.")
            except SmartHome.ValueOutOfRange as e:
                print(f"SERVER ERROR: Temperature out of range ({e.minVal:.2f} - {e.maxVal:.2f})")
            except ValueError: pass
        elif act in [3, 4, 5, 6]:
            handle_generic_option(thermostat, act - 2)
        else: break

def handle_generic_device(proxy):
    dev = SmartHome.DevicePrx.checkedCast(proxy)
    opts = GENERIC_OPTIONS + ["Back"]
    while True:
        act = print_menu(opts)
        if act in [0, 1, 2, 3]:
            handle_generic_option(dev, act)
        else: break

def main():
    with Ice.initialize(sys.argv) as communicator:
        print("Smart home client")

        while True:
            print("\nChoose floor:")
            loc_idx = print_menu([
                "Basement",
                "Ground floor",
                "First floor",
                "Exit program"
            ])
            
            if loc_idx == 4:
                print("Goodbye!")
                break
                
            ports = ["10001", "10002", "10003"]
            port = ports[loc_idx - 1]
            
            try:
                base = communicator.stringToProxy(f"smart_home:tcp -t 2000 -p {port}")
                directory = SmartHome.HomeDirectoryPrx.checkedCast(base)
                if not directory:
                    raise Exception("Proxy returned null")
            except Exception:
                print(f"\n[ERROR] Cannot connect to server on port {port}.")
                print("Make sure the Java server for this floor is currently running!\n")
                continue
            
            floor_names = ["Basement", "Ground floor", "First floor"]
            print(f"\nSuccessfully connected to: {floor_names[loc_idx - 1]}")

            while True:
                print(f"\nManagement ({floor_names[loc_idx - 1].upper()})")
                mode = print_menu(["Select and control device", "Get Global Logs from this floor", "Change floor (Back)"])

                if mode == 3: break
                if mode == 2:
                    print(f"\nLogs ({floor_names[loc_idx - 1]})")
                    logs = directory.getGlobalLogs()
                    print("\n".join(logs) if logs else "No log entries on server.")
                    print("\n")
                    continue

                devices = directory.getActiveDevices(SmartHome.Floor.All)

                if not devices:
                    print("No devices registered on this server.")
                    continue

                print("\nChoose device:")
                dev_idx = print_menu(devices + ["Back to Floor Menu"])
                if dev_idx == len(devices) + 1: continue

                proxy = communicator.stringToProxy(f"{devices[dev_idx - 1]}:tcp -p {port}")

                print(f"\nControl panel ({devices[dev_idx - 1]})")
                
                if SmartHome.FridgePrx.checkedCast(proxy):
                    handle_fridge(proxy)
                elif SmartHome.PTZCameraPrx.checkedCast(proxy):
                    handle_camera(proxy)
                elif SmartHome.NoctovisionCameraPrx.checkedCast(proxy):
                    handle_noctovision_camera(proxy)
                elif SmartHome.CameraPrx.checkedCast(proxy):
                    handle_basic_camera(proxy)
                elif SmartHome.LightPrx.checkedCast(proxy):
                    handle_light(proxy)
                elif SmartHome.BlindsPrx.checkedCast(proxy):
                    handle_blinds(proxy)
                elif SmartHome.FurnacePrx.checkedCast(proxy):
                    handle_furnace(proxy)
                elif SmartHome.EVChargerPrx.checkedCast(proxy):
                    handle_evcharger(proxy)
                elif SmartHome.ThermostatPrx.checkedCast(proxy):
                    handle_thermostat(proxy)
                else:
                    handle_generic_device(proxy)

if __name__ == "__main__":
    main()