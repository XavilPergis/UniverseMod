package net.xavil.hawklib.client.gl;

import org.lwjgl.opengl.GL45C;

public final class GlVertexArray extends GlObject {

	public GlVertexArray(int id, boolean owned) {
		super(id, owned);
	}

	public GlVertexArray() {
		super(GL45C.glCreateVertexArrays(), true);
	}

	@Override
	protected void destroy() {
		GL45C.glDeleteVertexArrays(this.id);
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.VERTEX_ARRAY;
	}

}
