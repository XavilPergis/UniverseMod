package net.xavil.hawklib.client.gl.texture;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.GlObject;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.hawklib.math.matrices.Vec3i;

public abstract class GlTexture extends GlObject {

	public static enum SamplerType {
		FLOAT, INT, UINT, SHADOW, NONE;
	}

	public static enum ComponentType {
		FLOAT, INT, UINT, NONE;
	}

	private static final int COLOR_FORMAT = 1 << 0;
	private static final int DEPTH_FORMAT = 1 << 1;
	private static final int STENCIL_FORMAT = 1 << 2;
	private static final int COLOR_RENDERABLE = 1 << 3;
	private static final int DEPTH_RENDERABLE = 1 << 4;
	private static final int STENCIL_RENDERABLE = 1 << 5;
	private static final int SRGB_ENCODED = 1 << 6;
	private static final int COMPRESSED = 1 << 7;
	private static final int NORMALIZED = 1 << 8;

	private static final int COMPRESSED_COLOR = COLOR_FORMAT | COMPRESSED;
	private static final int SIZED_COLOR = COLOR_FORMAT | COLOR_RENDERABLE;
	private static final int SIZED_DEPTH = DEPTH_FORMAT | DEPTH_RENDERABLE;
	private static final int SIZED_STENCIL = STENCIL_FORMAT | STENCIL_RENDERABLE;

