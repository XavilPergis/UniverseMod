package net.xavil.universal.client.flexible;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.util.math.matrices.Vec2i;
import net.xavil.util.math.matrices.Vec3i;

public abstract class Texture extends GlObject {

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

	protected int textureFormat = -1;
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
		// if (this.minFilter == filter)
		// 	return;
		bind();
		GlStateManager._texParameter(this.type.id, GL32.GL_TEXTURE_MIN_FILTER, filter.id);
		this.minFilter = filter;
	}

	public void setMagFilter(MagFilter filter) {
		// if (this.magFilter == filter)
			// return;
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
		// if (this.wrapModeS == mode)
			GlStateManager._texParameter(this.type.id, WrapAxis.S.id, mode.id);
		// if (this.wrapModeT == mode)
			GlStateManager._texParameter(this.type.id, WrapAxis.T.id, mode.id);
		// if (this.wrapModeR == mode)
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
