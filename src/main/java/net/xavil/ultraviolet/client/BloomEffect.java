package net.xavil.ultraviolet.client;

import javax.annotation.Nullable;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.GlFramebuffer;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.RenderTexture;

public final class BloomEffect {

	public static final class Settings {
		public final int maxPasses;
		public final int minResolution;

		public final double prefilterIntensity;
		public final double prefilterMaxBrightness;
		public final double blendIntensity;
		public final double threshold;
		public final double softThreshold;

		// dirt
		@Nullable
		public final GlTexture2d dirtTexture;
		public final double dirtIntensity;

		private Settings(Builder builder) {
			this.maxPasses = builder.maxPasses;
			this.minResolution = builder.minResolution;
			this.prefilterIntensity = builder.prefilterIntensity;
			this.prefilterMaxBrightness = builder.prefilterMaxBrightness;
			this.blendIntensity = builder.blendIntensity;
			this.threshold = builder.threshold;
			this.softThreshold = builder.softThreshold;
			this.dirtTexture = builder.dirtTexture;
			this.dirtIntensity = builder.dirtIntensity;
		}

		public static final class Builder {
			public int maxPasses = 8;
			public int minResolution = 4;

			public double prefilterIntensity = 1.0;
			public double prefilterMaxBrightness = 500.0;
			public double blendIntensity = 0.05;
			public double threshold = 1.0;
			public double softThreshold = 0.1;

			// dirt
			@Nullable
			public GlTexture2d dirtTexture = null;
			public double dirtIntensity = 1.0;

			public Settings build() {
				return new Settings(this);
			}
		}
	}

	public static void render(GlFramebuffer output, GlTexture2d input) {
		final var builder = new Settings.Builder();
		builder.dirtTexture = GlTexture2d.importTexture(RenderHelper.LENS_DIRT_LOCATION);
		render(builder.build(), output, input);
	}

	private static void drawDownsample(GlFramebuffer output, GlTexture2d input, int level) {
		final var shader = UltravioletShaders.SHADER_BLOOM_DOWNSAMPLE.get();
		output.bind();
		output.clear();
		input.setWrapMode(GlTexture.WrapMode.CLAMP_TO_EDGE);
		shader.setUniformSampler("uPreviousSampler", input);
		shader.setUniformi("uSrcSize", input.size().d2());
		shader.setUniformi("uDstSize", output.size());
		shader.setUniformi("uLevel", level);
		BufferRenderer.drawFullscreen(shader);
	}

	private static void drawUpsample(GlFramebuffer output, GlTexture2d prev, GlTexture2d adj,
			int level) {
		final var shader = UltravioletShaders.SHADER_BLOOM_UPSAMPLE.get();
		output.bind();
		output.clear();
		prev.setWrapMode(GlTexture.WrapMode.CLAMP_TO_EDGE);
		shader.setUniformSampler("uPreviousSampler", prev);
		shader.setUniformSampler("uAdjacentSampler", adj);
		shader.setUniformi("uSrcSize", prev.size().d2());
		shader.setUniformi("uDstSize", output.size());
		shader.setUniformi("uLevel", level);
		BufferRenderer.drawFullscreen(shader);
	}

	public static void render(Settings settings, GlFramebuffer output, GlTexture2d input) {
		if (settings.maxPasses == 0) {
			// we always want to write something to `output`, so that callers can assume
			// that the framebuffer is valid for use.
			if (!output.writesTo(input)) {
				output.bind();
				BufferRenderer.drawFullscreen(input);
			}
			return;
		}

		final var downsampleShader = UltravioletShaders.SHADER_BLOOM_DOWNSAMPLE.get();
		final var upsampleShader = UltravioletShaders.SHADER_BLOOM_UPSAMPLE.get();
		downsampleShader.setUniformi("uQuality", 1);
		upsampleShader.setUniformi("uQuality", 1);
		downsampleShader.setUniformi("uTotalLevels", settings.maxPasses);
		upsampleShader.setUniformi("uTotalLevels", settings.maxPasses);
		downsampleShader.setUniformf("uPrefilterIntensity", settings.prefilterIntensity);
		downsampleShader.setUniformf("uPrefilterMaxBrightness", settings.prefilterMaxBrightness);
		downsampleShader.setUniformf("uThreshold", settings.threshold);
		downsampleShader.setUniformf("uSoftThreshold", settings.softThreshold);
		upsampleShader.setUniformf("uBlendIntensity", settings.blendIntensity);

		if (settings.dirtTexture != null) {
			downsampleShader.setUniformi("uUseDirtTexture", 1);
			upsampleShader.setUniformi("uUseDirtTexture", 1);
			downsampleShader.setUniformSampler("uDirtTexture", settings.dirtTexture);
			upsampleShader.setUniformSampler("uDirtTexture", settings.dirtTexture);
			downsampleShader.setUniformf("uDirtIntensity", settings.dirtIntensity);
			upsampleShader.setUniformf("uDirtIntensity", settings.dirtIntensity);
		} else {
			downsampleShader.setUniformi("uUseDirtTexture", 0);
			upsampleShader.setUniformi("uUseDirtTexture", 0);
		}

		try (final var disposer = Disposable.scope()) {
			final var downsampleStack = new Vector<GlTexture2d>();

			var currentSize = input.size().d2();
			var previous = input;
			while (downsampleStack.size() < settings.maxPasses) {
				// start with half resolution!
				currentSize = currentSize.floorDiv(2);
				final var level = downsampleStack.size();
				if (currentSize.x <= settings.minResolution || currentSize.y <= settings.minResolution)
					break;
				final var target = disposer.attach(RenderTexture.HDR_COLOR.acquireTemporary(currentSize));
				drawDownsample(target.framebuffer, previous, level);
				downsampleStack.push(target.colorTexture);
				previous = target.colorTexture;
			}

			previous = downsampleStack.pop().unwrap();
			while (!downsampleStack.isEmpty()) {
				final var level = downsampleStack.size();
				final var adj = downsampleStack.pop().unwrap();
				final var target = disposer.attach(RenderTexture.HDR_COLOR.acquireTemporary(adj.size().d2()));
				drawUpsample(target.framebuffer, previous, adj, level);
				previous = target.colorTexture;
			}

			if (!output.writesTo(input)) {
				drawUpsample(output, previous, input, 0);
			} else {
				// idk a better way to do this that avoids the copy operation
				final var inputCopy = disposer.attach(RenderTexture.acquireTemporaryCopy(input));
				drawUpsample(output, previous, inputCopy.colorTexture, 0);
			}
		}
	}
}
