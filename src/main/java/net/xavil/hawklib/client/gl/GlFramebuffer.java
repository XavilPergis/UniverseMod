package net.xavil.hawklib.client.gl;

import java.util.Objects;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.matrices.Vec2i;

public final class GlFramebuffer extends GlObject {

	public record Viewport(Vec2i pos, Vec2i size) {
	}

	private Vec2i size = Vec2i.ZERO;
	private Viewport viewport;

	// NOTE: color attachment indices are the same as their draw buffer indices.

	private final GlFragmentWrites fragmentWrites;
	private boolean areDrawBuffersDirty = true;
	private final MutableSet<String> enabledFragmentWrites = MutableSet.hashSet();
	private final MutableMap<Integer, GlFramebufferAttachment> colorAttachments = MutableMap.hashMap();
	private GlFramebufferAttachment depthAttachment = null;

	private final RenderTarget importedTarget;

	private static GlFramebuffer MAIN_FRAMEBUFFER = null;

	public static final GlFramebuffer getMainFramebuffer() {
		final var target = Minecraft.getInstance().getMainRenderTarget();
		if (MAIN_FRAMEBUFFER != null && MAIN_FRAMEBUFFER.id == target.frameBufferId) {
			return MAIN_FRAMEBUFFER;
		}

		MAIN_FRAMEBUFFER = new GlFramebuffer(target);
		MAIN_FRAMEBUFFER.enableAllColorAttachments();

		return MAIN_FRAMEBUFFER;
	}

	public GlFramebuffer(RenderTarget imported) {
		super(ObjectType.FRAMEBUFFER, imported.frameBufferId, false);
		this.importedTarget = imported;
		this.fragmentWrites = GlFragmentWrites.COLOR_ONLY;
		this.size = new Vec2i(imported.width, imported.height);
		this.viewport = new Viewport(Vec2i.ZERO, this.size);
		// TODO: copy clear colors from imported target
		final var colorTex = GlTexture2d.importFromRenderTargetColor(imported);
		final var depthTex = GlTexture2d.importFromRenderTargetDepth(imported);
		if (colorTex != null)
			setColorTarget(GlFragmentWrites.COLOR, new GlFramebufferAttachment.Texture2d(false, colorTex));
		if (depthTex != null)
			setDepthAttachment(new GlFramebufferAttachment.Texture2d(false, depthTex));
	}

	public GlFramebuffer(GlFragmentWrites fragmentWrites, Vec2i size) {
		super(ObjectType.FRAMEBUFFER, GL45C.glCreateFramebuffers(), true);
		this.importedTarget = null;
		this.size = size;
		this.viewport = new Viewport(Vec2i.ZERO, size);
		this.fragmentWrites = fragmentWrites;
	}

	public static Vec2i currentWindowSize() {
		final var client = Minecraft.getInstance();
		return new Vec2i(client.getWindow().getWidth(), client.getWindow().getHeight());
	}

	public GlFramebuffer(GlFragmentWrites fragmentWrites) {
		this(fragmentWrites, currentWindowSize());
	}

	@Override
	public void close() {
		super.close();
		if (this.depthAttachment != null)
			this.depthAttachment.close();
		this.colorAttachments.values().forEach(GlFramebufferAttachment::close);
	}

	public void resize(Vec2i size) {
		this.size = size;
		this.viewport = new Viewport(Vec2i.ZERO, size);
		for (final var target : this.colorAttachments.values().iterable()) {
			if (!target.owned)
				continue;
			if (target instanceof GlFramebufferAttachment.Texture2d texTarget) {
				texTarget.target.resize(size.x, size.y);
			} else if (target instanceof GlFramebufferAttachment.Renderbuffer texTarget) {
				texTarget.target.resize(size.x, size.y);
			}
		}
		if (this.depthAttachment != null && this.depthAttachment.owned) {
			if (this.depthAttachment instanceof GlFramebufferAttachment.Texture2d texTarget) {
				texTarget.target.resize(size.x, size.y);
			} else if (this.depthAttachment instanceof GlFramebufferAttachment.Renderbuffer texTarget) {
				texTarget.target.resize(size.x, size.y);
			}
		}
	}

	private void updateSizeIfNeeded() {
		if (this.importedTarget == null)
			return;
		if (this.size.x == this.importedTarget.width && this.size.y == this.importedTarget.height)
			return;
		this.colorAttachments.values()
				.chain(Iterator.once(this.depthAttachment))
				.map(att -> att.asTexture2d())
				.filterNull()
				.forEach(tex -> tex.updateCachedSize(this.importedTarget.width, this.importedTarget.height));
		this.size = new Vec2i(this.importedTarget.width, this.importedTarget.height);
		this.viewport = new Viewport(Vec2i.ZERO, this.size);
	}

	public Vec2i size() {
		updateSizeIfNeeded();
		return this.size;
	}

