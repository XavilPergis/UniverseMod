package net.xavil.hawklib.client.gl;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.hawklib.client.gl.GlState.BlendEquation;
import net.xavil.hawklib.client.gl.GlState.BlendFactor;
import net.xavil.hawklib.client.gl.GlState.CullFace;
import net.xavil.hawklib.client.gl.GlState.DepthFunc;
import net.xavil.hawklib.client.gl.GlState.FrontFace;
import net.xavil.hawklib.client.gl.GlState.PolygonMode;
import net.xavil.hawklib.client.gl.texture.GlTexture;

public final class UnmanagedStateSink implements GlStateSink {

	public static final UnmanagedStateSink INSTANCE = new UnmanagedStateSink();

	private UnmanagedStateSink() {
	}

	@Override
	public void bindFramebuffer(int target, int id) {
		GL45C.glBindFramebuffer(target, id);
	}

	@Override
	public void bindBuffer(GlBuffer.Type target, int id) {
		GL45C.glBindBuffer(target.id, id);
	}

	@Override
	public void bindTexture(GlTexture.Type target, int id) {
		// NOTE: GlStateManager only tracks 2d textures
		if (target == GlTexture.Type.D2)
			GlStateManager.TEXTURES[GlStateManager.activeTexture].binding = id;
		GL45C.glBindTexture(target.id, id);
	}

	@Override
	public void bindVertexArray(int id) {
		GL45C.glBindVertexArray(id);
	}

	@Override
	public void bindProgram(int id) {
		GL45C.glUseProgram(id);
	}

	@Override
	public void bindRenderbuffer(int id) {
		GL45C.glBindRenderbuffer(GL45C.GL_RENDERBUFFER, id);
	}

	@Override
	public void bindTextureUnit(int unit) {
		RenderSystem.activeTexture(unit);
	}

	@Override
	public void polygonMode(PolygonMode mode) {
		GlStateManager._polygonMode(GL45C.GL_FRONT_AND_BACK, mode.id);
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
		GL45C.glCullFace(cullFace.id);
	}

	@Override
	public void frontFace(FrontFace frontFace) {
		GL45C.glFrontFace(frontFace.id);
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
		GL45C.glBlendEquationSeparate(blendEquationRgb.id, blendEquationAlpha.id);
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
	public void enableProgramPointSize(boolean enable) {
		if (enable) {
			GL45C.glEnable(GL45C.GL_PROGRAM_POINT_SIZE);
		} else {
			GL45C.glDisable(GL45C.GL_PROGRAM_POINT_SIZE);
		}
	}
}
