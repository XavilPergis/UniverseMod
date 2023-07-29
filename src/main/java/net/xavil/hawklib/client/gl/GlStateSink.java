package net.xavil.hawklib.client.gl;

import java.nio.ByteBuffer;

import net.xavil.hawklib.client.gl.shader.ShaderStage;
import net.xavil.hawklib.client.gl.texture.GlTexture;

public interface GlStateSink {

	/**
	 * Allocate a GL object name with the provided object type, except for
	 * {@link GlObject.ObjectType#SHADER}, which must be created via
	 * {@link #createShader(ShaderStage.Stage)}. Attempting to create an object of
	 * this type via this method will throw an exception.
	 * 
	 * @param objectType The type of GL object whose name to generate
	 * @return The name of the newly-allocated object
	 */
	int createObject(GlObject.ObjectType objectType);

	/**
	 * @see #createObject(GlObject.ObjectType)
	 * @param stage The shader stage that this object will be associated with
	 * @return The name of the newly-created shader stage object
	 */
	int createShader(ShaderStage.Stage stage);

	void deleteObject(GlObject.ObjectType objectType, int id);

	void bindFramebuffer(int target, int id);

	void bindBuffer(GlBuffer.Type target, int id);

	void bindTexture(GlTexture.Type target, int id);

	void bindVertexArray(int id);

	void drawBuffers(int[] buffers);

	void useProgram(int id);

	void bindRenderbuffer(int id);

	void activeTexture(int unit);

	void enableCull(boolean enable);

	void enableBlend(boolean enable);

	void enableDepthTest(boolean enable);

	void enableLogicOp(boolean enable);

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

	void bufferData(GlBuffer.Type target, ByteBuffer data, GlBuffer.UsageHint usage);

	void enableProgramPointSize(boolean enable);

}
