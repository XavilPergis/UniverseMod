package net.xavil.hawklib.client.gl.shader;

import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.GlObject;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.hawklib.math.matrices.Vec3i;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec2Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec4Access;

public final class ShaderProgram extends GlObject {

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

	public static enum UniformType {
		// @formatter:off
		// ===== vectors =====
		FLOAT1 (GL32C.GL_FLOAT,             "float",  1, ComponentType.FLOAT),
		FLOAT2 (GL32C.GL_FLOAT_VEC2,        "vec2",   2, ComponentType.FLOAT),
		FLOAT3 (GL32C.GL_FLOAT_VEC3,        "vec3",   3, ComponentType.FLOAT),
		FLOAT4 (GL32C.GL_FLOAT_VEC4,        "vec4",   4, ComponentType.FLOAT),
		// weirdly enough, this can be returned by glGetActiveUniform, even though it is
		// not actually possible to upload a uniform double!
		DOUBLE1(GL32C.GL_DOUBLE,            "double", 1, ComponentType.INVALID),
		INT1   (GL32C.GL_INT,               "int",    1, ComponentType.INT),
		INT2   (GL32C.GL_INT_VEC2,          "ivec2",  2, ComponentType.INT),
		INT3   (GL32C.GL_INT_VEC3,          "ivec3",  3, ComponentType.INT),
		INT4   (GL32C.GL_INT_VEC4,          "ivec4",  4, ComponentType.INT),
		UINT1  (GL32C.GL_UNSIGNED_INT,      "uint",   1, ComponentType.UINT),
		UINT2  (GL32C.GL_UNSIGNED_INT_VEC2, "uvec2",  2, ComponentType.UINT),
		UINT3  (GL32C.GL_UNSIGNED_INT_VEC3, "uvec3",  3, ComponentType.UINT),
		UINT4  (GL32C.GL_UNSIGNED_INT_VEC4, "uvec4",  4, ComponentType.UINT),
		// booleans are uploaded via glUniform*i
		BOOL1  (GL32C.GL_BOOL,              "bool",   1, ComponentType.INT),
		BOOL2  (GL32C.GL_BOOL_VEC2,         "bvec2",  2, ComponentType.INT),
		BOOL3  (GL32C.GL_BOOL_VEC3,         "bvec3",  3, ComponentType.INT),
		BOOL4  (GL32C.GL_BOOL_VEC4,         "bvec4",  4, ComponentType.INT),

		// ===== matrices =====
		FLOAT_MAT2x2(GL32C.GL_FLOAT_MAT2,   "mat2",   4,  ComponentType.FLOAT),
		FLOAT_MAT3x3(GL32C.GL_FLOAT_MAT3,   "mat3",   9,  ComponentType.FLOAT),
		FLOAT_MAT4x4(GL32C.GL_FLOAT_MAT4,   "mat4",   16, ComponentType.FLOAT),
		FLOAT_MAT2x3(GL32C.GL_FLOAT_MAT2x3, "mat2x3", 6,  ComponentType.FLOAT),
		FLOAT_MAT2x4(GL32C.GL_FLOAT_MAT2x4, "mat2x4", 8,  ComponentType.FLOAT),
		FLOAT_MAT3x2(GL32C.GL_FLOAT_MAT3x2, "mat3x2", 6,  ComponentType.FLOAT),
		FLOAT_MAT3x4(GL32C.GL_FLOAT_MAT3x4, "mat3x4", 12, ComponentType.FLOAT),
		FLOAT_MAT4x2(GL32C.GL_FLOAT_MAT4x2, "mat4x2", 8,  ComponentType.FLOAT),
		FLOAT_MAT4x3(GL32C.GL_FLOAT_MAT4x3, "mat4x3", 12, ComponentType.FLOAT),

