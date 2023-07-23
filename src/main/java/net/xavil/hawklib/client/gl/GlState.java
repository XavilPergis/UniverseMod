package net.xavil.hawklib.client.gl;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.hawklib.client.gl.shader.ShaderStage;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture.Type;

public final class GlState implements GlStateSink {

	private static final UnmanagedStateSink UNMANAGED = UnmanagedStateSink.INSTANCE;

	public static enum EnableFlag {
		ENABLED(GL32C.GL_TRUE, "Enabled"),
		DISABLED(GL32C.GL_FALSE, "Disabled");

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
		ADD(GL32C.GL_FUNC_ADD, "Add"),
		SUBTRACT(GL32C.GL_FUNC_SUBTRACT, "Subtract"),
		REVERSE_SUBTRACT(GL32C.GL_FUNC_REVERSE_SUBTRACT, "Reverse Subtract"),
		MIN(GL32C.GL_MIN, "Minimum"),
		MAX(GL32C.GL_MAX, "Maximum");

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
				case GL32C.GL_FUNC_ADD -> ADD;
				case GL32C.GL_FUNC_SUBTRACT -> SUBTRACT;
				case GL32C.GL_FUNC_REVERSE_SUBTRACT -> REVERSE_SUBTRACT;
				case GL32C.GL_MIN -> MIN;
				case GL32C.GL_MAX -> MAX;
				default -> null;
			};
		}
	}

	public static enum BlendFactor {
		ZERO(GL32C.GL_ZERO, "Zero"),
		ONE(GL32C.GL_ONE, "One"),
		SRC_COLOR(GL32C.GL_SRC_COLOR, "Source Color"),
		SRC_ALPHA(GL32C.GL_SRC_ALPHA, "Source Alpha"),
		DST_COLOR(GL32C.GL_DST_COLOR, "Destination Color"),
		DST_ALPHA(GL32C.GL_DST_ALPHA, "Destination Alpha"),
		CONSTANT_COLOR(GL32C.GL_CONSTANT_COLOR, "Constant Color"),
		CONSTANT_ALPHA(GL32C.GL_CONSTANT_ALPHA, "Constant Alpha"),
		ONE_MINUS_SRC_COLOR(GL32C.GL_ONE_MINUS_SRC_COLOR, "One Minus Source Color"),
		ONE_MINUS_SRC_ALPHA(GL32C.GL_ONE_MINUS_SRC_ALPHA, "One Minus Source Alpha"),
		ONE_MINUS_DST_COLOR(GL32C.GL_ONE_MINUS_DST_COLOR, "One Minus Destination Color"),
		ONE_MINUS_DST_ALPHA(GL32C.GL_ONE_MINUS_DST_ALPHA, "One Minus Destination Alpha"),
		ONE_MINUS_CONSTANT_COLOR(GL32C.GL_ONE_MINUS_CONSTANT_COLOR, "One Minus Constant Color"),
		ONE_MINUS_CONSTANT_ALPHA(GL32C.GL_ONE_MINUS_CONSTANT_ALPHA, "One Minus Constant Alpha"),
		SRC_ALPHA_SATURATE(GL32C.GL_SRC_ALPHA_SATURATE, "Alpha Saturate");

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
				case GL32C.GL_ZERO -> ZERO;
				case GL32C.GL_ONE -> ONE;
				case GL32C.GL_SRC_COLOR -> SRC_COLOR;
				case GL32C.GL_SRC_ALPHA -> SRC_ALPHA;
				case GL32C.GL_DST_COLOR -> DST_COLOR;
				case GL32C.GL_DST_ALPHA -> DST_ALPHA;
				case GL32C.GL_CONSTANT_COLOR -> CONSTANT_COLOR;
				case GL32C.GL_CONSTANT_ALPHA -> CONSTANT_ALPHA;
				case GL32C.GL_ONE_MINUS_SRC_COLOR -> ONE_MINUS_SRC_COLOR;
				case GL32C.GL_ONE_MINUS_SRC_ALPHA -> ONE_MINUS_SRC_ALPHA;
				case GL32C.GL_ONE_MINUS_DST_COLOR -> ONE_MINUS_DST_COLOR;
				case GL32C.GL_ONE_MINUS_DST_ALPHA -> ONE_MINUS_DST_ALPHA;
				case GL32C.GL_ONE_MINUS_CONSTANT_COLOR -> ONE_MINUS_CONSTANT_COLOR;
				case GL32C.GL_ONE_MINUS_CONSTANT_ALPHA -> ONE_MINUS_CONSTANT_ALPHA;
				case GL32C.GL_SRC_ALPHA_SATURATE -> SRC_ALPHA_SATURATE;
				default -> null;
			};
		}
	}

	public static enum DepthFunc {
		NEVER(GL32C.GL_NEVER, "Never"),
		LESS(GL32C.GL_LESS, "Less"),
		EQUAL(GL32C.GL_EQUAL, "Equal"),
		LEQUAL(GL32C.GL_LEQUAL, "Less or Equal"),
		GREATER(GL32C.GL_GREATER, "Greater"),
		NOTEQUAL(GL32C.GL_NOTEQUAL, "Not Equal"),
		GEQUAL(GL32C.GL_GEQUAL, "Greater or Equal"),
		ALWAYS(GL32C.GL_ALWAYS, "Always");

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
				case GL32C.GL_NEVER -> NEVER;
				case GL32C.GL_LESS -> LESS;
				case GL32C.GL_EQUAL -> EQUAL;
				case GL32C.GL_LEQUAL -> LEQUAL;
				case GL32C.GL_GREATER -> GREATER;
				case GL32C.GL_NOTEQUAL -> NOTEQUAL;
				case GL32C.GL_GEQUAL -> GEQUAL;
				case GL32C.GL_ALWAYS -> ALWAYS;
				default -> null;
			};
		}
	}

	public static enum FrontFace {
		CW(GL32C.GL_CW, "Clockwise"),
		CCW(GL32C.GL_CCW, "Counter-Clockwise");

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
				case GL32C.GL_CW -> CW;
				case GL32C.GL_CCW -> CCW;
				default -> null;
			};
		}
	}

	public static enum CullFace {
		FRONT(GL32C.GL_FRONT, "Front"),
		BACK(GL32C.GL_BACK, "Back"),
		FRONT_AND_BACK(GL32C.GL_FRONT_AND_BACK, "Front and Back");

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
				case GL32C.GL_FRONT -> FRONT;
				case GL32C.GL_BACK -> BACK;
				case GL32C.GL_FRONT_AND_BACK -> FRONT_AND_BACK;
				default -> null;
			};
		}
	}

	public static enum PolygonMode {
		POINT(GL32C.GL_POINT, "Point"),
		LINE(GL32C.GL_LINE, "Line"),
		FILL(GL32C.GL_FILL, "Fill");

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
				case GL32C.GL_POINT -> POINT;
				case GL32C.GL_LINE -> LINE;
				case GL32C.GL_FILL -> FILL;
				default -> null;
			};
		}
	}

	public static enum LogicOp {
		CLEAR(GL32C.GL_CLEAR, "Clear"),
		SET(GL32C.GL_SET, "Set"),
		COPY(GL32C.GL_COPY, "Copy"),
		COPY_INVERTED(GL32C.GL_COPY_INVERTED, "Copy Inverted"),
		NOOP(GL32C.GL_NOOP, "No-op"),
		INVERT(GL32C.GL_INVERT, "Invert"),
		AND(GL32C.GL_AND, "AND"),
		NAND(GL32C.GL_NAND, "NAND"),
		OR(GL32C.GL_OR, "OR"),
		NOR(GL32C.GL_NOR, "NOR"),
		XOR(GL32C.GL_XOR, "XOR"),
		XNOR(GL32C.GL_EQUIV, "XNOR"),
		AND_REVERSE(GL32C.GL_AND_REVERSE, "AND Reverse"),
		AND_INVERTED(GL32C.GL_AND_INVERTED, "AND Inverted"),
		OR_REVERSE(GL32C.GL_OR_REVERSE, "OR Reverse"),
		OR_INVERTED(GL32C.GL_OR_INVERTED, "OR Inverted");

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
				case GL32C.GL_CLEAR -> CLEAR;
				case GL32C.GL_SET -> SET;
				case GL32C.GL_COPY -> COPY;
				case GL32C.GL_COPY_INVERTED -> COPY_INVERTED;
				case GL32C.GL_NOOP -> NOOP;
				case GL32C.GL_INVERT -> INVERT;
				case GL32C.GL_AND -> AND;
				case GL32C.GL_NAND -> NAND;
				case GL32C.GL_OR -> OR;
				case GL32C.GL_NOR -> NOR;
				case GL32C.GL_XOR -> XOR;
				case GL32C.GL_EQUIV -> XNOR;
				case GL32C.GL_AND_REVERSE -> AND_REVERSE;
				case GL32C.GL_AND_INVERTED -> AND_INVERTED;
				case GL32C.GL_OR_REVERSE -> OR_REVERSE;
				case GL32C.GL_OR_INVERTED -> OR_INVERTED;
				default -> null;
			};
		}
	}

	public static final int MAX_TEXTURE_UNITS = Math.min(32, GlLimits.MAX_TEXTURE_IMAGE_UNITS);
	public static final int TEXTURE_TARGET_COUNT = GlTexture.Type.values().length;
	public static final int BUFFER_TARGET_COUNT = GlBuffer.Type.values().length;

	private GlState parentState = null;

	private PolygonMode polygonMode = null;

	// culling
	private EnableFlag cullingEnabled = null; // default: EnableFlag.DISABLED
	private CullFace cullFace = null; // default: CullFace.BACK
	private FrontFace frontFace = null; // default: FrontFace.CCW

	// depth testing
	private EnableFlag depthMask = null; // default: EnableFlag.ENABLED
	private EnableFlag depthTestEnabled = null; // default: EnableFlag.DISABLED
	private DepthFunc depthFunc = null; // default: DepthFunc.LESS

	// blending
	private EnableFlag blendingEnabled = null; // default: EnableFlag.DISABLED
	private BlendEquation blendEquationRgb = null; // default: BlendEquation.ADD
	private BlendEquation blendEquationAlpha = null; // default: BlendEquation.ADD
	private BlendFactor blendFactorSrcRgb = null; // default: BlendFactor.ONE
	private BlendFactor blendFactorDstRgb = null; // default: BlendFactor.ZERO
	private BlendFactor blendFactorSrcAlpha = null; // default: BlendFactor.ONE
	private BlendFactor blendFactorDstAlpha = null; // default: BlendFactor.ZERO

	private EnableFlag colorMaskR = null; // default: EnableFlag.ENABLED
	private EnableFlag colorMaskG = null; // default: EnableFlag.ENABLED
	private EnableFlag colorMaskB = null; // default: EnableFlag.ENABLED
	private EnableFlag colorMaskA = null; // default: EnableFlag.ENABLED

	private EnableFlag logicOpEnabled = null; // default: EnableFlag.DISABLED
	private LogicOp logicOp = null; // default: LogicOp.COPY

	// Integer.MIN_VALUE is a "vacant" sentinil value.
	private int viewportX = Integer.MIN_VALUE;
	private int viewportY = Integer.MIN_VALUE;
	// these are not the actual initial viewport width and height. the real values
	// here are the window width and height as they were when the context was
	// created.
	private int viewportWidth = Integer.MIN_VALUE;
	private int viewportHeight = Integer.MIN_VALUE;

	public final class TextureUnit {
		public final int textureUnitId;
		// each bit represents one texture target. if a bit is 1, then the texture bound
		// to the corresponding texture target differs from the binding as it was then
		// this state was captured. This is used to quickly skip syncing texture units
		// that have not had their bindings changed.
		public int bindingRestoreMask = 0;
		public final int[] boundTextures = new int[TEXTURE_TARGET_COUNT];

		public TextureUnit(int textureUnitId) {
			this.textureUnitId = textureUnitId;
			for (int i = 0; i < this.boundTextures.length; ++i) {
				this.boundTextures[i] = Integer.MIN_VALUE;
			}
		}

		public void setBinding(int target, int id) {
			this.boundTextures[target] = id;
			this.bindingRestoreMask &= ~(1 << target);
			if (parentState.textureUnits[this.textureUnitId].boundTextures[target] != id)
				this.bindingRestoreMask |= 1 << target;
		}

		public void copyStateFrom(TextureUnit src) {
			this.bindingRestoreMask = 0;
			for (int i = 0; i < TEXTURE_TARGET_COUNT; ++i) {
				this.boundTextures[i] = src.boundTextures[i];
				// this.prevTextures[i] = src.boundTextures[i];
			}
		}
	}

	// NOTE: vanilla only ever uses GL_TEXTURE_2D, so it doesnt need to track each
	// target for each unit, and instead just tracks that specific binding per unit.
	public final TextureUnit[] textureUnits = new TextureUnit[MAX_TEXTURE_UNITS];

	public final int[] boundBuffers = new int[BUFFER_TARGET_COUNT];
	public int boundVertexArray = Integer.MIN_VALUE;
	public int boundProgram = Integer.MIN_VALUE;
	public int boundDrawFramebuffer = Integer.MIN_VALUE;
	public int boundReadFramebuffer = Integer.MIN_VALUE;
	public int boundRenderbuffer = Integer.MIN_VALUE;
	public int boundTextureUnit = Integer.MIN_VALUE;

	public GlState() {
		for (int i = 0; i < MAX_TEXTURE_UNITS; ++i) {
			this.textureUnits[i] = new TextureUnit(i);
		}
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
		for (int i = 0; i < MAX_TEXTURE_UNITS; ++i) {
			this.textureUnits[i].copyStateFrom(src.textureUnits[i]);
		}
		this.boundVertexArray = src.boundVertexArray;
		this.boundProgram = src.boundProgram;
		this.boundDrawFramebuffer = src.boundDrawFramebuffer;
		this.boundReadFramebuffer = src.boundReadFramebuffer;
		this.boundRenderbuffer = src.boundRenderbuffer;
		this.boundTextureUnit = src.boundTextureUnit;
	}

	public void restorePrevious() {
		// @formatter:off
		if (this.parentState.polygonMode != null && this.polygonMode != this.parentState.polygonMode)
			UNMANAGED.polygonMode(this.parentState.polygonMode);
		if (this.parentState.cullingEnabled != null && this.cullingEnabled != this.parentState.cullingEnabled)
			UNMANAGED.enableCull(this.parentState.cullingEnabled != EnableFlag.DISABLED);
		if (this.parentState.cullFace != null && this.cullFace != this.parentState.cullFace)
			UNMANAGED.cullFace(this.parentState.cullFace);
		if (this.parentState.frontFace != null && this.frontFace != this.parentState.frontFace)
			UNMANAGED.frontFace(this.parentState.frontFace);
		if (this.parentState.depthMask != null && this.depthMask != this.parentState.depthMask)
			UNMANAGED.depthMask(this.parentState.depthMask != EnableFlag.DISABLED);
		if (this.parentState.depthTestEnabled != null && this.depthTestEnabled != this.parentState.depthTestEnabled)
			UNMANAGED.enableDepthTest(this.parentState.depthTestEnabled != EnableFlag.DISABLED);
		if (this.parentState.depthFunc != null && this.depthFunc != this.parentState.depthFunc)
			UNMANAGED.depthFunc(this.parentState.depthFunc);
		if (this.parentState.blendingEnabled != null && this.blendingEnabled != this.parentState.blendingEnabled)
			UNMANAGED.enableBlend(this.parentState.blendingEnabled != EnableFlag.DISABLED);

		if ((this.parentState.blendEquationRgb != null && this.blendEquationRgb != this.parentState.blendEquationRgb)
				|| (this.parentState.blendEquationAlpha != null && this.blendEquationAlpha != this.parentState.blendEquationAlpha))
			UNMANAGED.blendEquation(this.parentState.blendEquationRgb, this.parentState.blendEquationAlpha);

		if ((this.parentState.blendFactorSrcRgb != null && this.blendFactorSrcRgb != this.parentState.blendFactorSrcRgb)
				|| (this.parentState.blendFactorDstRgb != null && this.blendFactorDstRgb != this.parentState.blendFactorDstRgb)
				|| (this.parentState.blendFactorSrcAlpha != null && this.blendFactorSrcAlpha != this.parentState.blendFactorSrcAlpha)
				|| (this.parentState.blendFactorDstAlpha != null && this.blendFactorDstAlpha != this.parentState.blendFactorDstAlpha))
			UNMANAGED.blendFunc(this.parentState.blendFactorSrcRgb, this.parentState.blendFactorDstRgb,
					this.parentState.blendFactorSrcAlpha, this.parentState.blendFactorDstAlpha);

		if ((this.parentState.colorMaskR != null && this.colorMaskR != this.parentState.colorMaskR)
				|| (this.parentState.colorMaskG != null && this.colorMaskG != this.parentState.colorMaskG)
				|| (this.parentState.colorMaskB != null && this.colorMaskB != this.parentState.colorMaskB)
				|| (this.parentState.colorMaskA != null && this.colorMaskA != this.parentState.colorMaskA))
			UNMANAGED.colorMask(this.parentState.colorMaskR != EnableFlag.DISABLED,
					this.parentState.colorMaskG != EnableFlag.DISABLED,
					this.parentState.colorMaskB != EnableFlag.DISABLED,
					this.parentState.colorMaskA != EnableFlag.DISABLED);

		if (this.parentState.logicOpEnabled != null && this.logicOpEnabled != this.parentState.logicOpEnabled)
			UNMANAGED.enableLogicOp(this.parentState.logicOpEnabled != EnableFlag.DISABLED);
		if (this.parentState.logicOp != null && this.logicOp != this.parentState.logicOp)
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

		for (var i = 0; i < MAX_TEXTURE_UNITS; ++i) {
			final var unit = this.textureUnits[i];
			final var parentUnit = this.parentState.textureUnits[i];
			if (unit.bindingRestoreMask == 0)
				continue;
			// this will be cleaned up later in the function.
			UNMANAGED.activeTexture(GL32C.GL_TEXTURE0 + i);
			this.boundTextureUnit = GL32C.GL_TEXTURE0 + i;
			for (final var binding : GlTexture.Type.values()) {
				final var prev = parentUnit.boundTextures[binding.ordinal()];
				final var cur = unit.boundTextures[binding.ordinal()];
				if (prev != cur)
					UNMANAGED.bindTexture(binding, prev);
			}
		}

		// @formatter:off
		if (this.parentState.boundVertexArray != Integer.MIN_VALUE && this.boundVertexArray != this.parentState.boundVertexArray)
			UNMANAGED.bindVertexArray(this.parentState.boundVertexArray);
		if (this.parentState.boundProgram != Integer.MIN_VALUE && this.boundProgram != this.parentState.boundProgram)
			UNMANAGED.useProgram(this.parentState.boundProgram);
		if (this.parentState.boundDrawFramebuffer != Integer.MIN_VALUE && this.boundDrawFramebuffer != this.parentState.boundDrawFramebuffer)
			UNMANAGED.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, this.parentState.boundDrawFramebuffer);
		if (this.parentState.boundReadFramebuffer != Integer.MIN_VALUE && this.boundReadFramebuffer != this.parentState.boundReadFramebuffer)
			UNMANAGED.bindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, this.parentState.boundReadFramebuffer);
		if (this.parentState.boundRenderbuffer != Integer.MIN_VALUE && this.boundRenderbuffer != this.parentState.boundRenderbuffer)
			UNMANAGED.bindRenderbuffer(this.parentState.boundRenderbuffer);
		if (this.parentState.boundTextureUnit != Integer.MIN_VALUE && this.boundTextureUnit != this.parentState.boundTextureUnit)
			UNMANAGED.activeTexture(this.parentState.boundTextureUnit);
		// @formatter:on
	}

	@Override
	public int createObject(GlObject.ObjectType objectType) {
		return UNMANAGED.createObject(objectType);
	}

	@Override
	public int createShader(ShaderStage.Stage stage) {
		return UNMANAGED.createShader(stage);
	}

	@Override
	public void deleteObject(GlObject.ObjectType objectType, int id) {
		if (objectType == GlObject.ObjectType.TEXTURE) {
			// this is a bit pricy... hopefully it will not be hit very often, though
			for (int i = 0; i < MAX_TEXTURE_UNITS; ++i) {
				final var unit = this.textureUnits[i];
				for (int j = 0; j < TEXTURE_TARGET_COUNT; ++j) {
					if (unit.boundTextures[j] == id)
						unit.setBinding(j, 0);
				}
			}
		}
		UNMANAGED.deleteObject(objectType, id);
	}

	@Override
	public void polygonMode(PolygonMode mode) {
		if (this.parentState.polygonMode == null) {
			this.parentState.polygonMode = getPolygonMode();
		}
		if (this.polygonMode != mode) {
			UNMANAGED.polygonMode(mode);
			this.polygonMode = mode;
		}
	}

	public PolygonMode getPolygonMode() {
		if (this.polygonMode == null)
			this.polygonMode = PolygonMode.from(GL32C.glGetInteger(GL32C.GL_POLYGON_MODE));
		return this.polygonMode;
	}

	@Override
	public void enableCull(boolean enable) {
		if (this.parentState.cullingEnabled == null) {
			this.parentState.cullingEnabled = EnableFlag.from(isCullEnabled());
		}
		if (EnableFlag.needsSync(this.cullingEnabled, enable)) {
			UNMANAGED.enableCull(enable);
			this.cullingEnabled = EnableFlag.from(enable);
		}
	}

	public boolean isCullEnabled() {
		if (this.cullingEnabled == null)
			this.cullingEnabled = EnableFlag.from(GL32C.glGetBoolean(GL32C.GL_CULL_FACE));
		return this.cullingEnabled == EnableFlag.ENABLED;
	}

	@Override
	public void cullFace(CullFace cullFace) {
		if (this.parentState.cullFace == null) {
			this.parentState.cullFace = getCullFace();
		}
		if (this.cullFace != cullFace) {
			UNMANAGED.cullFace(cullFace);
			this.cullFace = cullFace;
		}
	}

	public CullFace getCullFace() {
		if (this.cullFace == null)
			this.cullFace = CullFace.from(GL32C.glGetInteger(GL32C.GL_CULL_FACE_MODE));
		return this.cullFace;
	}

	@Override
	public void frontFace(FrontFace frontFace) {
		if (this.parentState.frontFace == null) {
			this.parentState.frontFace = getFrontFace();
		}
		if (this.frontFace != frontFace) {
			UNMANAGED.frontFace(frontFace);
			this.frontFace = frontFace;
		}
	}

	public FrontFace getFrontFace() {
		if (this.frontFace == null)
			this.frontFace = FrontFace.from(GL32C.glGetInteger(GL32C.GL_FRONT_FACE));
		return this.frontFace;
	}

	@Override
	public void enableDepthTest(boolean enable) {
		if (this.parentState.depthTestEnabled == null) {
			this.parentState.depthTestEnabled = EnableFlag.from(isDepthTestEnabled());
		}
		if (EnableFlag.needsSync(this.depthTestEnabled, enable)) {
			UNMANAGED.enableDepthTest(enable);
			this.depthTestEnabled = EnableFlag.from(enable);
		}
	}

	public boolean isDepthTestEnabled() {
		if (this.depthTestEnabled == null)
			this.depthTestEnabled = EnableFlag.from(GL32C.glGetBoolean(GL32C.GL_DEPTH_TEST));
		return this.depthTestEnabled != EnableFlag.DISABLED;
	}

	@Override
	public void depthMask(boolean depthMask) {
		if (this.parentState.depthMask == null) {
			this.parentState.depthMask = EnableFlag.from(isDepthMaskEnabled());
		}
		if (EnableFlag.needsSync(this.depthMask, depthMask)) {
			UNMANAGED.depthMask(depthMask);
			this.depthMask = EnableFlag.from(depthMask);
		}
	}

	public boolean isDepthMaskEnabled() {
		if (this.depthMask == null)
			this.depthMask = EnableFlag.from(GL32C.glGetBoolean(GL32C.GL_DEPTH_WRITEMASK));
		return this.depthMask != EnableFlag.DISABLED;
	}

	@Override
	public void depthFunc(DepthFunc depthFunc) {
		if (this.parentState.depthFunc == null) {
			this.parentState.depthFunc = getDepthFunc();
		}
		if (this.depthFunc != depthFunc) {
			UNMANAGED.depthFunc(depthFunc);
			this.depthFunc = depthFunc;
		}
	}

	public DepthFunc getDepthFunc() {
		if (this.depthFunc == null)
			this.depthFunc = DepthFunc.from(GL32C.glGetInteger(GL32C.GL_DEPTH_FUNC));
		return this.depthFunc;
	}

	@Override
	public void enableBlend(boolean enable) {
		if (this.parentState.blendingEnabled == null) {
			this.parentState.blendingEnabled = EnableFlag.from(isBlendEnabled());
		}
		if (EnableFlag.needsSync(this.blendingEnabled, enable)) {
			UNMANAGED.enableBlend(enable);
			this.blendingEnabled = EnableFlag.from(enable);
		}
	}

	public boolean isBlendEnabled() {
		if (this.blendingEnabled == null)
			this.blendingEnabled = EnableFlag.from(GL32C.glGetBoolean(GL32C.GL_BLEND));
		return this.blendingEnabled != EnableFlag.DISABLED;
	}

	@Override
	public void blendEquation(BlendEquation blendEquationRgb, BlendEquation blendEquationAlpha) {
		if (this.blendEquationRgb == null || this.blendEquationAlpha == null) {
			this.parentState.blendEquationRgb = getBlendEquationRgb();
			this.parentState.blendEquationAlpha = getBlendEquationAlpha();
		}
		if (this.blendEquationRgb != blendEquationRgb || this.blendEquationAlpha != blendEquationAlpha) {
			UNMANAGED.blendEquation(blendEquationRgb, blendEquationAlpha);
			this.blendEquationRgb = blendEquationRgb;
			this.blendEquationAlpha = blendEquationAlpha;
		}
	}

	public BlendEquation getBlendEquationRgb() {
		if (this.blendEquationRgb == null)
			this.blendEquationRgb = BlendEquation.from(GL32C.glGetInteger(GL32C.GL_BLEND_EQUATION_RGB));
		return this.blendEquationRgb;
	}

	public BlendEquation getBlendEquationAlpha() {
		if (this.blendEquationAlpha == null)
			this.blendEquationAlpha = BlendEquation.from(GL32C.glGetInteger(GL32C.GL_BLEND_EQUATION_ALPHA));
		return this.blendEquationAlpha;
	}

	@Override
	public void blendFunc(BlendFactor blendFactorSrcRgb, BlendFactor blendFactorDstRgb,
			BlendFactor blendFactorSrcAlpha, BlendFactor blendFactorDstAlpha) {
		if (this.parentState.blendFactorSrcRgb == null
				|| this.parentState.blendFactorDstRgb == null
				|| this.parentState.blendFactorSrcAlpha == null
				|| this.parentState.blendFactorDstAlpha == null) {
			this.parentState.blendFactorSrcRgb = getBlendFactorSrcRgb();
			this.parentState.blendFactorDstRgb = getBlendFactorDstRgb();
			this.parentState.blendFactorSrcAlpha = getBlendFactorSrcAlpha();
			this.parentState.blendFactorDstAlpha = getBlendFactorDstAlpha();
		}
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
		if (this.blendFactorSrcRgb == null)
			this.blendFactorSrcRgb = BlendFactor.from(GL32C.glGetInteger(GL32C.GL_BLEND_SRC_RGB));
		return this.blendFactorSrcRgb;
	}

	public BlendFactor getBlendFactorDstRgb() {
		if (this.blendFactorDstRgb == null)
			this.blendFactorDstRgb = BlendFactor.from(GL32C.glGetInteger(GL32C.GL_BLEND_DST_RGB));
		return this.blendFactorDstRgb;
	}

	public BlendFactor getBlendFactorSrcAlpha() {
		if (this.blendFactorSrcAlpha == null)
			this.blendFactorSrcAlpha = BlendFactor.from(GL32C.glGetInteger(GL32C.GL_BLEND_SRC_ALPHA));
		return this.blendFactorSrcAlpha;
	}

	public BlendFactor getBlendFactorDstAlpha() {
		if (this.blendFactorDstAlpha == null)
			this.blendFactorDstAlpha = BlendFactor.from(GL32C.glGetInteger(GL32C.GL_BLEND_DST_ALPHA));
		return this.blendFactorDstAlpha;
	}

	@Override
	public void colorMask(boolean colorMaskR, boolean colorMaskG, boolean colorMaskB, boolean colorMaskA) {
		if (this.parentState.colorMaskR == null
				|| this.parentState.colorMaskG == null
				|| this.parentState.colorMaskB == null
				|| this.parentState.colorMaskA == null) {
			queryColorMask();
			this.parentState.colorMaskR = this.colorMaskR;
			this.parentState.colorMaskG = this.colorMaskG;
			this.parentState.colorMaskB = this.colorMaskB;
			this.parentState.colorMaskA = this.colorMaskA;
		}
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

	private void queryColorMask() {
		try (final var stack = MemoryStack.stackPush()) {
			// we don't actually know what size GLboolean is... I can't find any way to
			// query its size, so we're just gonna hope its 1 byte and be on our way! D:
			final var buf = stack.malloc(4);
			GL32C.glGetBooleanv(GL32C.GL_COLOR_WRITEMASK, buf);
			this.colorMaskR = EnableFlag.from(buf.get(0) != GL32C.GL_FALSE);
			this.colorMaskG = EnableFlag.from(buf.get(1) != GL32C.GL_FALSE);
			this.colorMaskB = EnableFlag.from(buf.get(2) != GL32C.GL_FALSE);
			this.colorMaskA = EnableFlag.from(buf.get(3) != GL32C.GL_FALSE);
		}
	}

	public boolean isColorMaskREnabled() {
		if (this.colorMaskR == null)
			queryColorMask();
		return this.colorMaskR != EnableFlag.DISABLED;
	}

	public boolean isColorMaskGEnabled() {
		if (this.colorMaskG == null)
			queryColorMask();
		return this.colorMaskG != EnableFlag.DISABLED;
	}

	public boolean isColorMaskBEnabled() {
		if (this.colorMaskB == null)
			queryColorMask();
		return this.colorMaskB != EnableFlag.DISABLED;
	}

	public boolean isColorMaskAEnabled() {
		if (this.colorMaskA == null)
			queryColorMask();
		return this.colorMaskA != EnableFlag.DISABLED;
	}

	@Override
	public void enableLogicOp(boolean enable) {
		if (this.parentState.logicOpEnabled == null) {
			this.parentState.logicOpEnabled = EnableFlag.from(isLogicOpEnabled());
		}
		if (EnableFlag.needsSync(this.logicOpEnabled, enable)) {
			UNMANAGED.enableLogicOp(enable);
			this.logicOpEnabled = EnableFlag.from(enable);
		}
	}

	public boolean isLogicOpEnabled() {
		if (this.logicOpEnabled == null)
			this.logicOpEnabled = EnableFlag.from(GL32C.glGetBoolean(GL32C.GL_COLOR_LOGIC_OP));
		return this.logicOpEnabled != EnableFlag.DISABLED;
	}

	@Override
	public void logicOp(LogicOp logicOp) {
		if (this.parentState.logicOp == null) {
			this.parentState.logicOp = getLogicOp();
		}
		if (this.logicOp != logicOp) {
			UNMANAGED.logicOp(logicOp);
			this.logicOp = logicOp;
		}
	}

	public LogicOp getLogicOp() {
		if (this.logicOp == null)
			this.logicOp = LogicOp.from(GL32C.glGetInteger(GL32C.GL_LOGIC_OP_MODE));
		return this.logicOp;
	}

	@Override
	public void bindFramebuffer(int target, int id) {
		final var setRead = target == GL32C.GL_FRAMEBUFFER || target == GL32C.GL_READ_FRAMEBUFFER;
		final var setDraw = target == GL32C.GL_FRAMEBUFFER || target == GL32C.GL_DRAW_FRAMEBUFFER;
		if (setRead && setDraw && (this.boundReadFramebuffer != id || this.boundDrawFramebuffer != id)) {
			if (this.parentState.boundReadFramebuffer == Integer.MIN_VALUE) {
				this.parentState.boundReadFramebuffer = getBoundFramebuffer(GL32C.GL_READ_FRAMEBUFFER);
			}
			if (this.parentState.boundDrawFramebuffer == Integer.MIN_VALUE) {
				this.parentState.boundDrawFramebuffer = getBoundFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER);
			}
			UNMANAGED.bindFramebuffer(GL32C.GL_FRAMEBUFFER, id);
			this.boundReadFramebuffer = id;
			this.boundDrawFramebuffer = id;
		} else if (setRead && this.boundReadFramebuffer != id) {
			if (this.parentState.boundReadFramebuffer == Integer.MIN_VALUE) {
				this.parentState.boundReadFramebuffer = getBoundFramebuffer(GL32C.GL_READ_FRAMEBUFFER);
			}
			UNMANAGED.bindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, id);
			this.boundReadFramebuffer = id;
		} else if (setDraw && this.boundDrawFramebuffer != id) {
			if (this.parentState.boundDrawFramebuffer == Integer.MIN_VALUE) {
				this.parentState.boundDrawFramebuffer = getBoundFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER);
			}
			UNMANAGED.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, id);
			this.boundDrawFramebuffer = id;
		}
	}

	public int getBoundFramebuffer(int target) {
		if (target == GL32C.GL_READ_FRAMEBUFFER) {
			if (this.boundReadFramebuffer == Integer.MIN_VALUE)
				this.boundReadFramebuffer = GL32C.glGetInteger(GL32C.GL_READ_FRAMEBUFFER_BINDING);
			return this.boundReadFramebuffer;
		} else if (target == GL32C.GL_DRAW_FRAMEBUFFER) {
			if (this.boundDrawFramebuffer == Integer.MIN_VALUE)
				this.boundDrawFramebuffer = GL32C.glGetInteger(GL32C.GL_DRAW_FRAMEBUFFER_BINDING);
			return this.boundDrawFramebuffer;
		}
		throw new IllegalArgumentException(String.format(
				"cannot query bound framebuffer, 0x%X is not a valid binding point.",
				target));
	}

	@Override
	public void bindBuffer(GlBuffer.Type target, int id) {
		if (this.parentState.boundBuffers[target.ordinal()] == Integer.MIN_VALUE) {
			this.parentState.boundBuffers[target.ordinal()] = getBoundBuffer(target);
		}
		if (this.boundBuffers[target.ordinal()] != id) {
			UNMANAGED.bindBuffer(target, id);
			this.boundBuffers[target.ordinal()] = id;
		}
	}

	public int getBoundBuffer(GlBuffer.Type target) {
		if (this.boundBuffers[target.ordinal()] == Integer.MIN_VALUE) {
			this.boundBuffers[target.ordinal()] = GL32C.glGetInteger(target.bindingId);
		}
		return this.boundBuffers[target.ordinal()];
	}

	@Override
	public void bindTexture(GlTexture.Type target, int id) {
		getBoundTextureUnit();
		final var parentUnit = this.parentState.textureUnits[this.boundTextureUnit - GL32C.GL_TEXTURE0];
		if (parentUnit.boundTextures[target.ordinal()] == Integer.MIN_VALUE) {
			parentUnit.boundTextures[target.ordinal()] = getBoundTexture(target);
		}
		final var unit = this.textureUnits[this.boundTextureUnit - GL32C.GL_TEXTURE0];
		if (unit.boundTextures[target.ordinal()] != id) {
			UNMANAGED.bindTexture(target, id);
			unit.setBinding(target.ordinal(), id);
		}
	}

	public int getBoundTexture(GlTexture.Type target) {
		final var unit = this.textureUnits[getBoundTextureUnit() - GL32C.GL_TEXTURE0];
		if (unit.boundTextures[target.ordinal()] == Integer.MIN_VALUE) {
			unit.boundTextures[target.ordinal()] = GL32C.glGetInteger(target.bindingId);
		}
		return unit.boundTextures[target.ordinal()];
	}

	@Override
	public void bindVertexArray(int id) {
		if (this.parentState.boundVertexArray == Integer.MIN_VALUE) {
			this.parentState.boundVertexArray = getBoundVertexArray();
		}
		if (this.boundVertexArray != id) {
			UNMANAGED.bindVertexArray(id);
			this.boundVertexArray = id;
		}
	}

	public int getBoundVertexArray() {
		if (this.boundVertexArray == Integer.MIN_VALUE) {
			this.boundVertexArray = GL32C.glGetInteger(GL32C.GL_VERTEX_ARRAY_BINDING);
		}
		return this.boundVertexArray;
	}

	@Override
	public void useProgram(int id) {
		if (this.parentState.boundProgram == Integer.MIN_VALUE) {
			this.parentState.boundProgram = getBoundProgram();
		}
		if (this.boundProgram != id) {
			UNMANAGED.useProgram(id);
			this.boundProgram = id;
		}
	}

	public int getBoundProgram() {
		if (this.boundProgram == Integer.MIN_VALUE) {
			this.boundProgram = GL32C.glGetInteger(GL32C.GL_CURRENT_PROGRAM);
		}
		return this.boundProgram;
	}

	@Override
	public void bindRenderbuffer(int id) {
		if (this.parentState.boundRenderbuffer == Integer.MIN_VALUE) {
			this.parentState.boundRenderbuffer = getBoundRenderbuffer();
		}
		if (this.boundRenderbuffer != id) {
			UNMANAGED.bindRenderbuffer(id);
			this.boundRenderbuffer = id;
		}
	}

	public int getBoundRenderbuffer() {
		if (this.boundRenderbuffer == Integer.MIN_VALUE) {
			this.boundRenderbuffer = GL32C.glGetInteger(GL32C.GL_RENDERBUFFER_BINDING);
		}
		return this.boundRenderbuffer;
	}

	@Override
	public void activeTexture(int unit) {
		if (unit - GL32C.GL_TEXTURE0 >= MAX_TEXTURE_UNITS) {
			throw new IllegalArgumentException(String.format(
					"cannot bind texture unit %d, a maximum of %d texture units are supported.",
					unit - GL32C.GL_TEXTURE0, MAX_TEXTURE_UNITS));
		}
		if (this.parentState.boundTextureUnit == Integer.MIN_VALUE) {
			this.parentState.boundTextureUnit = getBoundTextureUnit();
		}
		if (this.boundTextureUnit != unit) {
			UNMANAGED.activeTexture(unit);
			this.boundTextureUnit = unit;
		}
	}

	public int getBoundTextureUnit() {
		if (this.boundTextureUnit == Integer.MIN_VALUE)
			this.boundTextureUnit = GL32C.glGetInteger(GL32C.GL_ACTIVE_TEXTURE);
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

	@Override
	public void drawBuffers(int[] buffers) {
		// no state to track
		UNMANAGED.drawBuffers(buffers);
	}

	public void drawBuffers(int framebuffer, int[] buffers) {
		// if (GlLimits.HAS_DIRECT_STATE_ACCESS) {
		// GL45C.glNamedFramebufferDrawBuffers(framebuffer, buffers);
		// return;
		// }
		final var prevBinding = this.boundDrawFramebuffer;
		bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, framebuffer);
		UNMANAGED.drawBuffers(buffers);
		bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, prevBinding);
	}

	@Override
	public void bufferData(GlBuffer.Type target, ByteBuffer data, GlBuffer.UsageHint usage) {
		// no state to track
		UNMANAGED.bufferData(target, data, usage);
	}

	public void bufferData(int id, ByteBuffer data, GlBuffer.UsageHint usage) {
		// if (GlLimits.HAS_DIRECT_STATE_ACCESS) {
		// GL45C.glNamedBufferData(id, data, usage.id);
		// return;
		// }
		final var prevBinding = this.boundBuffers[GlBuffer.Type.ARRAY.ordinal()];
		bindBuffer(GlBuffer.Type.ARRAY, id);
		UNMANAGED.bufferData(GlBuffer.Type.ARRAY, data, usage);
		bindBuffer(GlBuffer.Type.ARRAY, prevBinding);
	}

}
