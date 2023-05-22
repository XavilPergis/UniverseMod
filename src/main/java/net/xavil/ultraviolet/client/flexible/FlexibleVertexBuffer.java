package net.xavil.ultraviolet.client.flexible;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL32C;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.xavil.ultraviolet.client.gl.DrawState;
import net.xavil.ultraviolet.client.gl.GlBuffer;
import net.xavil.ultraviolet.client.gl.GlManager;
import net.xavil.ultraviolet.client.gl.shader.ShaderProgram;
import net.xavil.util.Assert;
import net.xavil.util.Disposable;

public class FlexibleVertexBuffer implements Disposable {
	private int vertexBufferId = -1;
	private int indexBufferId = -1;
	private int vertexArrayId = -1;

	private int bufferUsage = GL32C.GL_STATIC_DRAW;

	private FlexibleVertexMode mode;
	private VertexFormat format;

	private VertexFormat.IndexType indexType;
	private int indexCount;
	private boolean sequentialIndices;

	public void create(boolean createIndexBuffer, int usage) {
		if (this.vertexBufferId < 0)
			RenderSystem.glGenBuffers(id -> this.vertexBufferId = id);
		if (createIndexBuffer && this.indexBufferId < 0)
			RenderSystem.glGenBuffers(id -> this.indexBufferId = id);
		if (this.vertexArrayId < 0)
			RenderSystem.glGenVertexArrays(id -> this.vertexArrayId = id);
	}

	public boolean isCreated() {
		return this.vertexBufferId >= 0;
	}

	public void upload(FlexibleBufferBuilder builder) {
		if (builder.isBuilding())
			builder.end();
		final var finished = builder.popFinished();
		upload(finished.getSecond(), finished.getFirst(), GL32C.GL_STATIC_DRAW);
	}

	public void upload(ByteBuffer buffer, FinishedBuffer info, int usage) {
		if (!isCreated())
			create(false, usage);

		buffer.clear();

		this.mode = info.mode();
		this.format = info.format();
		this.sequentialIndices = info.sequentialIndex();
		this.indexCount = info.indexCount();
		this.bufferUsage = usage;

		final var byteLimit = info.vertexCount() * info.format().getVertexSize();
		buffer.position(0);
		buffer.limit(byteLimit);
		setupForFormat(info.format());
		// GlStateManager._glBindVertexArray(this.vertexArrayId);
		// GlManager.bindBuffer(GL32C.GlBuffer.Type.ARRAY, this.vertexBufferId);
		GlStateManager._glBufferData(GlBuffer.Type.ARRAY.id, buffer, this.bufferUsage);

		if (info.sequentialIndex()) {
			final var sequentialIndexBuffer = info.mode().getSequentialBuffer(info.indexCount());
			this.indexBufferId = sequentialIndexBuffer.name();
			this.indexType = sequentialIndexBuffer.type();
			GlManager.bindBuffer(GlBuffer.Type.ELEMENT, this.indexBufferId);
		} else {
			create(true, usage);
			buffer.position(byteLimit);
			buffer.limit(byteLimit + info.indexCount() * info.indexType().bytes);
			GlManager.bindBuffer(GlBuffer.Type.ELEMENT, this.indexBufferId);
			GlStateManager._glBufferData(GlBuffer.Type.ELEMENT.id, buffer, this.bufferUsage);
			this.indexType = info.indexType();
		}

		buffer.position(0);
		unbind();
	}

	public void draw(ShaderProgram shader, DrawState drawState) {
		if (this.indexCount <= 0)
			return;
		// TODO: Assert shader and vertex buffer have compatible formats.
		// Testing for equality between the formats is too strict; if we have a positon
		// attribute, it can be provided as *anything* that provides 3 components, not
		// necessarily just a float vec.
		// if (!this.format.equals(shader.getFormat())) {
		// 	throw new IllegalArgumentException(String.format(
		// 			"Shader format did not match vertex buffer format!\nShader Format: %s\nVertex Buffer Format: %s\n",
		// 			shader.getFormat(), this.format));
		// }
		GlManager.pushState();
		drawState.apply(GlManager.currentState());
		shader.bind();
		bind();
		GlStateManager._drawElements(this.mode.gl, this.indexCount, this.indexType.asGLType, 0L);
		GlManager.popState();
	}

	private void setupForFormat(VertexFormat format) {
		Assert.isTrue(isCreated());
		// if (this.format != null && this.format.equals(format))
		// return;
		BufferUploader.reset();
		if (this.format != null)
			this.format.clearBufferState();
		GlManager.bindVertexArray(this.vertexArrayId);
		GlManager.bindBuffer(GlBuffer.Type.ARRAY, this.vertexBufferId);
		format.setupBufferState();
		this.format = format;
	}

	public void bind() {
		GlStateManager._glBindVertexArray(this.vertexArrayId);
		GlManager.bindBuffer(GlBuffer.Type.ARRAY, this.vertexBufferId);
		GlManager.bindBuffer(GlBuffer.Type.ELEMENT, this.indexBufferId);
	}

	public static void unbind() {
		GlManager.bindVertexArray(0);
		GlManager.bindBuffer(GlBuffer.Type.ARRAY, 0);
		GlManager.bindBuffer(GlBuffer.Type.ELEMENT, 0);
	}

	@Override
	public void close() {
		if (this.vertexBufferId >= 0) {
			GlManager.deleteBuffer(this.vertexBufferId);
		}
		// sequential indices use a shared index buffer; we don't want to delete that!!
		if (!this.sequentialIndices && this.indexBufferId >= 0) {
			GlManager.deleteBuffer(this.indexBufferId);
		}
		if (this.vertexArrayId >= 0) {
			GlManager.deleteVertexArray(this.vertexArrayId);
		}
		this.vertexBufferId = -1;
		this.indexBufferId = -1;
		this.vertexArrayId = -1;
	}

}