		// ===== samplers =====
		SAMPLER_1D                       (GL32C.GL_SAMPLER_1D,                                "sampler1D",            GlTexture.Type.D1,          GlTexture.SamplerType.FLOAT),
		SAMPLER_2D                       (GL32C.GL_SAMPLER_2D,                                "sampler2D",            GlTexture.Type.D2,          GlTexture.SamplerType.FLOAT),
		SAMPLER_3D                       (GL32C.GL_SAMPLER_3D,                                "sampler3D",            GlTexture.Type.D3,          GlTexture.SamplerType.FLOAT),
		SAMPLER_CUBE                     (GL32C.GL_SAMPLER_CUBE,                              "samplerCube",          GlTexture.Type.CUBEMAP,     GlTexture.SamplerType.FLOAT),
		SAMPLER_1D_ARRAY                 (GL32C.GL_SAMPLER_1D_ARRAY,                          "sampler1DArray",       GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_ARRAY                 (GL32C.GL_SAMPLER_2D_ARRAY,                          "sampler2DArray",       GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_MULTISAMPLE           (GL32C.GL_SAMPLER_2D_MULTISAMPLE,                    "sampler2DMS",          GlTexture.Type.D2_MS,       GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_MULTISAMPLE_ARRAY     (GL32C.GL_SAMPLER_2D_MULTISAMPLE_ARRAY,              "sampler2DMSArray",     GlTexture.Type.D2_MS_ARRAY, GlTexture.SamplerType.FLOAT),
		SAMPLER_BUFFER                   (GL32C.GL_SAMPLER_BUFFER,                            "samplerBuffer",        GlTexture.Type.BUFFER,      GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_RECT                  (GL32C.GL_SAMPLER_2D_RECT,                           "sampler2DRect",        GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.FLOAT),
		SAMPLER_INT_1D                   (GL32C.GL_INT_SAMPLER_1D,                            "isampler1D",           GlTexture.Type.D1,          GlTexture.SamplerType.INT),
		SAMPLER_INT_2D                   (GL32C.GL_INT_SAMPLER_2D,                            "isampler2D",           GlTexture.Type.D2,          GlTexture.SamplerType.INT),
		SAMPLER_INT_3D                   (GL32C.GL_INT_SAMPLER_3D,                            "isampler3D",           GlTexture.Type.D3,          GlTexture.SamplerType.INT),
		SAMPLER_INT_CUBE                 (GL32C.GL_INT_SAMPLER_CUBE,                          "isamplerCube",         GlTexture.Type.CUBEMAP,     GlTexture.SamplerType.INT),
		SAMPLER_INT_1D_ARRAY             (GL32C.GL_INT_SAMPLER_1D_ARRAY,                      "isampler1DArray",      GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_ARRAY             (GL32C.GL_INT_SAMPLER_2D_ARRAY,                      "isampler2DArray",      GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_MULTISAMPLE       (GL32C.GL_INT_SAMPLER_2D_MULTISAMPLE,                "isampler2DMS",         GlTexture.Type.D2_MS,       GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_MULTISAMPLE_ARRAY (GL32C.GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY,          "isampler2DMSArray",    GlTexture.Type.D2_MS_ARRAY, GlTexture.SamplerType.INT),
		SAMPLER_INT_BUFFER               (GL32C.GL_INT_SAMPLER_BUFFER,                        "isamplerBuffer",       GlTexture.Type.BUFFER,      GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_RECT              (GL32C.GL_INT_SAMPLER_2D_RECT,                       "isampler2DRect",       GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.INT),
		SAMPLER_UINT_1D                  (GL32C.GL_UNSIGNED_INT_SAMPLER_1D,                   "usampler1D",           GlTexture.Type.D1,          GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D                  (GL32C.GL_UNSIGNED_INT_SAMPLER_2D,                   "usampler2D",           GlTexture.Type.D2,          GlTexture.SamplerType.UINT),
		SAMPLER_UINT_3D                  (GL32C.GL_UNSIGNED_INT_SAMPLER_3D,                   "usampler3D",           GlTexture.Type.D3,          GlTexture.SamplerType.UINT),
		SAMPLER_UINT_CUBE                (GL32C.GL_UNSIGNED_INT_SAMPLER_CUBE,                 "usamplerCube",         GlTexture.Type.CUBEMAP,     GlTexture.SamplerType.UINT),
		SAMPLER_UINT_1D_ARRAY            (GL32C.GL_UNSIGNED_INT_SAMPLER_1D_ARRAY,             "usampler2DArray",      GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_ARRAY            (GL32C.GL_UNSIGNED_INT_SAMPLER_2D_ARRAY,             "usampler2DArray",      GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_MULTISAMPLE      (GL32C.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE,       "usampler2DMS",         GlTexture.Type.D2_MS,       GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_MULTISAMPLE_ARRAY(GL32C.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY, "usampler2DMSArray",    GlTexture.Type.D2_MS_ARRAY, GlTexture.SamplerType.UINT),
		SAMPLER_UINT_BUFFER              (GL32C.GL_UNSIGNED_INT_SAMPLER_BUFFER,               "usamplerBuffer",       GlTexture.Type.BUFFER,      GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_RECT             (GL32C.GL_UNSIGNED_INT_SAMPLER_2D_RECT,              "usampler2DRect",       GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.UINT),
		SAMPLER_SHADOW_1D                (GL32C.GL_SAMPLER_1D_SHADOW,                         "sampler1DShadow",      GlTexture.Type.D1,          GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_2D                (GL32C.GL_SAMPLER_2D_SHADOW,                         "sampler2DShadow",      GlTexture.Type.D2,          GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_1D_ARRAY          (GL32C.GL_SAMPLER_1D_ARRAY_SHADOW,                   "sampler1DArrayShadow", GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_2D_ARRAY          (GL32C.GL_SAMPLER_2D_ARRAY_SHADOW,                   "sampler2DArrayShadow", GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_CUBE              (GL32C.GL_SAMPLER_CUBE_SHADOW,                       "samplerCubeShadow",    GlTexture.Type.CUBEMAP,     GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_2D_RECT           (GL32C.GL_SAMPLER_2D_RECT_SHADOW,                    "sampler2DRectShadow",  GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.SHADOW);
		// @formatter:on

		public final int id;
		public final String description;
		public final ComponentType componentType;
		public final int componentCount;

		// texture sampler types
		public final GlTexture.Type textureType;
		public final GlTexture.SamplerType samplerType;

		private UniformType(int id, String description, int componentCount, ComponentType componentType) {
			this.id = id;
			this.description = description;
			this.componentCount = componentCount;
			this.componentType = componentType;
			this.textureType = null;
			this.samplerType = null;
		}

		private UniformType(int id, String description, GlTexture.Type textureType, GlTexture.SamplerType samplerType) {
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

		public static UniformType from(GlTexture.SamplerType samplerType, GlTexture.Type textureType) {
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

		public static UniformType from(int id) {
			return switch (id) {
				case GL32C.GL_FLOAT -> FLOAT1;
				case GL32C.GL_FLOAT_VEC2 -> FLOAT2;
				case GL32C.GL_FLOAT_VEC3 -> FLOAT3;
				case GL32C.GL_FLOAT_VEC4 -> FLOAT4;
				case GL32C.GL_DOUBLE -> DOUBLE1;
				case GL32C.GL_INT -> INT1;
				case GL32C.GL_INT_VEC2 -> INT2;
				case GL32C.GL_INT_VEC3 -> INT3;
				case GL32C.GL_INT_VEC4 -> INT4;
				case GL32C.GL_UNSIGNED_INT -> UINT1;
				case GL32C.GL_UNSIGNED_INT_VEC2 -> UINT2;
				case GL32C.GL_UNSIGNED_INT_VEC3 -> UINT3;
				case GL32C.GL_UNSIGNED_INT_VEC4 -> UINT4;
				case GL32C.GL_BOOL -> BOOL1;
				case GL32C.GL_BOOL_VEC2 -> BOOL2;
				case GL32C.GL_BOOL_VEC3 -> BOOL3;
				case GL32C.GL_BOOL_VEC4 -> BOOL4;
				case GL32C.GL_FLOAT_MAT2 -> FLOAT_MAT2x2;
				case GL32C.GL_FLOAT_MAT3 -> FLOAT_MAT3x3;
				case GL32C.GL_FLOAT_MAT4 -> FLOAT_MAT4x4;
				case GL32C.GL_FLOAT_MAT2x3 -> FLOAT_MAT2x3;
				case GL32C.GL_FLOAT_MAT2x4 -> FLOAT_MAT2x4;
				case GL32C.GL_FLOAT_MAT3x2 -> FLOAT_MAT3x2;
				case GL32C.GL_FLOAT_MAT3x4 -> FLOAT_MAT3x4;
				case GL32C.GL_FLOAT_MAT4x2 -> FLOAT_MAT4x2;
				case GL32C.GL_FLOAT_MAT4x3 -> FLOAT_MAT4x3;
				case GL32C.GL_SAMPLER_1D -> SAMPLER_1D;
				case GL32C.GL_SAMPLER_2D -> SAMPLER_2D;
				case GL32C.GL_SAMPLER_3D -> SAMPLER_3D;
				case GL32C.GL_SAMPLER_CUBE -> SAMPLER_CUBE;
				case GL32C.GL_SAMPLER_1D_ARRAY -> SAMPLER_1D_ARRAY;
				case GL32C.GL_SAMPLER_2D_ARRAY -> SAMPLER_2D_ARRAY;
				case GL32C.GL_SAMPLER_2D_MULTISAMPLE -> SAMPLER_2D_MULTISAMPLE;
				case GL32C.GL_SAMPLER_2D_MULTISAMPLE_ARRAY -> SAMPLER_2D_MULTISAMPLE_ARRAY;
				case GL32C.GL_SAMPLER_BUFFER -> SAMPLER_BUFFER;
				case GL32C.GL_SAMPLER_2D_RECT -> SAMPLER_2D_RECT;
				case GL32C.GL_INT_SAMPLER_1D -> SAMPLER_INT_1D;
				case GL32C.GL_INT_SAMPLER_2D -> SAMPLER_INT_2D;
				case GL32C.GL_INT_SAMPLER_3D -> SAMPLER_INT_3D;
				case GL32C.GL_INT_SAMPLER_CUBE -> SAMPLER_INT_CUBE;
				case GL32C.GL_INT_SAMPLER_1D_ARRAY -> SAMPLER_INT_1D_ARRAY;
				case GL32C.GL_INT_SAMPLER_2D_ARRAY -> SAMPLER_INT_2D_ARRAY;
				case GL32C.GL_INT_SAMPLER_2D_MULTISAMPLE -> SAMPLER_INT_2D_MULTISAMPLE;
				case GL32C.GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY -> SAMPLER_INT_2D_MULTISAMPLE_ARRAY;
				case GL32C.GL_INT_SAMPLER_BUFFER -> SAMPLER_INT_BUFFER;
				case GL32C.GL_INT_SAMPLER_2D_RECT -> SAMPLER_INT_2D_RECT;
				case GL32C.GL_UNSIGNED_INT_SAMPLER_1D -> SAMPLER_UINT_1D;
				case GL32C.GL_UNSIGNED_INT_SAMPLER_2D -> SAMPLER_UINT_2D;
				case GL32C.GL_UNSIGNED_INT_SAMPLER_3D -> SAMPLER_UINT_3D;
				case GL32C.GL_UNSIGNED_INT_SAMPLER_CUBE -> SAMPLER_UINT_CUBE;
				case GL32C.GL_UNSIGNED_INT_SAMPLER_1D_ARRAY -> SAMPLER_UINT_1D_ARRAY;
				case GL32C.GL_UNSIGNED_INT_SAMPLER_2D_ARRAY -> SAMPLER_UINT_2D_ARRAY;
				case GL32C.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE -> SAMPLER_UINT_2D_MULTISAMPLE;
				case GL32C.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY -> SAMPLER_UINT_2D_MULTISAMPLE_ARRAY;
				case GL32C.GL_UNSIGNED_INT_SAMPLER_BUFFER -> SAMPLER_UINT_BUFFER;
				case GL32C.GL_UNSIGNED_INT_SAMPLER_2D_RECT -> SAMPLER_UINT_2D_RECT;
				case GL32C.GL_SAMPLER_1D_SHADOW -> SAMPLER_SHADOW_1D;
				case GL32C.GL_SAMPLER_2D_SHADOW -> SAMPLER_SHADOW_2D;
				case GL32C.GL_SAMPLER_1D_ARRAY_SHADOW -> SAMPLER_SHADOW_1D_ARRAY;
				case GL32C.GL_SAMPLER_2D_ARRAY_SHADOW -> SAMPLER_SHADOW_2D_ARRAY;
				case GL32C.GL_SAMPLER_CUBE_SHADOW -> SAMPLER_SHADOW_CUBE;
				case GL32C.GL_SAMPLER_2D_RECT_SHADOW -> SAMPLER_SHADOW_2D_RECT;
				default -> null;
			};
		}
	}

	public ShaderProgram(int id, boolean owned) {
		super(id, owned);
	}

	public ShaderProgram(ShaderInstance imported) {
		super(imported.getId(), false);
		this.wrappedVanillaShader = imported;
		queryUniforms();
		setupAttribBindings(imported.getVertexFormat(), false);
		setupFragLocations(GlFragmentWrites.VANILLA);
	}

	public ShaderProgram() {
		super(GlManager.createProgram(), true);
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.PROGRAM;
	}

	@Override
	public String debugDescription() {
		var desc = super.debugDescription();
		if (this.loadedFrom != null)
			desc += " (" + this.loadedFrom.toString() + ")";
		return desc;
	}

	private final class UploadContext {
		public int currentTextureUnit = 0;
	}

	public static class UniformSlot {
		public final UniformType type;
		public final String name;
		public final int size;
		public final int location;

		private boolean isDirty = false;
		private int[] intValues = null;
		private float[] floatValues = null;
		private GlTexture textureValue = null;

		private long lastTypeMismatchTime = Long.MIN_VALUE;

		public UniformSlot(UniformType type, String name, int size, int location) {
			this.type = type;
			this.name = name;
			this.size = size;
			this.location = location;

			if (this.type.componentType.isFloatType) {
				this.floatValues = new float[this.type.componentCount];
			} else if (this.type.componentType.isIntType) {
				this.intValues = new int[this.type.componentCount];
			}
		}

		public static UniformSlot query(ShaderProgram shader, int index) {
			try (final var stack = MemoryStack.stackPush()) {
				final var sizeBuffer = stack.mallocInt(1);
				final var typeBuffer = stack.mallocInt(1);
				final var name = GL32C.glGetActiveUniform(shader.id, index, sizeBuffer, typeBuffer);

				if (name.startsWith("GL32C.gl_"))
					return null;

				final var type = UniformType.from(typeBuffer.get(0));
				final var size = sizeBuffer.get(0);

				final var location = GL32C.glGetUniformLocation(shader.id, name);
				return new UniformSlot(type, name, size, location);
			}
		}

		private boolean setTexture(GlTexture texture) {
			final var old = this.textureValue;
			this.textureValue = texture;
			return texture != old;
		}

		private boolean setInt(int i, int value) {
			final var old = this.intValues[i];
			this.intValues[i] = value;
			return value != old;
		}

		private boolean setFloat(int i, float value) {
			final var old = this.floatValues[i];
			this.floatValues[i] = value;
			return value != old;
		}

		private boolean uploadMatrix() {
			switch (this.type) {
				case FLOAT_MAT2x2 -> GL32C.glUniformMatrix2fv(this.location, true, this.floatValues);
				case FLOAT_MAT3x3 -> GL32C.glUniformMatrix3fv(this.location, true, this.floatValues);
				case FLOAT_MAT4x4 -> GL32C.glUniformMatrix4fv(this.location, true, this.floatValues);
				case FLOAT_MAT2x3 -> GL32C.glUniformMatrix2x3fv(this.location, true, this.floatValues);
				case FLOAT_MAT2x4 -> GL32C.glUniformMatrix2x4fv(this.location, true, this.floatValues);
				case FLOAT_MAT3x2 -> GL32C.glUniformMatrix3x2fv(this.location, true, this.floatValues);
				case FLOAT_MAT3x4 -> GL32C.glUniformMatrix3x4fv(this.location, true, this.floatValues);
				case FLOAT_MAT4x2 -> GL32C.glUniformMatrix4x2fv(this.location, true, this.floatValues);
				case FLOAT_MAT4x3 -> GL32C.glUniformMatrix4x3fv(this.location, true, this.floatValues);
				default -> {
					return false;
				}
			}
			return true;
		}

		private boolean uploadSampler(UploadContext ctx) {
			if (this.type.componentType == ComponentType.SAMPLER) {
				if (this.textureValue == null) {
					// TODO: report error
					return true;
				}
				GlManager.activeTexture(GL32C.GL_TEXTURE0 + ctx.currentTextureUnit);
				this.textureValue.bind();
				GL32C.glUniform1i(this.location, ctx.currentTextureUnit);
				ctx.currentTextureUnit += 1;
				return true;
			}
			return false;
		}

		private boolean uploadFloats() {
			final var v = this.floatValues;
			switch (this.type.componentCount) {
				case 1 -> GL32C.glUniform1f(this.location, v[0]);
				case 2 -> GL32C.glUniform2f(this.location, v[0], v[1]);
				case 3 -> GL32C.glUniform3f(this.location, v[0], v[1], v[2]);
				case 4 -> GL32C.glUniform4f(this.location, v[0], v[1], v[2], v[3]);
				default -> {
					return false;
				}
			}
			return true;
		}

		private boolean uploadInts() {
			final var v = this.intValues;
			switch (this.type.componentCount) {
				case 1 -> GL32C.glUniform1i(this.location, v[0]);
				case 2 -> GL32C.glUniform2i(this.location, v[0], v[1]);
				case 3 -> GL32C.glUniform3i(this.location, v[0], v[1], v[2]);
				case 4 -> GL32C.glUniform4i(this.location, v[0], v[1], v[2], v[3]);
				default -> {
					return false;
				}
			}
			return true;
		}

		private void upload(UploadContext ctx) {
			if (this.type.componentType != ComponentType.SAMPLER && !this.isDirty)
				return;
			if (uploadSampler(ctx)) {
			} else if (uploadMatrix()) {
			} else if (this.type.componentType.isFloatType) {
				uploadFloats();
			} else if (this.type.componentType.isIntType) {
				uploadInts();
			}
			this.isDirty = false;
		}
	}

	public static final class AttributeSlot {
		public final String name;
		public final VertexFormatElement element;

		public AttributeSlot(String name, VertexFormatElement element) {
			this.name = name;
			this.element = element;
		}
	}

	private long errorReportThresholdNs = 100000000L;
	private ResourceLocation loadedFrom = null;
	private boolean areUniformsDirty = false;
	private boolean hasAnySamplerUniform = false;
	private final MutableMap<String, UniformSlot> uniforms = MutableMap.hashMap();
	private final MutableMap<String, AttributeSlot> attributes = MutableMap.hashMap();
	private VertexFormat format;
	private GlFragmentWrites fragmentWrites;

	private ShaderInstance wrappedVanillaShader;

	public VertexFormat format() {
		return this.format;
	}

	public Iterator<UniformSlot> uniforms() {
		return this.uniforms.values();
	}

	public Iterator<AttributeSlot> attributes() {
		return this.attributes.values();
	}

	public GlFragmentWrites fragmentWrites() {
		return this.fragmentWrites;
	}

	public ShaderInstance getWrappedVanillaShader() {
		return this.wrappedVanillaShader;
	}

	public void attachShader(ShaderStage shader) {
		GlStateManager.glAttachShader(this.id, shader.id);
	}

	public String infoLog() {
		return GL32C.glGetProgramInfoLog(this.id);
	}

	public boolean link(VertexFormat format, GlFragmentWrites fragmentWrites) {
		if (!this.owned) {
			HawkLib.LOGGER.warn("{}: linking ShaderProgram that is not owned!", toString());
		}
		GL32C.glLinkProgram(this.id);
		final var status = GL32C.glGetProgrami(this.id, GL32C.GL_LINK_STATUS);
		if (status == GL32C.GL_FALSE)
			return false;
		this.format = format;
		this.uniforms.clear();
		this.attributes.clear();
		this.hasAnySamplerUniform = false;
		queryUniforms();
		setupAttribBindings(format, true);
		setupFragLocations(fragmentWrites);
		return true;
	}

	private void queryUniforms() {
		final var uniformCount = GL32C.glGetProgrami(this.id, GL32C.GL_ACTIVE_UNIFORMS);
		for (int i = 0; i < uniformCount; ++i) {
			final var slot = UniformSlot.query(this, i);
			if (slot != null) {
				this.uniforms.insert(slot.name, slot);
				this.hasAnySamplerUniform |= slot.type.componentType == ComponentType.SAMPLER;
			}
		}
	}

	private void setupAttribBindings(VertexFormat format, boolean bind) {
		final var attribNames = format.getElementAttributeNames();
		final var attribs = format.getElements();
		for (int i = 0; i < attribs.size(); ++i) {
			final var attrib = attribs.get(i);
			final var attribName = attribNames.get(i);
			if (attrib.getUsage() == VertexFormatElement.Usage.PADDING)
				continue;
			if (bind)
				GL32C.glBindAttribLocation(this.id, i, attribName);
			this.attributes.insert(attribName, new AttributeSlot(attribName, attrib));
		}
	}

	private void setupFragLocations(GlFragmentWrites fragmentWrites) {
		final var count = fragmentWrites.getFragmentWriteCount();
		for (int i = 0; i < count; ++i) {
			GL32C.glBindFragDataLocation(this.id, i, fragmentWrites.getFragmentWriteName(i));
		}
		this.fragmentWrites = fragmentWrites;
	}

	private boolean checkType(UniformSlot slot, UniformType requestedType) {
		if (slot.type != requestedType) {
			// we want to avoid spamming the console, so we check if an error has occurred
			// on this slot within the past tenth of a second or so, and discard our output
			// if it has.
			final var currentTime = System.nanoTime();
			if (slot.lastTypeMismatchTime < currentTime - this.errorReportThresholdNs) {
				HawkLib.LOGGER.error("[{}] Cannot assign a value of type '{}' to uniform '{}' with type '{}'",
						debugDescription(), requestedType.description, slot.name, slot.type.description);
			}
			slot.lastTypeMismatchTime = currentTime;
			return false;
		}
		return true;
	}

	private UniformSlot getSlot(String uniformName, UniformType type) {
		final var slot = this.uniforms.get(uniformName).unwrapOrNull();
		if (slot == null || !checkType(slot, type))
			return null;
		return slot;
	}

	public void setUniformSampler(String uniformName, GlTexture texture) {
		final var uniformType = UniformType.from(texture.format().samplerType, texture.type);
		final var slot = getSlot(uniformName, uniformType);
		if (slot != null) {
			boolean d = false;
			d |= slot.setTexture(texture);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniform(String uniformName, int v0) {
		final var slot = getSlot(uniformName, UniformType.INT1);
		if (slot != null) {
			boolean d = false;
			d |= slot.setInt(0, v0);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniform(String uniformName, int v0, int v1) {
		final var slot = getSlot(uniformName, UniformType.INT2);
		if (slot != null) {
			boolean d = false;
			d |= slot.setInt(0, v0);
			d |= slot.setInt(1, v1);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniform(String uniformName, int v0, int v1, int v2) {
		final var slot = getSlot(uniformName, UniformType.INT3);
		if (slot != null) {
			boolean d = false;
			d |= slot.setInt(0, v0);
			d |= slot.setInt(1, v1);
			d |= slot.setInt(2, v2);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniform(String uniformName, int v0, int v1, int v2, int v3) {
		final var slot = getSlot(uniformName, UniformType.INT4);
		if (slot != null) {
			boolean d = false;
			d |= slot.setInt(0, v0);
			d |= slot.setInt(1, v1);
			d |= slot.setInt(2, v2);
			d |= slot.setInt(3, v3);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniform(String uniformName, float v0) {
		final var slot = getSlot(uniformName, UniformType.FLOAT1);
		if (slot != null) {
			boolean d = false;
			d |= slot.setFloat(0, v0);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniform(String uniformName, float v0, float v1) {
		final var slot = getSlot(uniformName, UniformType.FLOAT2);
		if (slot != null) {
			boolean d = false;
			d |= slot.setFloat(0, v0);
			d |= slot.setFloat(1, v1);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniform(String uniformName, float v0, float v1, float v2) {
		final var slot = getSlot(uniformName, UniformType.FLOAT3);
		if (slot != null) {
			boolean d = false;
			d |= slot.setFloat(0, v0);
			d |= slot.setFloat(1, v1);
			d |= slot.setFloat(2, v2);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniform(String uniformName, float v0, float v1, float v2, float v3) {
		final var slot = getSlot(uniformName, UniformType.FLOAT4);
		if (slot != null) {
			boolean d = false;
			d |= slot.setFloat(0, v0);
			d |= slot.setFloat(1, v1);
			d |= slot.setFloat(2, v2);
			d |= slot.setFloat(3, v3);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniformMatrix2x2(String uniformName,
			float r0c0, float r0c1,
			float r1c0, float r1c1) {
		final var slot = getSlot(uniformName, UniformType.FLOAT_MAT2x2);
		if (slot != null) {
			boolean d = false;
			int i = 0;
			d |= slot.setFloat(i++, r0c0);
			d |= slot.setFloat(i++, r0c1);
			d |= slot.setFloat(i++, r1c0);
			d |= slot.setFloat(i++, r1c1);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniformMatrix3x3(String uniformName,
			float r0c0, float r0c1, float r0c2,
			float r1c0, float r1c1, float r1c2,
			float r2c0, float r2c1, float r2c2) {
		final var slot = getSlot(uniformName, UniformType.FLOAT_MAT3x3);
		if (slot != null) {
			boolean d = false;
			int i = 0;
			d |= slot.setFloat(i++, r0c0);
			d |= slot.setFloat(i++, r0c1);
			d |= slot.setFloat(i++, r0c2);
			d |= slot.setFloat(i++, r1c0);
			d |= slot.setFloat(i++, r1c1);
			d |= slot.setFloat(i++, r1c2);
			d |= slot.setFloat(i++, r2c0);
			d |= slot.setFloat(i++, r2c1);
			d |= slot.setFloat(i++, r2c2);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniformMatrix4x4(String uniformName,
			float r0c0, float r0c1, float r0c2, float r0c3,
			float r1c0, float r1c1, float r1c2, float r1c3,
			float r2c0, float r2c1, float r2c2, float r2c3,
			float r3c0, float r3c1, float r3c2, float r3c3) {
		final var slot = getSlot(uniformName, UniformType.FLOAT_MAT4x4);
		if (slot != null) {
			boolean d = false;
			int i = 0;
			d |= slot.setFloat(i++, r0c0);
			d |= slot.setFloat(i++, r0c1);
			d |= slot.setFloat(i++, r0c2);
			d |= slot.setFloat(i++, r0c3);
			d |= slot.setFloat(i++, r1c0);
			d |= slot.setFloat(i++, r1c1);
			d |= slot.setFloat(i++, r1c2);
			d |= slot.setFloat(i++, r1c3);
			d |= slot.setFloat(i++, r2c0);
			d |= slot.setFloat(i++, r2c1);
			d |= slot.setFloat(i++, r2c2);
			d |= slot.setFloat(i++, r2c3);
			d |= slot.setFloat(i++, r3c0);
			d |= slot.setFloat(i++, r3c1);
			d |= slot.setFloat(i++, r3c2);
			d |= slot.setFloat(i++, r3c3);
			this.areUniformsDirty |= slot.isDirty |= d;
		}
	}

	public void setUniform(String uniformName, double v0) {
		setUniform(uniformName, (float) v0);
	}

	public void setUniform(String uniformName, double v0, double v1) {
		setUniform(uniformName, (float) v0, (float) v1);
	}

	public void setUniform(String uniformName, double v0, double v1, double v2) {
		setUniform(uniformName, (float) v0, (float) v1, (float) v2);
	}

	public void setUniform(String uniformName, double v0, double v1, double v2, double v3) {
		setUniform(uniformName, (float) v0, (float) v1, (float) v2, (float) v3);
	}

	public void setUniform(String uniformName, Vec2i v) {
		setUniform(uniformName, v.x, v.y);
	}

	public void setUniform(String uniformName, Vec3i v) {
		setUniform(uniformName, v.x, v.y, v.z);
	}

	public void setUniform(String uniformName, Vec2Access v) {
		setUniform(uniformName, v.x(), v.y());
	}

	public void setUniform(String uniformName, Vec3Access v) {
		setUniform(uniformName, v.x(), v.y(), v.z());
	}

	public void setUniform(String uniformName, Vec4Access v) {
		setUniform(uniformName, v.x(), v.y(), v.z(), v.w());
	}

	public void setUniform(String uniformName, Color color) {
		setUniform(uniformName, color.r(), color.g(), color.b(), color.a());
	}

	public void setUniform(String uniformName, Mat4Access v) {
		setUniformMatrix4x4(uniformName,
				(float) v.r0c0(), (float) v.r0c1(), (float) v.r0c2(), (float) v.r0c3(),
				(float) v.r1c0(), (float) v.r1c1(), (float) v.r1c2(), (float) v.r1c3(),
				(float) v.r2c0(), (float) v.r2c1(), (float) v.r2c2(), (float) v.r2c3(),
				(float) v.r3c0(), (float) v.r3c1(), (float) v.r3c2(), (float) v.r3c3());
	}

	public void bind() {
		GlManager.useProgram(this.id);
		if (this.hasAnySamplerUniform || this.areUniformsDirty) {
			// since texture unit binding can change sorta whenever, we always have to check
			// that the texture currently stored in a sampler uniform is the active texture
			// for the current texture unit.
			final var prevActiveTexture = GlManager.currentState().boundTextureUnit;
			final var uploadContext = new UploadContext();
			this.uniforms.values().forEach(slot -> slot.upload(uploadContext));
			GlManager.activeTexture(prevActiveTexture);
			this.areUniformsDirty = false;
		}
		if (this.wrappedVanillaShader != null) {
			this.wrappedVanillaShader.apply();
		}
	}

	public static void unbind() {
		GlManager.useProgram(0);
	}

}
