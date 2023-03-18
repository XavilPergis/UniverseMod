package net.xavil.universal.client.flexible;

import java.util.OptionalInt;

import org.lwjgl.opengl.GL31;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;

// a render that uses a custom internal texture format
public class FlexibleRenderTarget extends RenderTarget {

	public record FormatPair(int colorFormat0, OptionalInt depthFormat) {
		public boolean isDefault() {
			return this.colorFormat0 == GL31.GL_RGBA8
					&& this.depthFormat.equals(OptionalInt.of(GL31.GL_DEPTH_COMPONENT));
		}
	}

	public final FormatPair format;

	public FlexibleRenderTarget(int width, int height, FormatPair formatPair) {
		super(!formatPair.depthFormat.isEmpty());
		this.format = formatPair;
		RenderSystem.assertOnRenderThreadOrInit();
		this.resize(width, height, Minecraft.ON_OSX);
	}

	@Override
	public void createBuffers(int width, int height, boolean clearError) {
		RenderSystem.assertOnRenderThreadOrInit();
		int i = RenderSystem.maxSupportedTextureSize();
		if (width <= 0 || width > i || height <= 0 || height > i) {
			throw new IllegalArgumentException(
					"Window " + width + "x" + height + " size out of bounds (max. size: " + i + ")");
		}
		this.viewWidth = width;
		this.viewHeight = height;
		this.width = width;
		this.height = height;
		this.frameBufferId = GlStateManager.glGenFramebuffers();
		this.colorTextureId = TextureUtil.generateTextureId();
		if (this.useDepth) {
			this.depthBufferId = TextureUtil.generateTextureId();
			GlStateManager._bindTexture(this.depthBufferId);
			GlStateManager._texParameter(3553, 10241, 9728);
			GlStateManager._texParameter(3553, 10240, 9728);
			GlStateManager._texParameter(3553, 34892, 0);
			GlStateManager._texParameter(3553, 10242, 33071);
			GlStateManager._texParameter(3553, 10243, 33071);
			GlStateManager._texImage2D(3553, 0, this.format.depthFormat.getAsInt(), this.width, this.height, 0, 6402,
					5126, null);
		}
		this.setFilterMode(9728);
		GlStateManager._bindTexture(this.colorTextureId);
		GlStateManager._texParameter(3553, 10242, 33071);
		GlStateManager._texParameter(3553, 10243, 33071);
		GlStateManager._texImage2D(3553, 0, this.format.colorFormat0, this.width, this.height, 0, 6408, 5121, null);
		GlStateManager._glBindFramebuffer(36160, this.frameBufferId);
		GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, this.colorTextureId, 0);
		if (this.useDepth) {
			GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, this.depthBufferId, 0);
		}
		this.checkStatus();
		this.clear(clearError);
		this.unbindRead();
	}

}