	public void setColorTarget(String fragmentWriteId, @Nullable GlFramebufferAttachment target) {
		final var index = this.fragmentWrites.getFragmentWriteId(fragmentWriteId);
		final var oldTarget = this.colorAttachments.get(index).unwrapOrNull();
		if (Objects.equals(target, oldTarget))
			return;

		if (target != null) {
			if (this.owned)
				target.attach(this, GL45C.GL_COLOR_ATTACHMENT0 + index);
			this.colorAttachments.insertAndGet(index, target);
		} else {
			if (this.owned)
				GlFramebufferAttachment.detach(this, GL45C.GL_COLOR_ATTACHMENT0 + index);
			this.colorAttachments.removeAndGet(index);
		}
		if (oldTarget != null)
			oldTarget.close();
	}

	public void createColorTarget(String fragmentWriteId, GlTexture.Format textureFormat) {
		if (!textureFormat.isColorRenderable) {
			throw new IllegalArgumentException(String.format(
					"%s: '%s' is not a color-renderable format.",
					toString(), textureFormat));
		}
		final var texture = new GlTexture2d(false);
		updateSizeIfNeeded();
		texture.createStorage(textureFormat, this.size.x, this.size.y);
		final var target = new GlFramebufferAttachment.Texture2d(true, texture);
		setColorTarget(fragmentWriteId, target);
	}

	public GlFramebufferAttachment getColorTarget(String fragmentWriteId) {
		final var index = this.fragmentWrites.getFragmentWriteId(fragmentWriteId);
		return this.colorAttachments.get(index).unwrapOrNull();
	}

	public void setDepthAttachment(@Nullable GlFramebufferAttachment target) {
		final var oldTarget = this.depthAttachment;
		if (Objects.equals(target, oldTarget))
			return;

		if (target != null) {
			if (this.owned)
				target.attach(this, GL45C.GL_DEPTH_ATTACHMENT);
			this.depthAttachment = target;
		} else {
			if (this.owned)
				GlFramebufferAttachment.detach(this, GL45C.GL_DEPTH_ATTACHMENT);
			this.depthAttachment = null;
		}
		if (oldTarget != null)
			oldTarget.close();
	}

	public void createDepthTarget(boolean isDepthReadable, GlTexture.Format textureFormat) {
		if (!textureFormat.isDepthRenderable) {
			throw new IllegalArgumentException(String.format(
					"%s: '%s' is not a depth-renderable format.",
					toString(), textureFormat));
		}
		GlFramebufferAttachment target;
		if (isDepthReadable) {
			final var texture = new GlTexture2d(false);
			updateSizeIfNeeded();
			texture.createStorage(textureFormat, this.size.x, this.size.y);
			target = new GlFramebufferAttachment.Texture2d(true, texture);
		} else {
			final var texture = new GlRenderbuffer();
			updateSizeIfNeeded();
			texture.createStorage(textureFormat, this.size.x, this.size.y);
			target = new GlFramebufferAttachment.Renderbuffer(true, texture);
		}
		setDepthAttachment(target);
	}

	public GlFramebufferAttachment getDepthAttachment() {
		return this.depthAttachment;
	}

	public void setViewport(Viewport viewport) {
		this.viewport = viewport;
	}

	public void setColorAttachmentEnabled(String fragmentWriteId, boolean enable) {
		if (enable) {
			final var changed = this.enabledFragmentWrites.insert(fragmentWriteId);
			this.areDrawBuffersDirty |= changed;
		} else {
			final var changed = this.enabledFragmentWrites.remove(fragmentWriteId);
			this.areDrawBuffersDirty |= changed;
		}
	}

	public void enableAllColorAttachments() {
		final var count = this.fragmentWrites.getFragmentWriteCount();
		for (int i = 0; i < count; ++i) {
			final var name = this.fragmentWrites.getFragmentWriteName(i);
			setColorAttachmentEnabled(name, true);
		}
	}

	/**
	 * Copies the contents of this framebuffer into the target framebuffer. If this
	 * framebuffer is multisampled and the target is not, then this functions as a
	 * multisample "resolve" operation.
	 * 
	 * @param target The framebuffer to write into
	 */
	public void copyTo(GlFramebuffer target) {
		updateSizeIfNeeded();
		GL45C.glBlitNamedFramebuffer(
				this.id, target.id,
				this.viewport.pos.x, this.viewport.pos.y, this.viewport.size.x, this.viewport.size.y,
				target.viewport.pos.x, target.viewport.pos.y, target.viewport.size.x, target.viewport.size.y,
				GL45C.GL_COLOR_BUFFER_BIT, GL45C.GL_NEAREST);
	}

