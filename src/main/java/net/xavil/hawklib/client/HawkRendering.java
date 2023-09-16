package net.xavil.hawklib.client;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.GlFramebuffer;
import net.xavil.hawklib.client.gl.shader.AttributeSet;
import net.xavil.hawklib.client.gl.texture.GlTexture;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.ultraviolet.client.BloomEffect;
import net.xavil.ultraviolet.client.UltravioletShaders;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.RenderTexture;

public final class HawkRendering {

	public static final Event<RegisterShadersCallback> LOAD_SHADERS_EVENT = EventFactory
			.createArrayBacked(RegisterShadersCallback.class, callbacks -> acceptor -> {
				for (var callback : callbacks)
					callback.register(acceptor);
			});

	@FunctionalInterface
	public interface RegisterShadersCallback {
		void register(ShaderSink sink);
	}

	public static final class ShaderLoadDiagnostics {
		public int successfulLoadCount = 0;
		public MutableList<ResourceLocation> failedToLoad = new Vector<>();
	}

	public interface ShaderSink {
		default void accept(ResourceLocation name, AttributeSet attributeSet, GlFragmentWrites fragmentWrites) {
			accept(name, attributeSet, fragmentWrites, Iterator.empty());
		}

		void accept(ResourceLocation name, AttributeSet attributeSet, GlFragmentWrites fragmentWrites,
				Iterator<String> shaderDefines);
	}

	/**
	 * {@code input} may be a texture that is written to by {@code output}.
	 * 
	 * @param output The framebuffer to write the post-processing results to
	 * @param input  The source image to apply the post-processing effects to
	 */
	public static void applyPostProcessing(GlFramebuffer output, GlTexture2d input) {
		try (final var disposer = Disposable.scope()) {
			// bloom
			final var hdrPost = disposer.attach(RenderTexture.HDR_COLOR.acquireTemporary(input.size().d2()));
			BloomEffect.render(hdrPost.framebuffer, input);

			// tonemapping
			output.bind();
			final var postShader = UltravioletShaders.getShader(UltravioletShaders.SHADER_MAIN_POSTPROCESS_LOCATION);
			postShader.setUniform("uExposure", 1f);
			postShader.setUniformSampler("uSampler", hdrPost.colorTexture);
			BufferRenderer.drawFullscreen(postShader);
		}

	}

}
