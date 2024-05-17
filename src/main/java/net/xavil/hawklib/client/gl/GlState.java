package net.xavil.hawklib.client.gl;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryStack;

import net.xavil.hawklib.client.gl.texture.GlTexture;

public final class GlState implements GlStateSink {

	public static enum EnableFlag {
		ENABLED(GL45C.GL_TRUE, "Enabled"),
		DISABLED(GL45C.GL_FALSE, "Disabled");

		public final int id;
		public final String description;

		private EnableFlag(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static boolean needsSync(@Nullable EnableFlag flag, boolean requested) {
			return flag == null || (flag == ENABLED) != requested;
		}

		public static EnableFlag from(boolean enabled) {
			return enabled ? ENABLED : DISABLED;
		}
	}

	public static enum BlendEquation {
		ADD(GL45C.GL_FUNC_ADD, "Add"),
		SUBTRACT(GL45C.GL_FUNC_SUBTRACT, "Subtract"),
		REVERSE_SUBTRACT(GL45C.GL_FUNC_REVERSE_SUBTRACT, "Reverse Subtract"),
		MIN(GL45C.GL_MIN, "Minimum"),
		MAX(GL45C.GL_MAX, "Maximum");

		public final int id;
		public final String description;

		private BlendEquation(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static BlendEquation from(int id) {
			return switch (id) {
				case GL45C.GL_FUNC_ADD -> ADD;
				case GL45C.GL_FUNC_SUBTRACT -> SUBTRACT;
				case GL45C.GL_FUNC_REVERSE_SUBTRACT -> REVERSE_SUBTRACT;
				case GL45C.GL_MIN -> MIN;
				case GL45C.GL_MAX -> MAX;
				default -> null;
			};
		}
	}

	public static enum BlendFactor {
		ZERO(GL45C.GL_ZERO, "Zero"),
		ONE(GL45C.GL_ONE, "One"),
		SRC_COLOR(GL45C.GL_SRC_COLOR, "Source Color"),
		SRC_ALPHA(GL45C.GL_SRC_ALPHA, "Source Alpha"),
		DST_COLOR(GL45C.GL_DST_COLOR, "Destination Color"),
		DST_ALPHA(GL45C.GL_DST_ALPHA, "Destination Alpha"),
		CONSTANT_COLOR(GL45C.GL_CONSTANT_COLOR, "Constant Color"),
		CONSTANT_ALPHA(GL45C.GL_CONSTANT_ALPHA, "Constant Alpha"),
		ONE_MINUS_SRC_COLOR(GL45C.GL_ONE_MINUS_SRC_COLOR, "One Minus Source Color"),
		ONE_MINUS_SRC_ALPHA(GL45C.GL_ONE_MINUS_SRC_ALPHA, "One Minus Source Alpha"),
		ONE_MINUS_DST_COLOR(GL45C.GL_ONE_MINUS_DST_COLOR, "One Minus Destination Color"),
		ONE_MINUS_DST_ALPHA(GL45C.GL_ONE_MINUS_DST_ALPHA, "One Minus Destination Alpha"),
		ONE_MINUS_CONSTANT_COLOR(GL45C.GL_ONE_MINUS_CONSTANT_COLOR, "One Minus Constant Color"),
		ONE_MINUS_CONSTANT_ALPHA(GL45C.GL_ONE_MINUS_CONSTANT_ALPHA, "One Minus Constant Alpha"),
		SRC_ALPHA_SATURATE(GL45C.GL_SRC_ALPHA_SATURATE, "Alpha Saturate");

		public final int id;
		public final String description;

		private BlendFactor(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static BlendFactor from(int id) {
			return switch (id) {
				case GL45C.GL_ZERO -> ZERO;
				case GL45C.GL_ONE -> ONE;
				case GL45C.GL_SRC_COLOR -> SRC_COLOR;
				case GL45C.GL_SRC_ALPHA -> SRC_ALPHA;
				case GL45C.GL_DST_COLOR -> DST_COLOR;
				case GL45C.GL_DST_ALPHA -> DST_ALPHA;
				case GL45C.GL_CONSTANT_COLOR -> CONSTANT_COLOR;
				case GL45C.GL_CONSTANT_ALPHA -> CONSTANT_ALPHA;
				case GL45C.GL_ONE_MINUS_SRC_COLOR -> ONE_MINUS_SRC_COLOR;
				case GL45C.GL_ONE_MINUS_SRC_ALPHA -> ONE_MINUS_SRC_ALPHA;
				case GL45C.GL_ONE_MINUS_DST_COLOR -> ONE_MINUS_DST_COLOR;
				case GL45C.GL_ONE_MINUS_DST_ALPHA -> ONE_MINUS_DST_ALPHA;
				case GL45C.GL_ONE_MINUS_CONSTANT_COLOR -> ONE_MINUS_CONSTANT_COLOR;
				case GL45C.GL_ONE_MINUS_CONSTANT_ALPHA -> ONE_MINUS_CONSTANT_ALPHA;
				case GL45C.GL_SRC_ALPHA_SATURATE -> SRC_ALPHA_SATURATE;
				default -> null;
			};
		}
	}

