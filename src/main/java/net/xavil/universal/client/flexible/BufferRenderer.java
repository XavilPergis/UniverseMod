package net.xavil.universal.client.flexible;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

public final class BufferRenderer {

	private static final FlexibleBufferBuilder IMMEDIATE_BUILDER = new FlexibleBufferBuilder(0x200000);
	private static final FlexibleVertexBuffer IMMEDIATE_BUFFER = new FlexibleVertexBuffer();

	public static FlexibleBufferBuilder immediateBuilder() {
		return IMMEDIATE_BUILDER;
	}

	public static void draw(BufferBuilder builder) {
		BufferUploader.end(builder);
	}

	public static void draw(FlexibleBufferBuilder builder) {
		draw(RenderSystem.getShader(), builder);
	}

	public static void draw(ShaderInstance shader, FlexibleBufferBuilder builder) {
		final var bufferPair = builder.popFinished();
		draw(shader, bufferPair.getSecond(), bufferPair.getFirst());
	}

	public static void draw(ShaderInstance shader, ByteBuffer buffer, FinishedBuffer info) {
		IMMEDIATE_BUFFER.upload(buffer, info, GL32.GL_DYNAMIC_DRAW);
		setupDefaultShaderUniforms(shader);
		IMMEDIATE_BUFFER.draw(shader);
	}

	public static void setupDefaultShaderUniforms(ShaderInstance shader) {
		setupDefaultShaderUniforms(shader, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
	}

	public static void setupDefaultShaderUniforms(ShaderInstance shader,
			Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
		final var window = Minecraft.getInstance().getWindow();
		for (var i = 0; i < 8; ++i) {
			shader.setSampler("Sampler" + i, RenderSystem.getShaderTexture(i));
		}
		if (shader.MODEL_VIEW_MATRIX != null)
			shader.MODEL_VIEW_MATRIX.set(modelViewMatrix);
		if (shader.PROJECTION_MATRIX != null)
			shader.PROJECTION_MATRIX.set(projectionMatrix);
		if (shader.INVERSE_VIEW_ROTATION_MATRIX != null)
			shader.INVERSE_VIEW_ROTATION_MATRIX.set(RenderSystem.getInverseViewRotationMatrix());
		if (shader.COLOR_MODULATOR != null)
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
		if (shader.FOG_START != null)
			shader.FOG_START.set(RenderSystem.getShaderFogStart());
		if (shader.FOG_END != null)
			shader.FOG_END.set(RenderSystem.getShaderFogEnd());
		if (shader.FOG_COLOR != null)
			shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
		if (shader.FOG_SHAPE != null)
			shader.FOG_SHAPE.set(RenderSystem.getShaderFogShape().getIndex());
		if (shader.TEXTURE_MATRIX != null)
			shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
		if (shader.GAME_TIME != null)
			shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
		if (shader.SCREEN_SIZE != null)
			shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
		if (shader.LINE_WIDTH != null)
			shader.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
		RenderSystem.setupShaderLights(shader);
	}

}
