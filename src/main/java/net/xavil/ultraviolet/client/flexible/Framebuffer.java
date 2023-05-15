package net.xavil.ultraviolet.client.flexible;

import java.util.Objects;
import java.util.OptionalInt;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.util.Disposable;
import net.xavil.util.collections.interfaces.ImmutableMap;
import net.xavil.util.collections.interfaces.MutableMap;
import net.xavil.util.math.Color;
import net.xavil.util.math.matrices.Vec2i;

public final class Framebuffer extends GlObject {

	public record Viewport(Vec2i pos, Vec2i size) {
	}

	public static abstract sealed class Target implements Disposable {
		public final boolean owned;

		public Target(boolean owned) {
			this.owned = owned;
		}

		public abstract void attach(int attachmentPoint);

		public @Nullable Texture asTexture() {
			return null;
		}

		public @Nullable Texture2d asTexture2d() {
			return null;
		}

		public static void detach(int attachmentPoint) {
			GlStateManager._glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, attachmentPoint,
					GL32.GL_TEXTURE_2D, 0, 0);
		}

		public static final class Texture2dTarget extends Target {
			public final Texture2d target;

			public Texture2dTarget(boolean owned, Texture2d target) {
				super(owned);
				this.target = target;
			}

			@Override
			public void attach(int attachmentPoint) {
				GlStateManager._glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, attachmentPoint,
						GL32.GL_TEXTURE_2D, this.target.id(), 0);
			}

			@Override
			public Texture asTexture() {
				return this.target;
			}

			@Override
			public Texture2d asTexture2d() {
				return this.target;
			}

