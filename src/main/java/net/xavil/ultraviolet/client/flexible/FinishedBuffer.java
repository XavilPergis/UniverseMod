package net.xavil.ultraviolet.client.flexible;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.vertex.VertexFormat;

public record FinishedBuffer(
		VertexFormat format,
		FlexibleVertexMode mode,
		VertexFormat.IndexType indexType,
		int parentBufferOffset,
		int vertexCount,
		int indexCount,
		boolean sequentialIndex) {
	public int vertexBufferSize() {
		return this.vertexCount * this.format.getVertexSize();
	}

	public int indexBufferSize() {
		return this.sequentialIndex ? 0 : this.indexCount * this.indexType.bytes;
	}

	public int byteCount() {
		return vertexBufferSize() + indexBufferSize();
	}

	public ByteBuffer vertexData(ByteBuffer buffer) {
		final var dataLength = this.vertexCount * this.format.getVertexSize();
		return buffer.slice(0, dataLength);
	}

	public ByteBuffer indexData(ByteBuffer buffer) {
		if (this.sequentialIndex)
			return null;
		final var vertexDataLength = this.vertexCount * this.format.getVertexSize();
		final var indexDataLength = this.indexCount * this.indexType.bytes;
		return buffer.slice(vertexDataLength, vertexDataLength + indexDataLength);
	}
}