	public static enum Format {
		// @formatter:off
		// unspecified formats (implementation chooses the data layout)
		RGBA_UNSPECIFIED          (GL45C.GL_RGBA,               ComponentType.FLOAT, SamplerType.FLOAT,  4, SIZED_COLOR, "Implementation-Chosen RGBA"),
		RGB_UNSPECIFIED           (GL45C.GL_RGB,                ComponentType.FLOAT, SamplerType.FLOAT,  3, SIZED_COLOR, "Implementation-Chosen RGB"),
		RG_UNSPECIFIED            (GL45C.GL_RG,                 ComponentType.FLOAT, SamplerType.FLOAT,  2, SIZED_COLOR, "Implementation-Chosen RG"),
		R_UNSPECIFIED             (GL45C.GL_RED,                ComponentType.FLOAT, SamplerType.FLOAT,  1, SIZED_COLOR, "Implementation-Chosen R"),
		DEPTH_UNSPECIFIED         (GL45C.GL_DEPTH_COMPONENT,    ComponentType.FLOAT, SamplerType.SHADOW, 1, SIZED_DEPTH,  "Implementation-Chosen Depth"),
		STENCIL_UNSPECIFIED       (GL45C.GL_STENCIL_INDEX,      ComponentType.FLOAT, SamplerType.NONE,   1, SIZED_STENCIL, "Implementation-Chosen Stencil"),
		DEPTH_STENCIL_UNSPECIFIED (GL45C.GL_DEPTH_STENCIL,      ComponentType.FLOAT, SamplerType.SHADOW, 2, SIZED_DEPTH | SIZED_STENCIL, "Implementation-Chosen Depth + Stencil"),
		// 32-bit color
		RGBA32_FLOAT              (GL45C.GL_RGBA32F,            ComponentType.FLOAT, SamplerType.FLOAT,  4, SIZED_COLOR, "32-Bit RGBA Float"),
		RGBA32_UINT               (GL45C.GL_RGBA32UI,           ComponentType.UINT,  SamplerType.UINT,   4, SIZED_COLOR, "32-Bit RGBA Unsigned Integer"),
		RGBA32_INT                (GL45C.GL_RGBA32I,            ComponentType.INT,   SamplerType.INT,    4, SIZED_COLOR, "32-Bit RGBA Integer"),
		RGB32_FLOAT               (GL45C.GL_RGB32F,             ComponentType.FLOAT, SamplerType.FLOAT,  3, SIZED_COLOR, "32-Bit RGB Float"),
		RGB32_UINT                (GL45C.GL_RGB32UI,            ComponentType.UINT,  SamplerType.UINT,   3, SIZED_COLOR, "32-Bit RGB Unsigned Integer"),
		RGB32_INT                 (GL45C.GL_RGB32I,             ComponentType.INT,   SamplerType.INT,    3, SIZED_COLOR, "32-Bit RGB Integer"),
		RG32_FLOAT                (GL45C.GL_RG32F,              ComponentType.FLOAT, SamplerType.FLOAT,  2, SIZED_COLOR, "32-Bit RG Float"),
		RG32_UINT                 (GL45C.GL_RG32UI,             ComponentType.UINT,  SamplerType.UINT,   2, SIZED_COLOR, "32-Bit RG Unsigned Integer"),
		RG32_INT                  (GL45C.GL_RG32I,              ComponentType.INT,   SamplerType.INT,    2, SIZED_COLOR, "32-Bit RG Integer"),
		R32_FLOAT                 (GL45C.GL_R32F,               ComponentType.FLOAT, SamplerType.FLOAT,  1, SIZED_COLOR, "32-Bit R Float"),
		R32_UINT                  (GL45C.GL_R32UI,              ComponentType.UINT,  SamplerType.UINT,   1, SIZED_COLOR, "32-Bit R Unsigned Integer"),
		R32_INT                   (GL45C.GL_R32I,               ComponentType.INT,   SamplerType.INT,    1, SIZED_COLOR, "32-Bit R Integer"),
		// 16-bit color
		RGBA16_FLOAT              (GL45C.GL_RGBA16F,            ComponentType.FLOAT, SamplerType.FLOAT,  4, SIZED_COLOR, "16-Bit RGBA Float"),
		RGBA16_UINT               (GL45C.GL_RGBA16UI,           ComponentType.UINT,  SamplerType.UINT,   4, SIZED_COLOR, "16-Bit RGBA Unsigned Integer"),
		RGBA16_INT                (GL45C.GL_RGBA16I,            ComponentType.INT,   SamplerType.INT,    4, SIZED_COLOR, "16-Bit RGBA Integer"),
		RGB16_FLOAT               (GL45C.GL_RGB16F,             ComponentType.FLOAT, SamplerType.FLOAT,  3, SIZED_COLOR, "16-Bit RGB Float"),
		RGB16_UINT                (GL45C.GL_RGB16UI,            ComponentType.UINT,  SamplerType.UINT,   3, SIZED_COLOR, "16-Bit RGB Unsigned Integer"),
		RGB16_INT                 (GL45C.GL_RGB16I,             ComponentType.INT,   SamplerType.INT,    3, SIZED_COLOR, "16-Bit RGB Integer"),
		RG16_FLOAT                (GL45C.GL_RG16F,              ComponentType.FLOAT, SamplerType.FLOAT,  2, SIZED_COLOR, "16-Bit RG Float"),
		RG16_UINT                 (GL45C.GL_RG16UI,             ComponentType.UINT,  SamplerType.UINT,   2, SIZED_COLOR, "16-Bit RG Unsigned Integer"),
		RG16_INT                  (GL45C.GL_RG16I,              ComponentType.INT,   SamplerType.INT,    2, SIZED_COLOR, "16-Bit RG Integer"),
		R16_FLOAT                 (GL45C.GL_R16F,               ComponentType.FLOAT, SamplerType.FLOAT,  1, SIZED_COLOR, "16-Bit R Float"),
		R16_UINT                  (GL45C.GL_R16UI,              ComponentType.UINT,  SamplerType.UINT,   1, SIZED_COLOR, "16-Bit R Unsigned Integer"),
		R16_INT                   (GL45C.GL_R16I,               ComponentType.INT,   SamplerType.INT,    1, SIZED_COLOR, "16-Bit R Integer"),
		RGBA16_UINT_NORM          (GL45C.GL_RGBA16,             ComponentType.UINT,  SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "16-Bit RGBA Normalized Unsigned Integer"),
		RGBA16_INT_NORM           (GL45C.GL_RGBA16_SNORM,       ComponentType.INT,   SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "16-Bit RGBA Normalized Integer"),
		RGB16_UINT_NORM           (GL45C.GL_RGB16,              ComponentType.UINT,  SamplerType.FLOAT,  3, SIZED_COLOR | NORMALIZED, "16-Bit RGB Normalized Unsigned Integer"),
		RGB16_INT_NORM            (GL45C.GL_RGB16_SNORM,        ComponentType.INT,   SamplerType.FLOAT,  3, SIZED_COLOR | NORMALIZED, "16-Bit RGB Normalized Integer"),
		RG16_UINT_NORM            (GL45C.GL_RG16,               ComponentType.UINT,  SamplerType.FLOAT,  2, SIZED_COLOR | NORMALIZED, "16-Bit RG Normalized Unsigned Integer"),
		RG16_INT_NORM             (GL45C.GL_RG16_SNORM,         ComponentType.INT,   SamplerType.FLOAT,  2, SIZED_COLOR | NORMALIZED, "16-Bit RG Normalized Integer"),
		R16_UINT_NORM             (GL45C.GL_R16,                ComponentType.UINT,  SamplerType.FLOAT,  1, SIZED_COLOR | NORMALIZED, "16-Bit R Normalized Unsigned Integer"),
		R16_INT_NORM              (GL45C.GL_R16_SNORM,          ComponentType.INT,   SamplerType.FLOAT,  1, SIZED_COLOR | NORMALIZED, "16-Bit R Normalized Integer"),
		// 8-bit color
		RGBA8_UINT                (GL45C.GL_RGBA8UI,            ComponentType.UINT,  SamplerType.UINT,   4, SIZED_COLOR, "8-Bit RGBA Usnigned Integer"),
		RGBA8_INT                 (GL45C.GL_RGBA8I,             ComponentType.INT,   SamplerType.INT,    4, SIZED_COLOR, "8-Bit RGBA Integer"),
		RGB8_UINT                 (GL45C.GL_RGB8UI,             ComponentType.UINT,  SamplerType.UINT,   3, SIZED_COLOR, "8-Bit RGB Usnigned Integer"),
		RGB8_INT                  (GL45C.GL_RGB8I,              ComponentType.INT,   SamplerType.INT,    3, SIZED_COLOR, "8-Bit RGB Integer"),
		RG8_UINT                  (GL45C.GL_RG8UI,              ComponentType.UINT,  SamplerType.UINT,   2, SIZED_COLOR, "8-Bit RG Usnigned Integer"),
		RG8_INT                   (GL45C.GL_RG8I,               ComponentType.INT,   SamplerType.INT,    2, SIZED_COLOR, "8-Bit RG Integer"),
		R8_UINT                   (GL45C.GL_R8UI,               ComponentType.UINT,  SamplerType.UINT,   1, SIZED_COLOR, "8-Bit R Usnigned Integer"),
		R8_INT                    (GL45C.GL_R8I,                ComponentType.INT,   SamplerType.INT,    1, SIZED_COLOR, "8-Bit R Integer"),
		RGBA8_UINT_NORM           (GL45C.GL_RGBA8,              ComponentType.UINT,  SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "8-Bit RGBA Normalized Unsigned Integer"),
		RGBA8_INT_NORM            (GL45C.GL_RGBA8_SNORM,        ComponentType.INT,   SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "8-Bit RGBA Normalized Integer"),
		RGB8_UINT_NORM            (GL45C.GL_RGB8,               ComponentType.UINT,  SamplerType.FLOAT,  3, SIZED_COLOR | NORMALIZED, "8-Bit RGB Normalized Unsigned Integer"),
		RGB8_INT_NORM             (GL45C.GL_RGB8_SNORM,         ComponentType.INT,   SamplerType.FLOAT,  3, SIZED_COLOR | NORMALIZED, "8-Bit RGB Normalized Integer"),
		RG8_UINT_NORM             (GL45C.GL_RG8,                ComponentType.UINT,  SamplerType.FLOAT,  2, SIZED_COLOR | NORMALIZED, "8-Bit RG Normalized Unsigned Integer"),
		RG8_INT_NORM              (GL45C.GL_RG8_SNORM,          ComponentType.INT,   SamplerType.FLOAT,  2, SIZED_COLOR | NORMALIZED, "8-Bit RG Normalized Integer"),
		R8_UINT_NORM              (GL45C.GL_R8,                 ComponentType.UINT,  SamplerType.FLOAT,  1, SIZED_COLOR | NORMALIZED, "8-Bit R Normalized Unsigned Integer"),
		R8_INT_NORM               (GL45C.GL_R8_SNORM,           ComponentType.INT,   SamplerType.FLOAT,  1, SIZED_COLOR | NORMALIZED, "8-Bit R Normalized Integer"),
		RGBA8_UINT_NORM_SRGB      (GL45C.GL_SRGB8_ALPHA8,       ComponentType.UINT,  SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "8-Bit RGBA Normalized Unsigned Integer (sRGB-Encoded)"),
		RGB8_UINT_NORM_SRGB       (GL45C.GL_SRGB8,              ComponentType.UINT,  SamplerType.FLOAT,  3, SIZED_COLOR | NORMALIZED, "8-Bit RGB Normalized Unsigned Integer (sRGB-Encoded)"),
		// other color
		R3G3B2_UINT_NORM          (GL45C.GL_R3_G3_B2,           ComponentType.UINT,  SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "3-Bit RG + 2-Bit B Normalized Unsigned Integer"),
		RGB4_UINT_NORM            (GL45C.GL_RGB4,               ComponentType.UINT,  SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "4-Bit RGB Normalized Unsigned Integer"),
		RGB5_UINT_NORM            (GL45C.GL_RGB5,               ComponentType.UINT,  SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "5-Bit RGB Normalized Unsigned Integer"),
		RGB10_UINT_NORM           (GL45C.GL_RGB10,              ComponentType.UINT,  SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "10-Bit RGB Normalized Unsigned Integer"),
		RGB12_UINT_NORM           (GL45C.GL_RGB12,              ComponentType.UINT,  SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "12-Bit RGB Normalized Unsigned Integer"),
		RGB10A2_UINT_NORM         (GL45C.GL_RGB10_A2,           ComponentType.UINT,  SamplerType.FLOAT,  4, SIZED_COLOR | NORMALIZED, "10-Bit RGB + 2-Bit A Normalized Unsigned Integer"),
		R11G11B10_FLOAT           (GL45C.GL_R11F_G11F_B10F,     ComponentType.FLOAT, SamplerType.FLOAT,  3, SIZED_COLOR, "11-Bit RG + 10-Bit B Float"),
		RGB9E5_FLOAT              (GL45C.GL_RGB9_E5,            ComponentType.FLOAT, SamplerType.FLOAT,  3, SIZED_COLOR, "RGB9E5 Float"),
		// depth/stencil
		DEPTH32_FLOAT             (GL45C.GL_DEPTH_COMPONENT32F, ComponentType.FLOAT, SamplerType.SHADOW, 1, SIZED_DEPTH, "Depth32 Float"),
		DEPTH32_UINT_NORM         (GL45C.GL_DEPTH_COMPONENT32,  ComponentType.UINT,  SamplerType.SHADOW, 1, SIZED_DEPTH, "Depth32 Unsigned Integer"),
		DEPTH24_UINT_NORM         (GL45C.GL_DEPTH_COMPONENT24,  ComponentType.UINT,  SamplerType.SHADOW, 1, SIZED_DEPTH, "Depth24 Unsigned Integer"),
		DEPTH16_UINT_NORM         (GL45C.GL_DEPTH_COMPONENT16,  ComponentType.UINT,  SamplerType.SHADOW, 1, SIZED_DEPTH, "Depth16 Unsigned Integer"),
		STENCIL1                  (GL45C.GL_STENCIL_INDEX1,     ComponentType.UINT,  SamplerType.NONE,   1, SIZED_STENCIL, "Stencil1 Unsigned Integer"),
		STENCIL4                  (GL45C.GL_STENCIL_INDEX4,     ComponentType.UINT,  SamplerType.NONE,   1, SIZED_STENCIL, "Stencil4 Unsigned Integer"),
		STENCIL8                  (GL45C.GL_STENCIL_INDEX8,     ComponentType.UINT,  SamplerType.NONE,   1, SIZED_STENCIL, "Stencil8 Unsigned Integer"),
		STENCIL16                 (GL45C.GL_STENCIL_INDEX16,    ComponentType.UINT,  SamplerType.NONE,   1, SIZED_STENCIL, "Stencil16 Unsigned Integer"),
		// oh god uhhhhhhh these are two different component types!!!!
		DEPTH32_FLOAT_STENCIL8    (GL45C.GL_DEPTH32F_STENCIL8,  ComponentType.FLOAT, SamplerType.SHADOW, 2, SIZED_DEPTH | SIZED_STENCIL, "Depth32 Float + Stencil8 Unsigned Integer"),
		DEPTH24_UINT_NORM_STENCIL8(GL45C.GL_DEPTH24_STENCIL8,   ComponentType.FLOAT, SamplerType.SHADOW, 2, SIZED_DEPTH | SIZED_STENCIL, "Depth32 Unsigned Integer + Stencil8 Unsigned Integer"),
		// compressed color
		// i am just guessing that these are used with floating point samplers
		R_COMPRESSED_UNSPECIFIED        (GL45C.GL_COMPRESSED_RED,              ComponentType.NONE, SamplerType.FLOAT, 2, COMPRESSED_COLOR, "R Compressed"),
		RG_COMPRESSED_UNSPECIFIED       (GL45C.GL_COMPRESSED_RG,               ComponentType.NONE, SamplerType.FLOAT, 2, COMPRESSED_COLOR, "RG Compressed"),
		RGB_COMPRESSED_UNSPECIFIED      (GL45C.GL_COMPRESSED_RGB,              ComponentType.NONE, SamplerType.FLOAT, 2, COMPRESSED_COLOR, "RGB Compressed"),
		RGBA_COMPRESSED_UNSPECIFIED     (GL45C.GL_COMPRESSED_RGBA,             ComponentType.NONE, SamplerType.FLOAT, 2, COMPRESSED_COLOR, "RGBA Compressed"),
		RGB_COMPRESSED_UNSPECIFIED_SRGB (GL45C.GL_COMPRESSED_SRGB,             ComponentType.NONE, SamplerType.FLOAT, 2, COMPRESSED_COLOR | SRGB_ENCODED, "RGB Compressed (sRGB-Encoded)"),
		RGBA_COMPRESSED_UNSPECIFIED_SRGB(GL45C.GL_COMPRESSED_SRGB_ALPHA,       ComponentType.NONE, SamplerType.FLOAT, 2, COMPRESSED_COLOR | SRGB_ENCODED, "RGBA Compressed (sRGB-Encoded)"),
		R_COMPRESSED_RGTC1              (GL45C.GL_COMPRESSED_RED_RGTC1,        ComponentType.NONE, SamplerType.FLOAT, 2, COMPRESSED_COLOR, "R Compressed RGTC1"),
		R_SIGNED_COMPRESSED_RGTC1       (GL45C.GL_COMPRESSED_SIGNED_RED_RGTC1, ComponentType.NONE, SamplerType.FLOAT, 2, COMPRESSED_COLOR, "R Signed Compressed RGTC1"),
		RG_COMPRESSED_RGTC2             (GL45C.GL_COMPRESSED_RG_RGTC2,         ComponentType.NONE, SamplerType.FLOAT, 2, COMPRESSED_COLOR, "RG Compressed RGTC2"),
		RG_SIGNED_COMPRESSED_RGTC2      (GL45C.GL_COMPRESSED_SIGNED_RG_RGTC2,  ComponentType.NONE, SamplerType.FLOAT, 2, COMPRESSED_COLOR, "RG Signed Compressed RGTC2"),
		// unknown sentinel
		UNKNOWN(0, ComponentType.NONE, SamplerType.NONE, 0, 0, "Unknown");
		// @formatter:on

