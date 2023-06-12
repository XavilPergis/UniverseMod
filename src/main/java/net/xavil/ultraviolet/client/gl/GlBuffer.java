package net.xavil.ultraviolet.client.gl;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

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

	public static enum UsageHint {
		STREAM_DRAW(GL32C.GL_STREAM_DRAW, "Stream Draw"),
		STREAM_READ(GL32C.GL_STREAM_READ, "Stream Read"),
		STREAM_COPY(GL32C.GL_STREAM_COPY, "Stream Copy"),
		STATIC_DRAW(GL32C.GL_STATIC_DRAW, "Stataic Draw"),
		STATIC_READ(GL32C.GL_STATIC_READ, "Stataic Read"),
		STATIC_COPY(GL32C.GL_STATIC_COPY, "Stataic Copy"),
		DYNAMIC_DRAW(GL32C.GL_DYNAMIC_DRAW, "Dynamic Draw"),
		DYNAMIC_READ(GL32C.GL_DYNAMIC_READ, "Dynamic Read"),
		DYNAMIC_COPY(GL32C.GL_DYNAMIC_COPY, "Dynamic Copy");

		public final int id;
		public final String description;

		private UsageHint(int id, String description) {
			this.id = id;
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

	public void bufferData(ByteBuffer buffer, UsageHint usage) {
		GlManager.currentState().bufferData(this.id, buffer, usage);
	}

	@Override
	public ObjectType objectType() {
		return ObjectType.BUFFER;
	}

	public static GlBuffer importFromId(int id) {
		return new GlBuffer(id, false);
	}

	public static GlBuffer importFromAutoStorage(RenderSystem.AutoStorageIndexBuffer buffer) {
		return new GlBuffer(buffer.name(), false);
	}

}
