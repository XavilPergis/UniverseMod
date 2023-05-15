package net.xavil.ultraviolet.client.flexible;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

public final class Texture2d extends Texture {

	public Texture2d(boolean multisampled, int glId, boolean owned) {
		super(multisampled ? Type.D2_MS : Type.D2, glId, owned);
	}

	public Texture2d(boolean multisampled) {
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
	public Texture2d asTexture2d() {
		return this;
	}

	public void createStorage(Texture.Format textureFormat, int width, int height) {
		final var maxTextureSize = RenderSystem.maxSupportedTextureSize();
		if (width > maxTextureSize || height > maxTextureSize) {
			throw new IllegalArgumentException(debugDescription() + "Maximum texture size is " + maxTextureSize
					+ ", but the requested size was (" + width + ", " + height + ")");
		}
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
					GL32.GL_RGBA, GL32.GL_UNSIGNED_BYTE, null);
			case D2_MS -> GL32.glTexImage2DMultisample(
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

	public static Texture2d importFromRenderTarget(RenderTarget target) {
		final var ms = target instanceof FlexibleRenderTarget flex && flex.isMultisampled();
		return new Texture2d(ms, target.getColorTextureId(), false);
	}

}
