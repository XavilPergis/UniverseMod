package net.xavil.ultraviolet.client.gl;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.ultraviolet.client.gl.GlObject.ObjectType;
import net.xavil.ultraviolet.client.gl.GlState.BlendEquation;
import net.xavil.ultraviolet.client.gl.GlState.BlendFactor;
import net.xavil.ultraviolet.client.gl.GlState.CullFace;
import net.xavil.ultraviolet.client.gl.GlState.DepthFunc;
import net.xavil.ultraviolet.client.gl.GlState.FrontFace;
import net.xavil.ultraviolet.client.gl.GlState.PolygonMode;
import net.xavil.ultraviolet.client.gl.shader.ShaderStage;
import net.xavil.ultraviolet.client.gl.texture.GlTexture;

public final class UnmanagedStateSink implements GlStateSink {

	public static final UnmanagedStateSink INSTANCE = new UnmanagedStateSink();

	private final int[] tempNameHolder = new int[1];

	private UnmanagedStateSink() {
	}

	@Override
	public int createObject(GlObject.ObjectType objectType) {
		return switch (objectType) {
			case BUFFER -> GlStateManager._glGenBuffers();
			case FRAMEBUFFER -> GlStateManager.glGenFramebuffers();
			case PROGRAM -> GlStateManager.glCreateProgram();
			case RENDERBUFFER -> GlStateManager.glGenRenderbuffers();
			case SHADER ->
				throw new IllegalArgumentException("createShader() must be used to create a shader stage object");
			case TEXTURE -> GlStateManager._genTexture();
			case VERTEX_ARRAY -> GlStateManager._glGenVertexArrays();
		};
	}

	@Override
	public int createShader(ShaderStage.Stage stage) {
		return GlStateManager.glCreateShader(stage.id);
	}

	@Override
	public void deleteObject(GlObject.ObjectType objectType, int id) {
		switch (objectType) {
			case BUFFER -> GlStateManager._glDeleteBuffers(id);
			case FRAMEBUFFER -> GlStateManager._glDeleteFramebuffers(id);
			case PROGRAM -> GlStateManager.glDeleteProgram(id);
			case RENDERBUFFER -> GlStateManager._glDeleteRenderbuffers(id);
			case SHADER -> GlStateManager.glDeleteShader(id);
			case TEXTURE -> GlStateManager._deleteTexture(id);
			case VERTEX_ARRAY -> GlStateManager._glDeleteVertexArrays(id);
		}
	}

	@Override
	public void bindFramebuffer(int target, int id) {
		GL32C.glBindFramebuffer(target, id);
	}

	@Override
	public void bindBuffer(GlBuffer.Type target, int id) {
		GL32C.glBindBuffer(target.id, id);
	}

	@Override
	public void bindTexture(GlTexture.Type target, int id) {
		GlTexture.bindTexture(target.id, id);
	}

	@Override
	public void bindVertexArray(int id) {
		GL32C.glBindVertexArray(id);
	}

	@Override
	public void useProgram(int id) {
		GL32C.glUseProgram(id);
	}

	@Override
	public void bindRenderbuffer(int id) {
		GL32C.glBindRenderbuffer(GL32C.GL_RENDERBUFFER, id);
	}

	@Override
	public void activeTexture(int unit) {
		RenderSystem.activeTexture(unit);
	}

	@Override
	public void polygonMode(PolygonMode mode) {
		GlStateManager._polygonMode(GL32C.GL_FRONT_AND_BACK, mode.id);
	}

	@Override
	public void enableCull(boolean enable) {
		if (enable)
			RenderSystem.enableCull();
		else
			RenderSystem.disableCull();
	}

	@Override
	public void cullFace(CullFace cullFace) {
		GL32C.glCullFace(cullFace.id);
	}

	@Override
	public void frontFace(FrontFace frontFace) {
		GL32C.glFrontFace(frontFace.id);
	}

	@Override
	public void enableDepthTest(boolean enable) {
		if (enable)
			RenderSystem.enableDepthTest();
		else
			RenderSystem.disableDepthTest();
	}

	@Override
	public void enableLogicOp(boolean enable) {
		if (enable)
			RenderSystem.enableColorLogicOp();
		else
			RenderSystem.disableColorLogicOp();
	}

	@Override
	public void depthMask(boolean depthMask) {
		RenderSystem.depthMask(depthMask);
	}

	@Override
	public void depthFunc(DepthFunc depthFunc) {
		RenderSystem.depthFunc(depthFunc.id);
	}

	@Override
	public void enableBlend(boolean enable) {
		if (enable)
			RenderSystem.enableBlend();
		else
			RenderSystem.disableBlend();
	}

	@Override
	public void logicOp(GlState.LogicOp logicOp) {
		GlStateManager._logicOp(logicOp.id);
	}

	@Override
	public void blendEquation(BlendEquation blendEquationRgb, BlendEquation blendEquationAlpha) {
		// RenderSystem.blendEquation(0);
		GL32C.glBlendEquationSeparate(blendEquationRgb.id, blendEquationAlpha.id);
	}

	@Override
	public void blendFunc(BlendFactor blendFactorSrcRgb, BlendFactor blendFactorDstRgb,
			BlendFactor blendFactorSrcAlpha, BlendFactor blendFactorDstAlpha) {
		RenderSystem.blendFuncSeparate(blendFactorSrcRgb.id, blendFactorDstRgb.id, blendFactorSrcAlpha.id,
				blendFactorDstAlpha.id);
	}

	@Override
	public void colorMask(boolean colorMaskR, boolean colorMaskG, boolean colorMaskB, boolean colorMaskA) {
		RenderSystem.colorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
	}

	@Override
	public void setViewport(int x, int y, int w, int h) {
		RenderSystem.viewport(x, y, w, h);
	}

	@Override
	public void drawBuffers(int[] buffers) {
		GL32C.glDrawBuffers(buffers);
	}
}
