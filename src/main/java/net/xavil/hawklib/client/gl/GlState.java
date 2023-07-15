package net.xavil.hawklib.client.gl;

import java.nio.ByteBuffer;
import java.util.Arrays;

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

	public PolygonMode polygonMode = PolygonMode.FILL;

	// culling
	public boolean cullingEnabled = false;
	public CullFace cullFace = CullFace.BACK;
	public FrontFace frontFace = FrontFace.CCW;

	// depth testing
	public boolean depthMask = true;
	public boolean depthTestEnabled = false;
	public DepthFunc depthFunc = DepthFunc.LESS;

	// blending
	public boolean blendingEnabled = false;
	public BlendEquation blendEquationRgb = BlendEquation.ADD;
	public BlendEquation blendEquationAlpha = BlendEquation.ADD;
	public BlendFactor blendFactorSrcRgb = BlendFactor.ONE;
	public BlendFactor blendFactorDstRgb = BlendFactor.ZERO;
	public BlendFactor blendFactorSrcAlpha = BlendFactor.ONE;
	public BlendFactor blendFactorDstAlpha = BlendFactor.ZERO;

	public boolean colorMaskR = true;
	public boolean colorMaskG = true;
	public boolean colorMaskB = true;
	public boolean colorMaskA = true;

	public boolean logicOpEnabled = false;
	public LogicOp logicOp = LogicOp.COPY;

	public int viewportX = 0;
	public int viewportY = 0;
	// these are not the actual initial viewport width and height. the real values
	// here are the window width and height as they were when the context was
	// created.
	public int viewportWidth = 0;
	public int viewportHeight = 0;

	public static final class TextureUnit {
		// each bit represents one texture target. if a bit is 1, then the texture bound
		// to the corresponding texture target differs from the binding as it was then
		// this state was captured. This is used to quickly skip syncing texture units
		// that have not had their bindings changed.
		public int bindingRestoreMask = 0;
		public final int[] prevTextures = new int[TEXTURE_TARGET_COUNT];
		public final int[] boundTextures = new int[TEXTURE_TARGET_COUNT];

		public void setBinding(int target, int id) {
			this.boundTextures[target] = id;
			this.bindingRestoreMask &= ~(1 << target);
			if (this.prevTextures[target] != id)
				this.bindingRestoreMask |= 1 << target;
		}

		public void copyStateFrom(TextureUnit src) {
			this.bindingRestoreMask = 0;
			for (int i = 0; i < TEXTURE_TARGET_COUNT; ++i) {
				this.boundTextures[i] = src.boundTextures[i];
				this.prevTextures[i] = src.boundTextures[i];
			}
		}
	}

	// NOTE: vanilla only ever uses GL_TEXTURE_2D, so it doesnt need to track each
	// target for each unit, and instead just tracks that specific binding per unit.
	public final TextureUnit[] textureUnits = new TextureUnit[MAX_TEXTURE_UNITS];

	public final int[] boundBuffers = new int[BUFFER_TARGET_COUNT];
	public int boundVertexArray = 0;
	public int boundProgram = 0;
	public int boundDrawFramebuffer = 0;
	public int boundReadFramebuffer = 0;
	public int boundRenderbuffer = 0;
	public int boundTextureUnit = GL32C.GL_TEXTURE0;

	public GlState() {
		for (int i = 0; i < MAX_TEXTURE_UNITS; ++i) {
			this.textureUnits[i] = new TextureUnit();
		}
	}

	public void copyStateFrom(GlState src) {
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

	public void sync() {
		this.polygonMode = PolygonMode.from(GL32C.glGetInteger(GL32C.GL_POLYGON_MODE));
		this.cullingEnabled = GL32C.glGetBoolean(GL32C.GL_CULL_FACE);
		this.cullFace = CullFace.from(GL32C.glGetInteger(GL32C.GL_CULL_FACE_MODE));
		this.frontFace = FrontFace.from(GL32C.glGetInteger(GL32C.GL_FRONT_FACE));
		this.depthMask = GL32C.glGetBoolean(GL32C.GL_DEPTH_WRITEMASK);
		this.depthTestEnabled = GL32C.glGetBoolean(GL32C.GL_DEPTH_TEST);
		this.depthFunc = DepthFunc.from(GL32C.glGetInteger(GL32C.GL_DEPTH_FUNC));
		this.blendingEnabled = GL32C.glGetBoolean(GL32C.GL_BLEND);
		this.blendEquationRgb = BlendEquation.from(GL32C.glGetInteger(GL32C.GL_BLEND_EQUATION_RGB));
		this.blendEquationAlpha = BlendEquation.from(GL32C.glGetInteger(GL32C.GL_BLEND_EQUATION_ALPHA));
		this.blendFactorSrcRgb = BlendFactor.from(GL32C.glGetInteger(GL32C.GL_BLEND_SRC_RGB));
		this.blendFactorDstRgb = BlendFactor.from(GL32C.glGetInteger(GL32C.GL_BLEND_DST_RGB));
		this.blendFactorSrcAlpha = BlendFactor.from(GL32C.glGetInteger(GL32C.GL_BLEND_SRC_ALPHA));
		this.blendFactorDstAlpha = BlendFactor.from(GL32C.glGetInteger(GL32C.GL_BLEND_DST_ALPHA));
		try (final var stack = MemoryStack.stackPush()) {
			// we don't actually know what size GLboolean is... I can't find any way to
			// query its size, so we're just gonna hope its 1 byte and be on our way! D:
			final var buf = stack.malloc(4);
			GL32C.glGetBooleanv(GL32C.GL_COLOR_WRITEMASK, buf);
			this.colorMaskR = buf.get(0) != GL32C.GL_FALSE;
			this.colorMaskG = buf.get(1) != GL32C.GL_FALSE;
			this.colorMaskB = buf.get(2) != GL32C.GL_FALSE;
			this.colorMaskA = buf.get(3) != GL32C.GL_FALSE;
		}
		this.logicOpEnabled = GL32C.glGetBoolean(GL32C.GL_COLOR_LOGIC_OP);
		this.logicOp = LogicOp.from(GL32C.glGetInteger(GL32C.GL_LOGIC_OP_MODE));

		this.boundTextureUnit = GL32C.glGetInteger(GL32C.GL_ACTIVE_TEXTURE);
		for (int i = 0; i < MAX_TEXTURE_UNITS; ++i) {
			GL32C.glActiveTexture(GL32C.GL_TEXTURE0 + i);
			for (final var binding : GlTexture.Type.values()) {
				this.textureUnits[i].boundTextures[binding.ordinal()] = GL32C.glGetInteger(binding.bindingId);
			}
		}
		GL32C.glActiveTexture(this.boundTextureUnit);

		for (final var binding : GlBuffer.Type.values()) {
			this.boundBuffers[binding.ordinal()] = GL32C.glGetInteger(binding.bindingId);
		}

		this.boundVertexArray = GL32C.glGetInteger(GL32C.GL_VERTEX_ARRAY_BINDING);
		this.boundProgram = GL32C.glGetInteger(GL32C.GL_CURRENT_PROGRAM);
		this.boundDrawFramebuffer = GL32C.glGetInteger(GL32C.GL_DRAW_FRAMEBUFFER_BINDING);
		this.boundReadFramebuffer = GL32C.glGetInteger(GL32C.GL_READ_FRAMEBUFFER_BINDING);
		this.boundRenderbuffer = GL32C.glGetInteger(GL32C.GL_RENDERBUFFER_BINDING);
	}

	public void restore(GlState current) {
		if (this.polygonMode != current.polygonMode)
			UNMANAGED.polygonMode(this.polygonMode);

		if (this.cullingEnabled != current.cullingEnabled)
			UNMANAGED.enableCull(this.cullingEnabled);
		if (this.cullFace != current.cullFace)
			UNMANAGED.cullFace(this.cullFace);
		if (this.frontFace != current.frontFace)
			UNMANAGED.frontFace(this.frontFace);

		if (this.depthMask != current.depthMask)
			UNMANAGED.depthMask(this.depthMask);
		if (this.depthTestEnabled != current.depthTestEnabled)
			UNMANAGED.enableDepthTest(this.depthTestEnabled);
		if (this.depthFunc != current.depthFunc)
			UNMANAGED.depthFunc(this.depthFunc);

		if (this.blendingEnabled != current.blendingEnabled)
			UNMANAGED.enableBlend(this.blendingEnabled);
		if (this.blendEquationRgb != current.blendEquationRgb
				|| this.blendEquationAlpha != current.blendEquationAlpha)
			UNMANAGED.blendEquation(this.blendEquationRgb, this.blendEquationAlpha);
		if (this.blendFactorSrcRgb != current.blendFactorSrcRgb
				|| this.blendFactorDstRgb != current.blendFactorDstRgb
				|| this.blendFactorSrcAlpha != current.blendFactorSrcAlpha
				|| this.blendFactorDstAlpha != current.blendFactorDstAlpha)
			UNMANAGED.blendFunc(this.blendFactorSrcRgb, this.blendFactorDstRgb,
					this.blendFactorSrcAlpha, this.blendFactorDstAlpha);

		if (this.colorMaskR != current.colorMaskR
				|| this.colorMaskG != current.colorMaskG
				|| this.colorMaskB != current.colorMaskB
				|| this.colorMaskA != current.colorMaskA)
			UNMANAGED.colorMask(this.colorMaskR, this.colorMaskG, this.colorMaskB, this.colorMaskA);

		if (this.logicOpEnabled != current.logicOpEnabled)
			UNMANAGED.enableLogicOp(this.logicOpEnabled);
		if (this.logicOp != current.logicOp)
			UNMANAGED.logicOp(this.logicOp);

		// make sure we don't clobber VAO state here
		UNMANAGED.bindVertexArray(0);
		for (final var binding : GlBuffer.Type.values()) {
			final var toBind = this.boundBuffers[binding.ordinal()];
			if (toBind != current.boundBuffers[binding.ordinal()])
				UNMANAGED.bindBuffer(binding, toBind);
		}

		// `current` knows what texture units were bound when its state was set up.
		current.restoreTextureUnits();

		if (this.boundVertexArray != current.boundVertexArray)
			UNMANAGED.bindVertexArray(this.boundVertexArray);
		if (this.boundProgram != current.boundProgram)
			UNMANAGED.useProgram(this.boundProgram);
		if (this.boundDrawFramebuffer != current.boundDrawFramebuffer)
			UNMANAGED.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, this.boundDrawFramebuffer);
		if (this.boundReadFramebuffer != current.boundReadFramebuffer)
			UNMANAGED.bindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, this.boundReadFramebuffer);
		if (this.boundRenderbuffer != current.boundRenderbuffer)
			UNMANAGED.bindRenderbuffer(this.boundRenderbuffer);
		if (this.boundTextureUnit != current.boundTextureUnit)
			UNMANAGED.activeTexture(this.boundTextureUnit);
	}

	private void restoreTextureUnits() {
		for (var i = 0; i < MAX_TEXTURE_UNITS; ++i) {
			final var unit = this.textureUnits[i];
			if (unit.bindingRestoreMask == 0)
				continue;
			for (final var binding : GlTexture.Type.values()) {
				final var prev = unit.prevTextures[binding.ordinal()];
				final var cur = unit.boundTextures[binding.ordinal()];
				if (prev != cur)
					UNMANAGED.bindTexture(binding, prev);
			}
		}
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
		if (this.polygonMode != mode) {
			UNMANAGED.polygonMode(mode);
			this.polygonMode = mode;
		}
	}

	@Override
	public void enableCull(boolean enable) {
		if (this.cullingEnabled != enable) {
			UNMANAGED.enableCull(enable);
			this.cullingEnabled = enable;
		}
	}

	@Override
	public void cullFace(CullFace cullFace) {
		if (this.cullFace != cullFace) {
			UNMANAGED.cullFace(cullFace);
			this.cullFace = cullFace;
		}
	}

	@Override
	public void frontFace(FrontFace frontFace) {
		if (this.frontFace != frontFace) {
			UNMANAGED.frontFace(frontFace);
			this.frontFace = frontFace;
		}
	}

	@Override
	public void enableDepthTest(boolean enable) {
		if (this.cullingEnabled != enable) {
			UNMANAGED.enableDepthTest(enable);
			this.cullingEnabled = enable;
		}
	}

	@Override
	public void depthMask(boolean depthMask) {
		if (this.depthMask != depthMask) {
			UNMANAGED.depthMask(depthMask);
			this.depthMask = depthMask;
		}
	}

	@Override
	public void depthFunc(DepthFunc depthFunc) {
		if (this.depthFunc != depthFunc) {
			UNMANAGED.depthFunc(depthFunc);
			this.depthFunc = depthFunc;
		}
	}

	@Override
	public void enableBlend(boolean enable) {
		if (this.blendingEnabled != enable) {
			UNMANAGED.enableBlend(enable);
			this.blendingEnabled = enable;
		}
	}

	@Override
	public void blendEquation(BlendEquation blendEquationRgb, BlendEquation blendEquationAlpha) {
		if (this.blendEquationRgb != blendEquationRgb || this.blendEquationAlpha != blendEquationAlpha) {
			UNMANAGED.blendEquation(blendEquationRgb, blendEquationAlpha);
			this.blendEquationRgb = blendEquationRgb;
			this.blendEquationAlpha = blendEquationAlpha;
		}
	}

	@Override
	public void blendFunc(BlendFactor blendFactorSrcRgb, BlendFactor blendFactorDstRgb,
			BlendFactor blendFactorSrcAlpha, BlendFactor blendFactorDstAlpha) {
		if (this.blendFactorSrcRgb != blendFactorSrcRgb || this.blendFactorDstRgb != blendFactorDstRgb
				|| this.blendFactorSrcAlpha != blendFactorSrcAlpha || this.blendFactorDstAlpha != blendFactorDstAlpha) {
			UNMANAGED.blendFunc(blendFactorSrcRgb, blendFactorDstRgb,
					blendFactorSrcAlpha, blendFactorDstAlpha);
			this.blendFactorSrcRgb = blendFactorSrcRgb;
			this.blendFactorDstRgb = blendFactorDstRgb;
			this.blendFactorSrcAlpha = blendFactorSrcAlpha;
			this.blendFactorDstAlpha = blendFactorDstAlpha;
		}
	}

	@Override
	public void colorMask(boolean colorMaskR, boolean colorMaskG, boolean colorMaskB, boolean colorMaskA) {
		if (this.colorMaskR != colorMaskR || this.colorMaskG != colorMaskG
				|| this.colorMaskB != colorMaskB || this.colorMaskA != colorMaskA) {
			UNMANAGED.colorMask(colorMaskR, colorMaskG, colorMaskB, colorMaskA);
			this.colorMaskR = colorMaskR;
			this.colorMaskG = colorMaskG;
			this.colorMaskB = colorMaskB;
			this.colorMaskA = colorMaskA;
		}
	}

	@Override
	public void bindFramebuffer(int target, int id) {
		final var setRead = target == GL32C.GL_FRAMEBUFFER || target == GL32C.GL_READ_FRAMEBUFFER;
		final var setDraw = target == GL32C.GL_FRAMEBUFFER || target == GL32C.GL_DRAW_FRAMEBUFFER;
		if (setRead && setDraw && (this.boundReadFramebuffer != id || this.boundDrawFramebuffer != id)) {
			UNMANAGED.bindFramebuffer(GL32C.GL_FRAMEBUFFER, id);
			this.boundReadFramebuffer = id;
			this.boundDrawFramebuffer = id;
		} else if (setRead && this.boundReadFramebuffer != id) {
			UNMANAGED.bindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, id);
			this.boundReadFramebuffer = id;
		} else if (setDraw && this.boundDrawFramebuffer != id) {
			UNMANAGED.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, id);
			this.boundDrawFramebuffer = id;
		}
	}

	@Override
	public void bindBuffer(GlBuffer.Type target, int id) {
		if (this.boundBuffers[target.ordinal()] != id) {
			UNMANAGED.bindBuffer(target, id);
			this.boundBuffers[target.ordinal()] = id;
		}
	}

	@Override
	public void bindTexture(GlTexture.Type target, int id) {
		final var unit = this.textureUnits[this.boundTextureUnit - GL32C.GL_TEXTURE0];
		if (unit.boundTextures[target.ordinal()] != id) {
			UNMANAGED.bindTexture(target, id);
			unit.setBinding(target.ordinal(), id);
		}
	}

	@Override
	public void bindVertexArray(int id) {
		if (this.boundVertexArray != id) {
			UNMANAGED.bindVertexArray(id);
			this.boundVertexArray = id;
		}
	}

	@Override
	public void useProgram(int id) {
		if (this.boundProgram != id) {
			UNMANAGED.useProgram(id);
			this.boundProgram = id;
		}
	}

	@Override
	public void bindRenderbuffer(int id) {
		if (this.boundRenderbuffer != id) {
			UNMANAGED.bindRenderbuffer(id);
			this.boundRenderbuffer = id;
		}
	}

	@Override
	public void activeTexture(int unit) {
		if (unit - GL32C.GL_TEXTURE0 >= MAX_TEXTURE_UNITS) {
			throw new IllegalArgumentException(String.format(
					"cannot bind texture unit %d, a maximum of %d texture units are supported.",
					unit - GL32C.GL_TEXTURE0, MAX_TEXTURE_UNITS));
		}
		if (this.boundTextureUnit != unit) {
			UNMANAGED.activeTexture(unit);
			this.boundTextureUnit = unit;
		}
	}

	@Override
	public void enableLogicOp(boolean enable) {
		if (this.logicOpEnabled != enable) {
			UNMANAGED.enableLogicOp(enable);
			this.logicOpEnabled = enable;
		}
	}

	@Override
	public void logicOp(LogicOp logicOp) {
		if (this.logicOp != logicOp) {
			UNMANAGED.logicOp(logicOp);
			this.logicOp = logicOp;
		}
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
		// 	GL45C.glNamedFramebufferDrawBuffers(framebuffer, buffers);
		// 	return;
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
		// 	GL45C.glNamedBufferData(id, data, usage.id);
		// 	return;
		// }
		final var prevBinding = this.boundBuffers[GlBuffer.Type.ARRAY.ordinal()];
		bindBuffer(GlBuffer.Type.ARRAY, id);
		UNMANAGED.bufferData(GlBuffer.Type.ARRAY, data, usage);
		bindBuffer(GlBuffer.Type.ARRAY, prevBinding);
	}

}
