package net.xavil.universal.client.flexible;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.xavil.util.math.matrices.Vec3;

public final class CubemapTexture extends Texture {

	private static final Minecraft CLIENT = Minecraft.getInstance();
	private static final Vec3[] FACE_DIRS = { Vec3.XP, Vec3.XN, Vec3.YP, Vec3.YN, Vec3.ZP, Vec3.ZN, };
	private int size;
	private int textureFormat;

	public CubemapTexture() {
		super(GL32.GL_TEXTURE_CUBE_MAP);
	}

	public void createStorage(int textureFormat, int size) {
		this.textureFormat = textureFormat;
		this.size = size;
		for (int i = 0; i < 6; ++i) {
			final var binding = GL32.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i;
			GlStateManager._texImage2D(
					binding, 0,
					this.textureFormat,
					this.size, this.size, 0,
					// format + type are bogus values, and are ignored as we aren't actually doing
					// any data transfer here.
					GL32.GL_RGBA, GL32.GL_UNSIGNED_BYTE, null);
		}
	}

	public void renderToCubemap(Consumer<Vec3> consumer) {
		createStorage(GL32.GL_RGBA32F, 1024);
		final var fbo = GlStateManager.glGenFramebuffers();
		final var depthRbo = GL32.glGenRenderbuffers();
		try {
			GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, fbo);
			GL32.glDrawBuffer(GL32.GL_COLOR_ATTACHMENT0);
			GL32.glBindRenderbuffer(GL32.GL_RENDERBUFFER, depthRbo);
			GL32.glRenderbufferStorage(GL32.GL_RENDERBUFFER, GL32.GL_DEPTH_COMPONENT24, this.size, this.size);
			GL32.glFramebufferRenderbuffer(GL32.GL_FRAMEBUFFER, GL32.GL_DEPTH_ATTACHMENT, GL32.GL_RENDERBUFFER,
					depthRbo);
			RenderSystem.viewport(0, 0, this.size, this.size);

			for (int i = 0; i < 6; ++i) {
				final var faceDir = FACE_DIRS[i];
				GlStateManager._glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0,
						GL32.GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, this.glId, 0);

				final var status = GlStateManager.glCheckFramebufferStatus(GL32.GL_FRAMEBUFFER);
				if(status != GL32.GL_FRAMEBUFFER_COMPLETE)
					System.out.printf("Status error: %08x\n", status);

				RenderSystem.clearColor(1, 0, 0, 1);
				RenderSystem.clear(GL32.GL_COLOR_BUFFER_BIT | GL32.GL_DEPTH_BUFFER_BIT, false);
				consumer.accept(faceDir);
			}
		} finally {
			GlStateManager._glDeleteFramebuffers(fbo);
			GL32.glDeleteRenderbuffers(depthRbo);
			RenderSystem.viewport(0, 0, CLIENT.getWindow().getWidth(), CLIENT.getWindow().getHeight());
		}
	}

}
