package net.xavil.hawklib.client.flexible;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;

public class FlexibleVertexBuffer implements Disposable {
	private GlBuffer vertexBuffer = null;
	private GlBuffer indexBuffer = null;
	private int vertexArrayId = -1;

	private PrimitiveType primitiveType;
	private VertexFormat format;

	private VertexFormat.IndexType indexType;
	private int indexCount;

	private void createIfNeeded() {
		if (this.vertexBuffer == null)
			this.vertexBuffer = new GlBuffer();
		if (this.vertexArrayId < 0)
			this.vertexArrayId = GlManager.createVertexArray();
	}

	public void upload(VertexBuilder.BuiltBuffer buffer) {
		upload(buffer, GlBuffer.UsageHint.STATIC_DRAW);
	}

	public void upload(VertexBuilder.BuiltBuffer buffer, GlBuffer.UsageHint usage) {
		createIfNeeded();

		this.primitiveType = buffer.primitiveType;
		this.indexCount = buffer.indexCount;

		BufferUploader.reset();
		// BufferUploader.reset changes GL state and does not track it, so it must be
		// manually applied here.
		final var glState = GlManager.currentState();
		glState.boundBuffers[GlBuffer.Type.ARRAY.ordinal()] = 0;
		glState.boundBuffers[GlBuffer.Type.ELEMENT.ordinal()] = 0;
		glState.boundVertexArray = 0;

		GlManager.bindVertexArray(this.vertexArrayId);

		setupVertexBuffer(buffer, usage);

		if (this.format != null)
			this.format.clearBufferState();
		buffer.format.setupBufferState();
		this.format = buffer.format;

		setupIndexBuffer(buffer, usage);

		GlManager.bindVertexArray(0);

		// signals to the VertexBuilder that this buffer came from that it may reuse
		// this buffer's memory.
		buffer.close();
	}

	private void setupVertexBuffer(VertexBuilder.BuiltBuffer buffer, GlBuffer.UsageHint usage) {
		GlManager.bindBuffer(GlBuffer.Type.ARRAY, this.vertexBuffer.id);
		this.vertexBuffer.bufferData(buffer.vertexData, usage);
	}

	private void setupIndexBuffer(VertexBuilder.BuiltBuffer buffer, GlBuffer.UsageHint usage) {
		if (buffer.indexData != null) {
			if (this.indexBuffer == null)
				this.indexBuffer = new GlBuffer();
			GlManager.bindBuffer(GlBuffer.Type.ELEMENT, this.indexBuffer.id);
			this.indexBuffer.bufferData(buffer.indexData, usage);
			this.indexType = buffer.indexType;
		} else {
			final var sequentialIndexBuffer = buffer.primitiveType.getSequentialBuffer(buffer.indexCount);
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
		GlStateManager._drawElements(this.primitiveType.gl, this.indexCount, this.indexType.asGLType, 0L);
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
