package net.xavil.hawklib.client.gl.shader;

import java.lang.ref.WeakReference;

import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.ultraviolet.Mod;

public final class UniformSlot {

	public static enum ComponentType {
		INT(true, false),
		UINT(true, false),
		FLOAT(false, true),
		SAMPLER(true, false),
		IMAGE(true, false),
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
		SAMPLER_1D                       (GL45C.GL_SAMPLER_1D,                                "sampler1D",              GlTexture.Type.D1,          GlTexture.SamplerType.FLOAT),
		SAMPLER_2D                       (GL45C.GL_SAMPLER_2D,                                "sampler2D",              GlTexture.Type.D2,          GlTexture.SamplerType.FLOAT),
		SAMPLER_3D                       (GL45C.GL_SAMPLER_3D,                                "sampler3D",              GlTexture.Type.D3,          GlTexture.SamplerType.FLOAT),
		SAMPLER_CUBE                     (GL45C.GL_SAMPLER_CUBE,                              "samplerCube",            GlTexture.Type.CUBE,        GlTexture.SamplerType.FLOAT),
		SAMPLER_CUBE_ARRAY               (GL45C.GL_SAMPLER_CUBE_MAP_ARRAY,                    "samplerCubeArray",       GlTexture.Type.CUBE_ARRAY,  GlTexture.SamplerType.FLOAT),
		SAMPLER_1D_ARRAY                 (GL45C.GL_SAMPLER_1D_ARRAY,                          "sampler1DArray",         GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_ARRAY                 (GL45C.GL_SAMPLER_2D_ARRAY,                          "sampler2DArray",         GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_MULTISAMPLE           (GL45C.GL_SAMPLER_2D_MULTISAMPLE,                    "sampler2DMS",            GlTexture.Type.D2_MS,       GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_MULTISAMPLE_ARRAY     (GL45C.GL_SAMPLER_2D_MULTISAMPLE_ARRAY,              "sampler2DMSArray",       GlTexture.Type.D2_MS_ARRAY, GlTexture.SamplerType.FLOAT),
		SAMPLER_BUFFER                   (GL45C.GL_SAMPLER_BUFFER,                            "samplerBuffer",          GlTexture.Type.BUFFER,      GlTexture.SamplerType.FLOAT),
		SAMPLER_2D_RECT                  (GL45C.GL_SAMPLER_2D_RECT,                           "sampler2DRect",          GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.FLOAT),
		SAMPLER_INT_1D                   (GL45C.GL_INT_SAMPLER_1D,                            "isampler1D",             GlTexture.Type.D1,          GlTexture.SamplerType.INT),
		SAMPLER_INT_2D                   (GL45C.GL_INT_SAMPLER_2D,                            "isampler2D",             GlTexture.Type.D2,          GlTexture.SamplerType.INT),
		SAMPLER_INT_3D                   (GL45C.GL_INT_SAMPLER_3D,                            "isampler3D",             GlTexture.Type.D3,          GlTexture.SamplerType.INT),
		SAMPLER_INT_CUBE                 (GL45C.GL_INT_SAMPLER_CUBE,                          "isamplerCube",           GlTexture.Type.CUBE,        GlTexture.SamplerType.INT),
		SAMPLER_INT_CUBE_ARRAY           (GL45C.GL_INT_SAMPLER_CUBE,                          "isamplerCubeArray",      GlTexture.Type.CUBE_ARRAY,  GlTexture.SamplerType.INT),
		SAMPLER_INT_1D_ARRAY             (GL45C.GL_INT_SAMPLER_1D_ARRAY,                      "isampler1DArray",        GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_ARRAY             (GL45C.GL_INT_SAMPLER_2D_ARRAY,                      "isampler2DArray",        GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_MULTISAMPLE       (GL45C.GL_INT_SAMPLER_2D_MULTISAMPLE,                "isampler2DMS",           GlTexture.Type.D2_MS,       GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_MULTISAMPLE_ARRAY (GL45C.GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY,          "isampler2DMSArray",      GlTexture.Type.D2_MS_ARRAY, GlTexture.SamplerType.INT),
		SAMPLER_INT_BUFFER               (GL45C.GL_INT_SAMPLER_BUFFER,                        "isamplerBuffer",         GlTexture.Type.BUFFER,      GlTexture.SamplerType.INT),
		SAMPLER_INT_2D_RECT              (GL45C.GL_INT_SAMPLER_2D_RECT,                       "isampler2DRect",         GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.INT),
		SAMPLER_UINT_1D                  (GL45C.GL_UNSIGNED_INT_SAMPLER_1D,                   "usampler1D",             GlTexture.Type.D1,          GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D                  (GL45C.GL_UNSIGNED_INT_SAMPLER_2D,                   "usampler2D",             GlTexture.Type.D2,          GlTexture.SamplerType.UINT),
		SAMPLER_UINT_3D                  (GL45C.GL_UNSIGNED_INT_SAMPLER_3D,                   "usampler3D",             GlTexture.Type.D3,          GlTexture.SamplerType.UINT),
		SAMPLER_UINT_CUBE                (GL45C.GL_UNSIGNED_INT_SAMPLER_CUBE,                 "usamplerCube",           GlTexture.Type.CUBE,        GlTexture.SamplerType.UINT),
		SAMPLER_UINT_CUBE_ARRAY          (GL45C.GL_UNSIGNED_INT_SAMPLER_CUBE,                 "usamplerCubeArray",      GlTexture.Type.CUBE_ARRAY,  GlTexture.SamplerType.UINT),
		SAMPLER_UINT_1D_ARRAY            (GL45C.GL_UNSIGNED_INT_SAMPLER_1D_ARRAY,             "usampler2DArray",        GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_ARRAY            (GL45C.GL_UNSIGNED_INT_SAMPLER_2D_ARRAY,             "usampler2DArray",        GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_MULTISAMPLE      (GL45C.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE,       "usampler2DMS",           GlTexture.Type.D2_MS,       GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_MULTISAMPLE_ARRAY(GL45C.GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY, "usampler2DMSArray",      GlTexture.Type.D2_MS_ARRAY, GlTexture.SamplerType.UINT),
		SAMPLER_UINT_BUFFER              (GL45C.GL_UNSIGNED_INT_SAMPLER_BUFFER,               "usamplerBuffer",         GlTexture.Type.BUFFER,      GlTexture.SamplerType.UINT),
		SAMPLER_UINT_2D_RECT             (GL45C.GL_UNSIGNED_INT_SAMPLER_2D_RECT,              "usampler2DRect",         GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.UINT),
		SAMPLER_SHADOW_1D                (GL45C.GL_SAMPLER_1D_SHADOW,                         "sampler1DShadow",        GlTexture.Type.D1,          GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_2D                (GL45C.GL_SAMPLER_2D_SHADOW,                         "sampler2DShadow",        GlTexture.Type.D2,          GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_1D_ARRAY          (GL45C.GL_SAMPLER_1D_ARRAY_SHADOW,                   "sampler1DArrayShadow",   GlTexture.Type.D1_ARRAY,    GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_2D_ARRAY          (GL45C.GL_SAMPLER_2D_ARRAY_SHADOW,                   "sampler2DArrayShadow",   GlTexture.Type.D2_ARRAY,    GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_CUBE              (GL45C.GL_SAMPLER_CUBE_SHADOW,                       "samplerCubeShadow",      GlTexture.Type.CUBE,        GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_CUBE_ARRAY        (GL45C.GL_SAMPLER_CUBE_MAP_ARRAY_SHADOW,             "samplerCubeArrayShadow", GlTexture.Type.CUBE_ARRAY,  GlTexture.SamplerType.SHADOW),
		SAMPLER_SHADOW_2D_RECT           (GL45C.GL_SAMPLER_2D_RECT_SHADOW,                    "sampler2DRectShadow",    GlTexture.Type.RECTANGLE,   GlTexture.SamplerType.SHADOW),

		// ===== images =====
		IMAGE_1D                       (GL45C.GL_IMAGE_1D,                                "image1D",         GlTexture.Type.D1),
		IMAGE_2D                       (GL45C.GL_IMAGE_2D,                                "image2D",         GlTexture.Type.D2),
		IMAGE_3D                       (GL45C.GL_IMAGE_3D,                                "image3D",         GlTexture.Type.D3),
		IMAGE_CUBE                     (GL45C.GL_IMAGE_CUBE,                              "imageCube",       GlTexture.Type.CUBE),
		IMAGE_CUBE_ARRAY               (GL45C.GL_IMAGE_CUBE_MAP_ARRAY,                    "imageCubeArray",  GlTexture.Type.CUBE_ARRAY),
		IMAGE_1D_ARRAY                 (GL45C.GL_IMAGE_1D_ARRAY,                          "image1DArray",    GlTexture.Type.D1_ARRAY),
		IMAGE_2D_ARRAY                 (GL45C.GL_IMAGE_2D_ARRAY,                          "image2DArray",    GlTexture.Type.D2_ARRAY),
		IMAGE_2D_MULTISAMPLE           (GL45C.GL_IMAGE_2D_MULTISAMPLE,                    "image2DMS",       GlTexture.Type.D2_MS),
		IMAGE_2D_MULTISAMPLE_ARRAY     (GL45C.GL_IMAGE_2D_MULTISAMPLE_ARRAY,              "image2DMSArray",  GlTexture.Type.D2_MS_ARRAY),
		IMAGE_BUFFER                   (GL45C.GL_IMAGE_BUFFER,                            "imageBuffer",     GlTexture.Type.BUFFER),
		IMAGE_2D_RECT                  (GL45C.GL_IMAGE_2D_RECT,                           "image2DRect",     GlTexture.Type.RECTANGLE),
		IMAGE_INT_1D                   (GL45C.GL_INT_IMAGE_1D,                            "iimage1D",        GlTexture.Type.D1),
		IMAGE_INT_2D                   (GL45C.GL_INT_IMAGE_2D,                            "iimage2D",        GlTexture.Type.D2),
		IMAGE_INT_3D                   (GL45C.GL_INT_IMAGE_3D,                            "iimage3D",        GlTexture.Type.D3),
		IMAGE_INT_CUBE                 (GL45C.GL_INT_IMAGE_CUBE,                          "iimageCube",      GlTexture.Type.CUBE),
		IMAGE_INT_CUBE_ARRAY           (GL45C.GL_INT_IMAGE_CUBE_MAP_ARRAY,                "iimageCubeArray", GlTexture.Type.CUBE_ARRAY),
		IMAGE_INT_1D_ARRAY             (GL45C.GL_INT_IMAGE_1D_ARRAY,                      "iimage1DArray",   GlTexture.Type.D1_ARRAY),
		IMAGE_INT_2D_ARRAY             (GL45C.GL_INT_IMAGE_2D_ARRAY,                      "iimage2DArray",   GlTexture.Type.D2_ARRAY),
		IMAGE_INT_2D_MULTISAMPLE       (GL45C.GL_INT_IMAGE_2D_MULTISAMPLE,                "iimage2DMS",      GlTexture.Type.D2_MS),
		IMAGE_INT_2D_MULTISAMPLE_ARRAY (GL45C.GL_INT_IMAGE_2D_MULTISAMPLE_ARRAY,          "iimage2DMSArray", GlTexture.Type.D2_MS_ARRAY),
		IMAGE_INT_BUFFER               (GL45C.GL_INT_IMAGE_BUFFER,                        "iimageBuffer",    GlTexture.Type.BUFFER),
		IMAGE_INT_2D_RECT              (GL45C.GL_INT_IMAGE_2D_RECT,                       "iimage2DRect",    GlTexture.Type.RECTANGLE),
		IMAGE_UINT_1D                  (GL45C.GL_UNSIGNED_INT_IMAGE_1D,                   "uimage1D",        GlTexture.Type.D1),
		IMAGE_UINT_2D                  (GL45C.GL_UNSIGNED_INT_IMAGE_2D,                   "uimage2D",        GlTexture.Type.D2),
		IMAGE_UINT_3D                  (GL45C.GL_UNSIGNED_INT_IMAGE_3D,                   "uimage3D",        GlTexture.Type.D3),
		IMAGE_UINT_CUBE                (GL45C.GL_UNSIGNED_INT_IMAGE_CUBE,                 "uimageCube",      GlTexture.Type.CUBE),
		IMAGE_UINT_CUBE_ARRAY          (GL45C.GL_UNSIGNED_INT_IMAGE_CUBE_MAP_ARRAY,       "uimageCubeArray", GlTexture.Type.CUBE_ARRAY),
		IMAGE_UINT_1D_ARRAY            (GL45C.GL_UNSIGNED_INT_IMAGE_1D_ARRAY,             "uimage1DArray",   GlTexture.Type.D1_ARRAY),
		IMAGE_UINT_2D_ARRAY            (GL45C.GL_UNSIGNED_INT_IMAGE_2D_ARRAY,             "uimage2DArray",   GlTexture.Type.D2_ARRAY),
		IMAGE_UINT_2D_MULTISAMPLE      (GL45C.GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE,       "uimage2DMS",      GlTexture.Type.D2_MS),
		IMAGE_UINT_2D_MULTISAMPLE_ARRAY(GL45C.GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE_ARRAY, "uimage2DMSArray", GlTexture.Type.D2_MS_ARRAY),
		IMAGE_UINT_BUFFER              (GL45C.GL_UNSIGNED_INT_IMAGE_BUFFER,               "uimageBuffer",    GlTexture.Type.BUFFER),
		IMAGE_UINT_2D_RECT             (GL45C.GL_UNSIGNED_INT_IMAGE_2D_RECT,              "uimage2DRect",    GlTexture.Type.RECTANGLE),
		;
		// @formatter:on

		public final int id;
		public final String description;
		public final ComponentType componentType;
		public final int componentCount;

		// texture sampler/image types
		public final boolean isTexture;
		public final GlTexture.Type textureType;
		// only samplers have this one
		public final GlTexture.SamplerType samplerType;

		private Type(int id, String description, int componentCount, ComponentType componentType) {
			this.id = id;
			this.description = description;
			this.componentCount = componentCount;
			this.componentType = componentType;
			this.isTexture = false;
			this.textureType = null;
			this.samplerType = null;
		}

		private Type(int id, String description, GlTexture.Type textureType, GlTexture.SamplerType samplerType) {
			this.id = id;
			this.description = description;
			this.componentCount = 1;
			this.componentType = ComponentType.SAMPLER;
			this.isTexture = true;
			this.textureType = textureType;
			this.samplerType = samplerType;
		}

		private Type(int id, String description, GlTexture.Type textureType) {
			this.id = id;
			this.description = description;
			this.componentCount = 1;
			this.componentType = ComponentType.IMAGE;
			this.isTexture = true;
			this.textureType = textureType;
			this.samplerType = null;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static Type fromSampler(GlTexture.SamplerType samplerType, GlTexture.Type textureType) {
			return switch (samplerType) {
				case FLOAT -> switch (textureType) {
					case D1 -> SAMPLER_1D;
					case D2 -> SAMPLER_2D;
					case D3 -> SAMPLER_3D;
					case CUBE -> SAMPLER_CUBE;
					case CUBE_ARRAY -> SAMPLER_CUBE_ARRAY;
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
					case CUBE -> SAMPLER_INT_CUBE;
					case CUBE_ARRAY -> SAMPLER_INT_CUBE_ARRAY;
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
					case CUBE -> SAMPLER_UINT_CUBE;
					case CUBE_ARRAY -> SAMPLER_UINT_CUBE_ARRAY;
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
					case CUBE -> SAMPLER_SHADOW_CUBE;
					case CUBE_ARRAY -> SAMPLER_SHADOW_CUBE_ARRAY;
					case RECTANGLE -> SAMPLER_SHADOW_2D_RECT;
					default -> null;
				};
				default -> null;
			};
		}

		public static Type fromImage(GlTexture.ComponentType componentType, GlTexture.Type textureType) {
			return switch (componentType) {
				case FLOAT -> switch (textureType) {
					case D1 -> IMAGE_1D;
					case D2 -> IMAGE_2D;
					case D3 -> IMAGE_3D;
					case CUBE -> IMAGE_CUBE;
					case CUBE_ARRAY -> IMAGE_CUBE_ARRAY;
					case D1_ARRAY -> IMAGE_1D_ARRAY;
					case D2_ARRAY -> IMAGE_2D_ARRAY;
					case D2_MS -> IMAGE_2D_MULTISAMPLE;
					case D2_MS_ARRAY -> IMAGE_2D_MULTISAMPLE_ARRAY;
					case BUFFER -> IMAGE_BUFFER;
					case RECTANGLE -> IMAGE_2D_RECT;
				};
				case INT -> switch (textureType) {
					case D1 -> IMAGE_INT_1D;
					case D2 -> IMAGE_INT_2D;
					case D3 -> IMAGE_INT_3D;
					case CUBE -> IMAGE_INT_CUBE;
					case CUBE_ARRAY -> IMAGE_INT_CUBE_ARRAY;
					case D1_ARRAY -> IMAGE_INT_1D_ARRAY;
					case D2_ARRAY -> IMAGE_INT_2D_ARRAY;
					case D2_MS -> IMAGE_INT_2D_MULTISAMPLE;
					case D2_MS_ARRAY -> IMAGE_INT_2D_MULTISAMPLE_ARRAY;
					case BUFFER -> IMAGE_INT_BUFFER;
					case RECTANGLE -> IMAGE_INT_2D_RECT;
				};
				case UINT -> switch (textureType) {
					case D1 -> IMAGE_UINT_1D;
					case D2 -> IMAGE_UINT_2D;
					case D3 -> IMAGE_UINT_3D;
					case CUBE -> IMAGE_UINT_CUBE;
					case CUBE_ARRAY -> IMAGE_UINT_CUBE_ARRAY;
					case D1_ARRAY -> IMAGE_UINT_1D_ARRAY;
					case D2_ARRAY -> IMAGE_UINT_2D_ARRAY;
					case D2_MS -> IMAGE_UINT_2D_MULTISAMPLE;
					case D2_MS_ARRAY -> IMAGE_UINT_2D_MULTISAMPLE_ARRAY;
					case BUFFER -> IMAGE_UINT_BUFFER;
					case RECTANGLE -> IMAGE_UINT_2D_RECT;
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
				case GL45C.GL_IMAGE_1D -> IMAGE_1D;
				case GL45C.GL_IMAGE_2D -> IMAGE_2D;
				case GL45C.GL_IMAGE_3D -> IMAGE_3D;
				case GL45C.GL_IMAGE_CUBE -> IMAGE_CUBE;
				case GL45C.GL_IMAGE_1D_ARRAY -> IMAGE_1D_ARRAY;
				case GL45C.GL_IMAGE_2D_ARRAY -> IMAGE_2D_ARRAY;
				case GL45C.GL_IMAGE_2D_MULTISAMPLE -> IMAGE_2D_MULTISAMPLE;
				case GL45C.GL_IMAGE_2D_MULTISAMPLE_ARRAY -> IMAGE_2D_MULTISAMPLE_ARRAY;
				case GL45C.GL_IMAGE_BUFFER -> IMAGE_BUFFER;
				case GL45C.GL_IMAGE_2D_RECT -> IMAGE_2D_RECT;
				case GL45C.GL_INT_IMAGE_1D -> IMAGE_INT_1D;
				case GL45C.GL_INT_IMAGE_2D -> IMAGE_INT_2D;
				case GL45C.GL_INT_IMAGE_3D -> IMAGE_INT_3D;
				case GL45C.GL_INT_IMAGE_CUBE -> IMAGE_INT_CUBE;
				case GL45C.GL_INT_IMAGE_1D_ARRAY -> IMAGE_INT_1D_ARRAY;
				case GL45C.GL_INT_IMAGE_2D_ARRAY -> IMAGE_INT_2D_ARRAY;
				case GL45C.GL_INT_IMAGE_2D_MULTISAMPLE -> IMAGE_INT_2D_MULTISAMPLE;
				case GL45C.GL_INT_IMAGE_2D_MULTISAMPLE_ARRAY -> IMAGE_INT_2D_MULTISAMPLE_ARRAY;
				case GL45C.GL_INT_IMAGE_BUFFER -> IMAGE_INT_BUFFER;
				case GL45C.GL_INT_IMAGE_2D_RECT -> IMAGE_INT_2D_RECT;
				case GL45C.GL_UNSIGNED_INT_IMAGE_1D -> IMAGE_UINT_1D;
				case GL45C.GL_UNSIGNED_INT_IMAGE_2D -> IMAGE_UINT_2D;
				case GL45C.GL_UNSIGNED_INT_IMAGE_3D -> IMAGE_UINT_3D;
				case GL45C.GL_UNSIGNED_INT_IMAGE_CUBE -> IMAGE_UINT_CUBE;
				case GL45C.GL_UNSIGNED_INT_IMAGE_1D_ARRAY -> IMAGE_UINT_1D_ARRAY;
				case GL45C.GL_UNSIGNED_INT_IMAGE_2D_ARRAY -> IMAGE_UINT_2D_ARRAY;
				case GL45C.GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE -> IMAGE_UINT_2D_MULTISAMPLE;
				case GL45C.GL_UNSIGNED_INT_IMAGE_2D_MULTISAMPLE_ARRAY -> IMAGE_UINT_2D_MULTISAMPLE_ARRAY;
				case GL45C.GL_UNSIGNED_INT_IMAGE_BUFFER -> IMAGE_UINT_BUFFER;
				case GL45C.GL_UNSIGNED_INT_IMAGE_2D_RECT -> IMAGE_UINT_2D_RECT;
				default -> {
					Mod.LOGGER.error("unknown uniform type {}", id);
					yield null;
				}
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
	private SamplerUniform[] samplerValues = null;
	private ImageUniform[] imageValues = null;

	private long lastTypeMismatchTime = Long.MIN_VALUE;

	public static final class SamplerUniform {
		public final WeakReference<GlTexture> texture;

		public SamplerUniform(WeakReference<GlTexture> texture) {
			this.texture = texture;
		}

		public SamplerUniform(GlTexture texture) {
			this(new WeakReference<>(texture));
		}
	}

	public static final class ImageUniform {
		public final WeakReference<GlTexture> texture;
		public final int level;
		public final int layer;
		public final GlTexture.ImageAccess access;
		public final GlTexture.Format format;

		public ImageUniform(WeakReference<GlTexture> texture, int level, int layer, GlTexture.ImageAccess access,
				GlTexture.Format format) {
			this.texture = texture;
			this.level = level;
			this.layer = layer;
			this.access = access;
			this.format = format;
		}

		public ImageUniform(GlTexture texture, int level, int layer, GlTexture.ImageAccess access,
				GlTexture.Format format) {
			this(new WeakReference<>(texture), level, layer, access, format);
		}

		public ImageUniform(GlTexture texture, GlTexture.ImageAccess access, GlTexture.Format format) {
			this(texture, 0, 0, access, format);
		}

		public ImageUniform(GlTexture texture, GlTexture.ImageAccess access) {
			this(texture, 0, 0, access, texture.format());
		}

	}

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

		if (this.type.componentType == ComponentType.SAMPLER) {
			this.samplerValues = new SamplerUniform[size];
		} else if (this.type.componentType == ComponentType.IMAGE) {
			this.imageValues = new ImageUniform[size];
		} else if (this.type.componentType.isFloatType) {
			this.floatValues = new float[size * this.type.componentCount];
		} else if (this.type.componentType.isIntType) {
			this.intValues = new int[size * this.type.componentCount];
		}
	}

	public void markDirty(boolean dirty) {
		this.isDirty |= dirty;
	}

	public boolean setSampler(int i, SamplerUniform sampler) {
		final var old = this.samplerValues[i];
		this.samplerValues[i] = sampler;
		return old == null || !old.texture.refersTo(sampler.texture.get());
	}

	public boolean setImage(int i, ImageUniform image) {
		final var old = this.imageValues[i];
		this.imageValues[i] = image;
		return old == null || !old.texture.refersTo(image.texture.get()) || old.access != image.access
				|| old.format != image.format || old.layer != image.layer || old.level != image.level;
	}

	public boolean setInt(int i, int value) {
		if (this.intValues == null) {
			throw new IllegalStateException(String.format(
					"slot '%s' has type %s, but an int was written.",
					this.name, this.type));
		}
		if (i >= this.intValues.length) {
			throw new IllegalArgumentException(String.format(
					"slot '%s' has an int value storage of size %d, but index %d was written.",
					this.intValues.length, i));
		}
		final var old = this.intValues[i];
		this.intValues[i] = value;
		return value != old;
	}

	public boolean setFloat(int i, float value) {
		if (this.floatValues == null) {
			throw new IllegalStateException(String.format(
					"slot '%s' has type %s, but a float was written.",
					this.name, this.type));
		}
		if (i >= this.floatValues.length) {
			throw new IllegalArgumentException(String.format(
					"slot '%s' has a float value storage of size %d, but index %d was written.",
					this.floatValues.length, i));
		}
		final var old = this.floatValues[i];
		this.floatValues[i] = value;
		return value != old;
	}

	public static final class UploadContext {
		public int currentTextureUnit = 0;
		public int currentImageUnit = 0;
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

	private void uploadImages(ShaderProgram program, UploadContext ctx) {
		if (this.type.componentType != ComponentType.IMAGE)
			return;

		for (int i = 0; i < this.imageValues.length; ++i) {
			if (this.imageValues[i] == null) {
				// TODO: unbound image, report error
				return;
			}
			final var texture = this.imageValues[i].texture.get();
			if (texture == null || texture.isDestroyed()) {
				this.imageValues[i] = null;
				continue;
			}
			final var uniform = this.imageValues[i];
			GL45C.glBindImageTexture(ctx.currentImageUnit, texture.id, uniform.level, false, uniform.layer,
					uniform.access.id, uniform.format.id);
			GL45C.glProgramUniform1i(program.id, this.location, ctx.currentImageUnit);
			ctx.currentImageUnit += 1;
		}

	}

	private void uploadSamplers(ShaderProgram program, UploadContext ctx) {
		if (this.type.componentType != ComponentType.SAMPLER)
			return;

		for (int i = 0; i < this.samplerValues.length; ++i) {
			if (this.samplerValues[i] == null) {
				// TODO: unbound image, report error
				return;
			}
			final var texture = this.samplerValues[i].texture.get();
			if (texture == null || texture.isDestroyed()) {
				this.samplerValues[i] = null;
				continue;
			}
			// FIXME: we change texture unit bindings but don't tell GlStateManager about
			// it, which means vanilla has the possibility of getting into a weird state.
			// GlManager.activeTexture(GL45C.GL_TEXTURE0 + ctx.currentTextureUnit);
			// GlManager.bindTexture(texture.type, texture.id);
			GL45C.glBindTextureUnit(ctx.currentTextureUnit, texture.id);
			GL45C.glProgramUniform1i(program.id, this.location, ctx.currentTextureUnit);
			ctx.currentTextureUnit += 1;
		}
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
		if (!this.isDirty
				&& this.type.componentType != ComponentType.SAMPLER
				&& this.type.componentType != ComponentType.IMAGE)
			return;
		this.isDirty = false;

		if (uploadMatrix(program))
			return;
		if (this.type.componentType == ComponentType.SAMPLER) {
			uploadSamplers(program, ctx);
		} else if (this.type.componentType == ComponentType.IMAGE) {
			uploadImages(program, ctx);
		} else if (this.type.componentType.isFloatType) {
			uploadFloats(program);
		} else if (this.type.componentType.isIntType) {
			uploadInts(program);
		}
	}
}