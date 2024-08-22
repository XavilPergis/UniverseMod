package net.xavil.hawklib.client.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Disposable;

public class GlBuffer extends GlObject implements BufferSliceable {

	public static enum Type {
		// @formatter:off
		ARRAY(GL45C.GL_ARRAY_BUFFER, GL45C.GL_ARRAY_BUFFER_BINDING, false, "Vertex Buffer"),
		ELEMENT(GL45C.GL_ELEMENT_ARRAY_BUFFER, GL45C.GL_ELEMENT_ARRAY_BUFFER_BINDING, false, "Index Buffer"),
		COPY_READ(GL45C.GL_COPY_READ_BUFFER, GL45C.GL_COPY_READ_BUFFER_BINDING, false, "Copy Read Buffer"),
		COPY_WRITE(GL45C.GL_COPY_WRITE_BUFFER, GL45C.GL_COPY_WRITE_BUFFER_BINDING, false, "Copy Write Buffer"),
		PIXEL_UNPACK_BUFFER(GL45C.GL_PIXEL_UNPACK_BUFFER, GL45C.GL_PIXEL_UNPACK_BUFFER_BINDING, false, "Pixel Unpack Buffer"),
		PIXEL_PACK_BUFFER(GL45C.GL_PIXEL_PACK_BUFFER, GL45C.GL_PIXEL_PACK_BUFFER_BINDING, false, "Pixel Pack Buffer"),
		QUERY(GL45C.GL_QUERY_BUFFER, GL45C.GL_QUERY_BUFFER_BINDING, false, "Query Buffer"),
		TEXTURE(GL45C.GL_TEXTURE_BUFFER, GL45C.GL_TEXTURE_BUFFER_BINDING, false, "Texture Buffer"),
		TRANSFORM_FEEDBACK(GL45C.GL_TRANSFORM_FEEDBACK_BUFFER, GL45C.GL_TRANSFORM_FEEDBACK_BUFFER_BINDING, true, "Transform Feedback Buffer"),
		UNIFORM(GL45C.GL_UNIFORM_BUFFER, GL45C.GL_UNIFORM_BUFFER_BINDING, true, "Uniform Buffer"),
		DRAW_INDIRECT(GL45C.GL_DRAW_INDIRECT_BUFFER, GL45C.GL_DRAW_INDIRECT_BUFFER_BINDING, false, "Indirect Draw Buffer"),
		ATOMIC_COUNTER(GL45C.GL_ATOMIC_COUNTER_BUFFER, GL45C.GL_ATOMIC_COUNTER_BUFFER_BINDING, true, "Atomic Counter Buffer"),
		DISPATCH_INDIRECT(GL45C.GL_DISPATCH_INDIRECT_BUFFER, GL45C.GL_DISPATCH_INDIRECT_BUFFER_BINDING, false, "Indirect Dispatch Buffer"),
		SHADER_STORAGE(GL45C.GL_SHADER_STORAGE_BUFFER, GL45C.GL_SHADER_STORAGE_BUFFER_BINDING, true, "Shader Storage Buffer");
		// @formatter:on

		public final int id;
		public final int bindingId;
		public final String description;
		public final boolean canBindIndexedBuffer;

