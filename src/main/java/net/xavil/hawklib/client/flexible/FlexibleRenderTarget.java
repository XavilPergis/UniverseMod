package net.xavil.hawklib.client.flexible;

import java.util.OptionalInt;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.texture.GlTexture;

/**
 * An offscreen vanilla-compatible {@link RenderTarget} that supports
 * multisampling and custom formats.
 */
public class FlexibleRenderTarget extends RenderTarget {

	public record FramebufferFormat(boolean multisample, int colorFormat0, OptionalInt depthFormat) {
		public boolean isDefault() {
			return !this.multisample && this.colorFormat0 == GL32C.GL_RGBA8
					&& this.depthFormat.equals(OptionalInt.of(GL32C.GL_DEPTH_COMPONENT));
		}

		public GlTexture.Type textureTarget() {
			return this.multisample ? GlTexture.Type.D2_MS : GlTexture.Type.D2;
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

		final var target = this.format.textureTarget().id;

		this.viewWidth = width;
		this.viewHeight = height;
		this.width = width;
		this.height = height;
		this.frameBufferId = GlStateManager.glGenFramebuffers();
		this.colorTextureId = TextureUtil.generateTextureId();

		if (this.useDepth) {
			this.depthBufferId = TextureUtil.generateTextureId();
			GlManager.bindTexture(GlTexture.Type.from(target), this.depthBufferId);

			final var depthFormat = this.format.depthFormat.getAsInt();
			if (this.format.multisample) {
				GL32C.glTexImage2DMultisample(target, 4, depthFormat, this.width, this.height, true);
			} else {
				GlStateManager._texParameter(target, GL32C.GL_TEXTURE_MIN_FILTER, GL32C.GL_NEAREST);
				GlStateManager._texParameter(target, GL32C.GL_TEXTURE_MAG_FILTER, GL32C.GL_NEAREST);
				GlStateManager._texParameter(target, GL32C.GL_TEXTURE_COMPARE_MODE, GL32C.GL_NONE);
				GlStateManager._texParameter(target, GL32C.GL_TEXTURE_WRAP_S, GL32C.GL_CLAMP_TO_EDGE);
				GlStateManager._texParameter(target, GL32C.GL_TEXTURE_WRAP_T, GL32C.GL_CLAMP_TO_EDGE);
				GlStateManager._texImage2D(target, 0, depthFormat, this.width, this.height, 0,
						GL32C.GL_DEPTH_COMPONENT, GL32C.GL_FLOAT, null);
			}

		}

		this.filterMode = GL32C.GL_NEAREST;
		GlManager.bindTexture(GlTexture.Type.from(target), this.colorTextureId);
		if (this.format.multisample) {
			HawkLib.LOGGER.debug("created multisampled framebuffer");
			GL32C.glTexImage2DMultisample(target, 4, this.format.colorFormat0, this.width, this.height, true);
		} else {
			GlStateManager._texParameter(target, GL32C.GL_TEXTURE_MIN_FILTER, this.filterMode);
			GlStateManager._texParameter(target, GL32C.GL_TEXTURE_MAG_FILTER, this.filterMode);
			GlStateManager._texParameter(target, GL32C.GL_TEXTURE_WRAP_S, GL32C.GL_CLAMP_TO_EDGE);
			GlStateManager._texParameter(target, GL32C.GL_TEXTURE_WRAP_T, GL32C.GL_CLAMP_TO_EDGE);
			GlStateManager._texImage2D(target, 0, this.format.colorFormat0, this.width, this.height, 0,
					GL32C.GL_RGBA, GL32C.GL_UNSIGNED_BYTE, null);
		}
		GlManager.bindFramebuffer(GL32C.GL_FRAMEBUFFER, this.frameBufferId);
		GlStateManager._glFramebufferTexture2D(GL32C.GL_FRAMEBUFFER, GL32C.GL_COLOR_ATTACHMENT0, target,
				this.colorTextureId, 0);
		if (this.useDepth) {
			GlStateManager._glFramebufferTexture2D(GL32C.GL_FRAMEBUFFER, GL32C.GL_DEPTH_ATTACHMENT, target,
					this.depthBufferId, 0);
		}
		this.checkStatus();
		this.clear(clearError);
		this.unbindRead();
	}

	public void resolveTo(RenderTarget target) {
		GlManager.bindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, this.frameBufferId);
		GlManager.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, target.frameBufferId);
		GL32C.glBlitFramebuffer(
				0, 0, this.width, this.height,
				0, 0, target.width, target.height,
				GL32C.GL_COLOR_BUFFER_BIT, GL32C.GL_NEAREST);
		GlManager.bindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
	}

	public boolean isMultisampled() {
		return this.format.multisample;
	}

	@Override
	public void bindRead() {
		RenderSystem.assertOnRenderThread();
		GlManager.bindTexture(this.format.textureTarget(), this.colorTextureId);
	}

	@Override
	public void unbindRead() {
		RenderSystem.assertOnRenderThreadOrInit();
		GlManager.bindTexture(this.format.textureTarget(), 0);
	}

}
