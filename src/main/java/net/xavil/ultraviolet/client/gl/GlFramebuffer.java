package net.xavil.ultraviolet.client.gl;

import java.util.Objects;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL32C;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.gl.texture.GlTexture;
import net.xavil.ultraviolet.client.gl.texture.GlTexture2d;
import net.xavil.util.Assert;
import net.xavil.util.Option;
import net.xavil.util.collections.interfaces.MutableMap;
import net.xavil.util.collections.interfaces.MutableSet;
import net.xavil.util.math.Color;
import net.xavil.util.math.matrices.Vec2i;

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

	public GlFramebuffer(RenderTarget imported) {
		super(imported.frameBufferId, false);
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
		super(GlManager.createFramebuffer(), true);
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
	public ObjectType objectType() {
		return ObjectType.FRAMEBUFFER;
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

	public Vec2i size() {
		return this.size;
	}

	public void setColorTarget(String fragmentWriteId, @Nullable GlFramebufferAttachment target) {
		final var index = this.fragmentWrites.getFragmentWriteId(fragmentWriteId);
		final var oldTarget = this.colorAttachments.get(index).unwrapOrNull();
		if (Objects.equals(target, oldTarget))
			return;

		GlManager.bindFramebuffer(GL32C.GL_FRAMEBUFFER, this.id);

		if (target != null) {
			if (this.owned)
				target.attach(GL32C.GL_COLOR_ATTACHMENT0 + index);
			this.colorAttachments.insert(index, target);
		} else {
			if (this.owned)
				GlFramebufferAttachment.detach(GL32C.GL_COLOR_ATTACHMENT0 + index);
			this.colorAttachments.remove(index);
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

		GlManager.bindFramebuffer(GL32C.GL_FRAMEBUFFER, this.id);

		if (target != null) {
			if (this.owned)
				target.attach(GL32C.GL_DEPTH_ATTACHMENT);
			this.depthAttachment = target;
		} else {
			if (this.owned)
				GlFramebufferAttachment.detach(GL32C.GL_DEPTH_ATTACHMENT);
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
			texture.createStorage(textureFormat, this.size.x, this.size.y);
			target = new GlFramebufferAttachment.Texture2d(true, texture);
		} else {
			final var texture = new GlRenderbuffer();
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
		GlManager.bindFramebuffer(GL32C.GL_READ_FRAMEBUFFER, this.id);
		GlManager.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, target.id);
		GL32C.glBlitFramebuffer(
				this.viewport.pos.x, this.viewport.pos.y, this.viewport.size.x, this.viewport.size.y,
				target.viewport.pos.x, target.viewport.pos.y, target.viewport.size.x, target.viewport.size.y,
				GL32C.GL_COLOR_BUFFER_BIT, GL32C.GL_NEAREST);
		GlManager.bindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
	}

	public void checkStatus() {
		GlManager.bindFramebuffer(GL32C.GL_FRAMEBUFFER, this.id);
		final var status = GlStateManager.glCheckFramebufferStatus(GL32C.GL_FRAMEBUFFER);
		if (status != GL32C.GL_FRAMEBUFFER_COMPLETE) {
			Mod.LOGGER.error("{} was not complete (status {})", toString(), statusDescription(status));
			if (this.depthAttachment != null) {
				Mod.LOGGER.error("Depth attachment: {}", this.depthAttachment.toString());
			}
			for (int i = 0; i < this.fragmentWrites.getFragmentWriteCount(); ++i) {
				final var attachment = this.colorAttachments.get(i).unwrapOrNull();
				if (attachment == null)
					continue;
				Mod.LOGGER.error("Color attachment '{}': {}", this.fragmentWrites.getFragmentWriteName(i),
						this.colorAttachments.get(i).toString());
			}
			throw new IllegalStateException(String.format(
					"'%s' was not complete (status '%s')",
					toString(), statusDescription(status)));
		}
	}

	public static String statusDescription(int glId) {
		return switch (glId) {
			case GL32C.GL_FRAMEBUFFER_COMPLETE -> "GL_FRAMEBUFFER_COMPLETE";
			case GL32C.GL_FRAMEBUFFER_UNDEFINED -> "GL_FRAMEBUFFER_UNDEFINED";
			case GL32C.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
			case GL32C.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT -> "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
			case GL32C.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER";
			case GL32C.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER -> "GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER";
			case GL32C.GL_FRAMEBUFFER_UNSUPPORTED -> "GL_FRAMEBUFFER_UNSUPPORTED";
			case GL32C.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE -> "GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE";
			case GL32C.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS -> "GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS";
			default -> "<unknown>";
		};
	}

	/**
	 * Binds this framebuffer for reading and writing, and sets the OpenGL viewport
	 * state to match this framebuffer's size. You are encouraged to call this
	 * before doing any clearing operations, but it is not required.
	 */
	public void bind() {
		GlManager.bindFramebuffer(GL32C.GL_FRAMEBUFFER, this.id);
		GlManager.setViewport(this.viewport.pos.x, this.viewport.pos.y,
				this.viewport.size.x, this.viewport.size.y);
		syncDrawBuffers();
	}

	public static void unbind() {
		GlManager.bindFramebuffer(GL32C.GL_FRAMEBUFFER, 0);
	}

	private void syncDrawBuffers() {
		if (!this.areDrawBuffersDirty || !this.owned)
			return;
		final var count = this.fragmentWrites.getFragmentWriteCount();
		final var drawBuffers = new int[count];
		for (int i = 0; i < count; ++i) {
			final var name = this.fragmentWrites.getFragmentWriteName(i);
			drawBuffers[i] = this.enabledFragmentWrites.contains(name) ? GL32C.GL_COLOR_ATTACHMENT0 + i : GL32C.GL_NONE;
		}
		GlManager.drawBuffers(drawBuffers);
		this.areDrawBuffersDirty = false;
	}

	private static Option<float[]> getFloatClearValue(ClearState untyped) {
		if (untyped instanceof ClearState.SetFloat floatState) {
			return Option.some(floatState.clearValue);
		} else if (untyped instanceof ClearState.SetInt intState) {
			throw new IllegalArgumentException();
		}
		return Option.none();
	}

	private static Option<int[]> getIntClearValue(ClearState untyped) {
		if (untyped instanceof ClearState.SetFloat floatState) {
			throw new IllegalArgumentException();
		} else if (untyped instanceof ClearState.SetInt intState) {
			return Option.some(intState.clearValue);
		}
		return Option.none();
	}

	private static void clearBuffer(GlFramebufferAttachment attachment, int colorAttachmentIndex) {
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
					.ifSome(cv -> GL32C.glClearBufferfv(GL32C.GL_COLOR, colorAttachmentIndex, cv));
			case INT -> getIntClearValue(attachment.colorClearState)
					.ifSome(cv -> GL32C.glClearBufferiv(GL32C.GL_COLOR, colorAttachmentIndex, cv));
			case UINT -> getIntClearValue(attachment.colorClearState)
					.ifSome(cv -> GL32C.glClearBufferiv(GL32C.GL_COLOR, colorAttachmentIndex, cv));
			case SHADOW -> {
				final var depth = getFloatClearValue(attachment.depthClearState).unwrapOrNull();
				final var stencil = getIntClearValue(attachment.stencilClearState).unwrapOrNull();
				if (depth != null && stencil != null && format.isDepthFormat && format.isStencilFormat) {
					GL32C.glClearBufferfi(GL32C.GL_DEPTH_STENCIL, 0, depth[0], stencil[0]);
				} else if (depth != null && format.isDepthFormat) {
					GL32C.glClearBufferfv(GL32C.GL_DEPTH, colorAttachmentIndex, depth);
				} else if (stencil != null && format.isStencilFormat) {
					GL32C.glClearBufferiv(GL32C.GL_STENCIL, colorAttachmentIndex, stencil);
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
		final var prevFramebuffer = GlManager.currentState().boundDrawFramebuffer;
		GlManager.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, this.id);
		if (this.depthAttachment != null) {
			Assert.isTrue(this.depthAttachment.format().isDepthRenderable);
			GL32C.glClearBufferfv(GL32C.GL_DEPTH, 0, new float[] { clearValue });
		}
		GlManager.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, prevFramebuffer);
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
	public void clearColorAttachment(String fragmentWriteId, Color color) {
		final var index = this.fragmentWrites.getFragmentWriteId(fragmentWriteId);
		final var attachment = this.colorAttachments.get(index).unwrapOrNull();

		final var prevFramebuffer = GlManager.currentState().boundDrawFramebuffer;
		GlManager.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, this.id);
		if (attachment != null) {
			Assert.isTrue(attachment.format().isColorRenderable);
			Assert.isEqual(attachment.format().samplerType, GlTexture.SamplerType.FLOAT);
			GL32C.glClearBufferfv(GL32C.GL_COLOR, index, new float[] {
					color.r(), color.g(), color.b(), color.a(),
			});
		}
		GlManager.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, prevFramebuffer);
	}

	/**
	 * Like {@link #clearColorAttachment(String, Color)}, but uses
	 * {@link GlFramebufferAttachment#colorClearState} as the clear value.
	 */
	public void clearColorAttachment(String fragmentWriteId) {
		final var index = this.fragmentWrites.getFragmentWriteId(fragmentWriteId);
		final var attachment = this.colorAttachments.get(index).unwrapOrNull();

		final var prevFramebuffer = GlManager.currentState().boundDrawFramebuffer;
		GlManager.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, this.id);
		if (attachment != null) {
			Assert.isTrue(attachment.format().isColorRenderable);
			Assert.isEqual(attachment.format().samplerType, GlTexture.SamplerType.FLOAT);
			final var clearValue = getFloatClearValue(this.depthAttachment.colorClearState).unwrapOrNull();
			if (clearValue != null)
				GL32C.glClearBufferfv(GL32C.GL_COLOR, index, clearValue);
		}
		GlManager.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, prevFramebuffer);
	}


	/**
	 * This method clears all attachments to the clear value specified on each
	 * attaachment. For color attachments,
	 * {@link GlFramebufferAttachment#colorClearState} is used. For depth
	 * attachments, {@link GlFramebufferAttachment#depthClearState} is used.
	 */
	public void clear() {
		final var prevFramebuffer = GlManager.currentState().boundDrawFramebuffer;
		GlManager.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, this.id);
		try {
			this.colorAttachments.entries().forEach(entry -> {
				clearBuffer(entry.get().unwrap(), entry.key);
			});
			if (this.depthAttachment != null) {
				clearBuffer(this.depthAttachment, 0);
			}
		} finally {
			GlManager.bindFramebuffer(GL32C.GL_DRAW_FRAMEBUFFER, prevFramebuffer);
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
