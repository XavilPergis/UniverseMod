package net.xavil.ultraviolet.mixin.impl.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.platform.GlDebug;

import net.xavil.hawklib.client.gl.GlManager;

@Mixin(GlDebug.class)
public abstract class GlDebugMixin {
	@Shadow
	private static boolean debugEnabled;

	@Inject(method = "enableDebugCallback", at = @At("HEAD"), cancellable = true)
	private static void setupDebug(int debugVerbosity, boolean synchronous, CallbackInfo info) {
		if (GlManager.ENABLE_DEBUG) {
			GlManager.setupDebugMessageCallback();
			debugEnabled = true;
			info.cancel();
		}
	}
}
