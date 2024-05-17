package net.xavil.hawklib.client.gl.texture;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.client.flexible.FlexibleRenderTarget;
import net.xavil.hawklib.client.gl.GlLimits;
import net.xavil.hawklib.collections.interfaces.MutableMap;

public final class GlTexture2d extends GlTexture {

	public GlTexture2d(boolean multisampled, int glId, boolean owned) {
		super(multisampled ? Type.D2_MS : Type.D2, glId, owned);
	}

	public GlTexture2d(boolean multisampled) {
		super(multisampled ? Type.D2_MS : Type.D2);
	}

	public void resize(int width, int height) {
		if (this.textureFormat == null) {
			throw new IllegalStateException(
					debugDescription() + "Cannot resize a texture whose storage has not been previously allocated!");
		}
		createStorage(this.textureFormat, width, height);
	}

	public Slice slice2d(int lodLevel, int offsetX, int offsetY, int sizeX, int sizeY) {
		return new Slice(this, SliceDimension.D2, lodLevel, 0, 1, offsetX, offsetY, 0, sizeX, sizeY, 1);
	}

	public Slice slice2d(int offsetX, int offsetY, int sizeX, int sizeY) {
		return slice2d(offsetX, offsetY, sizeX, sizeY);
	}

	@Override
	public GlTexture2d asTexture2d() {
		return this;
	}

	public void updateCachedSize(int width, int height) {
		updateCachedSize(new Size(width, height, 1, 1));
	}

	public void createStorage(GlTexture.Format textureFormat, int width, int height) {
		GlLimits.validateTextureSize(width, height);
		if (this.storageAllocated)
			throw new IllegalStateException(String.format(
					"%s: Tried to update immutable texture storage",
					debugDescription()));

		switch (this.type) {
			case D2 -> GL45C.glTextureStorage2D(this.id, 1, textureFormat.id, width, height);
			case D2_MS -> GL45C.glTextureStorage2DMultisample(this.type.id, 4, textureFormat.id, width, height, true);
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
		final var res = new GlTexture2d(ms, target.getColorTextureId(), false);
		res.storageAllocated = true;
		return res;
	}

	public static GlTexture2d importFromRenderTargetDepth(RenderTarget target) {
		if (target.getDepthTextureId() == -1)
			return null;
		final var ms = target instanceof FlexibleRenderTarget flex && flex.isMultisampled();
		final var res = new GlTexture2d(ms, target.getDepthTextureId(), false);
		res.storageAllocated = true;
		return res;
	}

	public static final MutableMap<ResourceLocation, GlTexture2d> IMPORT_CACHE = MutableMap.hashMap();

	public static GlTexture2d importTexture(ResourceLocation location) {
		final var manager = Minecraft.getInstance().getTextureManager();
		final var currentTexture = manager.getTexture(location);

		if (!IMPORT_CACHE.containsKey(location) || IMPORT_CACHE.getOrThrow(location).id != currentTexture.getId()) {
			final var res = new GlTexture2d(false, currentTexture.getId(), false);
			res.storageAllocated = true;
			IMPORT_CACHE.insert(location, res);
		}

		return IMPORT_CACHE.getOrThrow(location);
	}

}
