package net.xavil.universal.client;

import java.io.IOException;

import com.mojang.blaze3d.vertex.VertexFormat;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.xavil.universal.mixin.accessor.GameRendererAccessor;

public final class ModRendering {

	public static final Event<RegisterShadersCallback> LOAD_SHADERS_EVENT = EventFactory
			.createArrayBacked(RegisterShadersCallback.class, callbacks -> acceptor -> {
				for (var callback : callbacks)
					callback.register(acceptor);
			});

	public static final String PLANET_SHADER = "universal_planet";
	public static final String RING_SHADER = "universal_ring";
	public static final String STAR_BILLBOARD_SHADER = "universal_star_billboard";

	public static ShaderInstance getShader(String id) {
		return GameRendererAccessor.getShader(Minecraft.getInstance().gameRenderer, id);
	}

	public interface ShaderSink {
		void accept(String name, VertexFormat vertexFormat);
	}

	public interface RegisterShadersCallback {
		void register(ShaderSink sink);
	}

}
