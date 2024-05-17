package net.xavil.hawklib.client.gl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.systems.RenderSystem;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.client.gl.texture.GlTexture;

public class GlBuffer extends GlObject {

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
	private long size = -1;

	private ByteBuffer mapping;
	// the flags the current mapping was created with
	private int mappingFlags;

	// for immutable buffers
	// the flags the immutable buffer storage was created with
	private int storageFlags;

	public GlBuffer(int id, boolean owned) {
		super(ObjectType.BUFFER, id, owned);
	}

	public GlBuffer() {
		super(ObjectType.BUFFER, GL45C.glCreateBuffers(), true);
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
					"%s: tried to create immutable storage on a buffer whose storage is already immutable",
					debugDescription()));
		GL45C.glNamedBufferSubData(this.id, offset, buffer);
	}

	public static final class Slice {
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

		public Slice slice(long offset, long size) {
			if (offset + size > this.size)
				throw new IllegalArgumentException(String.format(
						"%s: buffer slice bounds error: slice size is %d, but new slice covers %d to %d",
						this.buffer.debugDescription(), this.size, offset, offset + size));
			return new Slice(this.buffer, this.offset + offset, size);
		}

		// this operation does not automatically issue a memory barrier
		public void copyTo(Slice other, int size) {
			GL45C.glCopyNamedBufferSubData(this.buffer.id, other.buffer.id, this.offset, other.offset, size);
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
	}

	public Slice slice(long offset, long length) {
		return new Slice(this, offset, length);
	}

	public Slice slice() {
		return new Slice(this, 0, this.size);
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

		this.mapping = GL45C.glMapNamedBufferRange(this.id, 0, this.size, flags);
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

	public static GlBuffer importFromAutoStorage(RenderSystem.AutoStorageIndexBuffer buffer) {
		final var res = new GlBuffer(buffer.name(), false);
		res.storageMutability = StorageMutability.MUTABLE;
		// FIXME: AutoStorageIndexBuffer stores all the info needed to reconstruct
		// buffer size
		// res.size = ???;
		return res;
	}

}
