package net.xavil.hawklib.client.flexible;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.GlFramebuffer;
import net.xavil.hawklib.client.gl.GlFramebufferAttachment;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.client.gl.texture.GlTexture.WrapMode;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.math.matrices.Vec2i;

/**
 * A texture that may be rendered to. Only supports
 * {@link GlFragmentWrites#COLOR_ONLY} and an optional depth buffer.
 */
public final class RenderTexture implements Disposable {

	private static final Minecraft CLIENT = Minecraft.getInstance();
	private static final TexturePool POOL = new TexturePool();

	public static final StaticDescriptor HDR_COLOR_DEPTH = StaticDescriptor.builder()
			.withColorFormat(GlTexture.Format.RGBA16_FLOAT)
			.withDepthFormat(GlTexture.Format.DEPTH24_UINT_NORM, true)
			.build();
	public static final StaticDescriptor HDR_COLOR = StaticDescriptor.builder()
			.withColorFormat(GlTexture.Format.RGBA16_FLOAT)
			.build();
	public static final StaticDescriptor SDR_COLOR = StaticDescriptor.builder()
			.withColorFormat(GlTexture.Format.RGBA8_UINT_NORM)
			.build();

	public final TexturePool pool;
	public final StaticDescriptor descriptor;
	private int framesSinceLastUsed = 0;
	public final GlFramebuffer framebuffer;
	public final GlTexture2d colorTexture;
	public final GlTexture2d depthTexture;

	/**
	 * Information needed to create a {@link RenderTexture} that is likely not to
	 * change over the lifetime of an application. This notably does not include the
	 * size of the render texture.
	 */
	public static final class StaticDescriptor {
		public final GlTexture.Format colorFormat;
		/**
		 * The format of the depth texture. If this is null, no depth image will be
		 * allocated. If this is not null, then a depth image will be created, and depth
		 * testing will be available when rendering to render textures created with this
		 * descriptor.
		 */
		@Nullable
		public final GlTexture.Format depthFormat;
		/**
		 * Whether or not the depth texture is readable. If this is {@code false}, then
		 * a faster internal representation (OpenGL renderbuffer) will be used instead
		 * of a texture.
		 */
		public final boolean isDepthReadable;

		private StaticDescriptor(Builder builder) {
			this.colorFormat = builder.colorFormat;
			this.depthFormat = builder.depthFormat;
			this.isDepthReadable = builder.isDepthReadable;
		}

		public static Builder builder() {
			return new Builder();
		}

		public RenderTexture acquireTemporary() {
			return RenderTexture.POOL.acquire(this);
		}

		public RenderTexture acquireTemporary(Vec2i size) {
			return RenderTexture.POOL.acquire(size, this);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof StaticDescriptor other) {
				return this.colorFormat == other.colorFormat
						&& this.depthFormat == other.depthFormat
						&& this.isDepthReadable == other.isDepthReadable;
			}
			return false;
		}

		public static final class Builder {
			public GlTexture.Format colorFormat = GlTexture.Format.RGBA8_UINT_NORM;
			public GlTexture.Format depthFormat = null;
			public boolean isDepthReadable = true;

			private Builder() {
			}

			public Builder with(Consumer<Builder> consumer) {
				consumer.accept(this);
				return this;
			}

			public Builder withColorFormat(GlTexture.Format colorFormat) {
				this.colorFormat = colorFormat;
				return this;
			}

			public Builder withDepthFormat(GlTexture.Format depthFormat, boolean isDepthReadable) {
				this.depthFormat = depthFormat;
				this.isDepthReadable = isDepthReadable;
				return this;
			}

