package net.xavil.universal.client.camera;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.xavil.util.math.Mat4;
import net.xavil.util.math.Quat;
import net.xavil.util.math.Ray;
import net.xavil.util.math.Vec2;
import net.xavil.util.math.Vec3;

public class CachedCamera<T> {
	public final T camera;
	public final Vec3 pos, up, right, forward;
	public final Quat orientation;
	public final double metersPerUnit;

	public final Mat4 viewMatrix;
	public final Mat4 projectionMatrix;
	public final Mat4 inverseProjectionMatrix;
	public final Mat4 viewProjectionMatrix;
	public final Mat4 inverseViewProjectionMatrix;
	// public final Matrix4f viewMatrix;
	// public final Matrix4f projectionMatrix;
	// public final Matrix4f inverseProjectionMatrix;
	// public final Matrix4f viewProjectionMatrix;
	// public final Matrix4f inverseViewProjectionMatrix;

	public CachedCamera(T camera, Vec3 pos, Quat orientation, double metersPerUnit, Mat4 projectionMatrix) {
		this.camera = camera;
		this.pos = pos;
		this.up = orientation.inverse().transform(Vec3.XP);
		this.right = orientation.inverse().transform(Vec3.YP);
		// this.forward = orientation.transform(Vec3.ZN);
		this.orientation = orientation;
		this.metersPerUnit = metersPerUnit;
		this.forward = right.cross(up).normalize();

		this.projectionMatrix = projectionMatrix;
		this.inverseProjectionMatrix = this.projectionMatrix.inverse().unwrapOr(Mat4.IDENTITY);
		this.viewMatrix = Mat4.fromBases(up, right, forward.neg(), pos).inverse().unwrapOr(Mat4.IDENTITY);

		this.viewProjectionMatrix = this.viewMatrix.applyBefore(this.projectionMatrix);
		this.inverseViewProjectionMatrix = this.viewProjectionMatrix.inverse().unwrapOr(Mat4.IDENTITY);
	}

	public static Matrix4f matrixFromBases(Vec3 x, Vec3 y, Vec3 z, Vec3 pos) {
		final var mat = new Matrix4f();
		mat.m00 = (float) x.x;
		mat.m10 = (float) x.y;
		mat.m20 = (float) x.z;
		mat.m30 = 0.0f;
		mat.m01 = (float) y.x;
		mat.m11 = (float) y.y;
		mat.m21 = (float) y.z;
		mat.m31 = 0.0f;
		mat.m02 = (float) z.x;
		mat.m12 = (float) z.y;
		mat.m22 = (float) z.z;
		mat.m32 = 0.0f;
		mat.m03 = (float) pos.x;
		mat.m13 = (float) pos.y;
		mat.m23 = (float) pos.z;
		mat.m33 = 1.0f;
		return mat;
	}

	public RenderMatricesSnapshot setupRenderMatrices() {
		var snapshot = RenderMatricesSnapshot.capture();
		RenderSystem.setProjectionMatrix(this.projectionMatrix.asMinecraft());

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

	public boolean isAabbInFrustum(Vec3 min, Vec3 max) {
		// return true;
		final var boxPoints = new Vec3[8];
		int i = 0;
		boxPoints[i++] = worldToNdc(Vec3.from(min.x, min.y, min.z));
		boxPoints[i++] = worldToNdc(Vec3.from(min.x, min.y, max.z));
		boxPoints[i++] = worldToNdc(Vec3.from(min.x, max.y, min.z));
		boxPoints[i++] = worldToNdc(Vec3.from(min.x, max.y, max.z));
		boxPoints[i++] = worldToNdc(Vec3.from(max.x, min.y, min.z));
		boxPoints[i++] = worldToNdc(Vec3.from(max.x, min.y, max.z));
		boxPoints[i++] = worldToNdc(Vec3.from(max.x, max.y, min.z));
		boxPoints[i++] = worldToNdc(Vec3.from(max.x, max.y, max.z));

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

		var dir = Vec3.from(x, y, -1);
		dir = dir.transformBy(this.inverseProjectionMatrix);
		dir = this.orientation.inverse().transform(dir);
		return new Ray(this.pos, dir);
	}

	public static <T> CachedCamera<T> create(T camera, Vec3 pos, Quat orientation, double metersPerUnit,
			Mat4 projectionMatrix) {
		return new CachedCamera<T>(camera, pos, orientation, metersPerUnit, projectionMatrix);
	}

	public static Quat orientationFromMinecraftCamera(Camera camera) {
		final var px = Vec3.fromMinecraft(camera.getLeftVector()).neg();
		final var py = Vec3.fromMinecraft(camera.getUpVector());
		final var pz = px.cross(py);
		return Quat.fromOrthonormalBasis(px, py, pz);
	}

}