	public static enum DepthFunc {
		NEVER(GL45C.GL_NEVER, "Never"),
		LESS(GL45C.GL_LESS, "Less"),
		EQUAL(GL45C.GL_EQUAL, "Equal"),
		LEQUAL(GL45C.GL_LEQUAL, "Less or Equal"),
		GREATER(GL45C.GL_GREATER, "Greater"),
		NOTEQUAL(GL45C.GL_NOTEQUAL, "Not Equal"),
		GEQUAL(GL45C.GL_GEQUAL, "Greater or Equal"),
		ALWAYS(GL45C.GL_ALWAYS, "Always");

		public final int id;
		public final String description;

		private DepthFunc(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static DepthFunc from(int id) {
			return switch (id) {
				case GL45C.GL_NEVER -> NEVER;
				case GL45C.GL_LESS -> LESS;
				case GL45C.GL_EQUAL -> EQUAL;
				case GL45C.GL_LEQUAL -> LEQUAL;
				case GL45C.GL_GREATER -> GREATER;
				case GL45C.GL_NOTEQUAL -> NOTEQUAL;
				case GL45C.GL_GEQUAL -> GEQUAL;
				case GL45C.GL_ALWAYS -> ALWAYS;
				default -> null;
			};
		}
	}

	public static enum FrontFace {
		CW(GL45C.GL_CW, "Clockwise"),
		CCW(GL45C.GL_CCW, "Counter-Clockwise");

		public final int id;
		public final String description;

		private FrontFace(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static FrontFace from(int id) {
			return switch (id) {
				case GL45C.GL_CW -> CW;
				case GL45C.GL_CCW -> CCW;
				default -> null;
			};
		}
	}

	public static enum CullFace {
		FRONT(GL45C.GL_FRONT, "Front"),
		BACK(GL45C.GL_BACK, "Back"),
		FRONT_AND_BACK(GL45C.GL_FRONT_AND_BACK, "Front and Back");

		public final int id;
		public final String description;

		private CullFace(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static CullFace from(int id) {
			return switch (id) {
				case GL45C.GL_FRONT -> FRONT;
				case GL45C.GL_BACK -> BACK;
				case GL45C.GL_FRONT_AND_BACK -> FRONT_AND_BACK;
				default -> null;
			};
		}
	}

	public static enum PolygonMode {
		POINT(GL45C.GL_POINT, "Point"),
		LINE(GL45C.GL_LINE, "Line"),
		FILL(GL45C.GL_FILL, "Fill");

		public final int id;
		public final String description;

		private PolygonMode(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static PolygonMode from(int id) {
			return switch (id) {
				case GL45C.GL_POINT -> POINT;
				case GL45C.GL_LINE -> LINE;
				case GL45C.GL_FILL -> FILL;
				default -> null;
			};
		}
	}

	public static enum LogicOp {
		CLEAR(GL45C.GL_CLEAR, "Clear"),
		SET(GL45C.GL_SET, "Set"),
		COPY(GL45C.GL_COPY, "Copy"),
		COPY_INVERTED(GL45C.GL_COPY_INVERTED, "Copy Inverted"),
		NOOP(GL45C.GL_NOOP, "No-op"),
		INVERT(GL45C.GL_INVERT, "Invert"),
		AND(GL45C.GL_AND, "AND"),
		NAND(GL45C.GL_NAND, "NAND"),
		OR(GL45C.GL_OR, "OR"),
		NOR(GL45C.GL_NOR, "NOR"),
		XOR(GL45C.GL_XOR, "XOR"),
		XNOR(GL45C.GL_EQUIV, "XNOR"),
		AND_REVERSE(GL45C.GL_AND_REVERSE, "AND Reverse"),
		AND_INVERTED(GL45C.GL_AND_INVERTED, "AND Inverted"),
		OR_REVERSE(GL45C.GL_OR_REVERSE, "OR Reverse"),
		OR_INVERTED(GL45C.GL_OR_INVERTED, "OR Inverted");

