package net.xavil.hawklib.client.gl.texture;

import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.client.gl.GlFramebufferAttachment;
import net.xavil.hawklib.client.gl.GlLimits;
import net.xavil.hawklib.math.matrices.Vec3;

public final class GlTextureCubemap extends GlTexture {

	public static enum Face {
		// NOTE: enum ordinal is also the offset from +X, and the value you use to
		// upload a face with `glTextureSubImage3D`
		XP(GL45C.GL_TEXTURE_CUBE_MAP_POSITIVE_X, Vec3.XP, "+X"),
		XN(GL45C.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, Vec3.XN, "-X"),
		YP(GL45C.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, Vec3.YP, "+Y"),
		YN(GL45C.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, Vec3.YN, "-Y"),
		ZP(GL45C.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, Vec3.ZP, "+Z"),
		ZN(GL45C.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, Vec3.ZN, "-Z");

		public final int glId;
		public final Vec3 faceDir;
		public final String description;

		private Face(int glId, Vec3 faceDir, String description) {
			this.glId = glId;
			this.faceDir = faceDir;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}
	}

	public GlTextureCubemap() {
		super(Type.CUBE);
	}

	public Slice slice(int lodLevel, Face face) {
		return new Slice(this, SliceDimension.D2, lodLevel, face.ordinal(), 1, 0, 0, 0, this.size.width, this.size.height, 1);
	}

	public Slice slice(Face face) {
		return slice(0, face);
	}

	public void createStorage(GlTexture.Format textureFormat, int size) {
		GlLimits.validateTextureSize(size);
		if (this.storageAllocated)
			throw new IllegalStateException(String.format(
					"%s: Tried to update immutable texture storage",
					debugDescription()));

		GL45C.glTextureStorage2D(this.id, 1, textureFormat.id, size, size);
		this.textureFormat = textureFormat;
		this.size = new Size(size, size, 1, 1);
		this.storageAllocated = true;
	}

	public GlFramebufferAttachment.CubemapFace createFaceTarget(Face face) {
		return new GlFramebufferAttachment.CubemapFace(this, face);
	}

}
