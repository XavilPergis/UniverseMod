package net.xavil.hawklib.client.camera;

import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public abstract class HawkCamera {

    // how many meters there are for one abstract "camera unit". You can multiply
    // `focus`, or the result from `getPos()` by this factor to get those quantities
    // in meters.
    public final double metersPerUnit;

    // projection properties
    public double fovDeg = 90;
    public double nearPlane = 0.01;
    public double farPlane = 1e6;

    public HawkCamera(double metersPerUnit) {
        this.metersPerUnit = metersPerUnit;
    }

    public void tick(double dt) {
    }

    public abstract Vec3Access pos();

    public abstract Quat orientation();

    public Vec3Access focus() {
        return pos();
    }

    public abstract CachedCamera cached(CameraConfig config);

    // private Mat4 getProjectionMatrix(CameraConfig config) {
    //     final var window = Minecraft.getInstance().getWindow();
    //     final var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
    //     return Mat4.perspectiveProjectionReverseZ(Math.toRadians(this.fovDeg), aspectRatio,
    //             config.getNear(this.scale.current));
    // }

}