			@Override
			public void close() {
				if (this.owned)
					this.target.close();
			}
		}

		public static final class CubemapFaceTarget extends Target {
			public final CubemapTexture target;
			public final CubemapTexture.Face face;

			public CubemapFaceTarget(CubemapTexture target, CubemapTexture.Face face) {
				super(false);
				this.target = target;
				this.face = face;
			}

			@Override
			public void attach(int attachmentPoint) {
				GlStateManager._glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, attachmentPoint,
						this.face.glId, this.target.id(), 0);
			}

			@Override
			public Texture asTexture() {
				return this.target;
			}

			@Override
			public void close() {
				// individual cubemap faces cannot be owned.
			}
		}

		public static final class RenderbufferTarget extends Target {
			public final Renderbuffer target;

			public RenderbufferTarget(boolean owned, Renderbuffer target) {
				super(owned);
				this.target = target;
			}

			@Override
			public void attach(int attachmentPoint) {
				GlStateManager._glFramebufferRenderbuffer(GL32.GL_FRAMEBUFFER, attachmentPoint,
						GL32.GL_RENDERBUFFER, this.target.id());
			}

			@Override
			public void close() {
				if (this.owned)
					this.target.close();
			}
		}
	}

	public record FramebufferFormat(boolean multisample,
			ImmutableMap<Integer, Integer> colorFormats,
			OptionalInt depthFormat) {
	}

	private Vec2i size = Vec2i.ZERO;
	private Viewport viewport;

	private Color clearColor = Color.TRANSPARENT;
	private double clearDepth = 1.0;
	private int clearMask = 0;

	private final MutableMap<Integer, Target> colorTargets = MutableMap.hashMap();
	private Target depthTarget = null;

	public Framebuffer(Vec2i size) {
		super(GlStateManager.glGenFramebuffers(), true);
		this.size = size;
		this.viewport = new Viewport(Vec2i.ZERO, size);
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.FRAMEBUFFER;
	}

	@Override
	protected void release(int id) {
		GlStateManager._glDeleteFramebuffers(this.id);
	}

	private void recomputeSize() {
		this.size = Vec2i.ZERO;
		this.colorTargets.values().forEach(target -> {
			if (target instanceof Target.Texture2dTarget texTarget) {
				this.size = Vec2i.min(this.size, texTarget.target.size().d2());
			} else if (target instanceof Target.CubemapFaceTarget texTarget) {
				this.size = Vec2i.min(this.size, texTarget.target.size().d2());
			} else if (target instanceof Target.RenderbufferTarget texTarget) {
				this.size = Vec2i.min(this.size, texTarget.target.size());
			}
		});
		if (this.depthTarget != null) {
			if (this.depthTarget instanceof Target.Texture2dTarget texTarget) {
				this.size = Vec2i.min(this.size, texTarget.target.size().d2());
			} else if (this.depthTarget instanceof Target.CubemapFaceTarget texTarget) {
				this.size = Vec2i.min(this.size, texTarget.target.size().d2());
			} else if (this.depthTarget instanceof Target.RenderbufferTarget texTarget) {
				this.size = Vec2i.min(this.size, texTarget.target.size());
			}
		}
	}

	public void resize(Vec2i size) {
		this.size = size;
		this.viewport = new Viewport(Vec2i.ZERO, size);
		for (final var target : this.colorTargets.values().iterable()) {
			if (!target.owned)
				continue;
			if (target instanceof Target.Texture2dTarget texTarget) {
				texTarget.target.resize(size.x, size.y);
			} else if (target instanceof Target.RenderbufferTarget texTarget) {
				texTarget.target.resize(size.x, size.y);
			}
		}
		if (this.depthTarget != null && this.depthTarget.owned) {
			if (this.depthTarget instanceof Target.Texture2dTarget texTarget) {
				texTarget.target.resize(size.x, size.y);
			} else if (this.depthTarget instanceof Target.RenderbufferTarget texTarget) {
				texTarget.target.resize(size.x, size.y);
			}
		}
	}

	public void setColorTarget(int index, @Nullable Target target) {
		final var oldTarget = this.colorTargets.get(index).unwrapOrNull();
		if (Objects.equals(target, oldTarget))
			return;

		GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.id);

		if (target != null) {
			target.attach(GL32.GL_COLOR_ATTACHMENT0 + index);
			this.colorTargets.insert(index, target);
		} else {
			Target.detach(GL32.GL_COLOR_ATTACHMENT0 + index);
			this.colorTargets.remove(index);
		}
		if (oldTarget != null)
			oldTarget.close();
	}

	public void createColorTarget(int index, Texture.Format textureFormat) {
		final var texture = new Texture2d(false);
		texture.createStorage(textureFormat, this.size.x, this.size.y);
		final var target = new Target.Texture2dTarget(true, texture);
		setColorTarget(index, target);
	}

	public @Nullable Target getColorTarget(int index) {
		return this.colorTargets.get(index).unwrapOrNull();
	}

	public void setDepthTarget(@Nullable Target target) {
		final var oldTarget = this.depthTarget;
		if (Objects.equals(target, oldTarget))
			return;

		GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.id);

		if (target != null) {
			target.attach(GL32.GL_DEPTH_ATTACHMENT);
			this.depthTarget = target;
		} else {
			Target.detach(GL32.GL_DEPTH_ATTACHMENT);
			this.depthTarget = null;
		}
		if (oldTarget != null)
			oldTarget.close();
	}

	public void createDepthTarget(Texture.Format textureFormat) {
		final var texture = new Texture2d(false);
		texture.createStorage(textureFormat, this.size.x, this.size.y);
		final var target = new Target.Texture2dTarget(true, texture);
		setDepthTarget(target);
	}

	public @Nullable Target getDepthTarget() {
		return this.depthTarget;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
	}

	public void setOutputAttachments(int buf) {
		GlStateManager._glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, this.id);
		GL32.glDrawBuffers(buf);
	}

	public void setOutputAttachments(int... bufs) {
		final var remapped = new int[bufs.length];
		for (int i = 0; i < bufs.length; ++i)
			remapped[i] = GL32.GL_COLOR_ATTACHMENT0 + bufs[i];
		GlStateManager._glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, this.id);
		GL32.glDrawBuffers(remapped);
	}

	public void resolveTo(Framebuffer target) {
		GlStateManager._glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, this.id);
		GlStateManager._glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, target.id);
		GL32.glBlitFramebuffer(
				this.viewport.pos.x, this.viewport.pos.y, this.viewport.size.x, this.viewport.size.y,
				target.viewport.pos.x, target.viewport.pos.y, target.viewport.size.x, target.viewport.size.y,
				GL32.GL_COLOR_BUFFER_BIT, GL32.GL_NEAREST);
		GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, 0);
	}

	public void bind(boolean setViewport) {
		GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.id);
		if (setViewport) {
			RenderSystem.viewport(this.viewport.pos.x, this.viewport.pos.y,
					this.viewport.size.x, this.viewport.size.y);
		}
	}

	public static void unbind() {
		GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, 0);
	}

	public void checkStatus() {
		GlStateManager._glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.id);
		final var status = GlStateManager.glCheckFramebufferStatus(GL32.GL_FRAMEBUFFER);
		if (status != GL32.GL_FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException(
					"Framebuffer '" + this.id + "' was not complete: " + statusDescription(status));
		}
	}

	public static String statusDescription(int glId) {
		return switch (glId) {
			case GL32.GL_FRAMEBUFFER_COMPLETE -> "GL_FRAMEBUFFER_COMPLETE";
			case GL32.GL_FRAMEBUFFER_UNDEFINED -> "GL_FRAMEBUFFER_UNDEFINED";
			case GL32.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
			case GL32.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
			case GL32.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
			case GL32.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
			case GL32.GL_FRAMEBUFFER_UNSUPPORTED -> "GL_FRAMEBUFFER_UNSUPPORTED";
			case GL32.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
			case GL32.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS -> "GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS";
			default -> "<unknown>";
		};
	}

	public void setClearColor(Color color) {
		this.clearColor = color;
	}

	public void setClearDepth(double depth) {
		this.clearDepth = depth;
	}

	public void setClearMask(int clearMask) {
		this.clearMask = clearMask;
	}

	public void bindAndClear() {
		bind(true);
		// NOTE: this code assumes that the depth clear value will always be 1.0 when
		// this method is called, which is true for vanilla.
		if ((this.clearMask & GL32.GL_COLOR_BUFFER_BIT) != 0)
			RenderSystem.clearColor(this.clearColor.r(), this.clearColor.g(), this.clearColor.b(), this.clearColor.a());
		if ((this.clearMask & GL32.GL_DEPTH_BUFFER_BIT) != 0 && this.clearDepth != 1.0)
			RenderSystem.clearDepth(this.clearDepth);
		RenderSystem.clear(this.clearMask, false);
		if ((this.clearMask & GL32.GL_DEPTH_BUFFER_BIT) != 0 && this.clearDepth != 1.0)
			RenderSystem.clearDepth(1.0);
	}

}
