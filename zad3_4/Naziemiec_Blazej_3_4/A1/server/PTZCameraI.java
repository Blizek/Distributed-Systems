import SmartHome.*;
import com.zeroc.Ice.Current;
import java.util.*;

public class PTZCameraI extends DeviceI implements PTZCamera {
    private float pan = 0, tilt = 0, zoom = 1.0f;
    private boolean isPatrolling = false;
    private Random rand = new Random();

    public PTZCameraI(String id, Floor loc, HomeDirectoryI dir) {
        super(id, loc, "Yellow2137", dir);
    }

    @Override
    public void setPTZ(PTZPosition pos, Current c) throws ValueOutOfRange {
        if (!checkOn())
            return;
        if (pos.pan < -45 || pos.pan > 45)
            throw new ValueOutOfRange(-45, 45);
        if (pos.tilt < -30 || pos.tilt > 30)
            throw new ValueOutOfRange(-30, 30);
        this.pan = pos.pan;
        this.tilt = pos.tilt;
        log(String.format("PTZ set: P:%.1f T:%.1f", pan, tilt));
    }

    @Override
    public void zoomByPercentage(float p, Current c) throws ValueOutOfRange {
        if (!checkOn())
            return;
        float newZoom = this.zoom * (1 + p / 100);
        if (newZoom < 0.1f || newZoom > 100.0f) {
            throw new ValueOutOfRange(0.1f, 100.0f);
        }
        this.zoom = newZoom;
        log("Zoom changed to: " + zoom);
    }

    @Override
    public void resetZoom(Current c) {
        if (!checkOn())
            return;
        this.zoom = 1.0f;
        log("Zoom reset");
    }

    @Override
    public void backToPosition(Current c) {
        if (!checkOn())
            return;
        this.pan = 0;
        this.tilt = 0;
        log("Back to position");
    }

    @Override
    public boolean isMoving(Current c) {
        return isPatrolling;
    }

    @Override
    public void triggerVideoDump(Current c) {
        if (!checkOn())
            return;
        log("Generated dump to file DUMP_" + System.currentTimeMillis() + ".mp4");
    }

    @Override
    public void startPatrol(Current c) {
        if (!checkOn())
            return;
        isPatrolling = true;
        log("Patrol active");
    }

    @Override
    public void stopPatrol(Current c) {
        if (!checkOn())
            return;
        isPatrolling = false;
        log("Patrol stopped");
    }

    @Override
    protected Map<String, String> getExtraInfo() {
        float shift = 0.0f;
        if (isPatrolling) {
            shift = (rand.nextFloat() - 0.5f) * 1.2f;
        }
        return Map.of(
                "current_pan", String.format("%.2f", pan + shift),
                "current_tilt", String.format("%.2f", tilt),
                "zoom_level", String.format("%.2f", zoom),
                "is_patrolling", String.valueOf(isPatrolling));
    }
}