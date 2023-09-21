package net.xavil.hawklib.client.flexible;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;

public final class BufferRenderer {

	public static final VertexBuilder IMMEDIATE_BUILDER = new VertexBuilder(0x400000);
	public static final Mesh IMMEDIATE_BUFFER = new Mesh();

	public static void draw(ShaderProgram shader, VertexBuilder.BuiltBuffer buffer, DrawState drawState) {
		setupDefaultShaderUniforms(shader);
		IMMEDIATE_BUFFER.upload(buffer, GlBuffer.UsageHint.STREAM_DRAW);
		IMMEDIATE_BUFFER.draw(shader, drawState);
	}

	public static void drawFullscreen(GlTexture2d texture) {
		drawFullscreen(texture, HawkDrawStates.DRAW_STATE_DIRECT);
	}

	public static void drawFullscreen(GlTexture2d texture, DrawState drawState) {
		final var shader = HawkShaders.SHADER_BLIT.get();
		shader.setUniformSampler("uSampler", texture);
		drawFullscreen(shader, drawState);
	}

	public static void drawFullscreen(ShaderProgram shader) {
		drawFullscreen(shader, HawkDrawStates.DRAW_STATE_DIRECT_ALPHA_BLENDING);
	}

	public static void drawFullscreen(ShaderProgram shader, DrawState drawState) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER
				.beginGeneric(PrimitiveType.QUADS, BufferLayout.POSITION_TEX);
		builder.vertex(-1, 1, 0).uv0(0, 1).endVertex();
		builder.vertex(1, 1, 0).uv0(1, 1).endVertex();
		builder.vertex(1, -1, 0).uv0(1, 0).endVertex();
		builder.vertex(-1, -1, 0).uv0(0, 0).endVertex();
		final var built = builder.end();
		GlManager.pushState();
		GlManager.enableCull(false);
		draw(shader, built, drawState);
		GlManager.popState();
	}

	public static void setupDefaultShaderUniforms(ShaderProgram shader) {
		setupDefaultShaderUniforms(shader, RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix());
	}

	public static void setupCameraUniforms(ShaderProgram shader, CachedCamera camera) {
		shader.setUniformf("uCameraPos", camera.pos);
		shader.setUniformf("uCameraNear", camera.nearPlane);
		shader.setUniformf("uCameraFar", camera.farPlane);
		// shader.setUniform("uCameraFov", 0.0);
		shader.setUniformf("uMetersPerUnit", camera.metersPerUnit);
		shader.setUniformf("uViewMatrix", camera.viewMatrix);
		shader.setUniformf("uProjectionMatrix", camera.projectionMatrix);
		shader.setUniformf("uViewProjectionMatrix", camera.viewProjectionMatrix);
	}

	public static void setupDefaultShaderUniforms(ShaderProgram shader,
			Matrix4f modelViewMatrix, Matrix4f projectionMatrix) {

		// final var near = projectionMatrix.m23 / (projectionMatrix.m22 - 1.0);
		// final var far = projectionMatrix.m23 / (projectionMatrix.m22 + 1.0);
		// shader.setUniform("uCameraNear", near);
		// shader.setUniform("uCameraFar", far);

		shader.setUniformf("uViewMatrix", Mat4Access.from(modelViewMatrix));
		shader.setUniformf("uProjectionMatrix", Mat4Access.from(projectionMatrix));

		final var window = Minecraft.getInstance().getWindow();
		shader.setUniformf("uScreenSize", (float) window.getWidth(), (float) window.getHeight());

		setupDefaultVanillaUniforms(shader.getWrappedVanillaShader());
	}

	private static void setupDefaultVanillaUniforms(ShaderInstance shader) {
		if (shader == null)
			return;
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
