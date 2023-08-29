package net.xavil.hawklib.client.gl.texture;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.platform.GlStateManager;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.GlFramebuffer;
import net.xavil.hawklib.client.gl.GlFramebufferAttachment;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.hawklib.math.matrices.Vec3;

public final class GlCubemapTexture extends GlTexture {

	public static enum Face {
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

	public GlCubemapTexture() {
		super(Type.CUBEMAP);
	}

	public void createStorage(GlTexture.Format textureFormat, int size) {
		this.textureFormat = textureFormat;
		this.size = new Size(size, size, 1, 1);
		for (int i = 0; i < 6; ++i) {
			final var binding = GL45C.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i;
			GlStateManager._texImage2D(
					binding, 0,
					this.textureFormat.id,
					size, size, 0,
					// format + type are bogus values, and are ignored as we aren't actually doing
					// any data transfer here.
					GL45C.GL_RGBA, GL45C.GL_UNSIGNED_BYTE, null);
		}
	}

	public GlFramebufferAttachment.CubemapFace createFaceTarget(Face face) {
		return new GlFramebufferAttachment.CubemapFace(this, face);
	}

	public void renderToCubemap(Consumer<Vec3> consumer) {
		createStorage(GlTexture.Format.RGBA32_FLOAT, 1024);

		try (final var disposer = Disposable.scope()) {
			final var framebuffer = disposer.attach(new GlFramebuffer(GlFragmentWrites.COLOR_ONLY, Vec2i.broadcast(1024)));
			framebuffer.enableAllColorAttachments();
			framebuffer.createDepthTarget(false, Format.DEPTH_UNSPECIFIED);
			for (final var face : Face.values()) {
				framebuffer.setColorTarget(GlFragmentWrites.COLOR, createFaceTarget(face));
				framebuffer.checkStatus();
				framebuffer.bind();
				framebuffer.clear();
				consumer.accept(face.faceDir);
			}
		}
	}

}
