package net.xavil.ultraviolet.mixin.impl.render;

import org.lwjgl.opengl.GL45C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.xavil.ultraviolet.Mod;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {

	private boolean isMainTarget() {
		return (Object) this == (Object) Minecraft.getInstance().getMainRenderTarget();
	}

	// LMFAO
	@ModifyArg(method = "createBuffers", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V", remap = false, ordinal = 1), index = 2)
	private int makeMainColorBufferHdr(int internalformat) {
		if (isMainTarget()) {
			Mod.LOGGER.error("making main color buffer use a floating-point format...");
			return GL45C.GL_RGBA16F;
		} else {
			return internalformat;
		}
	}

	// @Inject(method = "<init>", at = @At("HEAD"))
	// private static void sdkjgkjhsdg(int width, int height, CallbackInfo info) {
	// 	System.err.println("AAAAAAAAAAAAAAAAAAAA 1");
	// 	Mod.LOGGER.warn("how about you");
	// }

	// @Inject(method = "createBuffers", at = @At("HEAD"))
	// private static void shdkhjgaklshfghafsdklsadfg(int width, int height, CallbackInfo info) {
	// 	System.err.println("AAAAAAAAAAAAAAAAAAAA 2");
	// 	Mod.LOGGER.warn("motherfucker are you going to be injected??");
	// }

}
