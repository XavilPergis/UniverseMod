package net.xavil.hawklib.client.flexible.vertex;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL45C;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlFence;
import net.xavil.hawklib.collections.impl.Vector;

// growable vertex builder that emits directly into a staging buffer.
public final class VertexBuilder implements Disposable {

	public static final int FRAMES_IN_FLIGHT = 3;

	// non-coherent, use explicit flushing!
	private static final int STAGING_FLAGS = GL45C.GL_DYNAMIC_STORAGE_BIT
			// we need the read bit because we need to copy the old staging buffer to the
			// new one when we resize.
			| GL45C.GL_MAP_READ_BIT
			| GL45C.GL_MAP_WRITE_BIT
			| GL45C.GL_MAP_PERSISTENT_BIT
			| GL45C.GL_CLIENT_STORAGE_BIT;

	private static final int MAP_FLAGS = GL45C.GL_MAP_PERSISTENT_BIT
			| GL45C.GL_MAP_WRITE_BIT
			| GL45C.GL_MAP_UNSYNCHRONIZED_BIT
			| GL45C.GL_MAP_FLUSH_EXPLICIT_BIT;

	// the main GL buffer that holds all in flight data
	private GlBuffer stagingBuffer;

	private final ByteBuffer vertexScratchSpace = ByteBuffer.allocateDirect(1024);

	private final PerFrameData[] perFrameData;
	private int nextFrame = 0;

	private ByteBuffer buffer;
	private PerFrameData currentPfd;

	private final GlFence.Pool fencePool = new GlFence.Pool();
	private final VertexDispatcher.Generic genericDispatcher = new VertexDispatcher.Generic();

	// indices relative to per-frame data pointers
	private int startOffset;
	private int currentOffset;
	private int nextOffset; // cached
	private int totalVertexSize; // cached
	private int duplicationCount; // cached

	private VertexDispatcher currentDispatcher = null;
	private BufferLayout currentLayout = null;
	private PrimitiveType primitiveType = null;
	private int vertexCount = 0;

	// we cannot start drawing while a FilledBuffer refers to us!
	private FilledBuffer filledBuffer;

	private static final class PerFrameData {
		public ByteBuffer stagingBufferPointer;
		public GlBuffer.Slice stagingBufferSlice;
		private final Vector<GlFence> fences = new Vector<>();

		public void sync() {
			while (!this.fences.isEmpty()) {
				final var fence = this.fences.popOrThrow();
				fence.clientWaitSync();
				fence.close();
			}
		}
	}

	public VertexBuilder(int initialCapacityBytes) {
		this.perFrameData = new PerFrameData[FRAMES_IN_FLIGHT];
		for (int i = 0; i < this.perFrameData.length; ++i) {
			this.perFrameData[i] = new PerFrameData();
		}
		createStagingBuffer(initialCapacityBytes);
		advanceFrame();
	}

	@Override
	public void close() {
		if (this.stagingBuffer != null)
			this.stagingBuffer.close();
	}

	public int currentOffset() {
		return this.currentOffset;
	}

	public final boolean isBuilding() {
		return this.currentLayout != null;
	}

	private void ensureCapacity() {
		// this function is very hot, it's called each time a vertex attribute is
		// specified...
		if (this.nextOffset <= this.buffer.capacity())
			return;

		// reallocating the staging buffer is NOT cheap lol
		final int newCapacity;
		if (this.buffer.capacity() == 0) {
			newCapacity = 512000;
		} else {
			newCapacity = Mth.floor(this.buffer.capacity() * 1.5);
		}
		createStagingBuffer(newCapacity);
	}

	private void createStagingBuffer(int capacity) {
		final var stagingBuffer = new GlBuffer();
		stagingBuffer.allocateImmutableStorage(this.perFrameData.length * capacity, STAGING_FLAGS);
		stagingBuffer.map(MAP_FLAGS);

		if (this.stagingBuffer != null)
			this.stagingBuffer.slice().flush();

		// setup per-frame data to point to new staging buffer!!
		for (int i = 0; i < this.perFrameData.length; ++i) {
			final var pfd = this.perFrameData[i];

			// sync point so we dont clobber the staging buffer before the GPU is finished
			// using them.
			pfd.sync();

			final var newSlice = stagingBuffer.slice(i * capacity, capacity);
			if (pfd.stagingBufferSlice != null) {
				pfd.stagingBufferSlice.copyTo(newSlice);
			}

			pfd.stagingBufferSlice = newSlice;
			pfd.stagingBufferPointer = newSlice.mappedPointer();
		}

		if (this.stagingBuffer != null)
			this.stagingBuffer.close();

		this.stagingBuffer = stagingBuffer;
		if (this.currentPfd != null)
			this.buffer = this.currentPfd.stagingBufferPointer;
	}

