package net.xavil.universal.client;

import org.lwjgl.opengl.GL32;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.flexible.Framebuffer;
import net.xavil.universal.client.flexible.Texture;
import net.xavil.universal.client.flexible.Texture.WrapMode;
import net.xavil.universal.client.flexible.Texture2d;
import net.xavil.util.Disposable;
import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.ImmutableList;
import net.xavil.util.collections.interfaces.MutableList;
import net.xavil.util.math.matrices.Vec2i;

public final class BloomRenderer {
	private int levelCount = 5;
	private final MutableList<BloomTextureHolder> textures = new Vector<>();
	private BloomTextureHolder topLevel;

	public record BloomSettings(double intensity, double threshold, double softThreshold) {
	}

	public static final BloomSettings DEFAULT_SETTINGS = new BloomSettings(1.0, 1.0, 0.5);

	public static final class BloomTextureHolder implements Disposable {
		public final Framebuffer framebuffer;
		public final Texture2d texture;

		public BloomTextureHolder(int level, Vec2i size) {
			this.framebuffer = new Framebuffer(size);
			this.framebuffer.createColorTarget(0, GL32.GL_RGBA16F);
			this.framebuffer.setClearMask(GL32.GL_COLOR_BUFFER_BIT);
			this.framebuffer.setDebugName("Bloom FBO Level " + level);
			this.texture = this.framebuffer.getColorTarget(0).asTexture2d();
			this.texture.setMinFilter(Texture.MinFilter.LINEAR);
			this.texture.setMagFilter(Texture.MagFilter.LINEAR);
			this.texture.setWrapMode(WrapMode.CLAMP_TO_EDGE);
			this.texture.setDebugName("Bloom Level " + level);
			this.framebuffer.checkStatus();
		}

		public void resize(Vec2i size) {
			this.framebuffer.resize(size);
		}

		@Override
		public void close() {
			this.framebuffer.close();
		}
	}

	public BloomRenderer(Vec2i size) {
		resize(size);
	}

	public Texture2d getOutputTexture() {
		return this.topLevel.texture;
	}

	public ImmutableList<BloomTextureHolder> getHolders() {
		return this.textures;
	}

	public void resize(Vec2i size) {
		if (!this.textures.isEmpty()) {
			this.textures.forEach(holder -> holder.close());
			this.textures.clear();
		}
		for (int i = 0; i < 7; ++i) {
			final var levelSize = size.floorDiv(1 << i);
			this.textures.push(new BloomTextureHolder(i, levelSize));
		}
		this.topLevel = this.textures.get(0);
	}

	public void render(Texture2d input) {
		render(DEFAULT_SETTINGS, input);
	}

	public void render(BloomSettings settings, Texture2d input) {
		final var downsampleShader = ModRendering.getShader(ModRendering.BLOOM_DOWNSAMPLE_SHADER);
		final var upsampleShader = ModRendering.getShader(ModRendering.BLOOM_UPSAMPLE_SHADER);
		final var prefilterShader = ModRendering.getShader(ModRendering.BLOOM_PREFILTER_SHADER);

		final var root = this.topLevel;
		root.framebuffer.bindAndClear();
		prefilterShader.setSampler("uSampler", input);
		// BufferRenderer.setUniform(prefilterShader, "uIntensity",
		// settings.intensity());
		BufferRenderer.setUniform(prefilterShader, "uIntensity", 1.0);
		// BufferRenderer.setUniform(prefilterShader, "uThreshold",
		// settings.threshold());
		BufferRenderer.setUniform(prefilterShader, "uThreshold", 1.5);
		// BufferRenderer.setUniform(prefilterShader, "uSoftThreshold",
		// settings.softThreshold());
		BufferRenderer.setUniform(prefilterShader, "uSoftThreshold", 0.9);
		prefilterShader.apply();
		BufferRenderer.drawFullscreen(prefilterShader);

		RenderSystem.disableBlend();
		for (int i = 1; i < this.textures.size(); ++i) {
			// take the image from one level up and filter + downsample
			final var src = this.textures.get(i - 1);
			final var dst = this.textures.get(i);
			dst.framebuffer.bindAndClear();
			downsampleShader.setSampler("uPreviousSampler", src.texture);
			BufferRenderer.setUniform(downsampleShader, "uSrcSize", src.texture.size().d2());
			BufferRenderer.setUniform(downsampleShader, "uDstSize", dst.texture.size().d2());
			BufferRenderer.setUniform(downsampleShader, "uLevel", i - 1);
			downsampleShader.apply();
			BufferRenderer.drawFullscreen(downsampleShader);
		}

		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
		for (int i = this.textures.size() - 1; i > 0; --i) {
			// take the image from one level down, and the downsample image at the same
			// level, and combine and filter.
			final var src = this.textures.get(i);
			final var dst = this.textures.get(i - 1);
			dst.framebuffer.bind(true);
			// BufferRenderer.drawFullscreen(src.texture);
			upsampleShader.setSampler("uPreviousSampler", src.texture);
			BufferRenderer.setUniform(upsampleShader, "uSrcSize", src.texture.size().d2());
			BufferRenderer.setUniform(upsampleShader, "uDstSize", dst.texture.size().d2());
			BufferRenderer.setUniform(upsampleShader, "uLevel", i - 1);
			upsampleShader.apply();
			BufferRenderer.drawFullscreen(upsampleShader);
		}
	}
}