		public final int id;
		public final String description;
		public final SamplerType samplerType;
		public final ComponentType componentType;
		public final int channelCount;
		public final boolean isColorFormat;
		public final boolean isDepthFormat;
		public final boolean isStencilFormat;
		public final boolean isSrgb;
		public final boolean isCompressed;
		public final boolean isColorRenderable;
		public final boolean isDepthRenderable;
		public final boolean isStencilRenderable;

		private Format(int id, ComponentType componentType, SamplerType samplerType, int channelCount, int flags,
				String description) {
			this.id = id;
			this.description = description;
			this.samplerType = samplerType;
			this.componentType = componentType;
			this.channelCount = channelCount;
			this.isColorFormat = (flags & COLOR_FORMAT) != 0;
			this.isDepthFormat = (flags & DEPTH_FORMAT) != 0;
			this.isStencilFormat = (flags & STENCIL_FORMAT) != 0;
			this.isColorRenderable = (flags & COLOR_RENDERABLE) != 0;
			this.isDepthRenderable = (flags & DEPTH_RENDERABLE) != 0;
			this.isStencilRenderable = (flags & STENCIL_RENDERABLE) != 0;
			this.isSrgb = (flags & SRGB_ENCODED) != 0;
			this.isCompressed = (flags & COMPRESSED) != 0;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static Format from(int value) {
			return switch (value) {
				case GL45C.GL_RGBA -> RGBA_UNSPECIFIED;
				case GL45C.GL_RGB -> RGB_UNSPECIFIED;
				case GL45C.GL_RG -> RG_UNSPECIFIED;
				case GL45C.GL_RED -> R_UNSPECIFIED;
				case GL45C.GL_DEPTH_COMPONENT -> DEPTH_UNSPECIFIED;
				case GL45C.GL_STENCIL_INDEX -> STENCIL_UNSPECIFIED;
				case GL45C.GL_DEPTH_STENCIL -> DEPTH_STENCIL_UNSPECIFIED;
				case GL45C.GL_RGBA32F -> RGBA32_FLOAT;
				case GL45C.GL_RGBA32UI -> RGBA32_UINT;
				case GL45C.GL_RGBA32I -> RGBA32_INT;
				case GL45C.GL_RGB32F -> RGB32_FLOAT;
				case GL45C.GL_RGB32UI -> RGB32_UINT;
				case GL45C.GL_RGB32I -> RGB32_INT;
				case GL45C.GL_RG32F -> RG32_FLOAT;
				case GL45C.GL_RG32UI -> RG32_UINT;
				case GL45C.GL_RG32I -> RG32_INT;
				case GL45C.GL_R32F -> R32_FLOAT;
				case GL45C.GL_R32UI -> R32_UINT;
				case GL45C.GL_R32I -> R32_INT;
				case GL45C.GL_RGBA16F -> RGBA16_FLOAT;
				case GL45C.GL_RGBA16UI -> RGBA16_UINT;
				case GL45C.GL_RGBA16I -> RGBA16_INT;
				case GL45C.GL_RGB16F -> RGB16_FLOAT;
				case GL45C.GL_RGB16UI -> RGB16_UINT;
				case GL45C.GL_RGB16I -> RGB16_INT;
				case GL45C.GL_RG16F -> RG16_FLOAT;
				case GL45C.GL_RG16UI -> RG16_UINT;
				case GL45C.GL_RG16I -> RG16_INT;
				case GL45C.GL_R16F -> R16_FLOAT;
				case GL45C.GL_R16UI -> R16_UINT;
				case GL45C.GL_R16I -> R16_INT;
				case GL45C.GL_RGBA16 -> RGBA16_UINT_NORM;
				case GL45C.GL_RGBA16_SNORM -> RGBA16_INT_NORM;
				case GL45C.GL_RGB16 -> RGB16_UINT_NORM;
				case GL45C.GL_RGB16_SNORM -> RGB16_INT_NORM;
				case GL45C.GL_RG16 -> RG16_UINT_NORM;
				case GL45C.GL_RG16_SNORM -> RG16_INT_NORM;
				case GL45C.GL_R16 -> R16_UINT_NORM;
				case GL45C.GL_R16_SNORM -> R16_INT_NORM;
				case GL45C.GL_RGBA8UI -> RGBA8_UINT;
				case GL45C.GL_RGBA8I -> RGBA8_INT;
				case GL45C.GL_RGB8UI -> RGB8_UINT;
				case GL45C.GL_RGB8I -> RGB8_INT;
				case GL45C.GL_RG8UI -> RG8_UINT;
				case GL45C.GL_RG8I -> RG8_INT;
				case GL45C.GL_R8UI -> R8_UINT;
				case GL45C.GL_R8I -> R8_INT;
				case GL45C.GL_RGBA8 -> RGBA8_UINT_NORM;
				case GL45C.GL_RGBA8_SNORM -> RGBA8_INT_NORM;
				case GL45C.GL_RGB8 -> RGB8_UINT_NORM;
				case GL45C.GL_RGB8_SNORM -> RGB8_INT_NORM;
				case GL45C.GL_RG8 -> RG8_UINT_NORM;
				case GL45C.GL_RG8_SNORM -> RG8_INT_NORM;
				case GL45C.GL_R8 -> R8_UINT_NORM;
				case GL45C.GL_R8_SNORM -> R8_INT_NORM;
				case GL45C.GL_SRGB8_ALPHA8 -> RGBA8_UINT_NORM_SRGB;
				case GL45C.GL_SRGB8 -> RGB8_UINT_NORM_SRGB;
				case GL45C.GL_R3_G3_B2 -> R3G3B2_UINT_NORM;
				case GL45C.GL_RGB4 -> RGB4_UINT_NORM;
				case GL45C.GL_RGB5 -> RGB5_UINT_NORM;
				case GL45C.GL_RGB10 -> RGB10_UINT_NORM;
				case GL45C.GL_RGB12 -> RGB12_UINT_NORM;
				case GL45C.GL_RGB10_A2 -> RGB10A2_UINT_NORM;
				case GL45C.GL_R11F_G11F_B10F -> R11G11B10_FLOAT;
				case GL45C.GL_RGB9_E5 -> RGB9E5_FLOAT;
				case GL45C.GL_DEPTH_COMPONENT32F -> DEPTH32_FLOAT;
				case GL45C.GL_DEPTH_COMPONENT32 -> DEPTH32_UINT_NORM;
				case GL45C.GL_DEPTH_COMPONENT24 -> DEPTH24_UINT_NORM;
				case GL45C.GL_DEPTH_COMPONENT16 -> DEPTH16_UINT_NORM;
				case GL45C.GL_STENCIL_INDEX1 -> STENCIL1;
				case GL45C.GL_STENCIL_INDEX4 -> STENCIL4;
				case GL45C.GL_STENCIL_INDEX8 -> STENCIL8;
				case GL45C.GL_STENCIL_INDEX16 -> STENCIL16;
				case GL45C.GL_DEPTH32F_STENCIL8 -> DEPTH32_FLOAT_STENCIL8;
				case GL45C.GL_DEPTH24_STENCIL8 -> DEPTH24_UINT_NORM_STENCIL8;
				case GL45C.GL_COMPRESSED_RED -> R_COMPRESSED_UNSPECIFIED;
				case GL45C.GL_COMPRESSED_RG -> RG_COMPRESSED_UNSPECIFIED;
				case GL45C.GL_COMPRESSED_RGB -> RGB_COMPRESSED_UNSPECIFIED;
				case GL45C.GL_COMPRESSED_RGBA -> RGBA_COMPRESSED_UNSPECIFIED;
				case GL45C.GL_COMPRESSED_SRGB -> RGB_COMPRESSED_UNSPECIFIED_SRGB;
				case GL45C.GL_COMPRESSED_SRGB_ALPHA -> RGBA_COMPRESSED_UNSPECIFIED_SRGB;
				case GL45C.GL_COMPRESSED_RED_RGTC1 -> R_COMPRESSED_RGTC1;
				case GL45C.GL_COMPRESSED_SIGNED_RED_RGTC1 -> R_SIGNED_COMPRESSED_RGTC1;
				case GL45C.GL_COMPRESSED_RG_RGTC2 -> RG_COMPRESSED_RGTC2;
				case GL45C.GL_COMPRESSED_SIGNED_RG_RGTC2 -> RG_SIGNED_COMPRESSED_RGTC2;
				default -> UNKNOWN;
			};
		}
	}

