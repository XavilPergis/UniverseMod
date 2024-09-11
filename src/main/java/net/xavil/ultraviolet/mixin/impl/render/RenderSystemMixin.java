package net.xavil.ultraviolet.mixin.impl.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.hawklib.client.flexible.vertex.VertexBuilder;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

	@Inject(method = "flipFrame(J)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), remap = false)
	private static void resetFlexibleBuilder(long i, CallbackInfo info) {
		VertexBuilder.advanceFrame();
	}

	@Inject(method = "defaultBlendFunc()V", at = @At("HEAD"), cancellable = true, remap = false)
	private static void fixDefaultBlendFunc(CallbackInfo info) {
		// soft overwrite
		info.cancel();
		// vanilla has [src=ONE, dst=ZERO] which seems just straight up wrong to me.
		// Maybe there's usecases where you'd want the other way, but this just writes
		// src's alpha directly to the image instead of keeping whats there (usually
		// just fully opaque). I think this usually doesnt matter for vanilla since it
		// kinda just ignores the main image alpha, but we need intact alpha to
		// composite translucent particles onto the sky.
		//
		// TODO: it might be better to be more precise with this instead of steamrolling
		// the default blend mode.
		//
		// This might break things.
		RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
	}

}
