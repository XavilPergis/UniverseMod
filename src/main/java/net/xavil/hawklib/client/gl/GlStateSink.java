package net.xavil.hawklib.client.gl;

import net.xavil.hawklib.client.gl.texture.GlTexture;

public interface GlStateSink {

	void bindFramebuffer(int target, int id);

	void bindBuffer(GlBuffer.Type target, int id);

	void bindTexture(GlTexture.Type target, int id);

	void bindVertexArray(int id);

	void bindProgram(int id);

	void bindRenderbuffer(int id);

	void bindTextureUnit(int unit);

	void enableCull(boolean enable);

	void enableBlend(boolean enable);

	void enableDepthTest(boolean enable);

	void enableLogicOp(boolean enable);

	void enableProgramPointSize(boolean enable);

	void polygonMode(GlState.PolygonMode mode);

	void cullFace(GlState.CullFace cullFace);

	void frontFace(GlState.FrontFace frontFace);

	void depthMask(boolean depthMask);

	void depthFunc(GlState.DepthFunc depthFunc);

	void logicOp(GlState.LogicOp logicOp);

	void blendEquation(GlState.BlendEquation blendEquationRgb, GlState.BlendEquation blendEquationAlpha);

	void blendFunc(GlState.BlendFactor blendFactorSrcRgb, GlState.BlendFactor blendFactorDstRgb,
			GlState.BlendFactor blendFactorSrcAlpha, GlState.BlendFactor blendFactorDstAlpha);

	void colorMask(boolean colorMaskR, boolean colorMaskG, boolean colorMaskB, boolean colorMaskA);

	void setViewport(int x, int y, int w, int h);

}
