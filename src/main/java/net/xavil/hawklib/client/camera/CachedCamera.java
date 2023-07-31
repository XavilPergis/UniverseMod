package net.xavil.hawklib.client.camera;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Camera;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.Ray;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;

public class CachedCamera<T> {
	public final T uncached;

	public final Vec3 pos, posTm;
	public final Vec3 up, right, forward;
	public final Vec3 down, left, backward;
	public final Quat orientation;
	public final double metersPerUnit;

	public final double nearPlane;
	public final double farPlane;

	public final Mat4 viewMatrix;
	public final Mat4 projectionMatrix;
	public final Mat4 inverseProjectionMatrix;
	public final Mat4 viewProjectionMatrix;
	public final Mat4 inverseViewProjectionMatrix;

	public CachedCamera(T camera, Vec3 pos, Quat orientation, double metersPerUnit, double nearPlane, double farPlane,
			Mat4 projectionMatrix) {
		this.uncached = camera;
		this.pos = pos;
		this.posTm = pos.mul(metersPerUnit / 1e12);
		this.up = orientation.inverse().transform(Vec3.XP);
		this.down = this.up.neg();
		this.right = orientation.inverse().transform(Vec3.YP);
		this.left = this.right.neg();
		this.orientation = orientation;
		this.metersPerUnit = metersPerUnit;
		this.forward = right.cross(up).normalize();
		this.backward = this.forward.neg();

		this.nearPlane = nearPlane;
		this.farPlane = farPlane;

		this.projectionMatrix = projectionMatrix;
		this.inverseProjectionMatrix = this.projectionMatrix.inverse().unwrapOr(Mat4.IDENTITY);
		this.viewMatrix = Mat4.fromBases(up, right, forward.neg(), pos).inverse().unwrapOr(Mat4.IDENTITY);

		this.viewProjectionMatrix = this.viewMatrix.appendTransform(this.projectionMatrix);
		this.inverseViewProjectionMatrix = this.viewProjectionMatrix.inverse().unwrapOr(Mat4.IDENTITY);
	}

	public CachedCamera(T camera, Mat4 viewMatrix, Mat4 projectionMatrix, double metersPerUnit, double nearPlane,
			double farPlane) {
		this.uncached = camera;

		this.viewMatrix = viewMatrix;

		this.projectionMatrix = projectionMatrix;
		this.inverseProjectionMatrix = this.projectionMatrix.inverse().unwrapOr(Mat4.IDENTITY);
		this.viewProjectionMatrix = this.viewMatrix.appendTransform(this.projectionMatrix);
		this.inverseViewProjectionMatrix = this.viewProjectionMatrix.inverse().unwrapOr(Mat4.IDENTITY);

		this.pos = viewMatrix.translation();
		this.posTm = pos.mul(metersPerUnit / 1e12);
		this.up = this.viewMatrix.basisX();
		this.down = this.up.neg();
		this.right = this.viewMatrix.basisY();
		this.left = this.right.neg();
		this.forward = this.viewMatrix.basisZ();
		this.backward = this.forward.neg();

		this.nearPlane = nearPlane;
		this.farPlane = farPlane;

		this.orientation = Quat.fromAffineMatrix(this.viewMatrix);
		this.metersPerUnit = metersPerUnit;
	}

	public RenderMatricesSnapshot setupRenderMatrices() {
		final var snapshot = RenderMatricesSnapshot.capture();
		RenderSystem.setProjectionMatrix(this.projectionMatrix.asMinecraft());

		final var poseStack = RenderSystem.getModelViewStack();
		poseStack.setIdentity();

		poseStack.mulPose(this.orientation.toMinecraft());
		final var inverseViewRotationMatrix = poseStack.last().normal().copy();
		if (inverseViewRotationMatrix.invert()) {
			RenderSystem.setInverseViewRotationMatrix(inverseViewRotationMatrix);
		}

		// it would be very nice for simplicity's sake to apply the camera's translation
		// here, but unfortunately, that can cause stuff to melt into floating point
		// soup at the scales we're dealing with. So instead, vertices are specified in
		// a space where the camera is at the origin (like in view space), but the
		// camera's rotation is not taken into account (like in world space).
		// Essentially a weird hybrid between the two. This is the same behavior as the
		// vanilla camera.
		RenderSystem.applyModelViewMatrix();
		return snapshot;
	}

	public boolean isAabbInFrustum(Vec3 min, Vec3 max) {
		// return true;
		final var boxPoints = new Vec3[8];
		int i = 0;
		boxPoints[i++] = worldToNdc(new Vec3(min.x, min.y, min.z));
		boxPoints[i++] = worldToNdc(new Vec3(min.x, min.y, max.z));
		boxPoints[i++] = worldToNdc(new Vec3(min.x, max.y, min.z));
		boxPoints[i++] = worldToNdc(new Vec3(min.x, max.y, max.z));
		boxPoints[i++] = worldToNdc(new Vec3(max.x, min.y, min.z));
		boxPoints[i++] = worldToNdc(new Vec3(max.x, min.y, max.z));
		boxPoints[i++] = worldToNdc(new Vec3(max.x, max.y, min.z));
		boxPoints[i++] = worldToNdc(new Vec3(max.x, max.y, max.z));

		boolean nx = true, px = true;
		boolean ny = true, py = true;
		boolean nz = true, pz = true;
		for (final var boxPoint : boxPoints) {
			nx &= boxPoint.x <= -1;
			px &= boxPoint.x >= 1;
			ny &= boxPoint.y <= -1;
			py &= boxPoint.y >= 1;
			nz &= boxPoint.z <= -1;
			pz &= boxPoint.z >= 1;
		}

		return !(nx || px || ny || py || nz || pz);
	}

	public Vec3 toCameraSpace(Vec3 posWorld) {
		return posWorld.sub(this.pos);
	}

	public Vec3 worldToNdc(Vec3 posWorld) {
		return posWorld.transformBy(this.viewProjectionMatrix);
	}

	public Vec3 ndcToWorld(Vec3 posNdc) {
		return posNdc.transformBy(this.inverseViewProjectionMatrix);
	}

	public Ray rayForPicking(Window window, Vec2 mousePos) {
		final var x = (2.0 * mousePos.x) / window.getGuiScaledWidth() - 1.0;
		final var y = 1.0 - (2.0 * mousePos.y) / window.getGuiScaledHeight();

		var dir = new Vec3(x, y, -1);
		dir = dir.transformBy(this.inverseProjectionMatrix);
		dir = this.orientation.inverse().transform(dir);
		return new Ray(this.pos, dir);
	}

	public static Quat orientationFromMinecraftCamera(Camera camera) {
		final var px = Vec3.from(camera.getLeftVector()).neg();
		final var py = Vec3.from(camera.getUpVector());
		final var pz = px.cross(py);
		return Quat.fromOrthonormalBasis(px, py, pz);
	}

}
