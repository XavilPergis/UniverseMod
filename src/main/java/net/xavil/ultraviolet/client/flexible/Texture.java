package net.xavil.ultraviolet.client.flexible;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.util.math.matrices.Vec2i;
import net.xavil.util.math.matrices.Vec3i;

public abstract class Texture extends GlObject {

	public static enum Format {
		R32G32B32A32_FLOAT(GL32.GL_RGBA32F, "R32G32B32A32_FLOAT", 4, false, false),
		R32G32B32A32_UINT(GL32.GL_RGBA32UI, "R32G32B32A32_UINT", 4, false, false),
		R32G32B32A32_INT(GL32.GL_RGBA32I, "R32G32B32A32_INT", 4, false, false),
		R32G32B32_FLOAT(GL32.GL_RGB32F, "R32G32B32_FLOAT", 3, false, false),
		R32G32B32_UINT(GL32.GL_RGB32UI, "R32G32B32_UINT", 3, false, false),
		R32G32B32_INT(GL32.GL_RGB32I, "R32G32B32_INT", 3, false, false),
		R32G32_FLOAT(GL32.GL_RG32F, "R32G32_FLOAT", 2, false, false),
		R32G32_UINT(GL32.GL_RG32UI, "R32G32_UINT", 2, false, false),
		R32G32_INT(GL32.GL_RG32I, "R32G32_INT", 2, false, false),
		R32_FLOAT(GL32.GL_R32F, "R32_FLOAT", 1, false, false),
		R32_UINT(GL32.GL_R32UI, "R32_UINT", 1, false, false),
		R32_INT(GL32.GL_R32I, "R32_INT", 1, false, false),
		R16G16B16A16_FLOAT(GL32.GL_RGBA16F, "R16G16B16A16_FLOAT", 4, false, false),
		R16G16B16A16_UINT_NORM(GL32.GL_RGBA16, "R16G16B16A16_UINT_NORM", 4, false, false),
		R16G16B16A16_UINT(GL32.GL_RGBA16UI, "R16G16B16A16_UINT", 4, false, false),
		R16G16B16A16_INT_NORM(GL32.GL_RGBA16_SNORM, "R16G16B16A16_INT_NORM", 4, false, false),
		R16G16B16A16_INT(GL32.GL_RGBA16I, "R16G16B16A16_INT", 4, false, false),
		R16G16B16_FLOAT(GL32.GL_RGB16F, "R16G16B16_FLOAT", 3, false, false),
		R16G16B16_UINT_NORM(GL32.GL_RGB16, "R16G16B16_UINT_NORM", 3, false, false),
		R16G16B16_UINT(GL32.GL_RGB16UI, "R16G16B16_UINT", 3, false, false),
		R16G16B16_INT_NORM(GL32.GL_RGB16_SNORM, "R16G16B16_INT_NORM", 3, false, false),
		R16G16B16_INT(GL32.GL_RGB16I, "R16G16B16_INT", 3, false, false),
		R16G16_FLOAT(GL32.GL_RG16F, "R16G16_FLOAT", 2, false, false),
		R16G16_UINT_NORM(GL32.GL_RG16, "R16G16_UINT_NORM", 2, false, false),
		R16G16_UINT(GL32.GL_RG16UI, "R16G16_UINT", 2, false, false),
		R16G16_INT_NORM(GL32.GL_RG16_SNORM, "R16G16_INT_NORM", 2, false, false),
		R16G16_INT(GL32.GL_RG16I, "R16G16_INT", 2, false, false),
		R16_FLOAT(GL32.GL_R16F, "R16_FLOAT", 1, false, false),
		R16_UINT_NORM(GL32.GL_R16, "R16_UINT_NORM", 1, false, false),
		R16_UINT(GL32.GL_R16UI, "R16_UINT", 1, false, false),
		R16_INT_NORM(GL32.GL_R16_SNORM, "R16_INT_NORM", 1, false, false),
		R16_INT(GL32.GL_R16I, "R16_INT", 1, false, false),
		R8G8B8A8_UINT_NORM(GL32.GL_RGBA8, "R8G8B8A8_UINT_NORM", 4, false, false),
		R8G8B8A8_UINT(GL32.GL_RGBA8UI, "R8G8B8A8_UINT", 4, false, false),
		R8G8B8A8_INT_NORM(GL32.GL_RGBA8_SNORM, "R8G8B8A8_INT_NORM", 4, false, false),
		R8G8B8A8_INT(GL32.GL_RGBA8I, "R8G8B8A8_INT", 4, false, false),
		R8G8B8_UINT_NORM(GL32.GL_RGB8, "R8G8B8_UINT_NORM", 3, false, false),
		R8G8B8_UINT(GL32.GL_RGB8UI, "R8G8B8_UINT", 3, false, false),
		R8G8B8_INT_NORM(GL32.GL_RGB8_SNORM, "R8G8B8_INT_NORM", 3, false, false),
		R8G8B8_INT(GL32.GL_RGB8I, "R8G8B8_INT", 3, false, false),
		R8G8_UINT_NORM(GL32.GL_RG8, "R8G8_UINT_NORM", 2, false, false),
		R8G8_UINT(GL32.GL_RG8UI, "R8G8_UINT", 2, false, false),
		R8G8_INT_NORM(GL32.GL_RG8_SNORM, "R8G8_INT_NORM", 2, false, false),
		R8G8_INT(GL32.GL_RG8I, "R8G8_INT", 2, false, false),
		R8_UINT_NORM(GL32.GL_R8, "R8_UINT_NORM", 1, false, false),
		R8_UINT(GL32.GL_R8UI, "R8_UINT", 1, false, false),
		R8_INT_NORM(GL32.GL_R8_SNORM, "R8_INT_NORM", 1, false, false),
		R8_INT(GL32.GL_R8I, "R8_INT", 1, false, false),
		R8G8B8_SRGB_A8(GL32.GL_SRGB8_ALPHA8, "R8G8B8_SRGB_A8", 4, false, false),
		R8G8B8_SRGB(GL32.GL_SRGB8, "R8G8B8_SRGB", 3, false, false),
		R10G10B10A2_UINT_NORM(GL32.GL_RGB10_A2, "R10G10B10A2_UINT_NORM", 4, false, false),
		R11G11B10_FLOAT(GL32.GL_R11F_G11F_B10F, "R11G11B10_FLOAT", 3, false, false),
		RGB9E5_FLOAT(GL32.GL_RGB9_E5, "RGB9E5_FLOAT", 3, false, false),
		COMPRESSED_RG_RGTC2(GL32.GL_COMPRESSED_RG_RGTC2, "GL_COMPRESSED_RG_RGTC2", 2, false, false),
		COMPRESSED_SIGNED_RG_RGTC2(GL32.GL_COMPRESSED_SIGNED_RG_RGTC2, "GL_COMPRESSED_SIGNED_RG_RGTC2", 2, false,
				false),
		COMPRESSED_RED_RGTC1(GL32.GL_COMPRESSED_RED_RGTC1, "GL_COMPRESSED_RED_RGTC1", 1, false, false),
		COMPRESSED_SIGNED_RED_RGTC1(GL32.GL_COMPRESSED_SIGNED_RED_RGTC1, "GL_COMPRESSED_SIGNED_RED_RGTC1", 1, false,
				false),
		DEPTH32_FLOAT(GL32.GL_DEPTH_COMPONENT32F, "GL_DEPTH_COMPONENT32F", 1, true, false),
		DEPTH24(GL32.GL_DEPTH_COMPONENT24, "GL_DEPTH_COMPONENT24", 1, true, false),
		DEPTH16(GL32.GL_DEPTH_COMPONENT16, "GL_DEPTH_COMPONENT16", 1, true, false),
		DEPTH32_FLOAT_STENCIL8(GL32.GL_DEPTH32F_STENCIL8, "GL_DEPTH32F_STENCIL8", 2, true, true),
		DEPTH24_STENCIL8(GL32.GL_DEPTH24_STENCIL8, "GL_DEPTH24_STENCIL8", 2, true, true);