	public static enum Type {
		D1(GL45C.GL_TEXTURE_1D, GL45C.GL_TEXTURE_BINDING_1D, "1D"),
		D2(GL45C.GL_TEXTURE_2D, GL45C.GL_TEXTURE_BINDING_2D, "2D"),
		D3(GL45C.GL_TEXTURE_3D, GL45C.GL_TEXTURE_BINDING_3D, "3D"),
		CUBEMAP(GL45C.GL_TEXTURE_CUBE_MAP, GL45C.GL_TEXTURE_BINDING_CUBE_MAP, "Cubemap"),
		D1_ARRAY(GL45C.GL_TEXTURE_1D_ARRAY, GL45C.GL_TEXTURE_BINDING_1D_ARRAY, "1D Array"),
		D2_ARRAY(GL45C.GL_TEXTURE_2D_ARRAY, GL45C.GL_TEXTURE_BINDING_2D_ARRAY, "2D Array"),
		D2_MS(GL45C.GL_TEXTURE_2D_MULTISAMPLE, GL45C.GL_TEXTURE_BINDING_2D_MULTISAMPLE, "2D Multisampled"),
		D2_MS_ARRAY(GL45C.GL_TEXTURE_2D_MULTISAMPLE_ARRAY, GL45C.GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY,
				"2D Multisampled Array"),
		BUFFER(GL45C.GL_TEXTURE_BUFFER, GL45C.GL_TEXTURE_BINDING_BUFFER, "Buffer-Backed"),
		RECTANGLE(GL45C.GL_TEXTURE_RECTANGLE, GL45C.GL_TEXTURE_BINDING_RECTANGLE, "Rectangle");

