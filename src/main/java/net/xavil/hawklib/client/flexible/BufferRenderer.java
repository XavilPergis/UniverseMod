package net.xavil.hawklib.client.flexible;

import net.xavil.hawklib.client.HawkDrawStates;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;

public final class BufferRenderer {

	public static final GrowableVertexBuilder IMMEDIATE_BUILDER = new GrowableVertexBuilder(0x400000);
	public static final Mesh IMMEDIATE_BUFFER = new Mesh();

	public static void draw(ShaderProgram shader, VertexBuilder.BuiltBuffer buffer, DrawState drawState) {
		shader.setupDefaultShaderUniforms();
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
				.beginGeneric(PrimitiveType.QUAD_DUPLICATED, BufferLayout.POSITION_TEX);
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

}
