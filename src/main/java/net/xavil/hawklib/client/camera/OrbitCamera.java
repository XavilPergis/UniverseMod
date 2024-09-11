package net.xavil.hawklib.client.camera;

import net.minecraft.client.Minecraft;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec3;

public class OrbitCamera extends HawkCamera {

	// TODO: focus is in camera units, unlike everything else, which are in render
	// units!
	public final MotionSmoother<Vec3> focus = new MotionSmoother<>(0.4, MotionSmoother.Interpolator.VEC3, Vec3.ZERO);
	public final MotionSmoother<Double> yaw = new MotionSmoother<>(0.4, MotionSmoother.Interpolator.DOUBLE, 0.0);
	public final MotionSmoother<Double> pitch = new MotionSmoother<>(0.4, MotionSmoother.Interpolator.DOUBLE, 0.0);
	public final MotionSmoother<Double> scale = new MotionSmoother<>(0.4, MotionSmoother.Interpolator.DOUBLE, 1.0);

	public OrbitCamera(double metersPerUnit) {
		super(metersPerUnit);
	}

	@Override
	public void tick(double dt) {
		this.focus.tick(dt);
		this.yaw.tick(dt);
		this.pitch.tick(dt);
		this.scale.tick(dt);
	}

	private Vec3 applyRotationRaw(Vec3 vec) {
		return vec.rotateX(-this.pitch.current).rotateY(-this.yaw.current);
	}

	@Override
	public Vec3 pos() {
		var backwards = applyRotationRaw(Vec3.YP).cross(applyRotationRaw(Vec3.XP));
		var backwardsTranslation = backwards.mul(this.scale.current);
		var cameraPos = this.focus.current.mul(1e12 / this.metersPerUnit).add(backwardsTranslation);
		return cameraPos;
	}

	@Override
	public Quat orientation() {
		var xRotQuat = Quat.axisAngle(Vec3.XP, this.pitch.current);
		var yRotQuat = Quat.axisAngle(Vec3.YP, this.yaw.current + Math.PI);
		return xRotQuat.mul(yRotQuat);
	}

	@Override
	public Cached cached(CameraConfig config) {
		return new Cached(this, config);
	}

	private Mat4 getProjectionMatrix(CameraConfig config) {
		var window = Minecraft.getInstance().getWindow();
		var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
		// return Mat4.perspectiveProjectionReverseZ(Math.toRadians(this.fovDeg),
		// aspectRatio,
		// config.getNear(this.scale.current));
		return Mat4.perspectiveProjection(Math.toRadians(this.fovDeg), aspectRatio,
				config.getNear(this.scale.current),
				config.getFar(this.scale.current));
	}

	public static class Cached extends CachedCamera {
		public final Vec3 focus;
		public final double scale;

		public Cached(OrbitCamera camera, CameraConfig config) {
			this(camera.focus.current, camera.pos(),
					camera.orientation(), camera.scale.current,
					camera.metersPerUnit,
					camera.getProjectionMatrix(config));
		}

		public Cached(Vec3 focus, Vec3 pos, Quat orientation, double scale,
				double metersPerUnit, Mat4 projectionMatrix) {
			this.focus = focus;
			this.scale = scale;
			load(pos, orientation, projectionMatrix, metersPerUnit);
		}

	}
}