		public final int id;
		public final String description;
		public final boolean isDepthFormat;
		public final boolean isStencilFormat;
		public final boolean isColorFormat;
		public final int channelCount;

		private Format(int id, String description, int channelCount, boolean depth, boolean stencil) {
			this.id = id;
			this.description = description;
			this.channelCount = channelCount;
			this.isColorFormat = !depth && !stencil;
			this.isDepthFormat = depth;
			this.isStencilFormat = stencil;
		}

		public static Format from(int value) {
			return switch (value) {
				case GL32.GL_RGBA32F -> R32G32B32A32_FLOAT;
				case GL32.GL_RGBA32UI -> R32G32B32A32_UINT;
				case GL32.GL_RGBA32I -> R32G32B32A32_INT;
				case GL32.GL_RGB32F -> R32G32B32_FLOAT;
				case GL32.GL_RGB32UI -> R32G32B32_UINT;
				case GL32.GL_RGB32I -> R32G32B32_INT;
				case GL32.GL_RG32F -> R32G32_FLOAT;
				case GL32.GL_RG32UI -> R32G32_UINT;
				case GL32.GL_RG32I -> R32G32_INT;
				case GL32.GL_R32F -> R32_FLOAT;
				case GL32.GL_R32UI -> R32_UINT;
				case GL32.GL_R32I -> R32_INT;
				case GL32.GL_RGBA16F -> R16G16B16A16_FLOAT;
				case GL32.GL_RGBA16 -> R16G16B16A16_UINT_NORM;
				case GL32.GL_RGBA16UI -> R16G16B16A16_UINT;
				case GL32.GL_RGBA16_SNORM -> R16G16B16A16_INT_NORM;
				case GL32.GL_RGBA16I -> R16G16B16A16_INT;
				case GL32.GL_RGB16F -> R16G16B16_FLOAT;
				case GL32.GL_RGB16 -> R16G16B16_UINT_NORM;
				case GL32.GL_RGB16UI -> R16G16B16_UINT;
				case GL32.GL_RGB16_SNORM -> R16G16B16_INT_NORM;
				case GL32.GL_RGB16I -> R16G16B16_INT;
				case GL32.GL_RG16F -> R16G16_FLOAT;
				case GL32.GL_RG16 -> R16G16_UINT_NORM;
				case GL32.GL_RG16UI -> R16G16_UINT;
				case GL32.GL_RG16_SNORM -> R16G16_INT_NORM;
				case GL32.GL_RG16I -> R16G16_INT;
				case GL32.GL_R16F -> R16_FLOAT;
				case GL32.GL_R16 -> R16_UINT_NORM;
				case GL32.GL_R16UI -> R16_UINT;
				case GL32.GL_R16_SNORM -> R16_INT_NORM;
				case GL32.GL_R16I -> R16_INT;
				case GL32.GL_RGBA8 -> R8G8B8A8_UINT_NORM;
				case GL32.GL_RGBA8UI -> R8G8B8A8_UINT;
				case GL32.GL_RGBA8_SNORM -> R8G8B8A8_INT_NORM;
				case GL32.GL_RGBA8I -> R8G8B8A8_INT;
				case GL32.GL_RGB8 -> R8G8B8_UINT_NORM;
				case GL32.GL_RGB8UI -> R8G8B8_UINT;
				case GL32.GL_RGB8_SNORM -> R8G8B8_INT_NORM;
				case GL32.GL_RGB8I -> R8G8B8_INT;
				case GL32.GL_RG8 -> R8G8_UINT_NORM;
				case GL32.GL_RG8UI -> R8G8_UINT;
				case GL32.GL_RG8_SNORM -> R8G8_INT_NORM;
				case GL32.GL_RG8I -> R8G8_INT;
				case GL32.GL_R8 -> R8_UINT_NORM;
				case GL32.GL_R8UI -> R8_UINT;
				case GL32.GL_R8_SNORM -> R8_INT_NORM;
				case GL32.GL_R8I -> R8_INT;
				case GL32.GL_SRGB8_ALPHA8 -> R8G8B8_SRGB_A8;
				case GL32.GL_SRGB8 -> R8G8B8_SRGB;
				case GL32.GL_RGB10_A2 -> R10G10B10A2_UINT_NORM;
				case GL32.GL_R11F_G11F_B10F -> R11G11B10_FLOAT;
				case GL32.GL_RGB9_E5 -> RGB9E5_FLOAT;
				case GL32.GL_COMPRESSED_RG_RGTC2 -> COMPRESSED_RG_RGTC2;
				case GL32.GL_COMPRESSED_SIGNED_RG_RGTC2 -> COMPRESSED_SIGNED_RG_RGTC2;
				case GL32.GL_COMPRESSED_RED_RGTC1 -> COMPRESSED_RED_RGTC1;
				case GL32.GL_COMPRESSED_SIGNED_RED_RGTC1 -> COMPRESSED_SIGNED_RED_RGTC1;
				case GL32.GL_DEPTH_COMPONENT32F -> DEPTH32_FLOAT;
				case GL32.GL_DEPTH_COMPONENT24 -> DEPTH24;
				case GL32.GL_DEPTH_COMPONENT16 -> DEPTH16;
				case GL32.GL_DEPTH32F_STENCIL8 -> DEPTH32_FLOAT_STENCIL8;
				case GL32.GL_DEPTH24_STENCIL8 -> DEPTH24_STENCIL8;
				default -> null;
			};
		}
	}

