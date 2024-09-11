package net.xavil.hawklib.client.camera;

import net.minecraft.client.Minecraft;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public final class FpCamera extends HawkCamera {

    public final MotionSmoother<Vec3> pos = new MotionSmoother<>(0.4, MotionSmoother.Interpolator.VEC3, Vec3.ZERO);
    // Tm/tick i think????
    public final MotionSmoother<Double> speed = new MotionSmoother<>(0.4, MotionSmoother.Interpolator.DOUBLE, 1.0);
    public Quat orientation = Quat.IDENTITY;

    public FpCamera(double metersPerUnit) {
        super(metersPerUnit);
    }

    private Mat4 getProjectionMatrix(CameraConfig config) {
        var window = Minecraft.getInstance().getWindow();
        var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
        // return Mat4.perspectiveProjectionReverseZ(Math.toRadians(this.fovDeg), aspectRatio,
        //         config.getNear(this.speed.current));
        return Mat4.perspectiveProjection(Math.toRadians(this.fovDeg), aspectRatio,
                config.getNear(this.speed.current),
                config.getFar(this.speed.current));
    }

    @Override
    public void tick(double dt) {
        this.pos.tick(dt);
        this.speed.tick(dt);
    }

    @Override
    public Vec3Access pos() {
        return this.pos.current;
    }

    @Override
    public Quat orientation() {
        return this.orientation;
    }

    @Override
    public CachedCamera cached(CameraConfig config) {
        final var cached = new CachedCamera();
        final var projMat = getProjectionMatrix(config);
        cached.load(this.pos.current, this.orientation, projMat, this.metersPerUnit);
        return cached;
    }

}
