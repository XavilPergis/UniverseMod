package net.xavil.ultraviolet.client.gl;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.platform.GlStateManager;

import net.xavil.ultraviolet.client.gl.shader.ShaderStage;
import net.xavil.ultraviolet.client.gl.texture.GlTexture;
import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.MutableList;

public final class GlManager {

	public static final GlManager INSTANCE = new GlManager();

	private final MutableList<GlState> stack = new Vector<>();
	private final MutableList<GlState> freeStates = new Vector<>();

	// the OpenGL state that needs to be restored after we pop our final state.
	private GlState unmanagedState = null;
	private GlState current = null;
	private GlStateSink currentSink = UnmanagedStateSink.INSTANCE;

	private GlManager() {
	}

	private GlState fetchNewState() {
		if (!this.freeStates.isEmpty()) {
			return this.freeStates.pop().unwrap();
		} else {
			return new GlState();
		}
	}

	private void push() {
		if (this.current == null) {
			this.unmanagedState = fetchNewState();
			this.unmanagedState.sync();
		}
		final var newState = fetchNewState();
		newState.copyStateFrom(this.current == null ? this.unmanagedState : this.stack.last().unwrap());
		this.stack.push(newState);
		this.current = newState;
		this.currentSink = this.current;
	}

	private void pop() {
		final var prev = this.stack.pop().unwrap();
		if (this.stack.isEmpty()) {
			this.current = null;
			this.currentSink = UnmanagedStateSink.INSTANCE;
			this.unmanagedState.restore(prev);
			this.unmanagedState = null;
		} else {
			this.current = this.stack.last().unwrap();
			this.current.restore(prev);
			this.currentSink = this.current;
		}
		this.freeStates.push(prev);
	}

	public static GlState currentState() {
		if (INSTANCE.current == null) {
			throw new IllegalStateException("Cannot access the current OpenGL state because GlManager is not active!");
		}
		return INSTANCE.current;
	}

	public static boolean isManaged() {
		return INSTANCE.current != null;
	}

	public static void pushState() {
		INSTANCE.push();
	}

	public static void popState() {
		INSTANCE.pop();
	}

	public static int createObject(GlObject.ObjectType objectType) {
		return INSTANCE.currentSink.createObject(objectType);
	}

	public static void deleteObject(GlObject.ObjectType objectType, int id) {
		INSTANCE.currentSink.deleteObject(objectType, id);
	}

	public static int createBuffer() {
		return createObject(GlObject.ObjectType.BUFFER);
	}

	public static void deleteBuffer(int id) {
		deleteObject(GlObject.ObjectType.BUFFER, id);
	}

	public static int createFramebuffer() {
		return createObject(GlObject.ObjectType.FRAMEBUFFER);
	}

	public static void deleteFramebuffer(int id) {
		deleteObject(GlObject.ObjectType.FRAMEBUFFER, id);
	}

	public static int createRenderbuffer() {
		return createObject(GlObject.ObjectType.RENDERBUFFER);
	}

	public static void deleteRenderbuffer(int id) {
		deleteObject(GlObject.ObjectType.RENDERBUFFER, id);
	}

	public static int createProgram() {
		return createObject(GlObject.ObjectType.PROGRAM);
	}

	public static void deleteProgram(int id) {
		deleteObject(GlObject.ObjectType.PROGRAM, id);
	}

	public static int createVertexArray() {
		return createObject(GlObject.ObjectType.VERTEX_ARRAY);
	}

	public static void deleteVertexArray(int id) {
		deleteObject(GlObject.ObjectType.VERTEX_ARRAY, id);
	}

	public static int createTexture() {
		return createObject(GlObject.ObjectType.TEXTURE);
	}

	public static void deleteTexture(int id) {
		deleteObject(GlObject.ObjectType.TEXTURE, id);
	}

	public static int createShader(ShaderStage.Stage stage) {
		return INSTANCE.currentSink.createShader(stage);
	}

	public static void deleteShader(int id) {
		deleteObject(GlObject.ObjectType.SHADER, id);
	}

	public static void bindFramebuffer(int target, int id) {
		INSTANCE.currentSink.bindFramebuffer(target, id);
	}

	public static void bindBuffer(GlBuffer.Type target, int id) {
		INSTANCE.currentSink.bindBuffer(target, id);
	}

	public static void bindTexture(GlTexture.Type target, int id) {
		INSTANCE.currentSink.bindTexture(target, id);
	}

	public static void bindVertexArray(int id) {
		INSTANCE.currentSink.bindVertexArray(id);
	}

	public static void useProgram(int id) {
		INSTANCE.currentSink.useProgram(id);
	}

	public static void bindRenderbuffer(int id) {
		INSTANCE.currentSink.bindRenderbuffer(id);
	}

	public static void activeTexture(int unit) {
		INSTANCE.currentSink.activeTexture(unit);
	}

	public static void enableCull(boolean enable) {
		INSTANCE.currentSink.enableCull(enable);
	}

	public static void enableBlend(boolean enable) {
		INSTANCE.currentSink.enableBlend(enable);
	}

	public static void enableDepthTest(boolean enable) {
		INSTANCE.currentSink.enableDepthTest(enable);
	}

	public static void enableLogicOp(boolean enable) {
		INSTANCE.currentSink.enableLogicOp(enable);
	}

	public static void polygonMode(GlState.PolygonMode mode) {
		INSTANCE.currentSink.polygonMode(mode);
	}

	public static void cullFace(GlState.CullFace cullFace) {
		INSTANCE.currentSink.cullFace(cullFace);
	}

	public static void frontFace(GlState.FrontFace frontFace) {
		INSTANCE.currentSink.frontFace(frontFace);
	}

	public static void depthMask(boolean depthMask) {
		INSTANCE.currentSink.depthMask(depthMask);
	}

	public static void depthFunc(GlState.DepthFunc depthFunc) {
		INSTANCE.currentSink.depthFunc(depthFunc);
	}

	public static void logicOp(GlState.LogicOp logicOp) {
		INSTANCE.currentSink.logicOp(logicOp);
	}

	public static void blendEquation(GlState.BlendEquation blendEquationRgb, GlState.BlendEquation blendEquationAlpha) {
		INSTANCE.currentSink.blendEquation(blendEquationRgb, blendEquationAlpha);
	}

	public static void blendFunc(GlState.BlendFactor blendFactorSrc, GlState.BlendFactor blendFactorDst) {
		INSTANCE.currentSink.blendFunc(blendFactorSrc, blendFactorDst, blendFactorSrc, blendFactorDst);
	}

	public static void blendFunc(GlState.BlendFactor blendFactorSrcRgb, GlState.BlendFactor blendFactorDstRgb,
			GlState.BlendFactor blendFactorSrcAlpha, GlState.BlendFactor blendFactorDstAlpha) {
		INSTANCE.currentSink.blendFunc(blendFactorSrcRgb, blendFactorDstRgb, blendFactorSrcAlpha, blendFactorDstAlpha);
	}

	public static void colorMask(boolean colorMaskR, boolean colorMaskG, boolean colorMaskB, boolean colorMaskA) {
		INSTANCE.currentSink.colorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
	}

	public static void setViewport(int x, int y, int w, int h) {
		INSTANCE.currentSink.setViewport(x, y, w, h);
	}

	public static void drawBuffers(int[] buffers) {
		INSTANCE.currentSink.drawBuffers(buffers);
	}
	
	public static void bufferData(GlBuffer.Type target, ByteBuffer data, GlBuffer.UsageHint usage) {
		INSTANCE.currentSink.bufferData(target, data, usage);
	}
}
