package net.xavil.universal.client.flexible;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.xavil.universal.client.camera.CachedCamera;
import net.xavil.util.math.matrices.interfaces.Mat4Access;
import net.xavil.util.math.matrices.interfaces.Vec3Access;

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

	public static void setupCameraUniforms(ShaderInstance shader, CachedCamera<?> camera) {
		setUniform(shader, "uCameraPos", camera.pos);
		setUniform(shader, "uCameraNear", 0.0);
		setUniform(shader, "uCameraFar", 0.0);
		setUniform(shader, "uCameraFov", 0.0);
		setUniform(shader, "uMetersPerUnit", camera.metersPerUnit);
		setUniform(shader, "uViewMatrix", camera.viewMatrix);
		setUniform(shader, "uProjectionMatrix", camera.projectionMatrix);
		setUniform(shader, "uViewProjectionMatrix", camera.viewProjectionMatrix);
	}

	public static void setUniform(ShaderInstance shader, String uniformName, double value) {
		shader.safeGetUniform(uniformName).set((float) value);
	}

	public static void setUniform(ShaderInstance shader, String uniformName, Vec3Access value) {
		shader.safeGetUniform(uniformName).set((float) value.x(), (float) value.y(), (float) value.z());
	}

	public static void setUniform(ShaderInstance shader, String uniformName, Mat4Access value) {
		shader.safeGetUniform(uniformName).setMat4x4(
				(float) value.r0c0(), (float) value.r0c1(), (float) value.r0c2(), (float) value.r0c3(),
				(float) value.r1c0(), (float) value.r1c1(), (float) value.r1c2(), (float) value.r1c3(),
				(float) value.r2c0(), (float) value.r2c1(), (float) value.r2c2(), (float) value.r2c3(),
				(float) value.r3c0(), (float) value.r3c1(), (float) value.r3c2(), (float) value.r3c3());
	}

	public static void setupDefaultShaderUniforms(ShaderInstance shader,
			Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {
		for (var i = 0; i < 8; ++i) {
			shader.setSampler("Sampler" + i, RenderSystem.getShaderTexture(i));
		}

		if (shader.MODEL_VIEW_MATRIX != null)
			shader.MODEL_VIEW_MATRIX.set(modelViewMatrix);
		shader.safeGetUniform("uViewMatrix").set(modelViewMatrix); // setUniform(shader, "uViewMatrix", modelViewMatrix);
		if (shader.PROJECTION_MATRIX != null)
			shader.PROJECTION_MATRIX.set(projectionMatrix);
		shader.safeGetUniform("uProjectionMatrix").set(projectionMatrix); // setUniform(shader, "uProjectionMatrix", projectionMatrix);
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
		final var window = Minecraft.getInstance().getWindow();
		if (shader.SCREEN_SIZE != null)
			shader.SCREEN_SIZE.set((float) window.getWidth(), (float) window.getHeight());
		if (shader.LINE_WIDTH != null)
			shader.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
		RenderSystem.setupShaderLights(shader);
	}

}
