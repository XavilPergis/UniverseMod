package net.xavil.hawklib.client.gl.shader;

import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.gl.texture.GlTexture;

public final class UniformSlot {

	public static enum ComponentType {
		INT(true, false),
		UINT(true, false),
		FLOAT(false, true),
		SAMPLER(true, false),
		INVALID(false, false);

		public final boolean isIntType;
		public final boolean isFloatType;

		private ComponentType(boolean isIntType, boolean isFloatType) {
			this.isIntType = isIntType;
			this.isFloatType = isFloatType;
		}
	}

	public static enum Type {
		// @formatter:off
		// ===== vectors =====
		FLOAT1 (GL45C.GL_FLOAT,             "float",  1, ComponentType.FLOAT),
		FLOAT2 (GL45C.GL_FLOAT_VEC2,        "vec2",   2, ComponentType.FLOAT),
		FLOAT3 (GL45C.GL_FLOAT_VEC3,        "vec3",   3, ComponentType.FLOAT),
		FLOAT4 (GL45C.GL_FLOAT_VEC4,        "vec4",   4, ComponentType.FLOAT),
		// weirdly enough, this can be returned by glGetActiveUniform, even though it is
		// not actually possible to upload a uniform double!
		DOUBLE1(GL45C.GL_DOUBLE,            "double", 1, ComponentType.INVALID),
		INT1   (GL45C.GL_INT,               "int",    1, ComponentType.INT),
		INT2   (GL45C.GL_INT_VEC2,          "ivec2",  2, ComponentType.INT),
		INT3   (GL45C.GL_INT_VEC3,          "ivec3",  3, ComponentType.INT),
		INT4   (GL45C.GL_INT_VEC4,          "ivec4",  4, ComponentType.INT),
		UINT1  (GL45C.GL_UNSIGNED_INT,      "uint",   1, ComponentType.UINT),
		UINT2  (GL45C.GL_UNSIGNED_INT_VEC2, "uvec2",  2, ComponentType.UINT),
		UINT3  (GL45C.GL_UNSIGNED_INT_VEC3, "uvec3",  3, ComponentType.UINT),
		UINT4  (GL45C.GL_UNSIGNED_INT_VEC4, "uvec4",  4, ComponentType.UINT),
		// booleans are uploaded via glUniform*i
		BOOL1  (GL45C.GL_BOOL,              "bool",   1, ComponentType.INT),
		BOOL2  (GL45C.GL_BOOL_VEC2,         "bvec2",  2, ComponentType.INT),
		BOOL3  (GL45C.GL_BOOL_VEC3,         "bvec3",  3, ComponentType.INT),
		BOOL4  (GL45C.GL_BOOL_VEC4,         "bvec4",  4, ComponentType.INT),

		// ===== matrices =====
		FLOAT_MAT2x2(GL45C.GL_FLOAT_MAT2,   "mat2",   4,  ComponentType.FLOAT),
		FLOAT_MAT3x3(GL45C.GL_FLOAT_MAT3,   "mat3",   9,  ComponentType.FLOAT),
		FLOAT_MAT4x4(GL45C.GL_FLOAT_MAT4,   "mat4",   16, ComponentType.FLOAT),
		FLOAT_MAT2x3(GL45C.GL_FLOAT_MAT2x3, "mat2x3", 6,  ComponentType.FLOAT),
		FLOAT_MAT2x4(GL45C.GL_FLOAT_MAT2x4, "mat2x4", 8,  ComponentType.FLOAT),
		FLOAT_MAT3x2(GL45C.GL_FLOAT_MAT3x2, "mat3x2", 6,  ComponentType.FLOAT),
		FLOAT_MAT3x4(GL45C.GL_FLOAT_MAT3x4, "mat3x4", 12, ComponentType.FLOAT),
		FLOAT_MAT4x2(GL45C.GL_FLOAT_MAT4x2, "mat4x2", 8,  ComponentType.FLOAT),
		FLOAT_MAT4x3(GL45C.GL_FLOAT_MAT4x3, "mat4x3", 12, ComponentType.FLOAT),

