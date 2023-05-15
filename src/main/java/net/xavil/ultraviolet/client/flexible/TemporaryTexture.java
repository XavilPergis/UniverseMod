package net.xavil.ultraviolet.client.flexible;

import org.lwjgl.opengl.GL32;

import net.minecraft.client.Minecraft;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.flexible.Texture.WrapMode;
import net.xavil.util.Disposable;
import net.xavil.util.collections.interfaces.MutableSet;
import net.xavil.util.math.Color;
import net.xavil.util.math.matrices.Vec2i;

public final class TemporaryTexture implements Disposable {

	public static final int TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT = 1000;
	public static int TEXTURE_RELEASE_THRESHOLD_FRAMES = 30;

	private static final MutableSet<TemporaryTexture> FREE_TEXTURES = MutableSet.identityHashSet();
	private static final MutableSet<TemporaryTexture> ALL_TEXTURES = MutableSet.identityHashSet();
	private static final Minecraft CLIENT = Minecraft.getInstance();

	public final Texture.Format format;
	public final Framebuffer framebuffer;
	public final Texture2d texture;
	private int framesSinceLastUsed = 0;

	private TemporaryTexture(int id, Vec2i size, Texture.Format format) {
		this.format = format;
		this.framebuffer = new Framebuffer(size);
		this.framebuffer.createColorTarget(0, format);
		this.framebuffer.setClearMask(GL32.GL_COLOR_BUFFER_BIT);
		this.framebuffer.setDebugName("Temporary Framebuffer " + id);
		this.texture = this.framebuffer.getColorTarget(0).asTexture2d();
		this.texture.setMinFilter(Texture.MinFilter.LINEAR);
		this.texture.setMagFilter(Texture.MagFilter.LINEAR);
		this.texture.setWrapMode(WrapMode.CLAMP_TO_EDGE);
		this.texture.setDebugName("Temporary " + id);
		this.framebuffer.checkStatus();
	}

	public static void tick() {
		if (TEXTURE_RELEASE_THRESHOLD_FRAMES > TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT) {
			Mod.LOGGER.warn("Temporary texture cleanup threshold was set to {}, which is higher than the limit of {}!",
					TEXTURE_RELEASE_THRESHOLD_FRAMES, TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT);
			TEXTURE_RELEASE_THRESHOLD_FRAMES = TEXTURE_RELEASE_THRESHOLD_FRAMES_LIMIT;
		}
		final var toRemove = MutableSet.<TemporaryTexture>identityHashSet();
		for (final var texture : ALL_TEXTURES.iterable()) {
			texture.framesSinceLastUsed += 1;
			if (texture.framesSinceLastUsed > TEXTURE_RELEASE_THRESHOLD_FRAMES) {
				toRemove.insert(texture);
			}
		}
		toRemove.forEach(tex -> {
			Mod.LOGGER.info("Released old temporary texture with size {} and format {} after {} frames",
					tex.texture.size.d2(), tex.format.description, TEXTURE_RELEASE_THRESHOLD_FRAMES);
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

	private static boolean isCompatible(TemporaryTexture texture, Texture.Format format, Vec2i size) {
		return texture.format == format
				&& size.x == texture.texture.size().width
				&& size.y == texture.texture.size().height;
	}

	public static TemporaryTexture getTemporary(Texture.Format format, Vec2i size) {
		TemporaryTexture tex = null;
		for (final var texture : FREE_TEXTURES.iterable()) {
			if (isCompatible(texture, format, size)) {
				tex = texture;
				break;
			}
		}
		if (tex == null) {
			Mod.LOGGER.info("Created new temporary texture with size {} and format {}", size, format.description);
			tex = new TemporaryTexture(ALL_TEXTURES.size(), size, format);
			ALL_TEXTURES.insert(tex);
		}
		FREE_TEXTURES.remove(tex);
		tex.framesSinceLastUsed = 0;
		tex.framebuffer.setClearColor(Color.TRANSPARENT);
		tex.framebuffer.bindAndClear();
		return tex;
	}

	public static TemporaryTexture getTemporary(Texture.Format format) {
		final var size = new Vec2i(CLIENT.getWindow().getWidth(), CLIENT.getWindow().getHeight());
		return getTemporary(format, size);
	}

	@Override
	public void close() {
		FREE_TEXTURES.insert(this);
	}

}
