package net.xavil.hawklib.client.flexible;

import java.nio.ByteBuffer;

import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;

// this could probably be more efficient by writing to a persistent-mapped buffer.
public final class DirectVertexBuilder extends VertexBuilder {

	private int vertexStartByteOffset = 0;
	private int vertexByteOffset = 0;
	private BufferLayout currentLayout = null;
	private PrimitiveType primitiveType = null;
	private ByteBuffer buffer;
	private int vertexCount = 0;
	private VertexDispatcher currentDispatcher;

	// the amount of buffers that have been created but not yet drawn. We cannot
	// reuse our allocation if we have undrawn buffers.
	private int undrawnBufferCount = 0;

	public ByteBuffer backingBufferView() {
		return this.buffer.asReadOnlyBuffer();
	}

	public ByteBuffer backingBuffer() {
		return this.buffer;
	}

	public BufferLayout currentLayout() {
		return this.currentLayout;
	}

	public void assertBuffersDrawn() {
		if (this.undrawnBufferCount > 0) {
			throw new IllegalStateException(String.format(
					"Vertex Builder had %d undrawn buffers!",
					this.undrawnBufferCount));
		}
	}

	public DirectVertexBuilder(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	private void ensureBufferCapacity(int additionalBytes) {
		if (this.vertexByteOffset + additionalBytes <= this.buffer.capacity())
			return;
		throw new IllegalStateException(String.format(
				"DirectVertexBuilder overrun: buffer is %d bytes, needed at least %d bytes",
				this.buffer.capacity(), this.vertexByteOffset + additionalBytes));
	}

	@Override
	public <T extends VertexDispatcher> T begin(T dispatcher, PrimitiveType mode, BufferLayout layout) {
		dispatcher.setup(this, layout, this.buffer);
		this.currentLayout = layout;
		this.primitiveType = mode;
		this.vertexStartByteOffset = Mth.roundToward(this.vertexByteOffset, 4);
		this.currentDispatcher = dispatcher;
		return dispatcher;
	}

	@Override
	public boolean isBuilding() {
		return this.currentLayout != null;
	}

	@Override
	public void reset() {
		this.vertexByteOffset = 0;
		this.vertexStartByteOffset = 0;
	}

	@Override
	protected void handleBufferDrawn(BuiltBuffer buffer) {
		this.undrawnBufferCount -= 1;
		if (this.undrawnBufferCount == 0) {
			reset();
		}
	}

	@Override
	protected BuiltBuffer end() {
		Assert.isTrue(isBuilding());
		final var indexCount = this.primitiveType.physicalIndexCount(this.vertexCount);
		final var indexType = VertexFormat.IndexType.least(indexCount);

		final var vertexData = this.buffer.slice(this.vertexStartByteOffset,
				this.vertexByteOffset - this.vertexStartByteOffset);

		final var built = this.new BuiltBuffer(vertexData, null, this.currentLayout,
				this.primitiveType,
				indexType, this.vertexCount, indexCount);
		this.vertexCount = 0;
		this.currentLayout = null;
		this.primitiveType = null;
		this.currentDispatcher = null;

		return built;
	}

	@Override
	protected void emitVertex() {
		try {
			final var vertexSize = this.currentLayout.byteStride;
			final var emissionSize = vertexSize * (this.primitiveType.duplicationCount + 1);

			ensureBufferCapacity(emissionSize);

			this.currentDispatcher.emit(this.vertexByteOffset);

			int writePos = this.vertexByteOffset + vertexSize;
			for (int i = 0; i < this.primitiveType.duplicationCount; ++i) {
				this.buffer.put(writePos, this.buffer, this.vertexByteOffset, vertexSize);
				writePos += vertexSize;
			}

			this.vertexByteOffset += emissionSize;
			this.vertexCount += this.primitiveType.duplicationCount + 1;
		} catch (Throwable t) {
			var msg = "Buffer Builder Error:\n";
			if (this.currentLayout == null)
				msg += "[***] VertexBuilder is not currently building!\n";
			msg += String.format("Current Capacity: %d\n", this.buffer.capacity());
			msg += String.format("Current Limit: %d\n", this.buffer.limit());
			msg += String.format("Current Offset: %d\n", this.vertexByteOffset);
			throw new RuntimeException(msg, t);
		}
	}

}
