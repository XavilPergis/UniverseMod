package net.xavil.ultraviolet.client;

import net.fabricmc.api.Environment;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class UltravioletVertexFormats {

	protected UltravioletVertexFormats() {
	}

	public static final BufferLayout VERTEX_FORMAT_PLANET = BufferLayout.builder()
			.element(BufferLayout.ELEMENT_FLOAT3, BufferLayout.Attribute.POSITION)
			.element(BufferLayout.ELEMENT_FLOAT3, BufferLayout.Attribute.NORMAL)
			.element(BufferLayout.ELEMENT_FLOAT2, BufferLayout.Attribute.UV0)
			.build();
	public static final BufferLayout VERTEX_FORMAT_BILLBOARD_REALISTIC = BufferLayout.builder()
			.element(BufferLayout.ELEMENT_FLOAT3, BufferLayout.Attribute.POSITION)
			.element(BufferLayout.ELEMENT_FLOAT_UBYTE_NORM4, BufferLayout.Attribute.COLOR)
			.element(BufferLayout.ELEMENT_FLOAT2, BufferLayout.Attribute.UV0)
			.build();
	public static final BufferLayout VERTEX_FORMAT_BILLBOARD_UI = BufferLayout.builder()
			.element(BufferLayout.ELEMENT_FLOAT3, BufferLayout.Attribute.POSITION)
			.element(BufferLayout.ELEMENT_FLOAT_UBYTE_NORM4, BufferLayout.Attribute.COLOR)
			.build();
}