		public final int id;
		public final int bindingId;
		public final String description;

		private Type(int id, int bindingId, String description) {
			this.id = id;
			this.bindingId = bindingId;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static Type from(int value) {
			return switch (value) {
				case GL45C.GL_TEXTURE_1D -> D1;
				case GL45C.GL_TEXTURE_2D -> D2;
				case GL45C.GL_TEXTURE_3D -> D3;
				case GL45C.GL_TEXTURE_CUBE_MAP -> CUBEMAP;
				case GL45C.GL_TEXTURE_1D_ARRAY -> D1_ARRAY;
				case GL45C.GL_TEXTURE_2D_ARRAY -> D2_ARRAY;
				case GL45C.GL_TEXTURE_2D_MULTISAMPLE -> D2_MS;
				case GL45C.GL_TEXTURE_2D_MULTISAMPLE_ARRAY -> D2_MS_ARRAY;
				case GL45C.GL_TEXTURE_BUFFER -> BUFFER;
				case GL45C.GL_TEXTURE_RECTANGLE -> RECTANGLE;
				default -> null;
			};
		}
	}

	public static final class Size {
		public final int width;
		public final int height;
		public final int depth;
		public final int layers;

		public static final Size ZERO = new Size(0, 0, 0, 0);

		public Size(int width, int height, int depth, int layers) {
			this.width = width;
			this.height = height;
			this.depth = depth;
			this.layers = layers;
		}

