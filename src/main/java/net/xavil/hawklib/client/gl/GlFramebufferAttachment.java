package net.xavil.hawklib.client.gl;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.platform.GlStateManager;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.texture.GlCubemapTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.client.gl.texture.GlTexture.Format;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec2i;

public abstract sealed class GlFramebufferAttachment implements Disposable {
	/**
	 * A flag that indicates whether or not this framebuffer target is owned. A
	 * target that is owned is responsible for managing its internal objects, such
	 * as resizing textures and deleting them when the target is closed. Targets
	 * that have this flag set to false act like "view" objects. Cubemap targets can
	 * never be owned.
	 */
	public final boolean owned;

	public ClearState colorClearState = ClearState.setFloat(Color.TRANSPARENT);
	public ClearState depthClearState = ClearState.setFloat(1.0f);
	public ClearState stencilClearState = ClearState.setInt(0);

	public GlFramebufferAttachment(boolean owned) {
		this.owned = owned;
	}

	/**
	 * Attaches this famebuffer target to the specified attachment point of the
	 * currently-bound framebuffer.
	 * 
	 * @param attachmentPoint The attachment point index
	 */
	public abstract void attach(int attachmentPoint);

	public static void detach(int attachmentPoint) {
		GlStateManager._glFramebufferTexture2D(GL32C.GL_FRAMEBUFFER, attachmentPoint,
				GL32C.GL_TEXTURE_2D, 0, 0);
	}

	/**
	 * This methods checks whether this framebuffer target will modify the contents
	 * of the passed object. A generic {@link GlObject} is taken so that both
	 * textures and renderbuffers can be queried. If the passed object is not a type
	 * of object that a framebuffer can render to, this method will return
	 * {@code false}.
	 * 
	 * @param texture The object to check
	 * @return {@code true} if {@code texture} is modified by this target
	 */
	public abstract boolean writesTo(GlObject texture);

	public abstract Vec2i size();

	public abstract GlTexture.Format format();

	/**
	 * If this framebuffer target writes to a texture object, this method will
	 * return that texture object. If this is a cubemap face target, this method
	 * will return the whole cubemap texture.
	 * 
	 * @return The framebuffer write target
	 */
	public @Nullable GlTexture asTexture() {
		return asTexture2d();
	}

	/**
	 * Like {@link #asTexture()}, except this will only return 2d texture objects.
	 * 
	 * @return The framebuffer write target
	 */
	public @Nullable GlTexture2d asTexture2d() {
		return null;
	}

	public static final class Texture2d extends GlFramebufferAttachment {
		public final GlTexture2d target;

		public Texture2d(boolean owned, GlTexture2d target) {
			super(owned);
			this.target = target;
		}

		@Override
		public void attach(int attachmentPoint) {
			GlStateManager._glFramebufferTexture2D(GL32C.GL_FRAMEBUFFER, attachmentPoint,
					GL32C.GL_TEXTURE_2D, this.target.id(), 0);
		}

		@Override
		public boolean writesTo(GlObject texture) {
			return this.target.equals(texture);
		}

		@Override
		public Vec2i size() {
			return this.target.size().d2();
		}

		@Override
		public Format format() {
			return this.target.format();
		}

		@Override
		public GlTexture2d asTexture2d() {
			return this.target;
		}

		@Override
		public String toString() {
			return (this.owned ? "owned " : "unowned ") + "texture 2d attachment: " + this.target;
		}

		@Override
		public void close() {
			if (this.owned)
				this.target.close();
		}
	}

	public static final class CubemapFace extends GlFramebufferAttachment {
		public final GlCubemapTexture target;
		public final GlCubemapTexture.Face face;

		public CubemapFace(GlCubemapTexture target, GlCubemapTexture.Face face) {
			super(false);
			this.target = target;
			this.face = face;
		}

		@Override
		public void attach(int attachmentPoint) {
			GlStateManager._glFramebufferTexture2D(GL32C.GL_FRAMEBUFFER, attachmentPoint,
					this.face.glId, this.target.id(), 0);
		}

		@Override
		public boolean writesTo(GlObject texture) {
			return this.target.equals(texture);
		}

		@Override
		public Vec2i size() {
			return this.target.size().d2();
		}

		@Override
		public Format format() {
			return this.target.format();
		}

		@Override
		public GlTexture asTexture() {
			return this.target;
		}

		@Override
		public String toString() {
			return "unowned cubemap " + this.face + " face attachment: " + this.target;
		}

		@Override
		public void close() {
			// individual cubemap faces cannot be owned.
		}
	}

	public static final class Renderbuffer extends GlFramebufferAttachment {
		public final GlRenderbuffer target;

		public Renderbuffer(boolean owned, GlRenderbuffer target) {
			super(owned);
			this.target = target;
		}

		@Override
		public void attach(int attachmentPoint) {
			GlStateManager._glFramebufferRenderbuffer(GL32C.GL_FRAMEBUFFER, attachmentPoint,
					GL32C.GL_RENDERBUFFER, this.target.id);
		}

		@Override
		public boolean writesTo(GlObject texture) {
			return this.target.equals(texture);
		}

		@Override
		public Vec2i size() {
			return this.target.size();
		}

		@Override
		public Format format() {
			return this.target.format();
		}

		@Override
		public String toString() {
			return (this.owned ? "owned " : "unowned ") + "renderbuffer attachment: " + this.target;
		}

		@Override
		public void close() {
			if (this.owned)
				this.target.close();
		}
	}
}