		public final int id;
		public final String description;

		private LogicOp(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static LogicOp from(int id) {
			return switch (id) {
				case GL45C.GL_CLEAR -> CLEAR;
				case GL45C.GL_SET -> SET;
				case GL45C.GL_COPY -> COPY;
				case GL45C.GL_COPY_INVERTED -> COPY_INVERTED;
				case GL45C.GL_NOOP -> NOOP;
				case GL45C.GL_INVERT -> INVERT;
				case GL45C.GL_AND -> AND;
				case GL45C.GL_NAND -> NAND;
				case GL45C.GL_OR -> OR;
				case GL45C.GL_NOR -> NOR;
				case GL45C.GL_XOR -> XOR;
				case GL45C.GL_EQUIV -> XNOR;
				case GL45C.GL_AND_REVERSE -> AND_REVERSE;
				case GL45C.GL_AND_INVERTED -> AND_INVERTED;
				case GL45C.GL_OR_REVERSE -> OR_REVERSE;
				case GL45C.GL_OR_INVERTED -> OR_INVERTED;
				default -> null;
			};
		}
	}

	public static final class ColorMask {
		public final boolean r, g, b, a;

		public ColorMask(boolean r, boolean g, boolean b, boolean a) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
		}
	}

	private static final UnmanagedStateSink UNMANAGED = UnmanagedStateSink.INSTANCE;

	public static final int MAX_TEXTURE_UNITS = Math.min(32, GlLimits.MAX_TEXTURE_IMAGE_UNITS);
	public static final int TEXTURE_TARGET_COUNT = GlTexture.Type.values().length;
	public static final int BUFFER_TARGET_COUNT = GlBuffer.Type.values().length;

	private GlState parentState = null;

	// culling
	private EnableFlag cullingEnabled = EnableFlag.DISABLED;
	private CullFace cullFace = CullFace.BACK;
	private FrontFace frontFace = FrontFace.CCW;

	// depth testing
	private EnableFlag depthMask = EnableFlag.ENABLED;
	private EnableFlag depthTestEnabled = EnableFlag.DISABLED;
	private DepthFunc depthFunc = DepthFunc.LESS;

	// blending
	private EnableFlag blendingEnabled = EnableFlag.DISABLED;
	private BlendEquation blendEquationRgb = BlendEquation.ADD;
	private BlendEquation blendEquationAlpha = BlendEquation.ADD;
	private BlendFactor blendFactorSrcRgb = BlendFactor.ONE;
	private BlendFactor blendFactorDstRgb = BlendFactor.ZERO;
	private BlendFactor blendFactorSrcAlpha = BlendFactor.ONE;
	private BlendFactor blendFactorDstAlpha = BlendFactor.ZERO;

	private EnableFlag colorMaskR = EnableFlag.ENABLED;
	private EnableFlag colorMaskG = EnableFlag.ENABLED;
	private EnableFlag colorMaskB = EnableFlag.ENABLED;
	private EnableFlag colorMaskA = EnableFlag.ENABLED;

	private EnableFlag logicOpEnabled = EnableFlag.DISABLED;
	private LogicOp logicOp = LogicOp.COPY;

	// other
	private PolygonMode polygonMode = PolygonMode.FILL;
	private EnableFlag programPointSizeEnabled = EnableFlag.DISABLED;

	// Integer.MIN_VALUE is a "vacant" sentinil value.
	private int viewportX = Integer.MIN_VALUE;
	private int viewportY = Integer.MIN_VALUE;
	// these are not the actual initial viewport width and height. the real values
	// here are the window width and height as they were when the context was
	// created.
	private int viewportWidth = Integer.MIN_VALUE;
	private int viewportHeight = Integer.MIN_VALUE;

	public final int[] boundBuffers = new int[BUFFER_TARGET_COUNT];
	public int boundVertexArray = Integer.MIN_VALUE;
	public int boundProgram = Integer.MIN_VALUE;
	public int boundDrawFramebuffer = Integer.MIN_VALUE;
	public int boundReadFramebuffer = Integer.MIN_VALUE;
	public int boundRenderbuffer = Integer.MIN_VALUE;
	public int boundTextureUnit = Integer.MIN_VALUE;

