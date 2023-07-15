package net.xavil.hawklib.client.camera;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;

public class RenderMatricesSnapshot {

	private Matrix4f prevModelViewMatrix = null;
	private Matrix3f prevModelViewNormalMatrix = null;
	private Matrix4f prevProjectionMatrix = null;
	private Matrix3f prevInverseViewRotationMatrix = null;

	/**
	 * This method captures the modelview matrix, inverse rotation matrix, and
	 * projection matrix from {@link RenderSystem}.
	 * 
	 * @return The current render system transformation matrices
	 */
	public static RenderMatricesSnapshot capture() {
		var mats = new RenderMatricesSnapshot();
		mats.prevInverseViewRotationMatrix = RenderSystem.getInverseViewRotationMatrix().copy();
		mats.prevProjectionMatrix = RenderSystem.getProjectionMatrix().copy();
		mats.prevModelViewMatrix = RenderSystem.getModelViewStack().last().pose().copy();
		mats.prevModelViewNormalMatrix = RenderSystem.getModelViewStack().last().normal().copy();
		return mats;
	}

	/**
	 * This method copies this transformation matrix snapshot to the actual
	 * {@link RenderSystem} matrices.
	 */
	public void restore() {
		RenderSystem.getModelViewStack().last().pose().load(this.prevModelViewMatrix);
		RenderSystem.getModelViewStack().last().normal().load(this.prevModelViewNormalMatrix);
		RenderSystem.applyModelViewMatrix();
		RenderSystem.setProjectionMatrix(this.prevProjectionMatrix);
		RenderSystem.setInverseViewRotationMatrix(this.prevInverseViewRotationMatrix);
	}

}