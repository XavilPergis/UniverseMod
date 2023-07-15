package net.xavil.hawklib.client.flexible;

import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.GlFramebuffer;
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

	public static final int TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT = 1000;
	public static int TEXTURE_RELEASE_THRESHOLD_FRAMES = 5;

	private static int nextRenderTextureId = 0;

	private static final MutableSet<RenderTexture> FREE_TEXTURES = MutableSet.identityHashSet();
	private static final MutableSet<RenderTexture> ALL_TEXTURES = MutableSet.identityHashSet();
	private static final Minecraft CLIENT = Minecraft.getInstance();

	public final GlFramebuffer framebuffer;
	public final GlTexture2d colorTexture;
	public final GlTexture2d depthTexture;
	public final StaticDescriptor descriptor;
	private int framesSinceLastUsed = 0;

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
		public final GlTexture.Format depthFormat;
		/**
		 * Whether or not the depth texture is readable. If this is {@code false}, then
		 * a faster internal representation (OpenGL renderbuffer) will be used instead
		 * of a texture.
		 */
		public final boolean isDepthReadable;

		public final GlTexture.MinFilter minFilterColor;
		public final GlTexture.MagFilter magFilterColor;
		public final GlTexture.WrapMode wrapModeColor;

		public final GlTexture.MinFilter minFilterDepth;
		public final GlTexture.MagFilter magFilterDepth;
		public final GlTexture.WrapMode wrapModeDepth;

		private StaticDescriptor(Builder builder) {
			this.colorFormat = builder.colorFormat;
			this.depthFormat = builder.depthFormat;
			this.isDepthReadable = builder.isDepthReadable;
			this.minFilterColor = builder.minFilterColor;
			this.magFilterColor = builder.magFilterColor;
			this.wrapModeColor = builder.wrapModeColor;
			this.minFilterDepth = builder.minFilterDepth;
			this.magFilterDepth = builder.magFilterDepth;
			this.wrapModeDepth = builder.wrapModeDepth;
		}

		public static StaticDescriptor create(Consumer<Builder> consumer) {
			return new Builder().with(consumer).build();
		}

		public static Builder builder() {
			return new Builder();
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
			public boolean isDepthReadable = false;
			public GlTexture.MinFilter minFilterColor = GlTexture.MinFilter.LINEAR;
			public GlTexture.MagFilter magFilterColor = GlTexture.MagFilter.LINEAR;
			public GlTexture.WrapMode wrapModeColor = WrapMode.CLAMP_TO_BORDER;
			public GlTexture.MinFilter minFilterDepth = GlTexture.MinFilter.NEAREST;
			public GlTexture.MagFilter magFilterDepth = GlTexture.MagFilter.NEAREST;
			public GlTexture.WrapMode wrapModeDepth = WrapMode.CLAMP_TO_EDGE;

			private Builder() {
			}

			public Builder with(Consumer<Builder> consumer) {
				consumer.accept(this);
				return this;
			}

			public StaticDescriptor build() {
				return new StaticDescriptor(this);
			}
		}
	}

	private RenderTexture(int id, Vec2i size, StaticDescriptor descriptor) {
		this.descriptor = descriptor;
		this.framebuffer = new GlFramebuffer(GlFragmentWrites.COLOR_ONLY, size);
		this.framebuffer.setDebugName("Temporary " + id);
		this.framebuffer.createColorTarget(GlFragmentWrites.COLOR, descriptor.colorFormat);

		// color target
		this.colorTexture = this.framebuffer.getColorTarget(GlFragmentWrites.COLOR).asTexture2d();
		this.colorTexture.setDebugName(String.format("Temporary %d '%s'", id, GlFragmentWrites.COLOR));
		this.colorTexture.setMinFilter(descriptor.minFilterColor);
		this.colorTexture.setMagFilter(descriptor.magFilterColor);
		this.colorTexture.setWrapMode(descriptor.wrapModeColor);

		// depth target
		if (descriptor.depthFormat != null) {
			this.framebuffer.createDepthTarget(descriptor.isDepthReadable, descriptor.depthFormat);
			this.depthTexture = this.framebuffer.getDepthAttachment().asTexture2d();
			if (this.depthTexture != null) {
				this.depthTexture.setMinFilter(descriptor.minFilterDepth);
				this.depthTexture.setMagFilter(descriptor.magFilterDepth);
				this.depthTexture.setWrapMode(descriptor.wrapModeDepth);
			}
		} else {
			this.depthTexture = null;
		}

		this.framebuffer.enableAllColorAttachments();
		this.framebuffer.checkStatus();
	}

	public static void tick() {
		if (TEXTURE_RELEASE_THRESHOLD_FRAMES > TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT) {
			HawkLib.LOGGER.warn("Temporary texture cleanup threshold was set to {}, which is higher than the limit of {}!",
					TEXTURE_RELEASE_THRESHOLD_FRAMES, TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT);
			TEXTURE_RELEASE_THRESHOLD_FRAMES = TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT;
		}
		final var toRemove = MutableSet.<RenderTexture>identityHashSet();
		for (final var texture : FREE_TEXTURES.iterable()) {
			texture.framesSinceLastUsed += 1;
			if (texture.framesSinceLastUsed > TEXTURE_RELEASE_THRESHOLD_FRAMES) {
				toRemove.insert(texture);
			}
		}
		toRemove.forEach(tex -> {
			HawkLib.LOGGER.info("Released old framebuffer {} with size {} after {} frames",
					tex.framebuffer.toString(), tex.framebuffer.size(), TEXTURE_RELEASE_THRESHOLD_FRAMES);
			tex.framebuffer.close();
			FREE_TEXTURES.remove(tex);
			ALL_TEXTURES.remove(tex);
		});
	}

	public static void releaseAll() {
		FREE_TEXTURES.clear();
		for (final var texture : ALL_TEXTURES.iterable()) {
			texture.framebuffer.close();
		}
		ALL_TEXTURES.clear();
	}

	private static boolean isCompatible(RenderTexture texture, Vec2i size, StaticDescriptor descriptor) {
		return texture.descriptor.equals(descriptor)
				&& size.x == texture.framebuffer.size().x
				&& size.y == texture.framebuffer.size().y;
	}

	public static RenderTexture getTemporary(Vec2i size, StaticDescriptor descriptor) {
		RenderTexture tex = null;
		for (final var texture : FREE_TEXTURES.iterable()) {
			if (isCompatible(texture, size, descriptor)) {
				tex = texture;
				break;
			}
		}
		if (tex == null) {
			HawkLib.LOGGER.info("Created new temporary texture with size {}", size);
			tex = new RenderTexture(nextRenderTextureId++, size, descriptor);
			ALL_TEXTURES.insert(tex);
		}
		FREE_TEXTURES.remove(tex);
		tex.framesSinceLastUsed = 0;
		return tex;
	}

	public static RenderTexture getTemporary(StaticDescriptor descriptor) {
		final var size = new Vec2i(CLIENT.getWindow().getWidth(), CLIENT.getWindow().getHeight());
		return getTemporary(size, descriptor);
	}

	public static RenderTexture getTemporaryCopy(GlTexture2d textureToCopy) {
		final var temp = getTemporary(textureToCopy.size().d2(), StaticDescriptor.create(builder -> {
			builder.colorFormat = textureToCopy.format();
		}));
		temp.framebuffer.bind();
		temp.framebuffer.clear();
		BufferRenderer.drawFullscreen(textureToCopy);
		return temp;
	}

	@Override
	public void close() {
		FREE_TEXTURES.insert(this);
	}

}
