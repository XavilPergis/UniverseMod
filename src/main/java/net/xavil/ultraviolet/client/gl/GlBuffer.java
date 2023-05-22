package net.xavil.ultraviolet.client.gl;

import org.lwjgl.opengl.GL32C;

public final class GlBuffer extends GlObject {

	public static enum Type {
		ARRAY(GL32C.GL_ARRAY_BUFFER, GL32C.GL_ARRAY_BUFFER_BINDING, "Vertex Buffer"),
		ELEMENT(GL32C.GL_ELEMENT_ARRAY_BUFFER, GL32C.GL_ELEMENT_ARRAY_BUFFER_BINDING, "Index Buffer"),
		TRANSFORM_FEEDBACK(GL32C.GL_TRANSFORM_FEEDBACK_BUFFER, GL32C.GL_TRANSFORM_FEEDBACK_BUFFER_BINDING,
				"Transform Feedback Buffer"),
		PIXEL_PACK_BUFFER(GL32C.GL_PIXEL_PACK_BUFFER, GL32C.GL_PIXEL_PACK_BUFFER_BINDING, "Pixel Pack Buffer"),
		PIXEL_UNPACK_BUFFER(GL32C.GL_PIXEL_UNPACK_BUFFER, GL32C.GL_PIXEL_UNPACK_BUFFER_BINDING, "Pixel Unpack Buffer"),
		UNIFORM(GL32C.GL_UNIFORM_BUFFER, GL32C.GL_UNIFORM_BUFFER_BINDING, "Uniform Buffer");

		public final int id;
		public final int bindingId;
		public final String description;

		private Type(int id, int bindingId, String description) {
			this.id = id;
			this.bindingId = bindingId;
			this.description = description;
		}

		@Override
		public String toString() {
			return this.description;
		}
	}

	public GlBuffer(int id, boolean owned) {
		super(id, owned);
	}

	public GlBuffer() {
		super(GlManager.createBuffer(), true);
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.BUFFER;
	}

	public static GlBuffer importFromId(int id) {
		return new GlBuffer(id, false);
	}

}
