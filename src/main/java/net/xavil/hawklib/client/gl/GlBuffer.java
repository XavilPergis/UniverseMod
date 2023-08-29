package net.xavil.hawklib.client.gl;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.systems.RenderSystem;

public class GlBuffer extends GlObject {

	public static enum Type {
		// @formatter:off
		ARRAY(GL45C.GL_ARRAY_BUFFER, GL45C.GL_ARRAY_BUFFER_BINDING, "Vertex Buffer"),
		ELEMENT(GL45C.GL_ELEMENT_ARRAY_BUFFER, GL45C.GL_ELEMENT_ARRAY_BUFFER_BINDING, "Index Buffer"),
		COPY_READ(GL45C.GL_COPY_READ_BUFFER, GL45C.GL_COPY_READ_BUFFER_BINDING, "Copy Read Buffer"),
		COPY_WRITE(GL45C.GL_COPY_WRITE_BUFFER, GL45C.GL_COPY_WRITE_BUFFER_BINDING, "Copy Write Buffer"),
		PIXEL_UNPACK_BUFFER(GL45C.GL_PIXEL_UNPACK_BUFFER, GL45C.GL_PIXEL_UNPACK_BUFFER_BINDING, "Pixel Unpack Buffer"),
		PIXEL_PACK_BUFFER(GL45C.GL_PIXEL_PACK_BUFFER, GL45C.GL_PIXEL_PACK_BUFFER_BINDING, "Pixel Pack Buffer"),
		QUERY(GL45C.GL_QUERY_BUFFER, GL45C.GL_QUERY_BUFFER_BINDING, "Query Buffer"),
		TEXTURE(GL45C.GL_TEXTURE_BUFFER, GL45C.GL_TEXTURE_BUFFER_BINDING, "Texture Buffer"),
		TRANSFORM_FEEDBACK(GL45C.GL_TRANSFORM_FEEDBACK_BUFFER, GL45C.GL_TRANSFORM_FEEDBACK_BUFFER_BINDING, "Transform Feedback Buffer"),
		UNIFORM(GL45C.GL_UNIFORM_BUFFER, GL45C.GL_UNIFORM_BUFFER_BINDING, "Uniform Buffer"),
		DRAW_INDIRECT(GL45C.GL_DRAW_INDIRECT_BUFFER, GL45C.GL_DRAW_INDIRECT_BUFFER_BINDING, "Indirect Draw Buffer"),
		ATOMIC_COUNTER(GL45C.GL_ATOMIC_COUNTER_BUFFER, GL45C.GL_ATOMIC_COUNTER_BUFFER_BINDING, "Atomic Counter Buffer"),
		DISPATCH_INDIRECT(GL45C.GL_DISPATCH_INDIRECT_BUFFER, GL45C.GL_DISPATCH_INDIRECT_BUFFER_BINDING, "Indirect Dispatch Buffer"),
		SHADER_STORAGE(GL45C.GL_SHADER_STORAGE_BUFFER, GL45C.GL_SHADER_STORAGE_BUFFER_BINDING, "Shader Storage Buffer");
		// @formatter:on

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
		STREAM_DRAW(GL45C.GL_STREAM_DRAW, "Stream Draw"),
		STREAM_READ(GL45C.GL_STREAM_READ, "Stream Read"),
		STREAM_COPY(GL45C.GL_STREAM_COPY, "Stream Copy"),
		STATIC_DRAW(GL45C.GL_STATIC_DRAW, "Stataic Draw"),
		STATIC_READ(GL45C.GL_STATIC_READ, "Stataic Read"),
		STATIC_COPY(GL45C.GL_STATIC_COPY, "Stataic Copy"),
		DYNAMIC_DRAW(GL45C.GL_DYNAMIC_DRAW, "Dynamic Draw"),
		DYNAMIC_READ(GL45C.GL_DYNAMIC_READ, "Dynamic Read"),
		DYNAMIC_COPY(GL45C.GL_DYNAMIC_COPY, "Dynamic Copy");

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
		super(GL45C.glCreateBuffers(), true);
	}

	@Override
	protected void destroy() {
		GL45C.glDeleteBuffers(this.id);
	}

	public void bufferData(ByteBuffer buffer, UsageHint usage) {
		GL45C.glNamedBufferData(this.id, buffer, usage.id);
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
