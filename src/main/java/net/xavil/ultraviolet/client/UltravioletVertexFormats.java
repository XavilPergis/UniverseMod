package net.xavil.ultraviolet.client;

import net.fabricmc.api.Environment;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class UltravioletVertexFormats {

	protected UltravioletVertexFormats() {
	}

	public static final BufferLayout PLANET_VERTEX_FORMAT = BufferLayout.builder()
			.element("aPos", BufferLayout.ELEMENT_FLOAT3, BufferLayout.Usage.POSITION, 0)
			.element("aNormal", BufferLayout.ELEMENT_FLOAT3, BufferLayout.Usage.NORMAL, 0)
			.element("aTexCoord0", BufferLayout.ELEMENT_FLOAT2, BufferLayout.Usage.UV, 0)
			.build();
	public static final BufferLayout BILLBOARD_FORMAT = BufferLayout.builder()
			.element("aPos", BufferLayout.ELEMENT_FLOAT3, BufferLayout.Usage.POSITION, 0)
			.element("aColor", BufferLayout.ELEMENT_FLOAT4, BufferLayout.Usage.COLOR, 0)
			.element("aTexCoord0", BufferLayout.ELEMENT_FLOAT2, BufferLayout.Usage.UV, 0)
			.build();
}
