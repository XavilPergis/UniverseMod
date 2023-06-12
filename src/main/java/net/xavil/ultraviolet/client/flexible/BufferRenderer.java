package net.xavil.ultraviolet.client.flexible;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import static net.xavil.ultraviolet.client.Shaders.*;
import static net.xavil.ultraviolet.client.DrawStates.*;
import net.xavil.ultraviolet.client.camera.CachedCamera;
import net.xavil.ultraviolet.client.gl.DrawState;
import net.xavil.ultraviolet.client.gl.GlBuffer;
import net.xavil.ultraviolet.client.gl.GlManager;
import net.xavil.ultraviolet.client.gl.shader.ShaderProgram;
import net.xavil.ultraviolet.client.gl.texture.GlTexture2d;
import net.xavil.util.math.matrices.interfaces.Mat4Access;

public final class BufferRenderer {

	public static final FlexibleBufferBuilder IMMEDIATE_BUILDER = new FlexibleBufferBuilder(0x200000);
	public static final FlexibleVertexBuffer IMMEDIATE_BUFFER = new FlexibleVertexBuffer();

	public static FlexibleBufferBuilder immediateBuilder() {
		return IMMEDIATE_BUILDER;
	}

	public static void draw(ShaderProgram shader, FlexibleBufferBuilder builder,
			DrawState drawState) {
		final var bufferPair = builder.popFinished();
		draw(shader, bufferPair.getSecond(), bufferPair.getFirst(), drawState);
	}

	public static void draw(ShaderProgram shader, ByteBuffer buffer, FinishedBuffer info,
			DrawState drawState) {
		setupDefaultShaderUniforms(shader);
		IMMEDIATE_BUFFER.upload(buffer, info, GlBuffer.UsageHint.STREAM_DRAW);
		IMMEDIATE_BUFFER.draw(shader, drawState);
	}

	public static void drawFullscreen(GlTexture2d texture) {
		drawFullscreen(texture, DRAW_STATE_DIRECT);
	}

	public static void drawFullscreen(GlTexture2d texture, DrawState drawState) {
		final var shader = getShader(SHADER_BLIT);
		shader.setUniformSampler("uSampler", texture);
		drawFullscreen(shader, drawState);
	}

	public static void drawFullscreen(ShaderProgram shader) {
		drawFullscreen(shader, DRAW_STATE_DIRECT_ALPHA_BLENDING);
	}

	public static void drawFullscreen(ShaderProgram shader, DrawState drawState) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		builder.vertex(-1, 1, 0).uv0(0, 1).endVertex();
		builder.vertex(1, 1, 0).uv0(1, 1).endVertex();
		builder.vertex(1, -1, 0).uv0(1, 0).endVertex();
		builder.vertex(-1, -1, 0).uv0(0, 0).endVertex();
		builder.end();
		GlManager.pushState();
		GlManager.enableCull(false);
		// if (drawState != null) {
		// drawState.apply(GlManager.currentState());
		// }
		draw(shader, builder, drawState);
		GlManager.popState();
	}

	public static void setupDefaultShaderUniforms(ShaderProgram shader) {
		setupDefaultShaderUniforms(shader, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
	}

	public static void setupCameraUniforms(ShaderProgram shader, CachedCamera<?> camera) {
		shader.setUniform("uCameraPos", camera.pos);
		shader.setUniform("uCameraNear", 0.0);
		shader.setUniform("uCameraFar", 0.0);
		shader.setUniform("uCameraFov", 0.0);
		shader.setUniform("uMetersPerUnit", camera.metersPerUnit);
		shader.setUniform("uViewMatrix", camera.viewMatrix);
		shader.setUniform("uProjectionMatrix", camera.projectionMatrix);
		shader.setUniform("uViewProjectionMatrix", camera.viewProjectionMatrix);
	}

	public static void setupDefaultShaderUniforms(ShaderProgram shader,
			Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {

		shader.setUniform("uViewMatrix", Mat4Access.from(modelViewMatrix));
		shader.setUniform("uProjectionMatrix", Mat4Access.from(projectionMatrix));

		if (shader.getWrappedVanillaShader() != null)
			setupDefaultVanillaUniforms(shader.getWrappedVanillaShader());
	}

	private static void setupDefaultVanillaUniforms(ShaderInstance shader) {
		if (shader.MODEL_VIEW_MATRIX != null)
			shader.MODEL_VIEW_MATRIX.set(RenderSystem.getModelViewMatrix());
		if (shader.PROJECTION_MATRIX != null)
			shader.PROJECTION_MATRIX.set(RenderSystem.getProjectionMatrix());
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