	public GlState() {
		for (int i = 0; i < this.boundBuffers.length; ++i) {
			this.boundBuffers[i] = Integer.MIN_VALUE;
		}
	}

	private static final GlState EMPTY_STATE = new GlState();

	public void reset(@Nullable GlState parent) {
		// clear the state if it's the root!
		if (parent == null) {
			copyStateFrom(EMPTY_STATE);
			return;
		}

		copyStateFrom(parent);
		this.parentState = parent;
	}

	private void copyStateFrom(GlState src) {
		this.parentState = src.parentState;
		this.polygonMode = src.polygonMode;
		this.cullingEnabled = src.cullingEnabled;
		this.cullFace = src.cullFace;
		this.frontFace = src.frontFace;
		this.depthMask = src.depthMask;
		this.depthTestEnabled = src.depthTestEnabled;
		this.depthFunc = src.depthFunc;
		this.blendingEnabled = src.blendingEnabled;
		this.blendEquationRgb = src.blendEquationRgb;
		this.blendEquationAlpha = src.blendEquationAlpha;
		this.blendFactorSrcRgb = src.blendFactorSrcRgb;
		this.blendFactorDstRgb = src.blendFactorDstRgb;
		this.blendFactorSrcAlpha = src.blendFactorSrcAlpha;
		this.blendFactorDstAlpha = src.blendFactorDstAlpha;
		this.colorMaskR = src.colorMaskR;
		this.colorMaskG = src.colorMaskG;
		this.colorMaskB = src.colorMaskB;
		this.colorMaskA = src.colorMaskA;
		this.logicOpEnabled = src.logicOpEnabled;
		this.logicOp = src.logicOp;
		for (int i = 0; i < BUFFER_TARGET_COUNT; ++i) {
			this.boundBuffers[i] = src.boundBuffers[i];
		}
		this.boundVertexArray = src.boundVertexArray;
		this.boundProgram = src.boundProgram;
		this.boundDrawFramebuffer = src.boundDrawFramebuffer;
		this.boundReadFramebuffer = src.boundReadFramebuffer;
		this.boundRenderbuffer = src.boundRenderbuffer;
		this.boundTextureUnit = src.boundTextureUnit;
	}

	/**
	 * Captures the current OpenGL state, storing it in this object. This is
	 * potentially a very expensive operation and should be avoided if possible.
	 */
	public void capture() {
		this.polygonMode = PolygonMode.from(GL45C.glGetInteger(GL45C.GL_POLYGON_MODE));
		this.cullingEnabled = EnableFlag.from(GL45C.glGetBoolean(GL45C.GL_CULL_FACE));
		this.cullFace = CullFace.from(GL45C.glGetInteger(GL45C.GL_CULL_FACE_MODE));
		this.frontFace = FrontFace.from(GL45C.glGetInteger(GL45C.GL_FRONT_FACE));
		this.depthTestEnabled = EnableFlag.from(GL45C.glGetBoolean(GL45C.GL_DEPTH_TEST));
		this.depthMask = EnableFlag.from(GL45C.glGetBoolean(GL45C.GL_DEPTH_WRITEMASK));
		this.depthFunc = DepthFunc.from(GL45C.glGetInteger(GL45C.GL_DEPTH_FUNC));
		this.blendingEnabled = EnableFlag.from(GL45C.glGetBoolean(GL45C.GL_BLEND));
		this.blendEquationRgb = BlendEquation.from(GL45C.glGetInteger(GL45C.GL_BLEND_EQUATION_RGB));
		this.blendEquationAlpha = BlendEquation.from(GL45C.glGetInteger(GL45C.GL_BLEND_EQUATION_ALPHA));
		this.blendFactorSrcRgb = BlendFactor.from(GL45C.glGetInteger(GL45C.GL_BLEND_SRC_RGB));
		this.blendFactorDstRgb = BlendFactor.from(GL45C.glGetInteger(GL45C.GL_BLEND_DST_RGB));
		this.blendFactorSrcAlpha = BlendFactor.from(GL45C.glGetInteger(GL45C.GL_BLEND_SRC_ALPHA));
		this.blendFactorDstAlpha = BlendFactor.from(GL45C.glGetInteger(GL45C.GL_BLEND_DST_ALPHA));
		this.logicOpEnabled = EnableFlag.from(GL45C.glGetBoolean(GL45C.GL_COLOR_LOGIC_OP));
		this.programPointSizeEnabled = EnableFlag.from(GL45C.glIsEnabled(GL45C.GL_PROGRAM_POINT_SIZE));

		try (final var stack = MemoryStack.stackPush()) {
			// we don't actually know what size GLboolean is... I can't find any way to
			// query its size, so we're just gonna hope its 1 byte and be on our way! D:
			final var buf = stack.malloc(4);
			GL45C.glGetBooleanv(GL45C.GL_COLOR_WRITEMASK, buf);
			this.colorMaskR = EnableFlag.from(buf.get(0) != GL45C.GL_FALSE);
			this.colorMaskG = EnableFlag.from(buf.get(1) != GL45C.GL_FALSE);
			this.colorMaskB = EnableFlag.from(buf.get(2) != GL45C.GL_FALSE);
			this.colorMaskA = EnableFlag.from(buf.get(3) != GL45C.GL_FALSE);
		}

		this.boundVertexArray = GL45C.glGetInteger(GL45C.GL_VERTEX_ARRAY_BINDING);
		this.boundProgram = GL45C.glGetInteger(GL45C.GL_CURRENT_PROGRAM);
		this.boundReadFramebuffer = GL45C.glGetInteger(GL45C.GL_READ_FRAMEBUFFER_BINDING);
		this.boundDrawFramebuffer = GL45C.glGetInteger(GL45C.GL_DRAW_FRAMEBUFFER_BINDING);
		this.boundRenderbuffer = GL45C.glGetInteger(GL45C.GL_RENDERBUFFER_BINDING);
		this.boundTextureUnit = GL45C.glGetInteger(GL45C.GL_ACTIVE_TEXTURE);

		for (final var target : GlBuffer.Type.values()) {
			this.boundBuffers[target.ordinal()] = GL45C.glGetInteger(target.bindingId);
		}

	}

