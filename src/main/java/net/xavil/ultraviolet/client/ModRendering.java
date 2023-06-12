package net.xavil.ultraviolet.client;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;
import net.xavil.ultraviolet.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.client.flexible.RenderTexture;
import net.xavil.ultraviolet.client.gl.GlFragmentWrites;
import net.xavil.ultraviolet.client.gl.GlFramebuffer;
import net.xavil.ultraviolet.client.gl.texture.GlTexture;
import net.xavil.ultraviolet.client.gl.texture.GlTexture2d;
import net.xavil.util.Disposable;
import net.xavil.util.iterator.Iterator;
import net.xavil.util.math.Color;

import static net.xavil.ultraviolet.client.Shaders.*;

public final class ModRendering {

	public static final Event<RegisterShadersCallback> LOAD_SHADERS_EVENT = EventFactory
			.createArrayBacked(RegisterShadersCallback.class, callbacks -> acceptor -> {
				for (var callback : callbacks)
					callback.register(acceptor);
			});

	// the one in `DefaultVertexFormat` has a component type of `short`, and we want
	// `float`.
	public static final VertexFormatElement ELEMENT_UV1 = new VertexFormatElement(1, VertexFormatElement.Type.FLOAT,
			VertexFormatElement.Usage.UV, 2);
	public static final VertexFormatElement ELEMENT_NORMAL = new VertexFormatElement(0, VertexFormatElement.Type.FLOAT,
			VertexFormatElement.Usage.NORMAL, 3);
	public static final VertexFormatElement ELEMENT_BILLBOARD_SIZE_INFO = new VertexFormatElement(0,
			VertexFormatElement.Type.FLOAT,
			VertexFormatElement.Usage.UV, 2);

	public static final VertexFormat PLANET_VERTEX_FORMAT = new VertexFormat(
			ImmutableMap.<String, VertexFormatElement>builder()
					.put("Position", DefaultVertexFormat.ELEMENT_POSITION)
					.put("UV0", DefaultVertexFormat.ELEMENT_UV0) // base color map
					// .put("UV1", ELEMENT_UV1) // normal map
					.put("Color", DefaultVertexFormat.ELEMENT_COLOR)
					.put("Normal", ELEMENT_NORMAL)
					.build());

	public static final VertexFormat BILLBOARD_FORMAT = new VertexFormat(
			ImmutableMap.<String, VertexFormatElement>builder()
					.put("Position", DefaultVertexFormat.ELEMENT_POSITION)
					.put("Color", DefaultVertexFormat.ELEMENT_COLOR)
					.put("SizeInfo", ELEMENT_BILLBOARD_SIZE_INFO)
					.build());

	public interface ShaderSink {
		default void accept(ResourceLocation name, VertexFormat vertexFormat, GlFragmentWrites fragmentWrites) {
			accept(name, vertexFormat, fragmentWrites, Iterator.empty());
		}

		void accept(ResourceLocation name, VertexFormat vertexFormat, GlFragmentWrites fragmentWrites,
				Iterator<String> shaderDefines);
	}

	public interface RegisterShadersCallback {
		void register(ShaderSink sink);
	}

	private static final RenderTexture.StaticDescriptor DESC = new RenderTexture.StaticDescriptor(
			GlTexture.Format.RGBA32_FLOAT);

	/**
	 * {@code input} may be a texture that is written to by {@code output}.
	 * 
	 * @param state  The current GL state
	 * @param output The framebuffer to write the post-processing results to
	 * @param input  The source image to apply the post-processing effects to
	 */
	public static void doPostProcessing(GlFramebuffer output, GlTexture2d input) {
		try (final var disposer = Disposable.scope()) {
			// bloom
			// final var hdrPost = disposer.attach(RenderTexture.getTemporary(input.size().d2(), DESC));
			// BloomEffect.render(hdrPost.framebuffer, input);

			// tonemapping
			output.bind();
			// final var postShader = getShader(SHADER_MAIN_POSTPROCESS);
			// postShader.setUniform("uExposure", 1f);
			// postShader.setUniformSampler("uSampler", hdrPost.colorTexture);
			// BufferRenderer.drawFullscreen(postShader);

			final var postShader = getShader(SHADER_BLIT);
			// postShader.setUniform("uExposure", 1f);
			postShader.setUniformSampler("uSampler", input);
			BufferRenderer.drawFullscreen(postShader);
		}

	}

}
