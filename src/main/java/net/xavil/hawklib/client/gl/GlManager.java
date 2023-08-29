package net.xavil.hawklib.client.gl;

import com.mojang.blaze3d.vertex.BufferUploader;

import net.minecraft.client.renderer.ShaderInstance;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;

public final class GlManager {

	public static final GlManager INSTANCE = new GlManager();

	private final MutableList<GlState> stack = new Vector<>();
	private final MutableList<GlState> freeStates = new Vector<>();

	// the OpenGL state that needs to be restored after we pop our final state.
	private GlState rootState = null;
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
			// vanilla tracks some state in random places so that it can deduplicate binds,
			// which need to be reset to make them always re-bind after we do stuff.
			//
			// im not sure why the last pop() doesn't restore all the state correctly,
			// though...
			BufferUploader.reset();
			ShaderInstance.lastProgramId = -1;

			this.rootState = fetchNewState();
			this.rootState.reset(null);
			this.rootState.sync();
		}

		final var newState = fetchNewState();
		newState.reset(this.current == null ? this.rootState : this.stack.last().unwrap());
		this.stack.push(newState);
		this.current = newState;
		this.currentSink = this.current;
	}

	private void pop() {
		final var prev = this.stack.pop().unwrap();
		prev.restorePrevious();

		if (this.stack.isEmpty()) {
			this.current = null;
			this.currentSink = UnmanagedStateSink.INSTANCE;
			this.freeStates.push(this.rootState);
			this.rootState = null;
		} else {
			this.currentSink = this.current = this.stack.last().unwrap();
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
		INSTANCE.currentSink.bindProgram(id);
	}

	public static void bindRenderbuffer(int id) {
		INSTANCE.currentSink.bindRenderbuffer(id);
	}

	public static void activeTexture(int unit) {
		INSTANCE.currentSink.bindTextureUnit(unit);
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

	public static void enableProgramPointSize(boolean enable) {
		INSTANCE.currentSink.enableProgramPointSize(enable);
	}
}
