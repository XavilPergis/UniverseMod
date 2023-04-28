package net.xavil.universal.client;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderInstance;
import net.xavil.universal.mixin.accessor.GameRendererAccessor;

public final class ModRendering {

	public static final Event<RegisterShadersCallback> LOAD_SHADERS_EVENT = EventFactory
			.createArrayBacked(RegisterShadersCallback.class, callbacks -> acceptor -> {
				for (var callback : callbacks)
					callback.register(acceptor);
			});

	public static final Event<RegisterPostProcessShadersCallback> LOAD_POST_PROCESS_SHADERS_EVENT = EventFactory
			.createArrayBacked(RegisterPostProcessShadersCallback.class, callbacks -> acceptor -> {
				for (var callback : callbacks)
					callback.register(acceptor);
			});

	public static final String PLANET_SHADER = "universal_planet";
	public static final String RING_SHADER = "universal_ring";
	public static final String STAR_BILLBOARD_SHADER = "universal_star_billboard";
	public static final String STAR_SHADER = "universal_star";
	public static final String GALAXY_PARTICLE_SHADER = "universal_galaxy_particle";

	public static final String COMPOSITE_SKY_CHAIN = "shaders/post/universal_composite_sky.json";

	// the one in `DefaultVertexFormat` has a component type of `short`, and we want `float`.
	public static final VertexFormatElement ELEMENT_UV1 = new VertexFormatElement(1, VertexFormatElement.Type.FLOAT,
			VertexFormatElement.Usage.UV, 2);
	public static final VertexFormatElement ELEMENT_NORMAL = new VertexFormatElement(0, VertexFormatElement.Type.FLOAT,
			VertexFormatElement.Usage.NORMAL, 3);

	public static final VertexFormat PLANET_VERTEX_FORMAT = new VertexFormat(
			ImmutableMap.<String, VertexFormatElement>builder()
					.put("Position", DefaultVertexFormat.ELEMENT_POSITION)
					.put("UV0", DefaultVertexFormat.ELEMENT_UV0) // base color map
					// .put("UV1", ELEMENT_UV1) // normal map
					.put("Color", DefaultVertexFormat.ELEMENT_COLOR)
					.put("Normal", ELEMENT_NORMAL)
					.build());

	public static ShaderInstance getShader(String id) {
		return GameRendererAccessor.getShader(Minecraft.getInstance().gameRenderer, id);
	}

	public static PostChain getPostChain(String id) {
		return GameRendererAccessor.getPostChain(Minecraft.getInstance().gameRenderer, id);
	}

	public interface ShaderSink {
		void accept(String name, VertexFormat vertexFormat);
	}

	public interface RegisterShadersCallback {
		void register(ShaderSink sink);
	}

	public interface PostProcessShaderSink {
		void accept(String location);
	}

	public interface RegisterPostProcessShadersCallback {
		void register(PostProcessShaderSink sink);
	}

}
