package net.xavil.universal.client.flexible;

import java.util.OptionalInt;

import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.xavil.universal.Mod;

// a render that uses a custom internal texture format
public class FlexibleRenderTarget extends RenderTarget {

	public record FramebufferFormat(boolean multisample, int colorFormat0, OptionalInt depthFormat) {
		public boolean isDefault() {
			return !this.multisample && this.colorFormat0 == GL31.GL_RGBA8
					&& this.depthFormat.equals(OptionalInt.of(GL31.GL_DEPTH_COMPONENT));
		}

		public int textureTarget() {
			return this.multisample ? GL32.GL_TEXTURE_2D_MULTISAMPLE : GL32.GL_TEXTURE_2D;
		}
	}

	public final FramebufferFormat format;

	public FlexibleRenderTarget(int width, int height, FramebufferFormat formatPair) {
		super(!formatPair.depthFormat.isEmpty());
		this.format = formatPair;
		RenderSystem.assertOnRenderThreadOrInit();
		this.resize(width, height, Minecraft.ON_OSX);
	}

	@Override
	public void createBuffers(int width, int height, boolean clearError) {
		RenderSystem.assertOnRenderThreadOrInit();
		final var maxTextureSize = RenderSystem.maxSupportedTextureSize();
		if (width <= 0 || width > maxTextureSize || height <= 0 || height > maxTextureSize) {
			throw new IllegalArgumentException(
					"Window " + width + "x" + height + " size out of bounds (max. size: " + maxTextureSize + ")");
		}

		final var target = this.format.textureTarget();

		this.viewWidth = width;
		this.viewHeight = height;
		this.width = width;
		this.height = height;
		this.frameBufferId = GlStateManager.glGenFramebuffers();
		this.colorTextureId = TextureUtil.generateTextureId();

		if (this.useDepth) {
			this.depthBufferId = TextureUtil.generateTextureId();
			bindTexture(target, this.depthBufferId);
			
			final var depthFormat = this.format.depthFormat.getAsInt();
			if (this.format.multisample) {
				GL32.glTexImage2DMultisample(target, 4, depthFormat, this.width, this.height, true);
			} else {
				GlStateManager._texParameter(target, GL32.GL_TEXTURE_MIN_FILTER, GL32.GL_NEAREST);
				GlStateManager._texParameter(target, GL32.GL_TEXTURE_MAG_FILTER, GL32.GL_NEAREST);
				GlStateManager._texParameter(target, GL32.GL_TEXTURE_COMPARE_MODE, GL32.GL_NONE);
				GlStateManager._texParameter(target, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
				GlStateManager._texParameter(target, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);
				GlStateManager._texImage2D(target, 0, depthFormat, this.width, this.height, 0,
						GL32.GL_DEPTH_COMPONENT, GL32.GL_FLOAT, null);
			}

		}

		this.filterMode = GL32.GL_NEAREST;
		bindTexture(target, this.colorTextureId);
		if (this.format.multisample) {
			Mod.LOGGER.debug("created multisampled framebuffer");
			GL32.glTexImage2DMultisample(target, 4, this.format.colorFormat0, this.width, this.height,
			true);
		} else {
			GlStateManager._texParameter(target, GL32.GL_TEXTURE_MIN_FILTER, this.filterMode);
			GlStateManager._texParameter(target, GL32.GL_TEXTURE_MAG_FILTER, this.filterMode);
			GlStateManager._texParameter(target, GL32.GL_TEXTURE_WRAP_S, GL32.GL_CLAMP_TO_EDGE);
			GlStateManager._texParameter(target, GL32.GL_TEXTURE_WRAP_T, GL32.GL_CLAMP_TO_EDGE);
			GlStateManager._texImage2D(target, 0, this.format.colorFormat0, this.width, this.height, 0,
					GL32.GL_RGBA, GL32.GL_UNSIGNED_BYTE, null);
		}
		GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.frameBufferId);
		GlStateManager._glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, target,
				this.colorTextureId, 0);
		if (this.useDepth) {
			GlStateManager._glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_DEPTH_ATTACHMENT, target,
					this.depthBufferId, 0);
		}
		this.checkStatus();
		this.clear(clearError);
		this.unbindRead();
	}

	public void resolveTo(RenderTarget target) {
		GlStateManager._glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, this.frameBufferId);
		GlStateManager._glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
		GL32.glBlitFramebuffer(
				0, 0, this.width, this.height,
				0, 0, target.width, target.height,
				GL32.GL_COLOR_BUFFER_BIT, GL32.GL_NEAREST);
		GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, 0);
	}

	public boolean isMultisampled() {
		return this.format.multisample;
	}

	@Override
	public void bindRead() {
		RenderSystem.assertOnRenderThread();
		bindTexture(this.format.textureTarget(), this.colorTextureId);
	}

	@Override
	public void unbindRead() {
		RenderSystem.assertOnRenderThreadOrInit();
		bindTexture(this.format.textureTarget(), 0);
	}

	public static void bindTexture(int target, int id) {
		RenderSystem.assertOnRenderThreadOrInit();
		if (id != GlStateManager.TEXTURES[GlStateManager.activeTexture].binding) {
			GlStateManager.TEXTURES[GlStateManager.activeTexture].binding = id;
			GL32.glBindTexture(target, id);
		}
	}

}
