package net.xavil.universal.mixin.impl;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;

@Mixin(Window.class)
public abstract class WindowMixin {

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V", shift = At.Shift.AFTER, remap = false))
	private void hintMultisample(WindowEventHandler windowEventHandler, ScreenManager screenManager,
			DisplayData displayData, String string, String string2, CallbackInfo info) {
		GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4);
	}

}
