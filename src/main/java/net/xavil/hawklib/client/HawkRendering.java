package net.xavil.hawklib.client;

import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.GlFragmentWrites;
import net.xavil.hawklib.client.gl.GlFramebuffer;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.client.flexible.BufferRenderer;

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
		default void accept(ResourceLocation name, VertexFormat vertexFormat, GlFragmentWrites fragmentWrites) {
			accept(name, vertexFormat, fragmentWrites, Iterator.empty());
		}

		void accept(ResourceLocation name, VertexFormat vertexFormat, GlFragmentWrites fragmentWrites,
				Iterator<String> shaderDefines);
	}

	/**
	 * {@code input} may be a texture that is written to by {@code output}.
	 * 
	 * @param output The framebuffer to write the post-processing results to
	 * @param input  The source image to apply the post-processing effects to
	 */
	public static void doPostProcessing(GlFramebuffer output, GlTexture2d input) {
		try (final var disposer = Disposable.scope()) {
			// bloom
			// final var hdrPost =
			// disposer.attach(RenderTexture.getTemporary(input.size().d2(), DESC));
			// BloomEffect.render(hdrPost.framebuffer, input);

			// tonemapping
			output.bind();
			// final var postShader = getShader(SHADER_MAIN_POSTPROCESS);
			// postShader.setUniform("uExposure", 1f);
			// postShader.setUniformSampler("uSampler", hdrPost.colorTexture);
			// BufferRenderer.drawFullscreen(postShader);

			final var postShader = HawkShaders.getShader(HawkShaders.SHADER_BLIT);
			// postShader.setUniform("uExposure", 1f);
			postShader.setUniformSampler("uSampler", input);
			BufferRenderer.drawFullscreen(postShader);
		}

	}

}