	public void restorePrevious() {
		// @formatter:off
		if (this.polygonMode != this.parentState.polygonMode)
			UNMANAGED.polygonMode(this.parentState.polygonMode);
		if (this.cullingEnabled != this.parentState.cullingEnabled)
			UNMANAGED.enableCull(this.parentState.cullingEnabled != EnableFlag.DISABLED);
		if (this.cullFace != this.parentState.cullFace)
			UNMANAGED.cullFace(this.parentState.cullFace);
		if (this.frontFace != this.parentState.frontFace)
			UNMANAGED.frontFace(this.parentState.frontFace);
		if (this.depthMask != this.parentState.depthMask)
			UNMANAGED.depthMask(this.parentState.depthMask != EnableFlag.DISABLED);
		if (this.depthTestEnabled != this.parentState.depthTestEnabled)
			UNMANAGED.enableDepthTest(this.parentState.depthTestEnabled != EnableFlag.DISABLED);
		if (this.depthFunc != this.parentState.depthFunc)
			UNMANAGED.depthFunc(this.parentState.depthFunc);
		if (this.blendingEnabled != this.parentState.blendingEnabled)
			UNMANAGED.enableBlend(this.parentState.blendingEnabled != EnableFlag.DISABLED);

		if (this.blendEquationRgb != this.parentState.blendEquationRgb
				|| this.blendEquationAlpha != this.parentState.blendEquationAlpha)
			UNMANAGED.blendEquation(this.parentState.blendEquationRgb, this.parentState.blendEquationAlpha);

		if (this.blendFactorSrcRgb != this.parentState.blendFactorSrcRgb
				|| this.blendFactorDstRgb != this.parentState.blendFactorDstRgb
				|| this.blendFactorSrcAlpha != this.parentState.blendFactorSrcAlpha
				|| this.blendFactorDstAlpha != this.parentState.blendFactorDstAlpha)
			UNMANAGED.blendFunc(this.parentState.blendFactorSrcRgb, this.parentState.blendFactorDstRgb,
					this.parentState.blendFactorSrcAlpha, this.parentState.blendFactorDstAlpha);

		if (this.colorMaskR != this.parentState.colorMaskR
				|| this.colorMaskG != this.parentState.colorMaskG
				|| this.colorMaskB != this.parentState.colorMaskB
				|| this.colorMaskA != this.parentState.colorMaskA)
			UNMANAGED.colorMask(this.parentState.colorMaskR != EnableFlag.DISABLED,
					this.parentState.colorMaskG != EnableFlag.DISABLED,
					this.parentState.colorMaskB != EnableFlag.DISABLED,
					this.parentState.colorMaskA != EnableFlag.DISABLED);

		if (this.logicOpEnabled != this.parentState.logicOpEnabled)
			UNMANAGED.enableLogicOp(this.parentState.logicOpEnabled != EnableFlag.DISABLED);
		if (this.logicOp != this.parentState.logicOp)
			UNMANAGED.logicOp(this.parentState.logicOp);
		// @formatter:on

		// make sure we don't clobber VAO state here
		UNMANAGED.bindVertexArray(0);
		for (final var binding : GlBuffer.Type.values()) {
			final var parentBuffer = this.boundBuffers[binding.ordinal()];
			final var selfBuffer = this.boundBuffers[binding.ordinal()];
			if (parentBuffer != Integer.MIN_VALUE && parentBuffer != selfBuffer) {
				UNMANAGED.bindBuffer(binding, selfBuffer);
			}
		}

		// @formatter:off
		if (this.boundVertexArray != this.parentState.boundVertexArray)
			UNMANAGED.bindVertexArray(this.parentState.boundVertexArray);
		if (this.boundProgram != this.parentState.boundProgram)
			UNMANAGED.bindProgram(this.parentState.boundProgram);
		if (this.boundDrawFramebuffer != this.parentState.boundDrawFramebuffer)
			UNMANAGED.bindFramebuffer(GL45C.GL_DRAW_FRAMEBUFFER, this.parentState.boundDrawFramebuffer);
		if (this.boundReadFramebuffer != this.parentState.boundReadFramebuffer)
			UNMANAGED.bindFramebuffer(GL45C.GL_READ_FRAMEBUFFER, this.parentState.boundReadFramebuffer);
		if (this.boundRenderbuffer != this.parentState.boundRenderbuffer)
			UNMANAGED.bindRenderbuffer(this.parentState.boundRenderbuffer);
		if (this.boundTextureUnit != this.parentState.boundTextureUnit)
			UNMANAGED.bindTextureUnit(this.parentState.boundTextureUnit);
		// @formatter:on
	}

