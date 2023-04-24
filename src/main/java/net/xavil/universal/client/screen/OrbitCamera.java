package net.xavil.universal.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.xavil.util.math.Mat4;
import net.xavil.util.math.Quat;
import net.xavil.util.math.Vec3;

public class OrbitCamera {

	public static class MotionSmoother<T> {
		public final double smoothingFactor;
		private final LerpFunction<T> lerpFunction;
		private T target, current, previous;

		public interface LerpFunction<T> {
			T lerp(double t, T start, T end);

			static LerpFunction<Double> DOUBLES = (t, a, b) -> Mth.lerp(t, a, b);
			static LerpFunction<Vec3> VECTORS = (t, a, b) -> Vec3.from(Mth.lerp(t, a.x, b.x), Mth.lerp(t, a.y, b.y),
					Mth.lerp(t, a.z, b.z));
		}

		public MotionSmoother(double smoothingFactor, T initialValue, LerpFunction<T> lerpFunction) {
			this.smoothingFactor = smoothingFactor;
			this.lerpFunction = lerpFunction;
			set(initialValue);
		}

		public static MotionSmoother<Double> smoothDoubles(double smoothingFactor) {
			return new MotionSmoother<>(smoothingFactor, 0.0, LerpFunction.DOUBLES);
		}

		public static MotionSmoother<Vec3> smoothVectors(double smoothingFactor) {
			return new MotionSmoother<>(smoothingFactor, Vec3.ZERO, LerpFunction.VECTORS);
		}

		public void set(T value) {
			this.current = this.previous = this.target = value;
		}

		public void setTarget(T value) {
			this.target = value;
		}

		public T get(float partialTick) {
			return this.lerpFunction.lerp(partialTick, this.previous, this.current);
		}

		public T getTarget() {
			return this.target;
		}

		public void tick() {
			this.previous = this.current;
			this.current = this.lerpFunction.lerp(this.smoothingFactor, this.current, this.target);
		}

		@Override
		public String toString() {
			return "MotionSmoother[target=" + this.target + ", current=" + this.current + "]";
		}
	}

	// how many meters there are for one abstract "camera unit". You can multiply
	// `focus`, or the result from `getPos()` by this factor to get those quantities
	// in meters.
	public final double metersPerUnit;

	// The amount of units that
	public final double renderScaleFactor;

	public final MotionSmoother<Vec3> focus = MotionSmoother.smoothVectors(0.2);
	public final MotionSmoother<Double> yaw = MotionSmoother.smoothDoubles(0.3);
	public final MotionSmoother<Double> pitch = MotionSmoother.smoothDoubles(0.3);
	public final MotionSmoother<Double> scale = MotionSmoother.smoothDoubles(0.2);

	private Vec3 velocity = Vec3.ZERO, prevVelocity = Vec3.ZERO;

	// projection properties
	public double fovDeg = 90;
	public double nearPlane = 0.01;
	public double farPlane = 1e6;

	public OrbitCamera(double metersPerUnit, double renderScaleFactor) {
		this.metersPerUnit = metersPerUnit;
		this.renderScaleFactor = renderScaleFactor;
	}

	public void tick() {
		// this works well enough, and gets us framerate independence, but it feels
		// slightly laggy to use, since we're always a tick behind the current target.
		// would be nice if there was a better way to do this.

		// TODO: polish: tilt camera slightly when moving, as if it was attached to
		// something that was being yoinked
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
		return this.focus.previous.div(this.renderScaleFactor).add(backwardsTranslation);
	}

	private Vec3 getCurPos() {
		final var backwards = Vec3.YP.rotateX(-this.pitch.current).rotateY(-this.yaw.current)
				.cross(Vec3.XP.rotateX(-this.pitch.current).rotateY(-this.yaw.current));
		var backwardsTranslation = backwards.mul(this.scale.current);
		return this.focus.current.div(this.renderScaleFactor).add(backwardsTranslation);
	}

	private Vec3 getPosRaw(float partialTick) {
		var backwards = applyRotationRaw(Vec3.YP, partialTick).cross(applyRotationRaw(Vec3.XP, partialTick));
		var backwardsTranslation = backwards.mul(this.scale.get(partialTick));
		var cameraPos = this.focus.get(partialTick).div(this.renderScaleFactor).add(backwardsTranslation);
		return cameraPos;
	}

	private Vec3 applyRotationRaw(Vec3 vec, float partialTick) {
		return vec.rotateX(-this.pitch.get(partialTick)).rotateY(-this.yaw.get(partialTick));
	}

	private Mat4 getProjectionMatrix(float partialTick) {
		var window = Minecraft.getInstance().getWindow();
		var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
		return Mat4.perspectiveProjection(Math.toRadians(this.fovDeg), aspectRatio,
				this.scale.get(partialTick) * this.nearPlane,
				this.scale.get(partialTick) * this.farPlane);
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

	public Cached cached(float partialTick) {
		return new Cached(this, partialTick);
	}

	public static class Cached extends CachedCamera<OrbitCamera> {
		public final Vec3 focus;
		public final double scale;

		public Cached(OrbitCamera camera, float partialTick) {
			this(camera, camera.focus.get(partialTick), camera.getPosRaw(partialTick),
					camera.getOrientation(partialTick), camera.scale.get(partialTick), camera.metersPerUnit,
					camera.getProjectionMatrix(partialTick), camera.renderScaleFactor);
		}

		public Cached(OrbitCamera camera, Vec3 focus, Vec3 pos, Quat orientation, double scale,
				double metersPerUnit, Mat4 projectionMatrix, double renderScale) {
			super(camera, pos, orientation, metersPerUnit, renderScale, projectionMatrix);
			this.focus = focus;
			this.scale = scale;
		}

	}
}