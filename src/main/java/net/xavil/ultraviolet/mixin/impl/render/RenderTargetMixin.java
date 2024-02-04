package net.xavil.ultraviolet.mixin.impl.render;

import org.lwjgl.opengl.GL45C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
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
			Mod.LOGGER.debug("making main color buffer use a floating-point format...");
			return GL45C.GL_RGBA16F;
		} else {
			return internalformat;
		}
	}

}