	public static enum Type {
		D1(GL32.GL_TEXTURE_1D, "1D"),
		D1_ARRAY(GL32.GL_TEXTURE_1D_ARRAY, "1D Array"),
		D2(GL32.GL_TEXTURE_2D, "2D"),
		D2_ARRAY(GL32.GL_TEXTURE_2D_ARRAY, "2D Array"),
		D2_MS(GL32.GL_TEXTURE_2D_MULTISAMPLE, "2D Multisampled"),
		D2_MS_ARRAY(GL32.GL_TEXTURE_2D_MULTISAMPLE_ARRAY, "2D Multisampled Array"),
		D3(GL32.GL_TEXTURE_3D, "3D"),
		CUBEMAP(GL32.GL_TEXTURE_CUBE_MAP, "Cubemap"),
		BUFFER(GL32.GL_TEXTURE_BUFFER, "Buffer-Backed"),
		RECTANGLE(GL32.GL_TEXTURE_RECTANGLE, "Rectangle");

		public final int id;
		public final String description;

		private Type(int gl, String description) {
			this.id = gl;
			this.description = description;
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
	}

	public static enum MinFilter {
		NEAREST(GL32.GL_NEAREST, "Nearest"),
		LINEAR(GL32.GL_LINEAR, "Linear"),
		NEAREST_MIPMAP_NEAREST(GL32.GL_NEAREST_MIPMAP_NEAREST, "Nearest from Nearest Mip"),
		LINEAR_MIPMAP_NEAREST(GL32.GL_LINEAR_MIPMAP_NEAREST, "Linear from Nearest Mip"),
		NEAREST_MIPMAP_LINEAR(GL32.GL_NEAREST_MIPMAP_LINEAR, "Nearest from Linear Mip"),
		LINEAR_MIPMAP_LINEAR(GL32.GL_LINEAR_MIPMAP_LINEAR, "Linear from Linear Mip");

