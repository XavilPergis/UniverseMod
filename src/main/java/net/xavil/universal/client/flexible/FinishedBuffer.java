package net.xavil.universal.client.flexible;

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
}
