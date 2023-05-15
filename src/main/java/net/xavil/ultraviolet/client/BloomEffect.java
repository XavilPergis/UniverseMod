package net.xavil.ultraviolet.client;

import net.xavil.ultraviolet.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.client.flexible.TemporaryTexture;
import net.xavil.ultraviolet.client.flexible.Texture;
import net.xavil.ultraviolet.client.flexible.Texture2d;
import net.xavil.util.Disposable;
import net.xavil.util.collections.Vector;
import net.xavil.util.math.matrices.Vec2i;

public final class BloomEffect {

	public record Settings(int passes, double intensity, double threshold, double softThreshold) {
	}

	public static final Settings DEFAULT_SETTINGS = new Settings(8, 0.1, 1.1, 0.9);

	public static TemporaryTexture render(Texture2d input) {
		return render(new Settings(8, 0.9, 1.5, 0.5), input);
	}

	private static TemporaryTexture drawDownsample(int level, Vec2i size, Texture2d input) {
		final var shader = ModRendering.getShader(ModRendering.BLOOM_DOWNSAMPLE_SHADER);
		final var target = TemporaryTexture.getTemporary(Texture.Format.R16G16B16A16_FLOAT, size);
		target.framebuffer.bindAndClear();

		shader.setSampler("uPreviousSampler", input);
		BufferRenderer.setUniform(shader, "uSrcSize", input.size().d2());
		BufferRenderer.setUniform(shader, "uDstSize", target.texture.size().d2());
		BufferRenderer.setUniform(shader, "uLevel", level);
		BufferRenderer.drawFullscreen(shader);
		return target;
	}

	private static TemporaryTexture drawUpsample(int level, Texture2d prev, Texture2d adj) {
		final var shader = ModRendering.getShader(ModRendering.BLOOM_UPSAMPLE_SHADER);
		final var target = TemporaryTexture.getTemporary(Texture.Format.R16G16B16A16_FLOAT, adj.size().d2());
		target.framebuffer.bindAndClear();

		shader.setSampler("uPreviousSampler", prev);
		shader.setSampler("uAdjacentSampler", adj);
		BufferRenderer.setUniform(shader, "uSrcSize", prev.size().d2());
		BufferRenderer.setUniform(shader, "uDstSize", adj.size().d2());
		BufferRenderer.setUniform(shader, "uLevel", level);
		BufferRenderer.drawFullscreen(shader);
		return target;
	}

	public static TemporaryTexture render(Settings settings, Texture2d input) {
		final var downsampleShader = ModRendering.getShader(ModRendering.BLOOM_DOWNSAMPLE_SHADER);
		final var upsampleShader = ModRendering.getShader(ModRendering.BLOOM_UPSAMPLE_SHADER);
		BufferRenderer.setUniform(downsampleShader, "uQuality", 1);
		BufferRenderer.setUniform(upsampleShader, "uQuality", 1);
		BufferRenderer.setUniform(downsampleShader, "uTotalLevels", settings.passes());
		BufferRenderer.setUniform(upsampleShader, "uTotalLevels", settings.passes());
		BufferRenderer.setUniform(downsampleShader, "uIntensity", settings.intensity());
		BufferRenderer.setUniform(downsampleShader, "uThreshold", settings.threshold());
		BufferRenderer.setUniform(downsampleShader, "uSoftThreshold", settings.softThreshold());

		try (final var disposer = Disposable.scope()) {
			final var downsampleStack = new Vector<Texture2d>();

			var currentSize = input.size().d2();
			var previous = input;
			while (downsampleStack.size() < settings.passes()) {
				// start with half resolution!
				currentSize = currentSize.floorDiv(2);
				final var level = downsampleStack.size();
				final var target = disposer.attach(drawDownsample(level, currentSize, previous));
				downsampleStack.push(target.texture);
				previous = target.texture;
			}

			previous = downsampleStack.pop().unwrap();
			while (!downsampleStack.isEmpty()) {
				final var level = downsampleStack.size();
				final var adj = downsampleStack.pop().unwrap();
				final var target = disposer.attach(drawUpsample(level, previous, adj));
				previous = target.texture;
			}

			return drawUpsample(0, previous, input);
		}
	}
}
