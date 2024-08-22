package net.xavil.hawklib.client.camera;

import net.minecraft.client.Minecraft;
import net.xavil.hawklib.math.NumericOps;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec3;

public class OrbitCamera {

	// how many meters there are for one abstract "camera unit". You can multiply
	// `focus`, or the result from `getPos()` by this factor to get those quantities
	// in meters.
	public final double metersPerUnit;

	// TODO: focus is in camera units, unlike everything else, which are in render
	// units!
	public final MotionSmoother<Vec3> focus = new MotionSmoother<>(0.6, NumericOps.VEC3);
	public final MotionSmoother<Double> yaw = new MotionSmoother<>(0.7, NumericOps.DOUBLE);
	public final MotionSmoother<Double> pitch = new MotionSmoother<>(0.7, NumericOps.DOUBLE);
	public final MotionSmoother<Double> scale = new MotionSmoother<>(0.5, NumericOps.DOUBLE);

	// projection properties
	public double fovDeg = 90;
	public double nearPlane = 0.01;
	public double farPlane = 1e6;

	public OrbitCamera(double metersPerUnit) {
		this.metersPerUnit = metersPerUnit;
	}

	public void tick(double dt) {
		this.focus.tick(dt);
		this.yaw.tick(dt);
		this.pitch.tick(dt);
		this.scale.tick(dt);
	}

	private Vec3 getPosRaw() {
		var backwards = applyRotationRaw(Vec3.YP).cross(applyRotationRaw(Vec3.XP));
		var backwardsTranslation = backwards.mul(this.scale.current);
		var cameraPos = this.focus.current.mul(1e12 / this.metersPerUnit).add(backwardsTranslation);
		return cameraPos;
	}

	private Vec3 applyRotationRaw(Vec3 vec) {
		return vec.rotateX(-this.pitch.current).rotateY(-this.yaw.current);
	}

	private Mat4 getProjectionMatrix(CameraConfig config) {
		var window = Minecraft.getInstance().getWindow();
		var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
		return Mat4.perspectiveProjection(Math.toRadians(this.fovDeg), aspectRatio,
				config.getNear(this.scale.current),
				config.getFar(this.scale.current));
	}

	private Quat getOrientation() {
		var xRotQuat = Quat.axisAngle(Vec3.XP, this.pitch.current);
		var yRotQuat = Quat.axisAngle(Vec3.YP, this.yaw.current + Math.PI);
		return xRotQuat.hamiltonProduct(yRotQuat);
	}

	public Cached cached(CameraConfig config) {
		return new Cached(this, config);
	}

	public static class Cached extends CachedCamera {
		public final Vec3 focus;
		public final double scale;

		public Cached(OrbitCamera camera, CameraConfig config) {
			this(camera.focus.current, camera.getPosRaw(),
					camera.getOrientation(), camera.scale.current,
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