		public Vec2i d2() {
			return new Vec2i(this.width, this.height);
		}

		public Vec3i d3() {
			return new Vec3i(this.width, this.height, this.depth);
		}

		@Override
		public String toString() {
			final var builder = new StringBuilder();
			builder.append(this.width);
			builder.append('x');
			builder.append(this.height);
			if (this.depth > 1) {
				builder.append('x');
				builder.append(this.depth);
			}
			if (this.layers > 1) {
				builder.append('x');
				builder.append(this.layers);
			}
			return builder.toString();
		}
	}

	public static enum MinFilter {
		NEAREST(GL45C.GL_NEAREST, "Nearest"),
		LINEAR(GL45C.GL_LINEAR, "Linear"),
		NEAREST_MIPMAP_NEAREST(GL45C.GL_NEAREST_MIPMAP_NEAREST, "Nearest from Nearest Mip"),
		LINEAR_MIPMAP_NEAREST(GL45C.GL_LINEAR_MIPMAP_NEAREST, "Linear from Nearest Mip"),
		NEAREST_MIPMAP_LINEAR(GL45C.GL_NEAREST_MIPMAP_LINEAR, "Nearest from Linear Mip"),
		LINEAR_MIPMAP_LINEAR(GL45C.GL_LINEAR_MIPMAP_LINEAR, "Linear from Linear Mip");

