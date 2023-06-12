package net.xavil.ultraviolet.client.flexible;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.xavil.ultraviolet.client.gl.DrawState;
import net.xavil.ultraviolet.client.gl.GlBuffer;
import net.xavil.ultraviolet.client.gl.GlManager;
import net.xavil.ultraviolet.client.gl.shader.ShaderProgram;
import net.xavil.util.Disposable;

public class FlexibleVertexBuffer implements Disposable {
	private GlBuffer vertexBuffer = null;
	private GlBuffer indexBuffer = null;
	private int vertexArrayId = -1;

	private FlexibleVertexMode mode;
	private VertexFormat format;

	private VertexFormat.IndexType indexType;
	private int indexCount;

	private void createIfNeeded() {
		if (this.vertexBuffer == null)
			this.vertexBuffer = new GlBuffer();
		if (this.vertexArrayId < 0)
			this.vertexArrayId = GlManager.createVertexArray();
	}

	public void upload(FlexibleBufferBuilder builder) {
		upload(builder, GlBuffer.UsageHint.STATIC_DRAW);
	}

	public void upload(FlexibleBufferBuilder builder, GlBuffer.UsageHint usage) {
		if (builder.isBuilding())
			builder.end();
		final var finished = builder.popFinished();
		upload(finished.getSecond(), finished.getFirst(), usage);
	}

	public void upload(ByteBuffer buffer, FinishedBuffer info, GlBuffer.UsageHint usage) {
		createIfNeeded();

		buffer.clear();

		this.mode = info.mode();
		this.indexCount = info.indexCount();

		BufferUploader.reset();
		final var glState = GlManager.currentState();
		glState.boundBuffers[GlBuffer.Type.ARRAY.ordinal()] = 0;
		glState.boundBuffers[GlBuffer.Type.ELEMENT.ordinal()] = 0;
		glState.boundVertexArray = 0;

		GlManager.bindVertexArray(this.vertexArrayId);

		setupVertexBuffer(buffer, info, usage);

		if (this.format != null)
			this.format.clearBufferState();
		info.format().setupBufferState();
		this.format = info.format();

		setupIndexBuffer(buffer, info, usage);

		GlManager.bindVertexArray(0);
		buffer.position(0);
	}

	private void setupVertexBuffer(ByteBuffer buffer, FinishedBuffer info, GlBuffer.UsageHint usage) {
		GlManager.bindBuffer(GlBuffer.Type.ARRAY, this.vertexBuffer.id);
		this.vertexBuffer.bufferData(info.vertexData(buffer), usage);
	}

	private void setupIndexBuffer(ByteBuffer buffer, FinishedBuffer info, GlBuffer.UsageHint usage) {
		final var explicitIndexData = info.indexData(buffer);
		if (explicitIndexData != null) {
			if (this.indexBuffer == null)
				this.indexBuffer = new GlBuffer();
			GlManager.bindBuffer(GlBuffer.Type.ELEMENT, this.indexBuffer.id);
			this.indexBuffer.bufferData(explicitIndexData, usage);
			this.indexType = info.indexType();
		} else {
			final var sequentialIndexBuffer = info.mode().getSequentialBuffer(info.indexCount());
			final var indexBuffer = GlBuffer.importFromAutoStorage(sequentialIndexBuffer);
			GlManager.bindBuffer(GlBuffer.Type.ELEMENT, indexBuffer.id);
			this.indexType = sequentialIndexBuffer.type();
		}
	}

	public void draw(ShaderProgram shader, DrawState drawState) {
		if (this.indexCount <= 0)
			return;
		// TODO: Assert shader and vertex buffer have compatible formats.
		// Testing for equality between the formats is too strict; if we have a positon
		// attribute, it can be provided as *anything* that provides 3 components, not
		// necessarily just a float vec.
		// if (!this.format.equals(shader.getFormat())) {
		// throw new IllegalArgumentException(String.format(
		// "Shader format did not match vertex buffer format!\nShader Format: %s\nVertex
		// Buffer Format: %s\n",
		// shader.getFormat(), this.format));
		// }
		GlManager.pushState();
		drawState.apply(GlManager.currentState());
		shader.bind();
		GlManager.bindVertexArray(this.vertexArrayId);
		GlStateManager._drawElements(this.mode.gl, this.indexCount, this.indexType.asGLType, 0L);
		GlManager.popState();
	}

	@Override
	public void close() {
		if (this.vertexBuffer != null)
			this.vertexBuffer.close();
		if (this.indexBuffer != null)
			this.indexBuffer.close();
		if (this.vertexArrayId >= 0) {
			GlManager.deleteVertexArray(this.vertexArrayId);
		}
		this.vertexBuffer = null;
		this.indexBuffer = null;
		this.vertexArrayId = -1;
	}

}
