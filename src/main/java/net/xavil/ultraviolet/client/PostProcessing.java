package net.xavil.ultraviolet.client;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.RenderTexture;
import net.xavil.hawklib.client.gl.GlFramebuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.ultraviolet.common.config.ClientConfig;
import net.xavil.ultraviolet.common.config.ConfigKey;
import net.xavil.hawklib.client.gl.GlObject;

public final class PostProcessing {

	public static final Event<PostProcessCallback> POST_PROCESS_WORLD_HDR = EventFactory
			.createArrayBacked(PostProcessCallback.class, callbacks -> (output, input) -> {
				runAllPostProcessing(callbacks, output, input);
			});

	public static final Event<PostProcessCallback> POST_PROCESS_WORLD_SDR = EventFactory
			.createArrayBacked(PostProcessCallback.class, callbacks -> (output, input) -> {
				runAllPostProcessing(callbacks, output, input);
			});

	private static void runAllPostProcessing(PostProcessCallback[] passes, RenderTexture output, GlTexture2d input) {
		// input and output are the same, and we have no passes to apply. this is no
		// work to do.
		if (passes.length == 0 && output.framebuffer.writesTo(input))
			return;

		// input and output are distinct, but there's no passes to apply. just perform a
		// straight copy. This could be done via a blit if `input` was a framebuffer as
		// well, which might be faster.
		if (passes.length == 0) {
			output.framebuffer.bind();
			BufferRenderer.drawFullscreen(input);
		}

		// one pass and distinct inputs and outputs - this means we can avoid allocating
		// a temporary render target.
		if (passes.length == 1 && !output.framebuffer.writesTo(input)) {
			passes[0].process(output, input);
		}

		// process each pass in pairs, so that we end up with the current image back in
		// `output` by the end of the loop.
		GlTexture2d currentInput = input;
		int i = 0;
		for (; i <= passes.length - 2; i += 2) {
			try (final var temp = RenderTexture.HDR_COLOR.acquireTemporary()) {
				passes[i].process(temp, currentInput);
				passes[i + 1].process(output, temp.colorTexture);
				currentInput = output.colorTexture;
			}
		}

		if (i < passes.length) {
			try (final var temp = RenderTexture.HDR_COLOR.acquireTemporary()) {
				passes[i].process(temp, currentInput);
				output.framebuffer.bind();
				BufferRenderer.drawFullscreen(temp.colorTexture);
			}
		}
	}

	public static void runWorldPostProcessing() {
		GlManager.pushState();
		final var mainTarget = new RenderTexture(GlFramebuffer.getMainFramebuffer());
		final var compositedHdr = SkyRenderer.INSTANCE.compositeMainWorld(mainTarget.framebuffer);
		try (final var tempHdr = RenderTexture.HDR_COLOR.acquireTemporary()) {
			POST_PROCESS_WORLD_HDR.invoker().process(tempHdr, compositedHdr);
			runTonemappingPass(mainTarget, tempHdr.colorTexture);
		}
		// despite the contract of PostProcessCallback.process, output does write to
		// input, but it's fine since it will be dispatched to `runAllPostProcessing`,
		// which ensures all event listeners observe fulfillment of the contract.
		POST_PROCESS_WORLD_SDR.invoker().process(mainTarget, mainTarget.colorTexture);
		GlManager.popState();

	}

	// reduces HDR information to SDR (can be thought of as transforming photometric quantities into colors)
	public static void runTonemappingPass(RenderTexture output, GlTexture2d input) {
		AverageLuminanceComputer.INSTANCE.compute(input);
		final var averageLuminance = AverageLuminanceComputer.INSTANCE.currentAverageBrightness();
		output.framebuffer.bind();
		final var postShader = UltravioletShaders.getShader(UltravioletShaders.SHADER_MAIN_POSTPROCESS_LOCATION);
		// postShader.setUniformf("uExposure", ClientConfig.get(ConfigKey.POST_SHADER_EXPOSURE));
		postShader.setUniformf("uExposure", 1f);
		postShader.setUniformf("uAverageLuminance", averageLuminance);
		postShader.setUniformSampler("uSampler", input);
		BufferRenderer.drawFullscreen(postShader);
	}

	@FunctionalInterface
	public interface PostProcessCallback {
		/**
		 * Apply post-processing to {@code input}, storing the result in {@code output}.
		 * For this callback, {@code input} and {@code output} are distinct (ie,
		 * {@code input.framebuffer.writesTo(output)} will return {@code false})
		 * 
		 * @param output The framebuffer to store the post-processing result in.
		 * @param input  The image to apply post-processing to.
		 * 
		 * @see GlFramebuffer#writesTo(GlObject)
		 */
		void process(RenderTexture output, GlTexture2d input);
	}

}
