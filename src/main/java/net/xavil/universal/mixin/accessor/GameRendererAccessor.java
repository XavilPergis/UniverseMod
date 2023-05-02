package net.xavil.universal.mixin.accessor;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderInstance;
import net.xavil.util.math.matrices.Mat4;

public interface GameRendererAccessor {

	double universal_getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting);

	Mat4 universal_makeProjectionMatrix(float near, float far, boolean applyViewBobTranslation, float partialTick);

	ShaderInstance universal_getShader(String id);

	PostChain universal_getPostChain(String id);

	static ShaderInstance getShader(GameRenderer renderer, String id) {
		return ((GameRendererAccessor) renderer).universal_getShader(id);
	}

	static PostChain getPostChain(GameRenderer renderer, String id) {
		return ((GameRendererAccessor) renderer).universal_getPostChain(id);
	}

	static double getFov(GameRenderer renderer) {
		return getFov(renderer, Minecraft.getInstance().getFrameTime());
	}

	static double getFov(GameRenderer renderer, float partialTicks) {
		return getFov(renderer, renderer.getMainCamera(), partialTicks, true);
	}

	static double getFov(GameRenderer renderer, Camera activeRenderInfo, float partialTicks, boolean useFOVSetting) {
		return ((GameRendererAccessor) renderer).universal_getFov(activeRenderInfo, partialTicks, useFOVSetting);
	}

	static Mat4 makeProjectionMatrix(GameRenderer renderer, float near, float far, boolean applyViewBobTranslation,
			float partialTick) {
		return ((GameRendererAccessor) renderer).universal_makeProjectionMatrix(near, far, applyViewBobTranslation,
				partialTick);
	}

}
