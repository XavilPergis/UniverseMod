package net.xavil.hawklib.client.camera;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.Ray;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public class CachedCamera {
	public final Vec3.Mutable pos = new Vec3.Mutable(), posTm = new Vec3.Mutable();
	public final Vec3.Mutable up = new Vec3.Mutable(), right = new Vec3.Mutable(), forward = new Vec3.Mutable();
	public final Vec3.Mutable down = new Vec3.Mutable(), left = new Vec3.Mutable(), backward = new Vec3.Mutable();
	public Quat orientation = Quat.IDENTITY;
	public double metersPerUnit = 1;

	public double nearPlane;
	public double farPlane;

	public final Mat4.Mutable viewMatrix = new Mat4.Mutable();
	public final Mat4.Mutable projectionMatrix = new Mat4.Mutable();
	public final Mat4.Mutable inverseProjectionMatrix = new Mat4.Mutable();
	public final Mat4.Mutable viewProjectionMatrix = new Mat4.Mutable();
	public final Mat4.Mutable inverseViewProjectionMatrix = new Mat4.Mutable();

	public void load(Vec3Access pos, Quat orientation, Mat4 projectionMatrix, double metersPerUnit) {
		Mat4.setRotationTranslation(this.viewMatrix, orientation, pos.neg());
		Mat4.invert(this.viewMatrix, this.viewMatrix);
		load(this.viewMatrix, projectionMatrix, metersPerUnit);
	}

	public void load(Mat4Access viewMatrix, Mat4Access projectionMatrix, double metersPerUnit) {
		Mat4.set(this.viewMatrix, viewMatrix);
		Mat4.set(this.projectionMatrix, projectionMatrix);

		Mat4.invert(this.inverseProjectionMatrix, this.projectionMatrix);
		Mat4.mul(this.viewProjectionMatrix, this.projectionMatrix, viewMatrix);
		Mat4.invert(this.inverseViewProjectionMatrix, this.viewProjectionMatrix);

		Mat4.storeTranslation(this.pos, viewMatrix);
		Vec3.mul(this.posTm, metersPerUnit / 1e12, this.pos);

		Mat4.basisX(this.right, this.viewMatrix);
		Mat4.basisY(this.up, this.viewMatrix);
		Mat4.basisZ(this.forward, this.viewMatrix);
		Vec3.neg(this.down, this.up);
		Vec3.neg(this.left, this.right);
		Vec3.neg(this.backward, this.forward);

		this.nearPlane = Mat4.mul(this.inverseProjectionMatrix, Vec3.ZN, 1.0).z;
		this.farPlane = Mat4.mul(this.inverseProjectionMatrix, Vec3.ZP, 1.0).z;

		this.orientation = Quat.fromAffineMatrix(this.viewMatrix);
		this.metersPerUnit = metersPerUnit;

	}

	public RenderMatricesSnapshot setupRenderMatrices() {
		final var snapshot = RenderMatricesSnapshot.capture();
		RenderSystem.setProjectionMatrix(this.projectionMatrix.asMinecraft());

		final var poseStack = RenderSystem.getModelViewStack();
		poseStack.setIdentity();

		poseStack.mulPose(this.orientation.asMinecraft());
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

	public record FrustumCorners(
			Vec3 nnn, Vec3 nnp,
			Vec3 npn, Vec3 npp,
			Vec3 pnn, Vec3 pnp,
			Vec3 ppn, Vec3 ppp) {
	}

	public FrustumCorners captureFrustumCornersWorld() {
		// @formatter:off
		final var nnn = new Vec3(-1, -1, -1).transformBy(this.inverseViewProjectionMatrix);
		final var nnp = new Vec3(-1, -1,  1).transformBy(this.inverseViewProjectionMatrix);
		final var npn = new Vec3(-1,  1, -1).transformBy(this.inverseViewProjectionMatrix);
		final var npp = new Vec3(-1,  1,  1).transformBy(this.inverseViewProjectionMatrix);
		final var pnn = new Vec3( 1, -1, -1).transformBy(this.inverseViewProjectionMatrix);
		final var pnp = new Vec3( 1, -1,  1).transformBy(this.inverseViewProjectionMatrix);
		final var ppn = new Vec3( 1,  1, -1).transformBy(this.inverseViewProjectionMatrix);
		final var ppp = new Vec3( 1,  1,  1).transformBy(this.inverseViewProjectionMatrix);
		// @formatter:on
		return new FrustumCorners(nnn, nnp, npn, npp, pnn, pnp, ppn, ppp);
	}

	public FrustumCorners captureFrustumCornersView() {
		// @formatter:off
		final var nnn = new Vec3(-1, -1, -1).transformBy(this.inverseProjectionMatrix);
		final var nnp = new Vec3(-1, -1,  1).transformBy(this.inverseProjectionMatrix);
		final var npn = new Vec3(-1,  1, -1).transformBy(this.inverseProjectionMatrix);
		final var npp = new Vec3(-1,  1,  1).transformBy(this.inverseProjectionMatrix);
		final var pnn = new Vec3( 1, -1, -1).transformBy(this.inverseProjectionMatrix);
		final var pnp = new Vec3( 1, -1,  1).transformBy(this.inverseProjectionMatrix);
		final var ppn = new Vec3( 1,  1, -1).transformBy(this.inverseProjectionMatrix);
		final var ppp = new Vec3( 1,  1,  1).transformBy(this.inverseProjectionMatrix);
		// @formatter:on
		return new FrustumCorners(nnn, nnp, npn, npp, pnn, pnp, ppn, ppp);
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
		return new Ray(this.pos.xyz(), dir);
	}

}
