package net.xavil.hawklib.client.camera;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
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
	public final MotionSmoother<Double> yaw = new MotionSmoother<>(0.6, NumericOps.DOUBLE);
	public final MotionSmoother<Double> pitch = new MotionSmoother<>(0.6, NumericOps.DOUBLE);
	public final MotionSmoother<Double> scale = new MotionSmoother<>(0.4, NumericOps.DOUBLE);

	private Vec3 velocity = Vec3.ZERO, prevVelocity = Vec3.ZERO;

	// projection properties
	public double fovDeg = 90;
	public double nearPlane = 0.01;
	public double farPlane = 1e6;

	public OrbitCamera(double metersPerUnit) {
		this.metersPerUnit = metersPerUnit;
	}

	public void tick() {
		// this works well enough, and gets us framerate independence, but it feels
		// slightly laggy to use, since we're always a tick behind the current target.
		// would be nice if there was a better way to do this.

		this.focus.tick();
		this.yaw.tick();
		this.pitch.tick();
		this.scale.tick();

		this.prevVelocity = this.velocity;
		this.velocity = getCurPos().sub(getPrevPos());
	}

	private Vec3 getPrevPos() {
		final var backwards = Vec3.YP.rotateX(-this.pitch.previous).rotateY(-this.yaw.previous)
				.cross(Vec3.XP.rotateX(-this.pitch.previous).rotateY(-this.yaw.previous));
		var backwardsTranslation = backwards.mul(this.scale.previous);
		return this.focus.previous.mul(1e12 / this.metersPerUnit).add(backwardsTranslation);
	}

	private Vec3 getCurPos() {
		final var backwards = Vec3.YP.rotateX(-this.pitch.current).rotateY(-this.yaw.current)
				.cross(Vec3.XP.rotateX(-this.pitch.current).rotateY(-this.yaw.current));
		var backwardsTranslation = backwards.mul(this.scale.current);
		return this.focus.current.mul(1e12 / this.metersPerUnit).add(backwardsTranslation);
	}

	private Vec3 getPosRaw(float partialTick) {
		var backwards = applyRotationRaw(Vec3.YP, partialTick).cross(applyRotationRaw(Vec3.XP, partialTick));
		var backwardsTranslation = backwards.mul(this.scale.get(partialTick));
		var cameraPos = this.focus.get(partialTick).mul(1e12 / this.metersPerUnit).add(backwardsTranslation);
		return cameraPos;
	}

	private Vec3 applyRotationRaw(Vec3 vec, float partialTick) {
		return vec.rotateX(-this.pitch.get(partialTick)).rotateY(-this.yaw.get(partialTick));
	}

	private Mat4 getProjectionMatrix(CameraConfig config, float partialTick) {
		var window = Minecraft.getInstance().getWindow();
		var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
		return Mat4.perspectiveProjection(Math.toRadians(this.fovDeg), aspectRatio,
				config.getNear(this.scale.get(partialTick)),
				config.getFar(this.scale.get(partialTick)));
	}

	private Quat getOrientationRaw(float partialTick) {
		var xRotQuat = Quat.axisAngle(Vec3.XP, this.pitch.get(partialTick));
		var yRotQuat = Quat.axisAngle(Vec3.YP, this.yaw.get(partialTick) + Math.PI);
		return xRotQuat.hamiltonProduct(yRotQuat);
	}

	private Quat getOrientation(float partialTick) {
		final var raw = getOrientationRaw(partialTick);

		final var vel = Vec3.lerp(partialTick, this.prevVelocity, this.velocity);
		if (vel.length() > 0.000000001) {
			final var vertYoinkAxis = vel.normalize().cross(Vec3.YP);
			final var yoinkStrength = Mth.clamp(vel.length() / (15 * this.scale.get(partialTick)), -Math.PI / 8,
					Math.PI / 8);
			return raw.hamiltonProduct(Quat.axisAngle(vertYoinkAxis, yoinkStrength));
		}

		return raw;
	}

	public Cached cached(CameraConfig config, float partialTick) {
		return new Cached(this, config, partialTick);
	}

	public static class Cached extends CachedCamera<OrbitCamera> {
		public final Vec3 focus;
		public final double scale;

		public Cached(OrbitCamera camera, CameraConfig config, float partialTick) {
			this(camera, camera.focus.get(partialTick), camera.getPosRaw(partialTick),
					camera.getOrientation(partialTick), camera.scale.get(partialTick),
					camera.metersPerUnit,
					camera.getProjectionMatrix(config, partialTick),
					config.getNear(camera.scale.get(partialTick)),
					config.getFar(camera.scale.get(partialTick)));
		}

		public Cached(OrbitCamera camera, Vec3 focus, Vec3 pos, Quat orientation, double scale,
				double metersPerUnit, Mat4 projectionMatrix,
				double nearPlane, double farPlane) {
			super(camera, pos, orientation, metersPerUnit, nearPlane, farPlane, projectionMatrix);
			this.focus = focus;
			this.scale = scale;
		}

	}
}