			public StaticDescriptor build() {
				return new StaticDescriptor(this);
			}
		}
	}

	public RenderTexture(GlFramebuffer imported) {
		this.pool = null;
		this.framebuffer = imported;

		this.colorTexture = this.framebuffer.getColorTarget(GlFragmentWrites.COLOR).asTexture2d();
		if (this.colorTexture == null)
			throw new IllegalArgumentException(String.format(
					"Tried to import {} as RenderTexture, but it has no main color buffer!",
					imported.debugDescription()));

		final var desc = StaticDescriptor.builder().withColorFormat(this.colorTexture.format());

		final var depth = imported.getDepthAttachment();
		if (depth != null) {
			this.depthTexture = depth.asTexture2d();
			final var isDepthReadable = !(depth instanceof GlFramebufferAttachment.Renderbuffer);
			desc.withDepthFormat(depth.format(), isDepthReadable);
		} else {
			this.depthTexture = null;
		}

		this.descriptor = desc.build();
	}

	private RenderTexture(TexturePool pool, Vec2i size, StaticDescriptor descriptor) {
		this.pool = pool;
		final var id = pool.nextRenderTextureId++;
		this.descriptor = descriptor;
		this.framebuffer = new GlFramebuffer(GlFragmentWrites.COLOR_ONLY, size);
		this.framebuffer.setDebugName("Temporary RenderTexture " + id);
		this.framebuffer.createColorTarget(GlFragmentWrites.COLOR, descriptor.colorFormat);

		// color target
		this.colorTexture = this.framebuffer.getColorTarget(GlFragmentWrites.COLOR).asTexture2d();
		this.colorTexture.setDebugName(String.format("Temporary RenderTexture %d '%s'", id, GlFragmentWrites.COLOR));
		this.colorTexture.setMinFilter(GlTexture.MinFilter.LINEAR);
		this.colorTexture.setMagFilter(GlTexture.MagFilter.LINEAR);
		this.colorTexture.setWrapMode(WrapMode.CLAMP_TO_BORDER);

		// depth target
		if (descriptor.depthFormat != null) {
			this.framebuffer.createDepthTarget(descriptor.isDepthReadable, descriptor.depthFormat);
			this.depthTexture = this.framebuffer.getDepthAttachment().asTexture2d();
			this.framebuffer.getDepthAttachment().asGlObject()
					.setDebugName("Temporary RenderTexture " + id + " Depth");
			if (this.depthTexture != null) {
				this.depthTexture.setMinFilter(GlTexture.MinFilter.NEAREST);
				this.depthTexture.setMagFilter(GlTexture.MagFilter.NEAREST);
				this.depthTexture.setWrapMode(WrapMode.CLAMP_TO_EDGE);
			}
		} else {
			this.depthTexture = null;
		}

		this.framebuffer.enableAllColorAttachments();
		this.framebuffer.checkStatus();
	}

	public static void tick() {
		POOL.tick();
	}

	public static void releaseAllTextures() {
		POOL.close();
	}

	private static boolean isCompatible(RenderTexture texture, Vec2i size, StaticDescriptor descriptor) {
		return texture.descriptor.equals(descriptor)
				&& size.x == texture.framebuffer.size().x
				&& size.y == texture.framebuffer.size().y;
	}

	public static RenderTexture acquireTemporaryCopy(GlTexture2d textureToCopy) {
		final var desc = StaticDescriptor.builder().withColorFormat(textureToCopy.format()).build();
		final var temp = POOL.acquire(textureToCopy.size().d2(), desc);
		temp.framebuffer.bind();
		temp.framebuffer.clear();
		BufferRenderer.drawFullscreen(textureToCopy);
		return temp;
	}

	public double aspectRatio() {
		final var size = this.framebuffer.size();
		return (double) size.x / (double) size.y;
	}

	@Override
	public void close() {
		if (this.pool != null)
			this.pool.freeTextures.insert(this);
	}

	public static final class TexturePool implements Disposable {
		public static final int TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT = 1000;
		private static int nextPoolId = 0;

		public final int id;
		public int textureReleaseThresholdFrames = 1;

		private int nextRenderTextureId = 0;
		private final MutableSet<RenderTexture> freeTextures = MutableSet.identityHashSet();
		private final MutableSet<RenderTexture> allTextures = MutableSet.identityHashSet();

		public TexturePool() {
			this.id = nextPoolId++;
		}

		@Override
		public void close() {
			this.freeTextures.clear();
			for (final var texture : this.allTextures.iterable()) {
				texture.framebuffer.close();
			}
			this.allTextures.clear();
		}

		public void tick() {
			if (this.textureReleaseThresholdFrames > TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT) {
				HawkLib.LOGGER.warn(
						"Temporary texture cleanup threshold was set to {}, which is higher than the limit of {}!",
						this.textureReleaseThresholdFrames, TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT);
				this.textureReleaseThresholdFrames = TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT;
			}
			final var toRemove = MutableSet.<RenderTexture>identityHashSet();
			for (final var texture : this.freeTextures.iterable()) {
				texture.framesSinceLastUsed += 1;
				if (texture.framesSinceLastUsed > this.textureReleaseThresholdFrames) {
					toRemove.insert(texture);
				}
			}
			toRemove.forEach(tex -> {
				HawkLib.LOGGER.info("Released old RenderTexture {} in pool {} with size {} after {} frames",
						tex.framebuffer.toString(), this.id, tex.framebuffer.size(), tex.framesSinceLastUsed - 1);
				tex.framebuffer.close();
				this.freeTextures.remove(tex);
				this.allTextures.remove(tex);
			});
		}

		public void release(RenderTexture texture) {
			this.freeTextures.insert(texture);
		}

		public RenderTexture acquire(Vec2i size, StaticDescriptor descriptor) {
			RenderTexture tex = this.freeTextures.iter()
					.findOrNull(texture -> isCompatible(texture, size, descriptor));
			if (tex == null) {
				HawkLib.LOGGER.info("Created new RenderTexture in pool {} with size {}", size, this.id);
				tex = new RenderTexture(this, size, descriptor);
				this.allTextures.insert(tex);
			}
			this.freeTextures.remove(tex);
			tex.framesSinceLastUsed = 0;
			return tex;
		}

		public RenderTexture acquire(StaticDescriptor descriptor) {
			final var size = new Vec2i(CLIENT.getWindow().getWidth(), CLIENT.getWindow().getHeight());
			return acquire(size, descriptor);
		}
	}

}