	public void checkStatus() {
		final var status = GL45C.glCheckNamedFramebufferStatus(this.id, GL45C.GL_FRAMEBUFFER);
		if (status != GL45C.GL_FRAMEBUFFER_COMPLETE) {
			HawkLib.LOGGER.error("{} was not complete (status {})", toString(), statusDescription(status));
			if (this.depthAttachment != null) {
				HawkLib.LOGGER.error("Depth attachment: {}", this.depthAttachment.toString());
			}
			for (int i = 0; i < this.fragmentWrites.getFragmentWriteCount(); ++i) {
				final var attachment = this.colorAttachments.get(i).unwrapOrNull();
				if (attachment == null)
					continue;
				HawkLib.LOGGER.error("Color attachment '{}': {}", this.fragmentWrites.getFragmentWriteName(i),
						this.colorAttachments.get(i).toString());
			}
			throw new IllegalStateException(String.format(
					"'%s' was not complete (status '%s')",
					toString(), statusDescription(status)));
		}
	}

	public static String statusDescription(int glId) {
		return switch (glId) {
			case GL45C.GL_FRAMEBUFFER_COMPLETE -> "GL_FRAMEBUFFER_COMPLETE";
			case GL45C.GL_FRAMEBUFFER_UNDEFINED -> "GL_FRAMEBUFFER_UNDEFINED";
			case GL45C.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
			case GL45C.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
			case GL45C.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
			case GL45C.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
			case GL45C.GL_FRAMEBUFFER_UNSUPPORTED -> "GL_FRAMEBUFFER_UNSUPPORTED";
			case GL45C.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
			case GL45C.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS -> "GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS";
			default -> "<unknown>";
		};
	}

	/**
	 * Binds this framebuffer for reading and writing, and sets the OpenGL viewport
	 * state to match this framebuffer's size. You are encouraged to call this
	 * before doing any clearing operations, but it is not required.
	 */
	public void bind() {
		updateSizeIfNeeded();
		GlManager.bindFramebuffer(GL45C.GL_FRAMEBUFFER, this.id);
		GlManager.setViewport(this.viewport.pos.x, this.viewport.pos.y,
				this.viewport.size.x, this.viewport.size.y);
		syncDrawBuffers();
		GlPerf.framebufferChanged();
	}

	public static void unbind() {
		GlManager.bindFramebuffer(GL45C.GL_FRAMEBUFFER, 0);
		GlPerf.framebufferChanged();
	}

	private void syncDrawBuffers() {
		if (!this.areDrawBuffersDirty || !this.owned)
			return;
		final var count = this.fragmentWrites.getFragmentWriteCount();
		final var drawBuffers = new int[count];
		for (int i = 0; i < count; ++i) {
			final var name = this.fragmentWrites.getFragmentWriteName(i);
			drawBuffers[i] = this.enabledFragmentWrites.contains(name) ? GL45C.GL_COLOR_ATTACHMENT0 + i : GL45C.GL_NONE;
		}
		GL45C.glNamedFramebufferDrawBuffers(this.id, drawBuffers);
		this.areDrawBuffersDirty = false;
	}

	private static Maybe<float[]> getFloatClearValue(ClearState untyped) {
		if (untyped instanceof ClearState.SetFloat floatState) {
			return Maybe.some(floatState.clearValue);
		} else if (untyped instanceof ClearState.SetInt) {
			throw new IllegalArgumentException();
		}
		return Maybe.none();
	}

	private static Maybe<int[]> getIntClearValue(ClearState untyped) {
		if (untyped instanceof ClearState.SetFloat) {
			throw new IllegalArgumentException();
		} else if (untyped instanceof ClearState.SetInt intState) {
			return Maybe.some(intState.clearValue);
		}
		return Maybe.none();
	}

	private void clearBuffer(GlFramebufferAttachment attachment, int colorAttachmentIndex) {
		final var format = attachment.format();
		if (format.isColorFormat && !format.isColorRenderable) {
			throw new IllegalArgumentException();
		} else if (format.isDepthFormat && !format.isDepthRenderable) {
			throw new IllegalArgumentException();
		} else if (format.isStencilFormat && !format.isStencilRenderable) {
			throw new IllegalArgumentException();
		}
		switch (format.samplerType) {
			case FLOAT -> getFloatClearValue(attachment.colorClearState)
					.ifSome(cv -> GL45C.glClearNamedFramebufferfv(this.id, GL45C.GL_COLOR, colorAttachmentIndex, cv));
			case INT -> getIntClearValue(attachment.colorClearState)
					.ifSome(cv -> GL45C.glClearNamedFramebufferiv(this.id, GL45C.GL_COLOR, colorAttachmentIndex, cv));
			case UINT -> getIntClearValue(attachment.colorClearState)
					.ifSome(cv -> GL45C.glClearNamedFramebufferiv(this.id, GL45C.GL_COLOR, colorAttachmentIndex, cv));
			case SHADOW -> {
				final var depth = getFloatClearValue(attachment.depthClearState).unwrapOrNull();
				final var stencil = getIntClearValue(attachment.stencilClearState).unwrapOrNull();
				if (depth != null && stencil != null && format.isDepthFormat && format.isStencilFormat) {
					GL45C.glClearNamedFramebufferfi(this.id, GL45C.GL_DEPTH_STENCIL, 0, depth[0], stencil[0]);
				} else if (depth != null && format.isDepthFormat) {
					GL45C.glClearNamedFramebufferfv(this.id, GL45C.GL_DEPTH, colorAttachmentIndex, depth);
				} else if (stencil != null && format.isStencilFormat) {
					GL45C.glClearNamedFramebufferiv(this.id, GL45C.GL_STENCIL, colorAttachmentIndex, stencil);
				}
			}
			case NONE -> {
			}
		}
	}