	protected void endVertex() {
		try {
			final var vertexSize = this.currentLayout.byteStride;

			int writePos = this.currentOffset;
			for (int i = 0; i < this.duplicationCount; ++i) {
				this.buffer.put(writePos, this.vertexScratchSpace, 0, vertexSize);
				writePos += vertexSize;
			}

			this.currentOffset += this.totalVertexSize;
			this.nextOffset = this.currentOffset + this.totalVertexSize;
			this.vertexCount += this.duplicationCount;

			// ensure capacity *after* so that we have space to write the next vertex into.
			// This is okay to put here specifically because we can resize.
			ensureCapacity();
		} catch (Throwable t) {
			throwBuilderError("Caught exception in endVertex()", t);
		}
	}

	public final <T extends VertexDispatcher> T begin(T dispatcher, PrimitiveType mode, BufferLayout layout) {
		if (this.currentLayout != null) {
			throwBuilderError(String.format("VertexBuilder is already building!"), null);
		}
		dispatcher.setup(this, layout, this.vertexScratchSpace);
		this.currentDispatcher = dispatcher;
		this.currentLayout = layout;
		this.primitiveType = mode;
		this.duplicationCount = mode == null ? 1 : this.primitiveType.duplicationCount + 1;
		this.startOffset = Mth.roundToward(this.currentOffset, 4);
		this.nextOffset = this.currentOffset + this.totalVertexSize;
		this.totalVertexSize = this.currentLayout.byteStride * (this.duplicationCount);
		ensureCapacity();
		return dispatcher;
	}

	public final VertexDispatcher.Generic beginGeneric(PrimitiveType mode, BufferLayout layout) {
		return begin(this.genericDispatcher, mode, layout);
	}

	protected final FilledBuffer end() {
		Assert.isTrue(isBuilding());

		final var size = this.currentOffset - this.startOffset;
		final var bufferSlice = this.currentPfd.stagingBufferSlice.slice(this.startOffset, size);
		bufferSlice.flush();

		final Runnable syncPointEmitter = () -> {
			final var fence = this.fencePool.acquire();
			this.currentPfd.fences.push(fence);
			this.filledBuffer = null;
			fence.signalFence();
		};

		this.filledBuffer = new FilledBuffer(this,
				bufferSlice, this.vertexCount,
				this.currentLayout, this.primitiveType,
				syncPointEmitter);

		this.vertexCount = 0;
		this.currentDispatcher = null;
		this.currentLayout = null;
		this.primitiveType = null;

		return this.filledBuffer;
	}

	public void advanceFrame() {
		this.currentOffset = this.startOffset = 0;
		this.currentPfd = this.perFrameData[this.nextFrame];
		this.buffer = this.currentPfd.stagingBufferPointer;
		this.nextFrame = (this.nextFrame + 1) % this.perFrameData.length;

		this.currentPfd.sync();
	}

	private final RuntimeException throwBuilderError(String msg, Throwable cause) {
		var res = "Buffer Builder Error:\n";
		res += msg + "\n";
		if (this.currentLayout == null)
			res += "[***] VertexBuilder is not currently building!\n";
		res += String.format("Offset: %d\n", this.currentOffset);
		res += String.format("Capacity: %d\n", this.buffer.capacity());
		res += String.format("Limit: %d\n", this.buffer.limit());
		res += String.format("Primitive Type: %s\n", this.primitiveType);
		res += String.format("Buffer Layout: %s\n", this.currentLayout);

		if (cause == null) {
			throw new IllegalStateException(res);
		} else {
			throw new IllegalStateException(res, cause);
		}
	}

}
