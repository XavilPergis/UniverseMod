package net.xavil.universal.mixin.accessor;

import com.mojang.math.Matrix4f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

public interface GameRendererAccessor {

	double universal_getFov(Camera activeRenderInfo, float partialTicks, boolean useFOVSetting);
	Matrix4f universal_makeProjectionMatrix(float near, float far, float partialTick);
	ShaderInstance universal_getShader(String id);

	static ShaderInstance getShader(GameRenderer renderer, String id) {
		return ((GameRendererAccessor) renderer).universal_getShader(id);
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

	static Matrix4f makeProjectionMatrix(GameRenderer renderer, float near, float far, float partialTick) {
		return ((GameRendererAccessor) renderer).universal_makeProjectionMatrix(near, far, partialTick);
	}

}
