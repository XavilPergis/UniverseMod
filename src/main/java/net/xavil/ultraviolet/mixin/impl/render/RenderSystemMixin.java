package net.xavil.ultraviolet.mixin.impl.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.hawklib.client.flexible.BufferRenderer;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
	
	@Inject(method = "flipFrame(J)V", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), remap = false)
	private static void resetFlexibleBuilder(long i, CallbackInfo info) {
		BufferRenderer.IMMEDIATE_BUILDER.advanceFrame();
	}

}
