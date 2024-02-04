package net.xavil.ultraviolet.mixin.accessor;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;

public interface LightTextureAccessor {

	NativeImage ultraviolet_lightPixels();

	DynamicTexture ultraviolet_lightTexture();

	ResourceLocation ultraviolet_lightTextureLocation();

	@SuppressWarnings("resource")
	static NativeImage lightPixels() {
		final var tex = Minecraft.getInstance().gameRenderer.lightTexture();
		return ((LightTextureAccessor) tex).ultraviolet_lightPixels();
	}

	@SuppressWarnings("resource")
	static DynamicTexture lightTexture() {
		final var tex = Minecraft.getInstance().gameRenderer.lightTexture();
		return ((LightTextureAccessor) tex).ultraviolet_lightTexture();
	}

	@SuppressWarnings("resource")
	static ResourceLocation lightTextureLocation() {
		final var tex = Minecraft.getInstance().gameRenderer.lightTexture();
		return ((LightTextureAccessor) tex).ultraviolet_lightTextureLocation();
	}

	static GlTexture2d get() {
		return GlTexture2d.importTexture(lightTextureLocation());
	}

}
