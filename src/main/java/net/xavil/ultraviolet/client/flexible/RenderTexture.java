package net.xavil.ultraviolet.client.flexible;

import net.minecraft.client.Minecraft;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.gl.GlFragmentWrites;
import net.xavil.ultraviolet.client.gl.GlFramebuffer;
import net.xavil.ultraviolet.client.gl.texture.GlTexture;
import net.xavil.ultraviolet.client.gl.texture.GlTexture.WrapMode;
import net.xavil.ultraviolet.client.gl.texture.GlTexture2d;
import net.xavil.util.Disposable;
import net.xavil.util.collections.interfaces.MutableSet;
import net.xavil.util.math.matrices.Vec2i;

/**
 * A texture that may be rendered to. Only supports
 * {@link GlFragmentWrites#COLOR_ONLY} and an optional depth buffer.
 */
public final class RenderTexture implements Disposable {

	public static final int TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT = 1000;
	public static int TEXTURE_RELEASE_THRESHOLD_FRAMES = 30;

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

		public StaticDescriptor(GlTexture.Format colorFormat, boolean isDepthReadable, GlTexture.Format depthFormat) {
			this.colorFormat = colorFormat;
			this.depthFormat = depthFormat;
			this.isDepthReadable = isDepthReadable;
		}

		public StaticDescriptor(GlTexture.Format colorFormat) {
			this(colorFormat, false, null);
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
	}

	private RenderTexture(int id, Vec2i size, StaticDescriptor descriptor) {
		this.descriptor = descriptor;
		this.framebuffer = new GlFramebuffer(GlFragmentWrites.COLOR_ONLY, size);
		this.framebuffer.setDebugName("Temporary Framebuffer " + id);
		this.framebuffer.createColorTarget(GlFragmentWrites.COLOR, descriptor.colorFormat);
		this.colorTexture = this.framebuffer.getColorTarget(GlFragmentWrites.COLOR).asTexture2d();
		this.colorTexture.setDebugName(String.format("Temporary %d '%s'", id, GlFragmentWrites.COLOR));
		this.colorTexture.setMinFilter(GlTexture.MinFilter.LINEAR);
		this.colorTexture.setMagFilter(GlTexture.MagFilter.LINEAR);
		this.colorTexture.setWrapMode(WrapMode.CLAMP_TO_BORDER);
		if (descriptor.depthFormat != null) {
			this.framebuffer.createDepthTarget(descriptor.isDepthReadable, descriptor.depthFormat);
			this.depthTexture = this.framebuffer.getDepthAttachment().asTexture2d();
		} else {
			this.depthTexture = null;
		}
		this.framebuffer.checkStatus();
	}

	public static void tick() {
		if (TEXTURE_RELEASE_THRESHOLD_FRAMES > TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT) {
			Mod.LOGGER.warn("Temporary texture cleanup threshold was set to {}, which is higher than the limit of {}!",
					TEXTURE_RELEASE_THRESHOLD_FRAMES, TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT);
			TEXTURE_RELEASE_THRESHOLD_FRAMES = TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT;
		}
		final var toRemove = MutableSet.<RenderTexture>identityHashSet();
		for (final var texture : ALL_TEXTURES.iterable()) {
			texture.framesSinceLastUsed += 1;
			if (texture.framesSinceLastUsed > TEXTURE_RELEASE_THRESHOLD_FRAMES) {
				toRemove.insert(texture);
			}
		}
		toRemove.forEach(tex -> {
			Mod.LOGGER.info("Released old framebuffer {} with size {} after {} frames",
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
			Mod.LOGGER.info("Created new temporary texture with size {}", size);
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

	public static RenderTexture getTemporaryWithSameInfo(GlTexture2d textureToCopy) {
	return getTemporary(textureToCopy.size().d2(), new StaticDescriptor(textureToCopy.format()));
	}

	public static RenderTexture getTemporaryCopy(GlTexture2d textureToCopy) {
		final var temp = getTemporaryWithSameInfo(textureToCopy);
		temp.framebuffer.bindAndClear();
		BufferRenderer.drawFullscreen(textureToCopy);
		return temp;
	}

	@Override
	public void close() {
		FREE_TEXTURES.insert(this);
	}

}