	/**
	 * This method clears the depth attachment, bypassing the
	 * {@link GlFramebufferAttachment#depthClearState} that is associated with the
	 * depth attachment.
	 * 
	 * @param clearValue The value to clear the depth attachment to
	 */
	public void clearDepthAttachment(float clearValue) {
		if (this.depthAttachment != null) {
			Assert.isTrue(this.depthAttachment.format().isDepthRenderable);
			GL45C.glClearNamedFramebufferfv(this.id, GL45C.GL_DEPTH, 0, new float[] { clearValue });
		}
	}

	/**
	 * Like {@link #clearDepthAttachment(float)}, but uses
	 * {@link GlFramebufferAttachment#depthClearState} as the clear value.
	 */
	public void clearDepthAttachment() {
		if (this.depthAttachment == null)
			return;
		final var depth = getFloatClearValue(this.depthAttachment.depthClearState).unwrapOrNull();
		if (depth != null)
			clearDepthAttachment(depth[0]);
	}

	/**
	 * This method clears the color attachment specified by {@code fragmentWriteId},
	 * bypassing the {@link GlFramebufferAttachment#colorClearState} that is
	 * associated with the color attachment. Note that this method may only be used
	 * on color buffers where {@link GlFramebufferAttachment#format()} has a sampler
	 * type of {@link GlTexture.SamplerType#FLOAT} (i.e., float buffers and
	 * normalized int buffers).
	 * 
	 * @param fragmentWriteId The color attachment to clear
	 * @param color           The value to clear the specified color attachment to
	 */
	public void clearColorAttachment(String fragmentWriteId, ColorRgba color) {
		final var index = this.fragmentWrites.getFragmentWriteId(fragmentWriteId);
		final var attachment = this.colorAttachments.get(index).unwrapOrNull();

		if (attachment != null) {
			Assert.isTrue(attachment.format().isColorRenderable);
			Assert.isEqual(attachment.format().samplerType, GlTexture.SamplerType.FLOAT);
			GL45C.glClearNamedFramebufferfv(this.id, GL45C.GL_COLOR, index, new float[] {
					color.r(), color.g(), color.b(), color.a(),
			});
		}
	}

	/**
	 * Like {@link #clearColorAttachment(String, ColorRgba)}, but uses
	 * {@link GlFramebufferAttachment#colorClearState} as the clear value.
	 */
	public void clearColorAttachment(String fragmentWriteId) {
		final var index = this.fragmentWrites.getFragmentWriteId(fragmentWriteId);
		final var attachment = this.colorAttachments.get(index).unwrapOrNull();

		if (attachment != null) {
			Assert.isTrue(attachment.format().isColorRenderable);
			Assert.isEqual(attachment.format().samplerType, GlTexture.SamplerType.FLOAT);
			final var clearValue = getFloatClearValue(this.depthAttachment.colorClearState).unwrapOrNull();
			if (clearValue != null)
				GL45C.glClearNamedFramebufferfv(this.id, GL45C.GL_COLOR, index, clearValue);
		}
	}

	/**
	 * This method clears all attachments to the clear value specified on each
	 * attaachment. For color attachments,
	 * {@link GlFramebufferAttachment#colorClearState} is used. For depth
	 * attachments, {@link GlFramebufferAttachment#depthClearState} is used.
	 */
	public void clear() {
		this.colorAttachments.entries().forEach(entry -> {
			clearBuffer(entry.get().unwrap(), entry.key);
		});
		if (this.depthAttachment != null) {
			clearBuffer(this.depthAttachment, 0);
		}
	}

	/**
	 * @param texture The object to check
	 * @return {@code true} if any of this framebuffer's attachments write to
	 *         {@code texture}
	 * @see GlFramebufferAttachment#writesTo(GlObject)
	 */
	public boolean writesTo(GlObject texture) {
		if (this.depthAttachment != null && this.depthAttachment.writesTo(texture)) {
			return true;
		}
		return this.colorAttachments.values().any(target -> target.writesTo(texture));
	}

}
