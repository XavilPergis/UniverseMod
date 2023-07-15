package net.xavil.ultraviolet.mixin.accessor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.math.matrices.Mat4;

public interface GameRendererAccessor {

	double ultraviolet_getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting);

	Mat4 ultraviolet_makeProjectionMatrix(float near, float far, boolean applyViewBobTranslation, float partialTick);

	ShaderProgram ultraviolet_getShader(ResourceLocation id);

	ShaderProgram ultraviolet_getVanillaShader(String id);

	static ShaderProgram getShader(GameRenderer renderer, ResourceLocation id) {
		return ((GameRendererAccessor) renderer).ultraviolet_getShader(id);
	}

	static ShaderProgram getVanillaShader(GameRenderer renderer, String id) {
		return ((GameRendererAccessor) renderer).ultraviolet_getVanillaShader(id);
	}

	static double getFov(GameRenderer renderer) {
		return getFov(renderer, Minecraft.getInstance().getFrameTime());
	}

	static double getFov(GameRenderer renderer, float partialTicks) {
		return getFov(renderer, renderer.getMainCamera(), partialTicks, true);
	}

	static double getFov(GameRenderer renderer, Camera activeRenderInfo, float partialTicks, boolean useFOVSetting) {
		return ((GameRendererAccessor) renderer).ultraviolet_getFov(activeRenderInfo, partialTicks, useFOVSetting);
	}

	static Mat4 makeProjectionMatrix(GameRenderer renderer, float near, float far, boolean applyViewBobTranslation,
			float partialTick) {
		return ((GameRendererAccessor) renderer).ultraviolet_makeProjectionMatrix(near, far, applyViewBobTranslation,
				partialTick);
	}

}