	@Override
	public void polygonMode(PolygonMode mode) {
		if (this.polygonMode != mode) {
			UNMANAGED.polygonMode(mode);
			this.polygonMode = mode;
		}
	}

	public PolygonMode getPolygonMode() {
		return this.polygonMode;
	}

	@Override
	public void enableCull(boolean enable) {
		if (EnableFlag.needsSync(this.cullingEnabled, enable)) {
			UNMANAGED.enableCull(enable);
			this.cullingEnabled = EnableFlag.from(enable);
		}
	}

	public boolean isCullEnabled() {
		return this.cullingEnabled == EnableFlag.ENABLED;
	}

	@Override
	public void cullFace(CullFace cullFace) {
		if (this.cullFace != cullFace) {
			UNMANAGED.cullFace(cullFace);
			this.cullFace = cullFace;
		}
	}

	public CullFace getCullFace() {
		return this.cullFace;
	}

	@Override
	public void frontFace(FrontFace frontFace) {
		if (this.frontFace != frontFace) {
			UNMANAGED.frontFace(frontFace);
			this.frontFace = frontFace;
		}
	}

	public FrontFace getFrontFace() {
		return this.frontFace;
	}

	@Override
	public void enableDepthTest(boolean enable) {
		if (EnableFlag.needsSync(this.depthTestEnabled, enable)) {
			UNMANAGED.enableDepthTest(enable);
			this.depthTestEnabled = EnableFlag.from(enable);
		}
	}

	public boolean isDepthTestEnabled() {
		return this.depthTestEnabled != EnableFlag.DISABLED;
	}

	@Override
	public void depthMask(boolean depthMask) {
		if (EnableFlag.needsSync(this.depthMask, depthMask)) {
			UNMANAGED.depthMask(depthMask);
			this.depthMask = EnableFlag.from(depthMask);
		}
	}

	public boolean isDepthMaskEnabled() {
		return this.depthMask != EnableFlag.DISABLED;
	}

	@Override
	public void depthFunc(DepthFunc depthFunc) {
		if (this.depthFunc != depthFunc) {
			UNMANAGED.depthFunc(depthFunc);
			this.depthFunc = depthFunc;
		}
	}

	public DepthFunc getDepthFunc() {
		return this.depthFunc;
	}

	@Override
	public void enableBlend(boolean enable) {
		if (EnableFlag.needsSync(this.blendingEnabled, enable)) {
			UNMANAGED.enableBlend(enable);
			this.blendingEnabled = EnableFlag.from(enable);
		}
	}