		public final int id;
		public final String description;

		private MinFilter(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static MinFilter from(int id) {
			return switch (id) {
				case GL45C.GL_NEAREST -> NEAREST;
				case GL45C.GL_LINEAR -> LINEAR;
				case GL45C.GL_NEAREST_MIPMAP_NEAREST -> NEAREST_MIPMAP_NEAREST;
				case GL45C.GL_LINEAR_MIPMAP_NEAREST -> LINEAR_MIPMAP_NEAREST;
				case GL45C.GL_NEAREST_MIPMAP_LINEAR -> NEAREST_MIPMAP_LINEAR;
				case GL45C.GL_LINEAR_MIPMAP_LINEAR -> LINEAR_MIPMAP_LINEAR;
				default -> null;
			};
		}
	}

	public static enum MagFilter {
		NEAREST(GL45C.GL_NEAREST, "Nearest"),
		LINEAR(GL45C.GL_LINEAR, "Linear");

		public final int id;
		public final String description;

		private MagFilter(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static MagFilter from(int id) {
			return switch (id) {
				case GL45C.GL_NEAREST -> NEAREST;
				case GL45C.GL_LINEAR -> LINEAR;
				default -> null;
			};
		}
	}

	public static enum WrapAxis {
		S(GL45C.GL_TEXTURE_WRAP_S, "S"),
		T(GL45C.GL_TEXTURE_WRAP_T, "T"),
		R(GL45C.GL_TEXTURE_WRAP_T, "R");

		public final int id;
		public final String description;

		private WrapAxis(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}
	}

	public static enum WrapMode {
		CLAMP_TO_EDGE(GL45C.GL_CLAMP_TO_EDGE, "Clamp to Edge"),
		CLAMP_TO_BORDER(GL45C.GL_CLAMP_TO_BORDER, "Clamp to Border"),
		MIRRORED_REPEAT(GL45C.GL_MIRRORED_REPEAT, "Mirrored Repeat"),
		REPEAT(GL45C.GL_REPEAT, "Repeat");

		public final int id;
		public final String description;

		private WrapMode(int id, String description) {
			this.id = id;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}

		public static WrapMode from(int id) {
			return switch (id) {
				case GL45C.GL_CLAMP_TO_EDGE -> CLAMP_TO_EDGE;
				case GL45C.GL_CLAMP_TO_BORDER -> CLAMP_TO_BORDER;
				case GL45C.GL_MIRRORED_REPEAT -> MIRRORED_REPEAT;
				case GL45C.GL_REPEAT -> REPEAT;
				default -> null;
			};
		}
	}

	public final Type type;

	protected Size size = Size.ZERO;
	protected boolean storageAllocated = false;

	protected Format textureFormat;
	protected MinFilter minFilter;
	protected MagFilter magFilter;
	protected WrapMode wrapModeS;
	protected WrapMode wrapModeT;
	protected WrapMode wrapModeR;

	protected GlTexture(Type type, int id, boolean owned) {
		super(id, owned);
		this.type = type;
		queryTexParams();
	}

	protected GlTexture(Type type) {
		super(GL45C.glCreateTextures(type.id), true);
		this.type = type;
		queryTexParams();
	}

	@Override
	protected void destroy() {
		// FIXME: update GlStateManager stuff
		GL45C.glDeleteTextures(this.id);
	}

	@Override
	public void writeDebugInfo(StringBuilder output) {
		super.writeDebugInfo(output);
		output.append(String.format("  size: [%s] %s\n",
				String.valueOf(this.type), String.valueOf(this.size)));
		output.append(String.format("  format: %s\n",
				String.valueOf(this.textureFormat)));
		output.append(String.format("  filtering: [min:%s] [max:%s]\n",
				String.valueOf(this.minFilter), String.valueOf(this.magFilter)));
		output.append(String.format("  wrap: [S:%s] [T:%s] [R:%s]\n",
				String.valueOf(this.wrapModeS), String.valueOf(this.wrapModeT), String.valueOf(this.wrapModeR)));
	}