		private Type(int id, int bindingId, boolean canBindIndexedBuffer, String description) {
			this.id = id;
			this.bindingId = bindingId;
			this.description = description;
			this.canBindIndexedBuffer = canBindIndexedBuffer;
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
		STATIC_DRAW(GL45C.GL_STATIC_DRAW, "Static Draw"),
		STATIC_READ(GL45C.GL_STATIC_READ, "Static Read"),
		STATIC_COPY(GL45C.GL_STATIC_COPY, "Static Copy"),
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

	public static enum BufferAccess {
		READ(GL45C.GL_READ_ONLY, "Read-only"),
		WRITE(GL45C.GL_WRITE_ONLY, "Write-only"),
		READ_WRITE(GL45C.GL_READ_WRITE, "Read/Write");

		public final int id;
		public final String description;

		private BufferAccess(int id, String description) {
			this.id = id;
			this.description = description;
		}
	}

	private static enum StorageMutability {
		MUTABLE, IMMUTABLE,
	}

	// for both mutable and immutable buffers
	private StorageMutability storageMutability;
	private long size = 0;

	private ByteBuffer mapping;
	// the flags the current mapping was created with
	private int mappingFlags;

	// for immutable buffers
	// the flags the immutable buffer storage was created with
	private int storageFlags;

	public GlBuffer(int id, boolean owned) {
		super(ObjectType.BUFFER, id, owned);
		this.size = GL45C.glGetNamedBufferParameteri64(this.id, GL45C.GL_BUFFER_SIZE);
	}

	public GlBuffer() {
		super(ObjectType.BUFFER, GL45C.glCreateBuffers(), true);
	}

	public static GlBuffer createMappedStagingBuffer(long size) {
		final var buffer = new GlBuffer();
		buffer.allocateImmutableStorage(size,
				GL45C.GL_MAP_PERSISTENT_BIT | GL45C.GL_MAP_WRITE_BIT | GL45C.GL_CLIENT_STORAGE_BIT);
		buffer.map(GL45C.GL_MAP_PERSISTENT_BIT | GL45C.GL_MAP_WRITE_BIT);
		return buffer;
	}

	public void allocateMutableStorage(ByteBuffer buffer, UsageHint usage) {
		if (this.storageMutability == StorageMutability.IMMUTABLE)
			throw new IllegalStateException(String.format(
					"%s: tried to create immutable storage on a buffer whose storage is already immutable",
					debugDescription()));
		this.storageMutability = StorageMutability.MUTABLE;
		this.size = buffer.remaining();
		GL45C.glNamedBufferData(this.id, buffer, usage.id);
	}

	public void bufferSubData(ByteBuffer buffer, long offset) {
		if ((this.storageFlags & GL45C.GL_DYNAMIC_STORAGE_BIT) == 0)
			throw new IllegalStateException(String.format(
					"%s: tried to update buffer data on buffer without dynamic storage",
					debugDescription()));
		GL45C.glNamedBufferSubData(this.id, offset, buffer);
	}

	public static final class Slice implements BufferSliceable {
		public final GlBuffer buffer;
		public final long offset, size;

		public Slice(GlBuffer buffer, long offset, long size) {
			if (offset + size > buffer.size)
				throw new IllegalArgumentException(String.format(
						"%s: buffer slice bounds error: buffer size is %d, but slice covers %d to %d",
						buffer.debugDescription(), buffer.size, offset, offset + size));
			this.buffer = buffer;
			this.offset = offset;
			this.size = size;
		}

		public void bindRange(Type target, int index) {
			Assert.isTrue(target.canBindIndexedBuffer);
			if (this.offset == 0 && this.size == this.buffer.size) {
				GL45C.glBindBufferBase(target.id, index, this.buffer.id);
			} else {
				GL45C.glBindBufferRange(target.id, index, this.buffer.id, this.offset, this.size);
			}
		}

		@Override
		public Slice slice() {
			return this;
		}

		@Override
		public Slice slice(long offset, long size) {
			if (offset + size > this.size)
				throw new IllegalArgumentException(String.format(
						"%s: buffer slice bounds error: slice size is %d, but new slice covers %d to %d",
						this.buffer.debugDescription(), this.size, offset, offset + size));
			return new Slice(this.buffer, this.offset + offset, size);
		}

		// this operation does not automatically issue a memory barrier
		public void copyTo(Slice other, long size) {
			if (size > this.size || size > other.size)
				throw new IllegalArgumentException(String.format(
						"%s: buffer slice bounds error: copy size is %d, but source slice size is %d and destination slice size is %d",
						this.buffer.debugDescription(), size, this.size, other.size));

			GL45C.glCopyNamedBufferSubData(this.buffer.id, other.buffer.id, this.offset, other.offset, size);
		}

		public void copyTo(Slice other) {
			copyTo(other, this.size);
		}

		public void flush() {
			if ((this.buffer.mappingFlags & GL45C.GL_MAP_FLUSH_EXPLICIT_BIT) == 0)
				throw new IllegalStateException(String.format(
						"%s: tried to flush buffer range when it was not mapped with GL_MAP_FLUSH_EXPLICIT_BIT",
						this.buffer.debugDescription()));
			GL45C.glFlushMappedNamedBufferRange(this.buffer.id, this.offset, this.size);
		}

		public void clear(int internalFormat) {
			GL45C.glClearNamedBufferSubData(this.buffer.id, internalFormat, this.offset, this.size, GL45C.GL_RED,
					GL45C.GL_UNSIGNED_BYTE, (ByteBuffer) null);
		}

		@Nullable
		public ByteBuffer mappedPointer() {
			if (this.buffer.mapping == null)
				return null;
			// FIXME: integer overflow
			return this.buffer.mapping.slice((int) this.offset, (int) this.size).order(ByteOrder.nativeOrder());
		}
	}

	public Slice slice(long offset, long length) {
		return new Slice(this, offset, length);
	}

	public Slice slice() {
		return new Slice(this, 0, this.size);
	}

	public long size() {
		return this.size;
	}

	public ByteBuffer map(int flags) {
		if (this.mapping != null)
			throw new IllegalStateException(String.format(
					"%s: tried to map buffer that was already mapped",
					debugDescription()));
		if ((flags & GL45C.GL_MAP_PERSISTENT_BIT) != 0 && (this.storageFlags & GL45C.GL_MAP_PERSISTENT_BIT) == 0)
			throw new IllegalStateException(String.format(
					"%s: tried to create persistent mapping on buffer with incompatible storage",
					debugDescription()));
		if ((flags & GL45C.GL_MAP_READ_BIT) != 0 && (this.storageFlags & GL45C.GL_MAP_READ_BIT) == 0)
			throw new IllegalStateException(String.format(
					"%s: tried to create readable mapping on buffer with non-readable storage",
					debugDescription()));
		if ((flags & GL45C.GL_MAP_WRITE_BIT) != 0 && (this.storageFlags & GL45C.GL_MAP_WRITE_BIT) == 0)
			throw new IllegalStateException(String.format(
					"%s: tried to create writable mapping on buffer with non-writable storage",
					debugDescription()));

		this.mapping = GL45C.glMapNamedBufferRange(this.id, 0, this.size, flags).order(ByteOrder.nativeOrder());
		this.mappingFlags = flags;
		return this.mapping;
	}

	public void unmap() {
		if ((this.mappingFlags & GL45C.GL_MAP_PERSISTENT_BIT) != 0)
			throw new IllegalStateException(String.format(
					"%s: tried to unmap persistently-mapped buffer",
					debugDescription()));
		if (this.mapping == null)
			throw new IllegalStateException(String.format(
					"%s: tried to unmap buffer that was not already mapped",
					debugDescription()));
		GL45C.glUnmapNamedBuffer(this.id);
		this.mapping = null;
	}

	private void validateImmutableStorage(long size) {
		if (this.storageMutability == StorageMutability.IMMUTABLE) {
			// immutable storage already initialized
			throw new IllegalStateException(String.format(
					"Tried to update immutable buffer storage from size %d to size %d",
					this.size, size));
		} else if (this.storageMutability == StorageMutability.MUTABLE) {
			// mutable storage already initialized
			throw new IllegalStateException(String.format(
					"Tried to update mutable buffer storage from size %d to size %d",
					this.size, size));
		}
	}

	public void allocateImmutableStorage(long size, int flags) {
		validateImmutableStorage(size);
		this.size = size;
		this.storageMutability = StorageMutability.IMMUTABLE;
		this.storageFlags = flags;
		GL45C.glNamedBufferStorage(this.id, size, flags);
	}

	public void allocateImmutableStorage(ByteBuffer data, int flags) {
		validateImmutableStorage(size);
		this.size = data.remaining();
		this.storageMutability = StorageMutability.IMMUTABLE;
		this.storageFlags = flags;
		GL45C.glNamedBufferStorage(this.id, data, flags);
	}

	public static GlBuffer importFromId(int id) {
		return new GlBuffer(id, false);
	}

	public static final class GrowableBuffer implements Disposable, BufferSliceable {
		private GlBuffer buffer = new GlBuffer();
		private long bufferSizeInUse = 0;
		private String debugName;
		private GrowthStrategy growthStrategy;

		public interface GrowthStrategy {

			long nextCapacity(long currentCapacity, long neededSize);

			static GrowthStrategy MINIMAL = (currentCapacity, neededSize) -> neededSize;

			static GrowthStrategy geometric(long initialCapacity, float growthFactor) {
				return (currentCapacity, neededSize) -> {
					long cap = currentCapacity > 0 ? currentCapacity : initialCapacity;
					while (cap < neededSize)
						cap *= growthFactor;
					return cap;
				};
			}

			static GrowthStrategy bucketed(long bucketSize) {
				return (currentCapacity, neededSize) -> bucketSize * (Math.floorDiv(neededSize, bucketSize) + 1);
			}

		}

		public GrowableBuffer() {
			this(GrowthStrategy.MINIMAL);
		}

		public GrowableBuffer(GrowthStrategy growthStrategy) {
			this.growthStrategy = growthStrategy;
		}

		public void setDebugName(String name) {
			this.debugName = name;
			this.buffer.setDebugName(this.debugName);
		}

		@Override
		public void close() {
			this.buffer.close();
		}

		private void growIfNeeded(long neededSize) {
			if (neededSize <= this.buffer.size())
				return;

			final var newCapacity = this.growthStrategy.nextCapacity(this.buffer.size(), neededSize);
			Assert.isGreaterOrEqual(newCapacity, neededSize);
			if (this.buffer != null)
				this.buffer.close();
			this.buffer = new GlBuffer();
			this.buffer.setDebugName(this.debugName);
			this.buffer.allocateImmutableStorage(neededSize, GL45C.GL_DYNAMIC_STORAGE_BIT);
		}

		/**
		 * Set the contents of this buffer, allocating a new larger buffer if needed.
		 * 
		 * @param data          The data to upload.
		 * @param sizeAlignment A value which the length of the buffer must be aligned
		 *                      to.
		 */
		public void setContents(Slice data) {
			growIfNeeded(data.size);
			data.copyTo(this.buffer.slice());
			this.bufferSizeInUse = data.size;
		}

		public void setContents(long size, Consumer<ByteBuffer> writer) {
			growIfNeeded(size);
			try (final var stagingBuffer = createMappedStagingBuffer(size)) {
				writer.accept(stagingBuffer.slice().mappedPointer());
				stagingBuffer.slice().copyTo(this.buffer.slice(), size);
			}
			this.bufferSizeInUse = size;
		}

		public void shrinkToFit() {
			if (this.bufferSizeInUse == this.buffer.size())
				return;
			final var oldBuffer = this.buffer;
			this.buffer = new GlBuffer();
			this.buffer.setDebugName(this.debugName);
			this.buffer.allocateImmutableStorage(this.bufferSizeInUse, GL45C.GL_DYNAMIC_STORAGE_BIT);
			oldBuffer.slice(0, this.bufferSizeInUse).copyTo(this.buffer.slice());
			if (oldBuffer != null)
				oldBuffer.close();
		}

		/**
		 * @return A slice covering the portion of the buffer that's currently in use.
		 */
		@Override
		public Slice slice() {
			return this.buffer.slice(0, this.bufferSizeInUse);
		}

		public long size() {
			return this.bufferSizeInUse;
		}

		public long capacity() {
			return this.buffer.size;
		}
	}

}
