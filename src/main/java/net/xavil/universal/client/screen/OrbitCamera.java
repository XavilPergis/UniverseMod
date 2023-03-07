package net.xavil.universal.client.screen;

import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
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

	public final MotionSmoother<Vec3> focus = MotionSmoother.smoothVectors(0.6);
	public final MotionSmoother<Double> yaw = MotionSmoother.smoothDoubles(0.6);
	public final MotionSmoother<Double> pitch = MotionSmoother.smoothDoubles(0.6);
	public final MotionSmoother<Double> scale = MotionSmoother.smoothDoubles(0.2);

	// projection properties
	public double fovDeg = 90;
	public double nearPlane = 0.01;
	public double farPlane = 10000;

	public OrbitCamera(double metersPerUnit, double renderScaleFactor) {
		this.metersPerUnit = metersPerUnit;
		this.renderScaleFactor = renderScaleFactor;
	}

	public void tick() {
		// this works well enough, and gets us framerate independence, but it feels
		// slightly laggy to use, since we're always a tick behind the current target.
		// would be nice if there was a better way to do this.
		this.focus.tick();
		this.yaw.tick();
		this.pitch.tick();
		this.scale.tick();
	}

	public Vec3 getPos(float partialTick) {
		var backwards = getUpVector(partialTick).cross(getRightVector(partialTick));
		var backwardsTranslation = backwards.mul(this.scale.get(partialTick));
		var cameraPos = this.focus.get(partialTick).div(this.renderScaleFactor).add(backwardsTranslation);
		return cameraPos;
	}

	public Vec3 getUpVector(float partialTick) {
		return Vec3.YP.rotateX(-this.pitch.get(partialTick)).rotateY(-this.yaw.get(partialTick));
	}

	public Vec3 getRightVector(float partialTick) {
		return Vec3.XP.rotateX(-this.pitch.get(partialTick)).rotateY(-this.yaw.get(partialTick));
	}

	public Matrix4f getProjectionMatrix() {
		var window = Minecraft.getInstance().getWindow();
		var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
		return Matrix4f.perspective((float) this.fovDeg, aspectRatio,
				(float) (this.scale.get(0) * this.nearPlane),
				(float) (this.scale.get(0) * this.farPlane));
	}

	public Quat getOrientation(float partialTick) {
		// var xRot = Vector3f.XP.rotation(this.pitch.get(partialTick).floatValue());
		// var yRot = Vector3f.YP.rotation(this.yaw.get(partialTick).floatValue() +
		// Mth.PI);
		// var res = new Quaternion(xRot);
		// res.mul(yRot);
		// return res;
		var xRot = Quat.axisAngle(Vec3.XP, this.pitch.get(partialTick));
		var yRot = Quat.axisAngle(Vec3.YP, this.yaw.get(partialTick) + Mth.PI);
		return xRot.hamiltonProduct(yRot);
	}

	public Cached cached(float partialTick) {
		return new Cached(this, partialTick);
	}

	public static class Cached extends CachedCamera<OrbitCamera> {
		public final Vec3 focus;
		public final double scale;

		public Cached(OrbitCamera camera, float partialTick) {
			this(camera,
					camera.getUpVector(partialTick),
					camera.getRightVector(partialTick),
					camera.focus.get(partialTick),
					camera.getPos(partialTick),
					camera.getOrientation(partialTick),
					camera.scale.get(partialTick),
					camera.metersPerUnit,
					camera.getProjectionMatrix());
		}

		public Cached(OrbitCamera camera, Vec3 up, Vec3 right, Vec3 focus, Vec3 pos, Quat orientation,
				double scale, double metersPerUnit, Matrix4f projectionMatrix) {
			super(camera, pos, up, right, orientation, metersPerUnit, projectionMatrix);
			this.focus = focus;
			this.scale = scale;
		}

	}
}