		public final int id;
		public final String description;

		private MinFilter(int id, String description) {
			this.id = id;
			this.description = description;
		}

		public static MinFilter from(int id) {
			return switch (id) {
				case GL32.GL_NEAREST -> NEAREST;
				case GL32.GL_LINEAR -> LINEAR;
				case GL32.GL_NEAREST_MIPMAP_NEAREST -> NEAREST_MIPMAP_NEAREST;
				case GL32.GL_LINEAR_MIPMAP_NEAREST -> LINEAR_MIPMAP_NEAREST;
				case GL32.GL_NEAREST_MIPMAP_LINEAR -> NEAREST_MIPMAP_LINEAR;
				case GL32.GL_LINEAR_MIPMAP_LINEAR -> LINEAR_MIPMAP_LINEAR;
				default -> null;
			};
		}
	}

	public static enum MagFilter {
		NEAREST(GL32.GL_NEAREST, "Nearest"),
		LINEAR(GL32.GL_LINEAR, "Linear");

		public final int id;
		public final String description;

		private MagFilter(int id, String description) {
			this.id = id;
			this.description = description;
		}

		public static MagFilter from(int id) {
			return switch (id) {
				case GL32.GL_NEAREST -> NEAREST;
				case GL32.GL_LINEAR -> LINEAR;
				default -> null;
			};
		}
	}

	public static enum WrapAxis {
		S(GL32.GL_TEXTURE_WRAP_S, "S"),
		T(GL32.GL_TEXTURE_WRAP_T, "T"),
		R(GL32.GL_TEXTURE_WRAP_T, "R");

		public final int id;
		public final String description;

		private WrapAxis(int id, String description) {
			this.id = id;
			this.description = description;
		}
	}

	public static enum WrapMode {
		CLAMP_TO_EDGE(GL32.GL_CLAMP_TO_EDGE, "Clamp to Edge"),
		CLAMP_TO_BORDER(GL32.GL_CLAMP_TO_BORDER, "Clamp to Border"),
		MIRRORED_REPEAT(GL32.GL_MIRRORED_REPEAT, "Mirrored Repeat"),
		REPEAT(GL32.GL_REPEAT, "Repeat");

		public final int id;
		public final String description;

		private WrapMode(int id, String description) {
			this.id = id;
			this.description = description;
		}