	public boolean isBlendEnabled() {
		return this.blendingEnabled != EnableFlag.DISABLED;
	}

	@Override
	public void blendEquation(BlendEquation blendEquationRgb, BlendEquation blendEquationAlpha) {
		if (this.blendEquationRgb != blendEquationRgb || this.blendEquationAlpha != blendEquationAlpha) {
			UNMANAGED.blendEquation(blendEquationRgb, blendEquationAlpha);
			this.blendEquationRgb = blendEquationRgb;
			this.blendEquationAlpha = blendEquationAlpha;
		}
	}

	public BlendEquation getBlendEquationRgb() {
		return this.blendEquationRgb;
	}

	public BlendEquation getBlendEquationAlpha() {
		return this.blendEquationAlpha;
	}

	@Override
	public void blendFunc(BlendFactor blendFactorSrcRgb, BlendFactor blendFactorDstRgb,
			BlendFactor blendFactorSrcAlpha, BlendFactor blendFactorDstAlpha) {
		if (this.blendFactorSrcRgb != blendFactorSrcRgb
				|| this.blendFactorDstRgb != blendFactorDstRgb
				|| this.blendFactorSrcAlpha != blendFactorSrcAlpha
				|| this.blendFactorDstAlpha != blendFactorDstAlpha) {
			UNMANAGED.blendFunc(blendFactorSrcRgb, blendFactorDstRgb,
					blendFactorSrcAlpha, blendFactorDstAlpha);
			this.blendFactorSrcRgb = blendFactorSrcRgb;
			this.blendFactorDstRgb = blendFactorDstRgb;
			this.blendFactorSrcAlpha = blendFactorSrcAlpha;
			this.blendFactorDstAlpha = blendFactorDstAlpha;
		}
	}

	public BlendFactor getBlendFactorSrcRgb() {
		return this.blendFactorSrcRgb;
	}

	public BlendFactor getBlendFactorDstRgb() {
		return this.blendFactorDstRgb;
	}

	public BlendFactor getBlendFactorSrcAlpha() {
		return this.blendFactorSrcAlpha;
	}

	public BlendFactor getBlendFactorDstAlpha() {
		return this.blendFactorDstAlpha;
	}

	@Override
	public void colorMask(boolean colorMaskR, boolean colorMaskG, boolean colorMaskB, boolean colorMaskA) {
		if (EnableFlag.needsSync(this.colorMaskR, colorMaskR)
				|| EnableFlag.needsSync(this.colorMaskG, colorMaskG)
				|| EnableFlag.needsSync(this.colorMaskB, colorMaskB)
				|| EnableFlag.needsSync(this.colorMaskA, colorMaskA)) {
			UNMANAGED.colorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
			this.colorMaskR = EnableFlag.from(colorMaskR);
			this.colorMaskG = EnableFlag.from(colorMaskG);
			this.colorMaskB = EnableFlag.from(colorMaskB);
			this.colorMaskA = EnableFlag.from(colorMaskA);
		}
	}

	public boolean isColorMaskREnabled() {
		return this.colorMaskR != EnableFlag.DISABLED;
	}

	public boolean isColorMaskGEnabled() {
		return this.colorMaskG != EnableFlag.DISABLED;
	}

	public boolean isColorMaskBEnabled() {
		return this.colorMaskB != EnableFlag.DISABLED;
	}

	public boolean isColorMaskAEnabled() {
		return this.colorMaskA != EnableFlag.DISABLED;
	}

	public ColorMask colorMask() {
		return new ColorMask(isColorMaskREnabled(),
				isColorMaskGEnabled(),
				isColorMaskBEnabled(),
				isColorMaskAEnabled());
	}

	@Override
	public void enableLogicOp(boolean enable) {
		if (EnableFlag.needsSync(this.logicOpEnabled, enable)) {
			UNMANAGED.enableLogicOp(enable);
			this.logicOpEnabled = EnableFlag.from(enable);
		}
	}

	public boolean isLogicOpEnabled() {
		return this.logicOpEnabled != EnableFlag.DISABLED;
	}

	@Override
	public void logicOp(LogicOp logicOp) {
		if (this.logicOp != logicOp) {
			UNMANAGED.logicOp(logicOp);
			this.logicOp = logicOp;
		}
	}

