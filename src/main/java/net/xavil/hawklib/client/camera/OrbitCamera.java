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

	// private Vec3 velocity = Vec3.ZERO, prevVelocity = Vec3.ZERO;
	// private final MotionSmoother<Vec3> yoinkVelocity = new MotionSmoother<>(0.3,
	// NumericOps.VEC3);

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
		// this.yoinkVelocity.tick(dt);

		// this.prevVelocity = this.velocity;
		// this.velocity = getCurPos().sub(getPrevPos());
		// this.yoinkVelocity.target = Vec3.ZERO;
	}

	// private Vec3 getPrevPos() {
	// final var backwards =
	// Vec3.YP.rotateX(-this.pitch.previous).rotateY(-this.yaw.previous)
	// .cross(Vec3.XP.rotateX(-this.pitch.previous).rotateY(-this.yaw.previous));
	// var backwardsTranslation = backwards.mul(this.scale.previous);
	// return this.focus.previous.mul(1e12 /
	// this.metersPerUnit).add(backwardsTranslation);
	// }

	// private Vec3 getCurPos() {
	// final var backwards =
	// Vec3.YP.rotateX(-this.pitch.current).rotateY(-this.yaw.current)
	// .cross(Vec3.XP.rotateX(-this.pitch.current).rotateY(-this.yaw.current));
	// var backwardsTranslation = backwards.mul(this.scale.current);
	// return this.focus.current.mul(1e12 /
	// this.metersPerUnit).add(backwardsTranslation);
	// }

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

	private Quat getOrientationRaw() {
		var xRotQuat = Quat.axisAngle(Vec3.XP, this.pitch.current);
		var yRotQuat = Quat.axisAngle(Vec3.YP, this.yaw.current + Math.PI);
		return xRotQuat.hamiltonProduct(yRotQuat);
	}

	private Quat getOrientation() {
		final var raw = getOrientationRaw();

		// final var vel1 = Vec3.lerp(partialTick, this.prevVelocity, this.velocity);
		// if (vel1.lengthSquared() > this.yoinkVelocity.current.lengthSquared()) {
		// 	// this.yoinkVelocity.current = Vec3.lerp(0.99, vel1,
		// 	// this.yoinkVelocity.current);
		// }

		// final var vel = this.yoinkVelocity.current;
		// if (vel.length() > 1e-8) {
		// 	final var vertYoinkAxis = vel.normalize().cross(Vec3.YP);
		// 	final var yoinkStrength = Mth.clamp(
		// 			vel.length() / (5 * this.scale.get(partialTick)),
		// 			-Math.PI / 8, Math.PI / 8);
		// 	return raw.hamiltonProduct(Quat.axisAngle(vertYoinkAxis, yoinkStrength));
		// }

		return raw;
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
			// super(camera, pos, orientation, metersPerUnit, nearPlane, farPlane,
			// projectionMatrix);
			this.focus = focus;
			this.scale = scale;
			load(pos, orientation, projectionMatrix, metersPerUnit);
		}

	}
}