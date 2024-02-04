package net.xavil.ultraviolet.mixin.impl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.ClientMod;
import net.xavil.ultraviolet.mixin.accessor.LightTextureAccessor;

@Mixin(LightTexture.class)
public abstract class LightTextureMixin implements AutoCloseable, LightTextureAccessor {
	@Override
	@Accessor("lightPixels")
	public abstract NativeImage ultraviolet_lightPixels();

	@Override
	@Accessor("lightTexture")
	public abstract DynamicTexture ultraviolet_lightTexture();

	@Override
	@Accessor("lightTextureLocation")
	public abstract ResourceLocation ultraviolet_lightTextureLocation();

	private Vector3f baseSkyColor;

	private Vec3.Mutable scratchColor = new Vec3.Mutable();

	// @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target =
	// "Lcom/mojang/math/Vector3f/Vector3f;<init>(FFF)V", ordinal = 0))
	// private void storeSkyColor(Vector3f v, float r, float g, float b) {
	// this.baseSkyColor = v;
	// }

	// @Inject(method = "updateLightTexture", at = @At(value = "INVOKE", target =
	// "Lcom/mojang/math/Vector3f/Vector3f;<init>()V", ordinal = 0))
	// private void storeLightColor(float partialTick, CallbackInfo info) {
	// Vec3.set(this.scratchColor, this.baseSkyColor.x(), this.baseSkyColor.y(),
	// this.baseSkyColor.z());
	// ClientMod.modifySkyColor(this.scratchColor, partialTick);
	// this.baseSkyColor.set((float) this.scratchColor.x, (float)
	// this.scratchColor.y, (float) this.scratchColor.z);
	// }

}