	public LogicOp getLogicOp() {
		if (this.logicOp == null)
			this.logicOp = LogicOp.from(GL45C.glGetInteger(GL45C.GL_LOGIC_OP_MODE));
		return this.logicOp;
	}

	@Override
	public void enableProgramPointSize(boolean enable) {
		if (EnableFlag.needsSync(this.programPointSizeEnabled, enable)) {
			UNMANAGED.enableProgramPointSize(enable);
			this.programPointSizeEnabled = EnableFlag.from(enable);
		}
	}

	public boolean isProgramPointSizeEnabled() {
		return this.programPointSizeEnabled != EnableFlag.DISABLED;
	}

	@Override
	public void bindFramebuffer(int target, int id) {
		final var setRead = target == GL45C.GL_FRAMEBUFFER || target == GL45C.GL_READ_FRAMEBUFFER;
		final var setDraw = target == GL45C.GL_FRAMEBUFFER || target == GL45C.GL_DRAW_FRAMEBUFFER;
		if (setRead && setDraw && (this.boundReadFramebuffer != id || this.boundDrawFramebuffer != id)) {
			UNMANAGED.bindFramebuffer(GL45C.GL_FRAMEBUFFER, id);
			this.boundReadFramebuffer = id;
			this.boundDrawFramebuffer = id;
		} else if (setRead && this.boundReadFramebuffer != id) {
			UNMANAGED.bindFramebuffer(GL45C.GL_READ_FRAMEBUFFER, id);
			this.boundReadFramebuffer = id;
		} else if (setDraw && this.boundDrawFramebuffer != id) {
			UNMANAGED.bindFramebuffer(GL45C.GL_DRAW_FRAMEBUFFER, id);
			this.boundDrawFramebuffer = id;
		}
	}

	public int getBoundFramebuffer(int target) {
		if (target == GL45C.GL_READ_FRAMEBUFFER) {
			return this.boundReadFramebuffer;
		} else if (target == GL45C.GL_DRAW_FRAMEBUFFER) {
			return this.boundDrawFramebuffer;
		}
		throw new IllegalArgumentException(String.format(
				"cannot query bound framebuffer, 0x%X is not a valid binding point.",
				target));
	}

	@Override
	public void bindBuffer(GlBuffer.Type target, int id) {
		if (this.boundBuffers[target.ordinal()] != id) {
			UNMANAGED.bindBuffer(target, id);
			this.boundBuffers[target.ordinal()] = id;
		}
	}

	public int getBoundBuffer(GlBuffer.Type target) {
		return this.boundBuffers[target.ordinal()];
	}

	@Override
	public void bindTexture(GlTexture.Type target, int id) {
		UNMANAGED.bindTexture(target, id);
	}

	@Override
	public void bindVertexArray(int id) {
		if (this.boundVertexArray != id) {
			UNMANAGED.bindVertexArray(id);
			this.boundVertexArray = id;
		}
	}

	public int getBoundVertexArray() {
		return this.boundVertexArray;
	}

	@Override
	public void bindProgram(int id) {
		if (this.boundProgram != id) {
			UNMANAGED.bindProgram(id);
			this.boundProgram = id;
		}
	}

	public int getBoundProgram() {
		return this.boundProgram;
	}

	@Override
	public void bindRenderbuffer(int id) {
		if (this.boundRenderbuffer != id) {
			UNMANAGED.bindRenderbuffer(id);
			this.boundRenderbuffer = id;
		}
	}

	public int getBoundRenderbuffer() {
		return this.boundRenderbuffer;
	}

	@Override
	public void bindTextureUnit(int unit) {
		if (unit - GL45C.GL_TEXTURE0 >= MAX_TEXTURE_UNITS) {
			throw new IllegalArgumentException(String.format(
					"cannot bind texture unit %d, a maximum of %d texture units are supported.",
					unit - GL45C.GL_TEXTURE0, MAX_TEXTURE_UNITS));
		}
		if (this.boundTextureUnit != unit) {
			UNMANAGED.bindTextureUnit(unit);
			this.boundTextureUnit = unit;
		}
	}

	public int getBoundTextureUnit() {
		return this.boundTextureUnit;
	}

	@Override
	public void setViewport(int x, int y, int w, int h) {
		if (this.viewportX != x || this.viewportY != y || this.viewportWidth != w || this.viewportHeight != h) {
			UNMANAGED.setViewport(x, y, w, h);
			this.viewportX = x;
			this.viewportY = y;
			this.viewportWidth = w;
			this.viewportHeight = h;
		}
	}

}
