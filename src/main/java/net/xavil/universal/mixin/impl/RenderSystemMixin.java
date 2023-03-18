package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.universal.client.flexible.BufferRenderer;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {
	
	@Inject(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), remap = false)
	private void resetFlexibleBuilder(long i, CallbackInfo info) {
		BufferRenderer.immediateBuilder().reset();
	}

}