		public static WrapMode from(int id) {
			return switch (id) {
				case GL32.GL_CLAMP_TO_EDGE -> CLAMP_TO_EDGE;
				case GL32.GL_CLAMP_TO_BORDER -> CLAMP_TO_BORDER;
				case GL32.GL_MIRRORED_REPEAT -> MIRRORED_REPEAT;
				case GL32.GL_REPEAT -> REPEAT;
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

	protected Texture(Type type, int id, boolean owned) {
		super(id, owned);
		this.type = type;
		queryTexParams();
	}

	protected Texture(Type type) {
		super(TextureUtil.generateTextureId(), true);
		this.type = type;
		// bind to set this texture's type
		bind();
		queryTexParams();
	}

	private void queryTexParams() {
		bind();
		this.minFilter = MinFilter.from(GL32.glGetTexParameteri(type.id, GL32.GL_TEXTURE_MIN_FILTER));
		this.magFilter = MagFilter.from(GL32.glGetTexParameteri(type.id, GL32.GL_TEXTURE_MAG_FILTER));
		this.wrapModeS = WrapMode.from(GL32.glGetTexParameteri(type.id, GL32.GL_TEXTURE_WRAP_S));
		this.wrapModeT = WrapMode.from(GL32.glGetTexParameteri(type.id, GL32.GL_TEXTURE_WRAP_T));
		this.wrapModeR = WrapMode.from(GL32.glGetTexParameteri(type.id, GL32.GL_TEXTURE_WRAP_R));
		if (this.type != Type.CUBEMAP) {
			final var sizeX = GL32.glGetTexLevelParameteri(type.id, 0, GL32.GL_TEXTURE_WIDTH);
			final var sizeY = GL32.glGetTexLevelParameteri(type.id, 0, GL32.GL_TEXTURE_HEIGHT);
			final var sizeZ = GL32.glGetTexLevelParameteri(type.id, 0, GL32.GL_TEXTURE_DEPTH);
			this.size = new Size(sizeX, sizeY, sizeZ, 1);
			this.textureFormat = Format.from(GL32.glGetTexLevelParameteri(type.id, 0, GL32.GL_TEXTURE_INTERNAL_FORMAT));
		} else {
			final var size = GL32.glGetTexLevelParameteri(GL32.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0,
					GL32.GL_TEXTURE_WIDTH);
			this.size = new Size(size, size, 1, 1);
			this.textureFormat = Format.from(GL32.glGetTexLevelParameteri(GL32.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0,
					GL32.GL_TEXTURE_INTERNAL_FORMAT));
		}
	}

	@Override
	protected void release(int id) {
		TextureUtil.releaseTextureId(this.id);
	}

	@Override
	public final ObjectType objectType() {
		return ObjectType.TEXTURE;
	}

	public Size size() {
		return this.size;
	}

	public boolean isValid() {
		return this.storageAllocated;
	}

	public int id() {
		return this.id;
	}

	public void bind() {
		bindTexture(this.type.id, this.id);
	}

	public @Nullable Texture2d asTexture2d() {
		return null;
	}

	public void setMinFilter(MinFilter filter) {
		if (this.minFilter == filter)
			return;
		bind();
		GlStateManager._texParameter(this.type.id, GL32.GL_TEXTURE_MIN_FILTER, filter.id);
		this.minFilter = filter;
	}

	public void setMagFilter(MagFilter filter) {
		if (this.magFilter == filter)
			return;
		bind();
		GlStateManager._texParameter(this.type.id, GL32.GL_TEXTURE_MAG_FILTER, filter.id);
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
		bind();
		GlStateManager._texParameter(this.type.id, axis.id, mode.id);
		switch (axis) {
			case S -> this.wrapModeS = mode;
			case T -> this.wrapModeT = mode;
			case R -> this.wrapModeR = mode;
		}
	}

	public void setWrapMode(WrapMode mode) {
		bind();
		if (this.wrapModeS != mode)
			GlStateManager._texParameter(this.type.id, WrapAxis.S.id, mode.id);
		if (this.wrapModeT != mode)
			GlStateManager._texParameter(this.type.id, WrapAxis.T.id, mode.id);
		if (this.wrapModeR != mode)
			GlStateManager._texParameter(this.type.id, WrapAxis.R.id, mode.id);
		this.wrapModeS = this.wrapModeT = this.wrapModeR = mode;
	}

	// @Override
	// public String toString() {
	// // [1920x1080x3 Texture "Main"]
	// // return
	// }

	public static void bindTexture(int target, int id) {
		RenderSystem.assertOnRenderThreadOrInit();
		if (id != GlStateManager.TEXTURES[GlStateManager.activeTexture].binding) {
			GlStateManager.TEXTURES[GlStateManager.activeTexture].binding = id;
			GL32.glBindTexture(target, id);
		}
	}

}
