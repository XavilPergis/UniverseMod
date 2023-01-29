package net.xavil.universal.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class OrbitCamera {

	public static class MotionSmoother<T> {
		public final double smoothingFactor;
		private final LerpFunction<T> lerpFunction;
		private T target, current, previous;

		public interface LerpFunction<T> {
			T lerp(double t, T start, T end);

			static LerpFunction<Double> DOUBLES = (t, a, b) -> Mth.lerp(t, a, b);
			static LerpFunction<Vec3> VECTORS = (t, a, b) -> new Vec3(Mth.lerp(t, a.x, b.x), Mth.lerp(t, a.y, b.y),
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

	public final double renderUnitFactor;

	public final MotionSmoother<Vec3> focus = MotionSmoother.smoothVectors(0.6);
	public final MotionSmoother<Double> yaw = MotionSmoother.smoothDoubles(0.6);
	public final MotionSmoother<Double> pitch = MotionSmoother.smoothDoubles(0.6);
	public final MotionSmoother<Double> scale = MotionSmoother.smoothDoubles(0.2);

	// projection properties
	public double fovDeg = 90;
	public double nearPlane = 0.1;
	public double farPlane = 10000;

	public OrbitCamera(double renderUnitFactor) {
		this.renderUnitFactor = renderUnitFactor;
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
		var backwardsTranslation = backwards.scale(this.scale.get(partialTick));
		var cameraPos = this.focus.get(partialTick).scale(1 / this.renderUnitFactor).add(backwardsTranslation);
		return cameraPos;
	}

	public Vec3 getUpVector(float partialTick) {
		return new Vec3(0, 1, 0).xRot((float) -this.pitch.get(partialTick)).yRot((float) -this.yaw.get(partialTick));
	}

	public Vec3 getRightVector(float partialTick) {
		return new Vec3(1, 0, 0).xRot((float) -this.pitch.get(partialTick)).yRot((float) -this.yaw.get(partialTick));
	}

	public Matrix4f getProjectionMatrix() {
		var window = Minecraft.getInstance().getWindow();
		var aspectRatio = (float) window.getWidth() / (float) window.getHeight();
		return Matrix4f.perspective((float) this.fovDeg, aspectRatio, (float) this.nearPlane,
				(float) this.farPlane);
	}

	public Quaternion getOrientation(float partialTick) {
		var xRot = Vector3f.XP.rotation(this.pitch.get(partialTick).floatValue());
		var yRot = Vector3f.YP.rotation(this.yaw.get(partialTick).floatValue() + Mth.PI);
		var res = new Quaternion(xRot);
		res.mul(yRot);
		return res;
	}

	public RenderMatricesSnapshot setupRenderMatrices(float partialTick) {
		var snapshot = RenderMatricesSnapshot.capture();
		RenderSystem.setProjectionMatrix(getProjectionMatrix());

		var poseStack = RenderSystem.getModelViewStack();
		poseStack.setIdentity();

		var rotation = getOrientation(partialTick);
		poseStack.mulPose(rotation);
		Matrix3f inverseViewRotationMatrix = poseStack.last().normal().copy();
		if (inverseViewRotationMatrix.invert()) {
			RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
		}

		var cameraPos = getPos(partialTick);
		poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
		RenderSystem.applyModelViewMatrix();
		return snapshot;
	}
}