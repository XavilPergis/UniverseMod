package net.xavil.ultraviolet.client.gl.texture;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.xavil.ultraviolet.client.flexible.FlexibleRenderTarget;
import net.xavil.ultraviolet.client.gl.GlLimits;

public final class GlTexture2d extends GlTexture {

	public GlTexture2d(boolean multisampled, int glId, boolean owned) {
		super(multisampled ? Type.D2_MS : Type.D2, glId, owned);
	}

	public GlTexture2d(boolean multisampled) {
		super(multisampled ? Type.D2_MS : Type.D2);
	}

	public void resize(int width, int height) {
		if (this.textureFormat != null) {
			throw new IllegalStateException(
					debugDescription() + "Cannot resize a texture whose storage has not been previously allocated!");
		}
		createStorage(this.textureFormat, width, height);
	}

	@Override
	public GlTexture2d asTexture2d() {
		return this;
	}

	public void createStorage(GlTexture.Format textureFormat, int width, int height) {
		GlLimits.validateTextureSize(width, height);
		if (this.textureFormat == textureFormat && this.size.width == width && this.size.height == height)
			return;

		bind();
		switch (this.type) {
			case D2 -> GlStateManager._texImage2D(
					this.type.id, 0,
					textureFormat.id,
					width, height, 0,
					// format + type are bogus values, and are ignored as we aren't actually doing
					// any data transfer here.
					GL32C.GL_RGBA, GL32C.GL_UNSIGNED_BYTE, null);
			case D2_MS -> GL32C.glTexImage2DMultisample(
					this.type.id, 4,
					textureFormat.id,
					width, height, true);
			default -> throw new IllegalStateException(
					debugDescription() + "Invalid type for 2d texture: " + this.type.description);
		}
		this.textureFormat = textureFormat;
		this.size = new Size(width, height, 1, 1);
		this.storageAllocated = true;
	}

	public static GlTexture2d importFromRenderTargetColor(RenderTarget target) {
		if (target.getColorTextureId() == -1)
			return null;
		final var ms = target instanceof FlexibleRenderTarget flex && flex.isMultisampled();
		return new GlTexture2d(ms, target.getColorTextureId(), false);
	}

	public static GlTexture2d importFromRenderTargetDepth(RenderTarget target) {
		if (target.getDepthTextureId() == -1)
			return null;
		final var ms = target instanceof FlexibleRenderTarget flex && flex.isMultisampled();
		return new GlTexture2d(ms, target.getDepthTextureId(), false);
	}

	public static GlTexture2d importTexture(ResourceLocation location) {
		final var client = Minecraft.getInstance();
		final var texture = client.getTextureManager().getTexture(location);
		texture.setFilter(true, false);
		return new GlTexture2d(false, texture.getId(), false);
	}

}