	private void queryTexParams() {
		this.minFilter = MinFilter.from(GL45C.glGetTextureParameteri(this.id, GL45C.GL_TEXTURE_MIN_FILTER));
		this.magFilter = MagFilter.from(GL45C.glGetTextureParameteri(this.id, GL45C.GL_TEXTURE_MAG_FILTER));
		this.wrapModeS = WrapMode.from(GL45C.glGetTextureParameteri(this.id, GL45C.GL_TEXTURE_WRAP_S));
		this.wrapModeT = WrapMode.from(GL45C.glGetTextureParameteri(this.id, GL45C.GL_TEXTURE_WRAP_T));
		this.wrapModeR = WrapMode.from(GL45C.glGetTextureParameteri(this.id, GL45C.GL_TEXTURE_WRAP_R));
		if (this.type != Type.CUBEMAP) {
			final var sizeX = GL45C.glGetTextureLevelParameteri(this.id, 0, GL45C.GL_TEXTURE_WIDTH);
			final var sizeY = GL45C.glGetTextureLevelParameteri(this.id, 0, GL45C.GL_TEXTURE_HEIGHT);
			final var sizeZ = GL45C.glGetTextureLevelParameteri(this.id, 0, GL45C.GL_TEXTURE_DEPTH);
			this.size = new Size(sizeX, sizeY, sizeZ, 1);
			this.textureFormat = Format
					.from(GL45C.glGetTextureLevelParameteri(this.id, 0, GL45C.GL_TEXTURE_INTERNAL_FORMAT));
			if (this.textureFormat == null) {
				HawkLib.LOGGER.error("texture object has unknown format '{}'",
						GL45C.glGetTextureLevelParameteri(this.id, 0, GL45C.GL_TEXTURE_INTERNAL_FORMAT));
			}
		} else {
			final var size = GL45C.glGetTextureLevelParameteri(this.id, 0, GL45C.GL_TEXTURE_WIDTH);
			this.size = new Size(size, size, 1, 1);
			this.textureFormat = Format
					.from(GL45C.glGetTexLevelParameteri(this.id, 0, GL45C.GL_TEXTURE_INTERNAL_FORMAT));
		}
	}

	@Override
	public final ObjectType objectType() {
		return ObjectType.TEXTURE;
	}

	public Size size() {
		return this.size;
	}

	public boolean isValid() {
		return this.storageAllocated && !this.isDestroyed();
	}

	public int id() {
		return this.id;
	}

	public void bind() {
		GlManager.bindTexture(this.type, this.id);
	}

	public @Nullable GlTexture2d asTexture2d() {
		return null;
	}

	public Format format() {
		return this.textureFormat;
	}

	public void setMinFilter(MinFilter filter) {
		if (this.minFilter == filter)
			return;
		GL45C.glTextureParameteri(this.id, GL45C.GL_TEXTURE_MIN_FILTER, filter.id);
		this.minFilter = filter;
	}

	public void setMagFilter(MagFilter filter) {
		if (this.magFilter == filter)
			return;
		GL45C.glTextureParameteri(this.id, GL45C.GL_TEXTURE_MAG_FILTER, filter.id);
		this.magFilter = filter;
	}

	public WrapMode getWrapMode(WrapAxis axis) {
		return switch (axis) {
			case S -> this.wrapModeS;
			case T -> this.wrapModeT;
			case R -> this.wrapModeR;
		};
	}

	public void setWrapMode(WrapAxis axis, WrapMode mode) {
		if (getWrapMode(axis) == mode)
			return;
		GL45C.glTextureParameteri(this.id, axis.id, mode.id);
		switch (axis) {
			case S -> this.wrapModeS = mode;
			case T -> this.wrapModeT = mode;
			case R -> this.wrapModeR = mode;
		}
	}

	public void setWrapMode(WrapMode mode) {
		if (this.wrapModeS != mode)
			GL45C.glTextureParameteri(this.id, WrapAxis.S.id, mode.id);
		if (this.wrapModeT != mode)
			GL45C.glTextureParameteri(this.id, WrapAxis.T.id, mode.id);
		if (this.wrapModeR != mode)
			GL45C.glTextureParameteri(this.id, WrapAxis.R.id, mode.id);
		this.wrapModeS = this.wrapModeT = this.wrapModeR = mode;
	}

	// public void createStorage(GlTexture.Format textureFormat, int width, int height, int depth) {
	// 	GlLimits.validateTextureSize(width, height, depth);
	// 	if (this.textureFormat == textureFormat
	// 			&& this.size.width == width
	// 			&& this.size.height == height
	// 			&& this.size.depth == depth)
	// 		return;

	// 	switch (this.type) {
	// 		case D2 -> GL45C.glTextureStorage2D(this.id, 1, textureFormat.id, width, height);
	// 		case D2_MS -> GL45C.glTextureStorage2DMultisample(this.type.id, 4, textureFormat.id, width, height, true);
	// 		default -> throw new IllegalStateException(
	// 				debugDescription() + "Invalid type for 2d texture: " + this.type.description);
	// 	}
	// 	this.textureFormat = textureFormat;
	// 	this.size = new Size(width, height, 1, 1);
	// 	this.storageAllocated = true;
	// }

}
