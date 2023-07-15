package net.xavil.hawklib.client.gl;

import com.mojang.blaze3d.vertex.VertexFormat;

public final class GlVertexArray extends GlObject {

	public GlVertexArray(int id, boolean owned) {
		super(id, owned);
	}

	public GlVertexArray() {
		super(GlManager.createVertexArray(), true);
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.VERTEX_ARRAY;
	}

	public void setup(VertexFormat format) {
		GlManager.bindVertexArray(this.id);
		format.setupBufferState();
	}
	
}
