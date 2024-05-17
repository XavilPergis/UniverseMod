package net.xavil.ultraviolet.mixin.impl.render;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;

import net.xavil.hawklib.client.gl.GlManager;

@Mixin(Window.class)
public abstract class WindowMixin {

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V", shift = At.Shift.AFTER, remap = false))
	private void addWindowHints(WindowEventHandler windowEventHandler, ScreenManager screenManager,
			DisplayData displayData, String string, String string2, CallbackInfo info) {
		// GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 4);
		if (GlManager.ENABLE_DEBUG)
			GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, GLFW.GLFW_TRUE);
	}

	// opengl 4.5 is from 2014. i want to use it. support should be good except for
	// macs, thought they might be able to run modern opengl via Zink anyways. So
	// like. whateva :p
	// WORKS ON MY MACHINE~
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwWindowHint(II)V", remap = false))
	private void modifyWindowHints(int hint, int value) {
		if (hint == GLFW.GLFW_CONTEXT_VERSION_MAJOR)
			value = 4;
		if (hint == GLFW.GLFW_CONTEXT_VERSION_MINOR)
			value = 5;
		GLFW.glfwWindowHint(hint, value);
	}

}
