package net.xavil.universal.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.xavil.util.math.Quat;
import net.xavil.util.math.Vec3;

public class CachedCamera<T> {
	public final T camera;
	public final Vec3 pos, up, right, forward;
	public final Quat orientation;
	public final double metersPerUnit;

	public final Matrix4f viewMatrix;
	public final Matrix4f projectionMatrix;
	public final Matrix4f viewProjectionMatrix;

	public CachedCamera(T camera, Vec3 pos, Vec3 up, Vec3 right, Quat orientation, double metersPerUnit,
			Matrix4f projectionMatrix) {
		this.camera = camera;
		this.pos = pos;
		this.up = up;
		this.right = right;
		this.orientation = orientation;
		this.metersPerUnit = metersPerUnit;
		this.projectionMatrix = projectionMatrix;
		this.forward = right.cross(up);

		this.viewMatrix = new Matrix4f();
		this.viewMatrix.m00 = (float) right.x;
		this.viewMatrix.m10 = (float) right.y;
		this.viewMatrix.m20 = (float) right.z;
		this.viewMatrix.m01 = (float) up.x;
		this.viewMatrix.m11 = (float) up.y;
		this.viewMatrix.m21 = (float) up.z;
		this.viewMatrix.m02 = (float) forward.x;
		this.viewMatrix.m12 = (float) forward.y;
		this.viewMatrix.m22 = (float) forward.z;
		this.viewMatrix.multiplyWithTranslation((float) pos.x, (float) pos.y, (float) pos.z);

		this.viewProjectionMatrix = this.projectionMatrix.copy();
		this.viewProjectionMatrix.multiply(this.viewMatrix);
	}

	public RenderMatricesSnapshot setupRenderMatrices() {
		var snapshot = RenderMatricesSnapshot.capture();
		RenderSystem.setProjectionMatrix(this.projectionMatrix);

		var poseStack = RenderSystem.getModelViewStack();
		poseStack.setIdentity();

		poseStack.mulPose(this.orientation.toMinecraft());
		Matrix3f inverseViewRotationMatrix = poseStack.last().normal().copy();
		if (inverseViewRotationMatrix.invert()) {
			RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
		}

		// it would be very nice for simplicity's sake to apply the camera's translation
		// here, but unfortunately, that can cause stuff to melt into floating point
		// soup at the scales we're dealing with. So instead, vertices are specified in
		// a space where the camera is at the origin (like in view space), but the
		// camer'as rotation is not taken into account (like in world space).
		// Essentially a weird hybrid between the two.
		RenderSystem.applyModelViewMatrix();
		return snapshot;
	}

	public Vec3 toCameraSpace(Vec3 posWorld) {
		return posWorld.sub(this.pos);
	}

	public Vec3 projectWorldSpace(Vec3 posWorld) {
		return posWorld.transformBy(this.viewProjectionMatrix);
	}

	public static <T> CachedCamera<T> create(T camera, Vec3 pos, float xRot, float yRot, Quat prependedRotation, Matrix4f projectionMatrix) {
		var xRotQuat = Quat.axisAngle(Vec3.XP, Math.toRadians(xRot));
		var yRotQuat = Quat.axisAngle(Vec3.YP, Math.toRadians(yRot) + Math.PI);
		var quat = xRotQuat.hamiltonProduct(yRotQuat).hamiltonProduct(prependedRotation);

		var up = quat.transform(Vec3.YP);
		var right = quat.transform(Vec3.XP);

		return new CachedCamera<T>(camera, pos, up, right, quat, 1, projectionMatrix);
	}

}
