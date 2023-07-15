package net.xavil.ultraviolet.client;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import net.fabricmc.api.Environment;
import net.fabricmc.api.EnvType;

@Environment(EnvType.CLIENT)
public class UltravioletVertexFormats {

	protected UltravioletVertexFormats() {
	}

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

}
