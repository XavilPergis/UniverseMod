package net.xavil.universal.client.flexible;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.xavil.universal.client.flexible.FlexibleBufferBuilder.FinishedBuffer;

public final class BufferRenderer {

	private static final FlexibleBufferBuilder IMMEDIATE_BUILDER = new FlexibleBufferBuilder(0x200000);

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

		// TODO: is this even right?
		buffer.clear();
		if (info.vertexCount() <= 0)
			return;

		final var byteLimit = info.vertexCount() * info.format().getVertexSize();
		buffer.position(0);
		buffer.limit(byteLimit);
		setupForFormat(info.format());
		GlStateManager._glBufferData(GL32.GL_ARRAY_BUFFER, buffer, GL32.GL_DYNAMIC_DRAW);

		int k = 0;
		if (info.sequentialIndex()) {
			final var sequentialIndexBuffer = RenderSystem.getSequentialBuffer(info.mode(), info.indexCount());
			GlStateManager._glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, sequentialIndexBuffer.name());
			k = sequentialIndexBuffer.type().asGLType;
		} else {
			int indexBuffer = info.format().getOrCreateIndexBufferObject();
			GlStateManager._glBindBuffer(GL32.GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
			buffer.position(byteLimit);
			buffer.limit(byteLimit + info.indexCount() * info.indexType().bytes);
			GlStateManager._glBufferData(GL32.GL_ELEMENT_ARRAY_BUFFER, buffer, 35048);
			k = info.indexType().asGLType;
		}

		setupDefaultShaderUniforms(shader);
		shader.apply();
		GlStateManager._drawElements(info.mode().asGLMode, info.indexCount(), k, 0L);
		shader.clear();
		buffer.position(0);
	}

	private static VertexFormat lastFormat = null;

	private static void setupForFormat(VertexFormat format) {
		BufferUploader.reset();
		if (lastFormat != null)
			lastFormat.clearBufferState();
		final var vao = format.getOrCreateVertexArrayObject();
		final var vbo = format.getOrCreateVertexBufferObject();
		GlStateManager._glBindVertexArray(vao);
		GlStateManager._glBindBuffer(GL32.GL_ARRAY_BUFFER, vbo);
		format.setupBufferState();
		lastFormat = format;
	}

	public static void setupDefaultShaderUniforms(ShaderInstance shader) {
		final var window = Minecraft.getInstance().getWindow();
		for (var i = 0; i < 8; ++i) {
			shader.setSampler("Sampler" + i, RenderSystem.getShaderTexture(i));
		}
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
		if (shader.SCREEN_SIZE != null)
			shader.SCREEN_SIZE.set(window.getWidth(), window.getHeight());
		if (shader.LINE_WIDTH != null)
			shader.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
		RenderSystem.setupShaderLights(shader);
	}

}
