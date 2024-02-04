package net.xavil.ultraviolet.mixin.impl.render;

import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.platform.GlStateManager;

import net.xavil.ultraviolet.Mod;

@Mixin(MainTarget.class)
public abstract class MainTargetMixin {

	// @Redirect(method = "allocateColorAttachment", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V", remap = false))
	// private static void makeMainColorBufferHdr(int target, int level, int internalformat, int width, int height,
	// 		int border, int format, int type, @Nullable IntBuffer pixels) {
	// 	Mod.LOGGER.warn(
	// 			"MainTarget texImage (via MainTargetMixin): target={}, level={}, width={}, height={}, border={}, format={}, type={}, pixels={}",
	// 			target, level, width, height, border, format, type, pixels);
	// 	GlStateManager._texImage2D(target, level, GL45C.GL_RGBA16F, width, height, border, format, type, pixels);
	// }
	// @Inject(method = "createFrameBuffer", at = @At("HEAD"))
	// private static void shdkhjgaklshfghafsdklsadfg(int width, int height, CallbackInfo info) {
	// 	System.err.println("AAAAAAAAAAAAAAAAAAAA 3");
	// 	Mod.LOGGER.warn("motherfucker are you going to be injected??");
	// }

}