		// ===== samplers =====
		SAMPLER_1D                       (GL45C.GL_SAMPLER_1D,                                "sampler1D",            GlTexture.Type.D1,          GlTexture.SamplerType.FLOAT),
		SAMPLER_2D                       (GL45C.GL_SAMPLER_2D,                                "sampler2D",            GlTexture.Type.D2,          GlTexture.SamplerType.FLOAT),
		SAMPLER_3D                       (GL45C.GL_SAMPLER_3D,                                "sampler3D",            GlTexture.Type.D3,          GlTexture.SamplerType.FLOAT),
		SAMPLER_CUBE                     (GL45C.GL_SAMPLER_CUBE,                              "samplerCube",          GlTexture.Type.CUBEMAP,     GlTexture.SamplerType.FLOAT),
		SAMPLER_1D_ARRAY                 (GL45C.GL_SAMPLER_1D_ARRAY,                          "sampler1DArray",       GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_ARRAY                 (GL45C.GL_SAMPLER_2D_ARRAY,                          "sampler2DArray",       GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_MULTISAMPLE           (GL45C.GL_SAMPLER_2D_MULTISAMPLE,                    "sampler2DMS",          GlTexture.Type.D2_MS,       GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_MULTISAMPLE_ARRAY     (GL45C.GL_SAMPLER_2D_MULTISAMPLE_ARRAY,              "sampler2DMSArray",     GlTexture.Type.D2_MS_ARRAY, GlTexture.SamplerType.FLOAT),
		SAMPLER_BUFFER                   (GL45C.GL_SAMPLER_BUFFER,                            "samplerBuffer",        GlTexture.Type.BUFFER,      GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_RECT                  (GL45C.GL_SAMPLER_2D_RECT,                           "sampler2DRect",        GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.FLOAT),
		SAMPLER_INT_1D                   (GL45C.GL_INT_SAMPLER_1D,                            "isampler1D",           GlTexture.Type.D1,          GlTexture.SamplerType.INT),
		SAMPLER_INT_2D                   (GL45C.GL_INT_SAMPLER_2D,                            "isampler2D",           GlTexture.Type.D2,          GlTexture.SamplerType.INT),
		SAMPLER_INT_3D                   (GL45C.GL_INT_SAMPLER_3D,                            "isampler3D",           GlTexture.Type.D3,          GlTexture.SamplerType.INT),
		SAMPLER_INT_CUBE                 (GL45C.GL_INT_SAMPLER_CUBE,                          "isamplerCube",         GlTexture.Type.CUBEMAP,     GlTexture.SamplerType.INT),
		SAMPLER_INT_1D_ARRAY             (GL45C.GL_INT_SAMPLER_1D_ARRAY,                      "isampler1DArray",      GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_ARRAY             (GL45C.GL_INT_SAMPLER_2D_ARRAY,                      "isampler2DArray",      GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_MULTISAMPLE       (GL45C.GL_INT_SAMPLER_2D_MULTISAMPLE,                "isampler2DMS",         GlTexture.Type.D2_MS,       GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_MULTISAMPLE_ARRAY (GL45C.GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY,          "isampler2DMSArray",    GlTexture.Type.D2_MS_ARRAY, GlTexture.SamplerType.INT),
		SAMPLER_INT_BUFFER               (GL45C.GL_INT_SAMPLER_BUFFER,                        "isamplerBuffer",       GlTexture.Type.BUFFER,      GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_RECT              (GL45C.GL_INT_SAMPLER_2D_RECT,                       "isampler2DRect",       GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.INT),
		SAMPLER_UINT_1D                  (GL45C.GL_UNSIGNED_INT_SAMPLER_1D,                   "usampler1D",           GlTexture.Type.D1,          GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D                  (GL45C.GL_UNSIGNED_INT_SAMPLER_2D,                   "usampler2D",           GlTexture.Type.D2,          GlTexture.SamplerType.UINT),
		SAMPLER_UINT_3D                  (GL45C.GL_UNSIGNED_INT_SAMPLER_3D,                   "usampler3D",           GlTexture.Type.D3,          GlTexture.SamplerType.UINT),
		SAMPLER_UINT_CUBE                (GL45C.GL_UNSIGNED_INT_SAMPLER_CUBE,                 "usamplerCube",         GlTexture.Type.CUBEMAP,     GlTexture.SamplerType.UINT),
		SAMPLER_UINT_1D_ARRAY            (GL45C.GL_UNSIGNED_INT_SAMPLER_1D_ARRAY,             "usampler2DArray",      GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_ARRAY            (GL45C.GL_UNSIGNED_INT_SAMPLER_2D_ARRAY,             "usampler2DArray",      GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_MULTISAMPLE      (GL45C.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE,       "usampler2DMS",         GlTexture.Type.D2_MS,       GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_MULTISAMPLE_ARRAY(GL45C.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY, "usampler2DMSArray",    GlTexture.Type.D2_MS_ARRAY, GlTexture.SamplerType.UINT),
		SAMPLER_UINT_BUFFER              (GL45C.GL_UNSIGNED_INT_SAMPLER_BUFFER,               "usamplerBuffer",       GlTexture.Type.BUFFER,      GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_RECT             (GL45C.GL_UNSIGNED_INT_SAMPLER_2D_RECT,              "usampler2DRect",       GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.UINT),
		SAMPLER_SHADOW_1D                (GL45C.GL_SAMPLER_1D_SHADOW,                         "sampler1DShadow",      GlTexture.Type.D1,          GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_2D                (GL45C.GL_SAMPLER_2D_SHADOW,                         "sampler2DShadow",      GlTexture.Type.D2,          GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_1D_ARRAY          (GL45C.GL_SAMPLER_1D_ARRAY_SHADOW,                   "sampler1DArrayShadow", GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_2D_ARRAY          (GL45C.GL_SAMPLER_2D_ARRAY_SHADOW,                   "sampler2DArrayShadow", GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_CUBE              (GL45C.GL_SAMPLER_CUBE_SHADOW,                       "samplerCubeShadow",    GlTexture.Type.CUBEMAP,     GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_2D_RECT           (GL45C.GL_SAMPLER_2D_RECT_SHADOW,                    "sampler2DRectShadow",  GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.SHADOW);
		// @formatter:on

		public final int id;
		public final String description;
		public final ComponentType componentType;
		public final int componentCount;

		// texture sampler types
		public final GlTexture.Type textureType;
		public final GlTexture.SamplerType samplerType;

		private Type(int id, String description, int componentCount, ComponentType componentType) {
			this.id = id;
			this.description = description;
			this.componentCount = componentCount;
			this.componentType = componentType;
			this.textureType = null;
			this.samplerType = null;
		}

		private Type(int id, String description, GlTexture.Type textureType, GlTexture.SamplerType samplerType) {
			this.id = id;
			this.description = description;
			this.componentCount = 1;
			this.componentType = ComponentType.SAMPLER;
			this.textureType = textureType;
			this.samplerType = samplerType;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static Type from(GlTexture.SamplerType samplerType, GlTexture.Type textureType) {
			return switch (samplerType) {
				case FLOAT -> switch (textureType) {
					case D1 -> SAMPLER_1D;
					case D2 -> SAMPLER_2D;
					case D3 -> SAMPLER_3D;
					case CUBEMAP -> SAMPLER_CUBE;
					case D1_ARRAY -> SAMPLER_1D_ARRAY;
					case D2_ARRAY -> SAMPLER_2D_ARRAY;
					case D2_MS -> SAMPLER_2D_MULTISAMPLE;
					case D2_MS_ARRAY -> SAMPLER_2D_MULTISAMPLE_ARRAY;
					case BUFFER -> SAMPLER_BUFFER;
					case RECTANGLE -> SAMPLER_2D_RECT;
				};
				case INT -> switch (textureType) {
					case D1 -> SAMPLER_INT_1D;
					case D2 -> SAMPLER_INT_2D;
					case D3 -> SAMPLER_INT_3D;
					case CUBEMAP -> SAMPLER_INT_CUBE;
					case D1_ARRAY -> SAMPLER_INT_1D_ARRAY;
					case D2_ARRAY -> SAMPLER_INT_2D_ARRAY;
					case D2_MS -> SAMPLER_INT_2D_MULTISAMPLE;
					case D2_MS_ARRAY -> SAMPLER_INT_2D_MULTISAMPLE_ARRAY;
					case BUFFER -> SAMPLER_INT_BUFFER;
					case RECTANGLE -> SAMPLER_INT_2D_RECT;
				};
				case UINT -> switch (textureType) {
					case D1 -> SAMPLER_UINT_1D;
					case D2 -> SAMPLER_UINT_2D;
					case D3 -> SAMPLER_UINT_3D;
					case CUBEMAP -> SAMPLER_UINT_CUBE;
					case D1_ARRAY -> SAMPLER_UINT_1D_ARRAY;
					case D2_ARRAY -> SAMPLER_UINT_2D_ARRAY;
					case D2_MS -> SAMPLER_UINT_2D_MULTISAMPLE;
					case D2_MS_ARRAY -> SAMPLER_UINT_2D_MULTISAMPLE_ARRAY;
					case BUFFER -> SAMPLER_UINT_BUFFER;
					case RECTANGLE -> SAMPLER_UINT_2D_RECT;
				};
				case SHADOW -> switch (textureType) {
					case D1 -> SAMPLER_SHADOW_1D;
					case D2 -> SAMPLER_SHADOW_2D;
					case D1_ARRAY -> SAMPLER_SHADOW_1D_ARRAY;
					case D2_ARRAY -> SAMPLER_SHADOW_2D_ARRAY;
					case CUBEMAP -> SAMPLER_SHADOW_CUBE;
					case RECTANGLE -> SAMPLER_SHADOW_2D_RECT;
					default -> null;
				};
				default -> null;
			};
		}

		public static Type from(int id) {
			return switch (id) {
				case GL45C.GL_FLOAT -> FLOAT1;
				case GL45C.GL_FLOAT_VEC2 -> FLOAT2;
				case GL45C.GL_FLOAT_VEC3 -> FLOAT3;
				case GL45C.GL_FLOAT_VEC4 -> FLOAT4;
				case GL45C.GL_DOUBLE -> DOUBLE1;
				case GL45C.GL_INT -> INT1;
				case GL45C.GL_INT_VEC2 -> INT2;
				case GL45C.GL_INT_VEC3 -> INT3;
				case GL45C.GL_INT_VEC4 -> INT4;
				case GL45C.GL_UNSIGNED_INT -> UINT1;
				case GL45C.GL_UNSIGNED_INT_VEC2 -> UINT2;
				case GL45C.GL_UNSIGNED_INT_VEC3 -> UINT3;
				case GL45C.GL_UNSIGNED_INT_VEC4 -> UINT4;
				case GL45C.GL_BOOL -> BOOL1;
				case GL45C.GL_BOOL_VEC2 -> BOOL2;
				case GL45C.GL_BOOL_VEC3 -> BOOL3;
				case GL45C.GL_BOOL_VEC4 -> BOOL4;
				case GL45C.GL_FLOAT_MAT2 -> FLOAT_MAT2x2;
				case GL45C.GL_FLOAT_MAT3 -> FLOAT_MAT3x3;
				case GL45C.GL_FLOAT_MAT4 -> FLOAT_MAT4x4;
				case GL45C.GL_FLOAT_MAT2x3 -> FLOAT_MAT2x3;
				case GL45C.GL_FLOAT_MAT2x4 -> FLOAT_MAT2x4;
				case GL45C.GL_FLOAT_MAT3x2 -> FLOAT_MAT3x2;
				case GL45C.GL_FLOAT_MAT3x4 -> FLOAT_MAT3x4;
				case GL45C.GL_FLOAT_MAT4x2 -> FLOAT_MAT4x2;
				case GL45C.GL_FLOAT_MAT4x3 -> FLOAT_MAT4x3;
				case GL45C.GL_SAMPLER_1D -> SAMPLER_1D;
				case GL45C.GL_SAMPLER_2D -> SAMPLER_2D;
				case GL45C.GL_SAMPLER_3D -> SAMPLER_3D;
				case GL45C.GL_SAMPLER_CUBE -> SAMPLER_CUBE;
				case GL45C.GL_SAMPLER_1D_ARRAY -> SAMPLER_1D_ARRAY;
				case GL45C.GL_SAMPLER_2D_ARRAY -> SAMPLER_2D_ARRAY;
				case GL45C.GL_SAMPLER_2D_MULTISAMPLE -> SAMPLER_2D_MULTISAMPLE;
				case GL45C.GL_SAMPLER_2D_MULTISAMPLE_ARRAY -> SAMPLER_2D_MULTISAMPLE_ARRAY;
				case GL45C.GL_SAMPLER_BUFFER -> SAMPLER_BUFFER;
				case GL45C.GL_SAMPLER_2D_RECT -> SAMPLER_2D_RECT;
				case GL45C.GL_INT_SAMPLER_1D -> SAMPLER_INT_1D;
				case GL45C.GL_INT_SAMPLER_2D -> SAMPLER_INT_2D;
				case GL45C.GL_INT_SAMPLER_3D -> SAMPLER_INT_3D;
				case GL45C.GL_INT_SAMPLER_CUBE -> SAMPLER_INT_CUBE;
				case GL45C.GL_INT_SAMPLER_1D_ARRAY -> SAMPLER_INT_1D_ARRAY;
				case GL45C.GL_INT_SAMPLER_2D_ARRAY -> SAMPLER_INT_2D_ARRAY;
				case GL45C.GL_INT_SAMPLER_2D_MULTISAMPLE -> SAMPLER_INT_2D_MULTISAMPLE;
				case GL45C.GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY -> SAMPLER_INT_2D_MULTISAMPLE_ARRAY;
				case GL45C.GL_INT_SAMPLER_BUFFER -> SAMPLER_INT_BUFFER;
				case GL45C.GL_INT_SAMPLER_2D_RECT -> SAMPLER_INT_2D_RECT;
				case GL45C.GL_UNSIGNED_INT_SAMPLER_1D -> SAMPLER_UINT_1D;
				case GL45C.GL_UNSIGNED_INT_SAMPLER_2D -> SAMPLER_UINT_2D;
				case GL45C.GL_UNSIGNED_INT_SAMPLER_3D -> SAMPLER_UINT_3D;
				case GL45C.GL_UNSIGNED_INT_SAMPLER_CUBE -> SAMPLER_UINT_CUBE;
				case GL45C.GL_UNSIGNED_INT_SAMPLER_1D_ARRAY -> SAMPLER_UINT_1D_ARRAY;
				case GL45C.GL_UNSIGNED_INT_SAMPLER_2D_ARRAY -> SAMPLER_UINT_2D_ARRAY;
				case GL45C.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE -> SAMPLER_UINT_2D_MULTISAMPLE;
				case GL45C.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY -> SAMPLER_UINT_2D_MULTISAMPLE_ARRAY;
				case GL45C.GL_UNSIGNED_INT_SAMPLER_BUFFER -> SAMPLER_UINT_BUFFER;
				case GL45C.GL_UNSIGNED_INT_SAMPLER_2D_RECT -> SAMPLER_UINT_2D_RECT;
				case GL45C.GL_SAMPLER_1D_SHADOW -> SAMPLER_SHADOW_1D;
				case GL45C.GL_SAMPLER_2D_SHADOW -> SAMPLER_SHADOW_2D;
				case GL45C.GL_SAMPLER_1D_ARRAY_SHADOW -> SAMPLER_SHADOW_1D_ARRAY;
				case GL45C.GL_SAMPLER_2D_ARRAY_SHADOW -> SAMPLER_SHADOW_2D_ARRAY;
				case GL45C.GL_SAMPLER_CUBE_SHADOW -> SAMPLER_SHADOW_CUBE;
				case GL45C.GL_SAMPLER_2D_RECT_SHADOW -> SAMPLER_SHADOW_2D_RECT;
				default -> null;
			};
		}
	}

	private static final long ERROR_REPORT_THRESHOLD_NS = 100000000L;

	public final Type type;
	public final String name;
	public final int size;
	public final int location;

	private boolean isDirty = false;
	private int[] intValues = null;
	private float[] floatValues = null;
	private GlTexture[] textureValues = null;

	private long lastTypeMismatchTime = Long.MIN_VALUE;

	public static boolean checkType(UniformSlot slot, UniformSlot.Type requestedType) {
		if (slot.type != requestedType) {
			// we want to avoid spamming the console, so we check if an error has occurred
			// on this slot within the past tenth of a second or so, and discard our output
			// if it has.
			final var currentTime = System.nanoTime();
			if (slot.lastTypeMismatchTime < currentTime - ERROR_REPORT_THRESHOLD_NS) {
				HawkLib.LOGGER.error("Cannot assign a value of type '{}' to uniform '{}' with type '{}'",
						requestedType.description, slot.name, slot.type.description);
			}
			slot.lastTypeMismatchTime = currentTime;
			return false;
		}
		return true;
	}

	public UniformSlot(Type type, String name, int size, int location) {
		this.type = type;
		this.name = name;
		this.size = size;
		this.location = location;

		if (this.type.componentType.isFloatType) {
			this.floatValues = new float[size * this.type.componentCount];
		} else if (this.type.componentType != ComponentType.SAMPLER && this.type.componentType.isIntType) {
			this.intValues = new int[size * this.type.componentCount];
		} else {
			this.textureValues = new GlTexture[size];
		}
	}

	public void markDirty(boolean dirty) {
		this.isDirty |= dirty;
	}

	public int[] getIntValues() {
		return this.intValues;
	}

	public float[] getFloatValues() {
		return this.floatValues;
	}

	public GlTexture[] getTextureValues() {
		return this.textureValues;
	}

	public boolean setTexture(int i, GlTexture texture) {
		final var old = this.textureValues[i];
		this.textureValues[i] = texture;
		return texture != old;
	}

	public boolean setInt(int i, int value) {
		final var old = this.intValues[i];
		this.intValues[i] = value;
		return value != old;
	}

	public boolean setFloat(int i, float value) {
		final var old = this.floatValues[i];
		this.floatValues[i] = value;
		return value != old;
	}

	public static final class UploadContext {
		public int currentTextureUnit = 0;
	}

	private boolean uploadMatrix(ShaderProgram program) {
		switch (this.type) {
			case FLOAT_MAT2x2 -> GL45C.glProgramUniformMatrix2fv(program.id, this.location, true, this.floatValues);
			case FLOAT_MAT3x3 -> GL45C.glProgramUniformMatrix3fv(program.id, this.location, true, this.floatValues);
			case FLOAT_MAT4x4 -> GL45C.glProgramUniformMatrix4fv(program.id, this.location, true, this.floatValues);
			case FLOAT_MAT2x3 -> GL45C.glProgramUniformMatrix2x3fv(program.id, this.location, true, this.floatValues);
			case FLOAT_MAT2x4 -> GL45C.glProgramUniformMatrix2x4fv(program.id, this.location, true, this.floatValues);
			case FLOAT_MAT3x2 -> GL45C.glProgramUniformMatrix3x2fv(program.id, this.location, true, this.floatValues);
			case FLOAT_MAT3x4 -> GL45C.glProgramUniformMatrix3x4fv(program.id, this.location, true, this.floatValues);
			case FLOAT_MAT4x2 -> GL45C.glProgramUniformMatrix4x2fv(program.id, this.location, true, this.floatValues);
			case FLOAT_MAT4x3 -> GL45C.glProgramUniformMatrix4x3fv(program.id, this.location, true, this.floatValues);
			default -> {
				return false;
			}
		}
		return true;
	}

	private boolean uploadSamplers(ShaderProgram program, UploadContext ctx) {
		if (this.type.componentType != ComponentType.SAMPLER)
			return false;

		for (int i = 0; i < this.textureValues.length; ++i) {
			if (this.textureValues[i] == null) {
				// TODO: report error
				return true;
			}
			if (this.textureValues[i].isDestroyed())
				continue;
			// FIXME: we change texture unit bindings but don't tell GlStateManager about
			// it, which means vanilla has the possibility of getting into a weird state.
			GL45C.glBindTextureUnit(ctx.currentTextureUnit, this.textureValues[i].id);
			GL45C.glProgramUniform1i(program.id, this.location, ctx.currentTextureUnit);
			ctx.currentTextureUnit += 1;
		}
		return true;
	}

	private boolean uploadFloats(ShaderProgram program) {
		switch (this.type.componentCount) {
			case 1 -> GL45C.glProgramUniform1fv(program.id, this.location, this.floatValues);
			case 2 -> GL45C.glProgramUniform2fv(program.id, this.location, this.floatValues);
			case 3 -> GL45C.glProgramUniform3fv(program.id, this.location, this.floatValues);
			case 4 -> GL45C.glProgramUniform4fv(program.id, this.location, this.floatValues);
			default -> {
				return false;
			}
		}
		return true;
	}

	private boolean uploadInts(ShaderProgram program) {
		switch (this.type.componentCount) {
			case 1 -> GL45C.glProgramUniform1iv(program.id, this.location, this.intValues);
			case 2 -> GL45C.glProgramUniform2iv(program.id, this.location, this.intValues);
			case 3 -> GL45C.glProgramUniform3iv(program.id, this.location, this.intValues);
			case 4 -> GL45C.glProgramUniform4iv(program.id, this.location, this.intValues);
			default -> {
				return false;
			}
		}
		return true;
	}

	void upload(ShaderProgram program, UploadContext ctx) {
		if (this.type.componentType != ComponentType.SAMPLER && !this.isDirty)
			return;
		this.isDirty = false;

		if (uploadSamplers(program, ctx))
			return;
		if (uploadMatrix(program))
			return;

		if (this.type.componentType.isFloatType) {
			uploadFloats(program);
		} else if (this.type.componentType.isIntType) {
			uploadInts(program);
		}
	}
}