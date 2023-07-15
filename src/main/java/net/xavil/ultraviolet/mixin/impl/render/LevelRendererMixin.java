package net.xavil.ultraviolet.mixin.impl.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.ultraviolet.client.SkyRenderer;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

	@Shadow
	private Minecraft minecraft;

	@Inject(at = @At(value = "INVOKE", target = "Ljava/lang/Runnable;run()V", shift = At.Shift.AFTER, ordinal = 0), method = "renderSky(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/math/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", cancellable = true)
	private void renderSky(PoseStack poseStack, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean bl,
			Runnable runnable, CallbackInfo info) {
		if (this.minecraft.level != null) {
			GlManager.pushState();
			if (SkyRenderer.INSTANCE.renderSky(poseStack, projectionMatrix, tickDelta, camera, bl))
				info.cancel();
			GlManager.popState();
		}
	}

}
