package net.xavil.ultraviolet.client;

import static net.xavil.ultraviolet.client.Shaders.*;
import net.xavil.ultraviolet.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.client.flexible.RenderTexture;
import net.xavil.ultraviolet.client.gl.GlFramebuffer;
import net.xavil.ultraviolet.client.gl.texture.GlTexture;
import net.xavil.ultraviolet.client.gl.texture.GlTexture2d;
import net.xavil.util.Disposable;
import net.xavil.util.collections.Vector;

public final class BloomEffect {

	public record Settings(int passes, double intensity, double threshold, double softThreshold) {
	}

	public static final Settings DEFAULT_SETTINGS = new Settings(8, 0.1, 1.1, 0.9);

	public static void render(GlFramebuffer output, GlTexture2d input) {
		render(new Settings(8, 0.9, 1.5, 0.5), output, input);
	}

	private static void drawDownsample(GlFramebuffer output, GlTexture2d input, int level) {
		final var shader = getShader(SHADER_BLOOM_DOWNSAMPLE);
		output.bindAndClear();
		shader.setUniformSampler("uPreviousSampler", input);
		shader.setUniform("uSrcSize", input.size().d2());
		shader.setUniform("uDstSize", output.size());
		shader.setUniform("uLevel", level);
		BufferRenderer.drawFullscreen(shader);
	}

	private static void drawUpsample(GlFramebuffer output, GlTexture2d prev, GlTexture2d adj,
			int level) {
		final var shader = getShader(SHADER_BLOOM_UPSAMPLE);
		output.bindAndClear();

		shader.setUniformSampler("uPreviousSampler", prev);
		shader.setUniformSampler("uAdjacentSampler", adj);
		shader.setUniform("uSrcSize", prev.size().d2());
		shader.setUniform("uDstSize", output.size());
		shader.setUniform("uLevel", level);
		BufferRenderer.drawFullscreen(shader);
	}

	public final static GlTexture.Format BLOOM_FORMAT = GlTexture.Format.RGBA16_FLOAT;

	private final static RenderTexture.StaticDescriptor DESC = new RenderTexture.StaticDescriptor(
			GlTexture.Format.RGBA32_FLOAT);

	public static void render(Settings settings, GlFramebuffer output, GlTexture2d input) {
		if (settings.passes() == 0) {
			// we always want to write something to `output`, so that callers can assume
			// that the framebuffer is valid for use.
			if (!output.writesTo(input)) {
				output.bind(true);
				BufferRenderer.drawFullscreen(input);
			}
			return;
		}

		final var downsampleShader = getShader(SHADER_BLOOM_DOWNSAMPLE);
		final var upsampleShader = getShader(SHADER_BLOOM_UPSAMPLE);
		downsampleShader.setUniform("uQuality", 1);
		upsampleShader.setUniform("uQuality", 1);
		downsampleShader.setUniform("uTotalLevels", settings.passes());
		upsampleShader.setUniform("uTotalLevels", settings.passes());
		downsampleShader.setUniform("uIntensity", settings.intensity());
		downsampleShader.setUniform("uThreshold", settings.threshold());
		downsampleShader.setUniform("uSoftThreshold", settings.softThreshold());

		try (final var disposer = Disposable.scope()) {
			final var downsampleStack = new Vector<GlTexture2d>();

			var currentSize = input.size().d2();
			var previous = input;
			while (downsampleStack.size() < settings.passes()) {
				// start with half resolution!
				currentSize = currentSize.floorDiv(2);
				final var level = downsampleStack.size();
				final var target = disposer.attach(RenderTexture.getTemporary(currentSize, DESC));
				drawDownsample(target.framebuffer, previous, level);
				downsampleStack.push(target.colorTexture);
				previous = target.colorTexture;
			}

			previous = downsampleStack.pop().unwrap();
			while (!downsampleStack.isEmpty()) {
				final var level = downsampleStack.size();
				final var adj = downsampleStack.pop().unwrap();
				final var target = disposer.attach(RenderTexture.getTemporary(adj.size().d2(), DESC));
				drawUpsample(target.framebuffer, previous, adj, level);
				previous = target.colorTexture;
			}

			if (!output.writesTo(input)) {
				drawUpsample(output, previous, input, 0);
			} else {
				// idk a better way to do this that avoids the copy operation
				final var inputCopy = disposer.attach(RenderTexture.getTemporaryCopy(input));
				drawUpsample(output, previous, inputCopy.colorTexture, 0);
			}
		}
	}
}
