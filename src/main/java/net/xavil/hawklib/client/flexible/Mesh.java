package net.xavil.hawklib.client.flexible;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.vertex.VertexFormat;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.flexible.vertex.FilledBuffer;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlBuffer.Slice;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.GlVertexArray;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.iterator.Iterator;

public class Mesh implements Disposable {

	private static final SequentialIndexBufferPool INDEX_BUFFER_POOL = new SequentialIndexBufferPool();

	// if this field is null, then an auto instance buffer will be used instead.
	private BufferInfo indexBuffer = null;
	private VertexFormat.IndexType indexType = null;

	// index count is used when a custom index buffer is bound, otherwise
	// vertexCount is used.
	private int indexCount = 0;
	private int vertexCount = 0;

	// instancing is disabled by default
	private int instanceCount = 1;

	private BufferLayoutSet layouts;
	private PrimitiveType primitiveType;
	private final Vector<BufferInfo> vertexBuffers = new Vector<>();
	private final Vector<BufferInfo> freeBuffers = new Vector<>();

	public Mesh() {
	}

	@Override
	public void close() {
		if (this.indexBuffer != null)
			this.indexBuffer.close();
		this.indexBuffer = null;
	}

	public void setLayout(BufferLayoutSet layouts) {
		this.layouts = layouts;

		// it'd be better to never have this mesh in a state where the free buffers and
		// in-use buffers both have the same infos in them, but that would require
		// copying the vertex buffer list, and i dont want to do that. An exception here
		// could get us into a fucked up state, but im not gonna worry since the only
		// thing we could really get is an OOM exception here, and it would have to be
		// thrown in between the extend and the clear.
		this.freeBuffers.reserveTotal(this.vertexBuffers.size());
		this.freeBuffers.extend(this.vertexBuffers);
		this.vertexBuffers.clear();

		this.vertexBuffers.extend(Iterator.repeat(null, this.layouts.size()));
	}

	public void setPrimitiveType(PrimitiveType type) {
		this.primitiveType = type;
	}

	public void setVertexCount(int vertexCount) {
		this.vertexCount = vertexCount;
	}

	public void setIndexCount(int indexCount) {
		this.indexCount = indexCount;
	}

	public void setupAndUpload(FilledBuffer buffer) {
		Assert.isTrue(buffer.isValid());
		setLayout(buffer.layout.asLayoutSet);
		setPrimitiveType(buffer.primitiveType);
		setVertexCount(buffer.vertexCount);
		uploadVertexBuffer(0, buffer);
		buffer.finishUsing();
	}

	/**
	 * Upload a staging buffer to device memory and attach it to this mesh.
	 * 
	 * @param bufferIndex The buffer slot to attach this buffer to.
	 * @param buffer      The buffer to source vertex data from.
	 */
	public void uploadVertexBuffer(int bufferIndex, FilledBuffer buffer) {
		Assert.isTrue(buffer.isValid());
		uploadVertexBuffer(bufferIndex, buffer.vertexData, buffer.layout);
	}

	/**
	 * Upload a staging buffer to device memory and attach it to this mesh. Note
	 * that this does no synchronozation with regards to the vertex data, so care
	 * must be taken not to clobber it if it comes from an unsynchronized buffer.
	 * 
	 * @param bufferIndex  The buffer slot to attach this buffer to.
	 * @param vertexData   The vertex data to upload.
	 * @param bufferLayout The format of the given vertex data.
	 */
	public void uploadVertexBuffer(int bufferIndex, GlBuffer.Slice vertexData, BufferLayout bufferLayout) {
		if (bufferIndex >= this.vertexBuffers.size())
			throw new IllegalArgumentException(String.format(
					"mesh upload error: buffer slot %d does not exist in mesh with %d buffer slots",
					bufferIndex, this.vertexBuffers.size()));

		// this is the only thing we use `bufferLayout` for, but perhaps its good to
		// force diligence for the caller :P
		if (!this.layouts.get(bufferIndex).equals(bufferLayout))
			throw new IllegalArgumentException(String.format(
					"mesh upload error: buffer slot %d does not match provided layout",
					bufferIndex));

		final var prevBinding = this.vertexBuffers.get(bufferIndex);
		final var info = pickBestBufferInfo(prevBinding, vertexData.size);

		// if already had something bound to this buffer slot, and we're changing the
		// buffer, then put the old buffer back into the free buffer pool.
		if (prevBinding != null && prevBinding != info) {
			this.vertexBuffers.set(bufferIndex, info);
			this.freeBuffers.push(prevBinding);
		}

		info.setContents(vertexData);
		this.vertexBuffers.set(bufferIndex, info);
	}

	private BufferInfo pickBestBufferInfo(@Nullable BufferInfo initialCandidate, long requiredSize) {
		// pick the buffer that has the closest capacity to the required size. There
		// could be situations where this leads to undesirable results, but we would
		// need a slightly more upfront API to deal with those.
		//
		// Essentially, you could have a situation where you have one very large buffer
		// that's free, and upload a tiny bit of data to it, and then want to upload a
		// large amount of data, but could no longer put it in the high-capacity buffer.
		// if we knew everything we wanted to bind upfront, we could allocate things
		// slightly more efficiently...
		BufferInfo bestCandidate = initialCandidate;
		long bestSizeDiff = bestCandidate == null ? Long.MAX_VALUE
				: Math.abs(requiredSize - initialCandidate.buffer.size());
		for (int i = 0; i < this.freeBuffers.size(); ++i) {
			final var candidate = this.freeBuffers.get(i);
			final var sizeDiff = Math.abs(requiredSize - candidate.buffer.size());
			if (bestCandidate == null || sizeDiff < bestSizeDiff) {
				bestCandidate = candidate;
				bestSizeDiff = sizeDiff;
			}
		}

		if (bestCandidate != null) {
			this.freeBuffers.remove(this.freeBuffers.indexOf(bestCandidate));
			return bestCandidate;
		} else {
			return new BufferInfo();
		}

	}

	/**
	 * Shrink all buffer capacities to their current fill sizes, and discard all
	 * cached free buffers. This is not needed if you create a mesh and never rebind
	 * anything to it.
	 */
	public void shrinkToFit() {
		this.vertexBuffers.forEach(BufferInfo::shrinkToFit);
		this.freeBuffers.forEach(BufferInfo::close);
		this.freeBuffers.clear();
	}

	public void uploadIndexBuffer(GlBuffer.Slice indexData, VertexFormat.IndexType indexType) {
		if (this.indexBuffer == null)
			this.indexBuffer = pickBestBufferInfo(null, indexData.size);
		this.indexBuffer.setContents(indexData);
		this.indexType = indexType;
	}

	public void clearIndexBuffer() {
		if (this.indexBuffer == null)
			return;
		this.freeBuffers.push(this.indexBuffer);
		this.indexBuffer = null;
	}

	public void draw(ShaderProgram shader, DrawState drawState) {
		// nothing to draw if we specify no instances ;p
		if (this.instanceCount < 1)
			return;

		GlBuffer indexBuffer = null;
		VertexFormat.IndexType indexType = null;
		int indexCount = 0;
		if (this.indexBuffer != null) {
			indexBuffer = this.indexBuffer.buffer;
			indexType = this.indexType;
			indexCount = this.indexCount;
		} else if (this.primitiveType.indexPattern != null) {
			indexBuffer = INDEX_BUFFER_POOL.getIndexBuffer(this.primitiveType, this.vertexCount);
			indexType = INDEX_BUFFER_POOL.getIndexType(this.primitiveType);
			indexCount = this.primitiveType.physicalIndexCount(this.vertexCount);
		}

		if (this.primitiveType.indexPattern != null && indexCount <= 0)
			return;

		for (int i = 0; i < this.vertexBuffers.size(); ++i) {
			if (this.vertexBuffers.get(i) == null)
				throw new IllegalStateException(String.format(
						"mesh render error: buffer slot %d was not bound!",
						i));
		}

		final var vao = GlVertexArray.cachedVertexArray(shader.attributeSet(), this.layouts);
		for (int i = 0; i < this.vertexBuffers.size(); ++i) {
			final var buffer = this.vertexBuffers.get(i).slice();
			final var layout = this.layouts.get(i);
			vao.bindVertexBuffer(buffer, i, layout.byteStride);
		}

		GlManager.pushState();
		drawState.apply();
		shader.bind();
		vao.bind();

		if (this.primitiveType.indexPattern == null) {
			vao.bindElementBuffer(null);
			// FIXME: why is this here.
			GlManager.enableProgramPointSize(true);
			if (this.instanceCount > 1) {
				GL45C.glDrawArraysInstanced(this.primitiveType.gl, 0, this.vertexCount, this.instanceCount);
			} else {
				GL45C.glDrawArrays(this.primitiveType.gl, 0, this.vertexCount);
			}
		} else {
			vao.bindElementBuffer(indexBuffer);
			if (this.instanceCount > 1) {
				GL45C.glDrawElementsInstanced(this.primitiveType.gl, indexCount, indexType.asGLType, 0L,
						this.instanceCount);
			} else {
				GL45C.glDrawElements(this.primitiveType.gl, indexCount, indexType.asGLType, 0L);
			}
		}

		GlManager.popState();
	}

	private static final class BufferInfo implements Disposable {
		public GlBuffer buffer = new GlBuffer();
		public long bufferSizeInUse = 0;

		@Override
		public void close() {
			this.buffer.close();
		}

		public void setContents(Slice vertexData) {
			if (vertexData.size > this.buffer.size()) {
				if (this.buffer != null)
					this.buffer.close();
				this.buffer = new GlBuffer();
				this.buffer.allocateImmutableStorage(vertexData.size, GL45C.GL_DYNAMIC_STORAGE_BIT);
			}
			vertexData.copyTo(this.buffer.slice());
			this.bufferSizeInUse = vertexData.size;
		}

		public void shrinkToFit() {
			if (this.bufferSizeInUse == this.buffer.size())
				return;
			final var oldBuffer = this.buffer;
			this.buffer = new GlBuffer();
			this.buffer.allocateImmutableStorage(this.bufferSizeInUse, GL45C.GL_DYNAMIC_STORAGE_BIT);
			oldBuffer.slice(0, this.bufferSizeInUse).copyTo(this.buffer.slice());
			if (oldBuffer != null)
				oldBuffer.close();
		}

		public GlBuffer.Slice slice() {
			return this.buffer.slice(0, this.bufferSizeInUse);
		}
	}

	private static final class SequentialIndexBufferPool implements Disposable {
		private static final class Info implements Disposable {
			private final int[] indexPattern;

			private int currentIndexCount;
			private VertexFormat.IndexType currentIndexType;
			private GlBuffer currentBuffer;

			public Info(int[] indexPattern) {
				this.indexPattern = indexPattern;
			}

			@Override
			public void close() {
				if (this.currentBuffer != null)
					this.currentBuffer.close();
			}

			private static IntConsumer getIndexWriter(ByteBuffer buffer, VertexFormat.IndexType type) {
				return switch (type) {
					case BYTE -> index -> buffer.put((byte) index);
					case SHORT -> index -> buffer.putShort((short) index);
					case INT -> index -> buffer.putInt(index);
				};
			}

			private void updateBufferIfNeeded(PrimitiveType primitiveType, int vertexCount) {
				if (vertexCount <= this.currentIndexCount)
					return;

				final var indexCount = Mth.roundToward(2 * primitiveType.physicalIndexCount(vertexCount), 6);
				final var primitiveCount = indexCount / 6;
				final var indexType = VertexFormat.IndexType.least(indexCount);

				final var sharedIndexBuffer = new GlBuffer();
				sharedIndexBuffer.allocateImmutableStorage(indexCount * indexType.bytes, 0);

				try (final var stagingBuffer = new GlBuffer()) {
					stagingBuffer.allocateImmutableStorage(indexCount * indexType.bytes,
							GL45C.GL_MAP_PERSISTENT_BIT | GL45C.GL_MAP_WRITE_BIT | GL45C.GL_CLIENT_STORAGE_BIT);
					final var writer = getIndexWriter(
							stagingBuffer.map(GL45C.GL_MAP_PERSISTENT_BIT | GL45C.GL_MAP_WRITE_BIT), indexType);
					for (int i = 0; i < primitiveCount; ++i) {
						final var baseIndex = 4 * i;
						for (int j = 0; j < this.indexPattern.length; ++j)
							writer.accept(baseIndex + this.indexPattern[j]);
					}
					stagingBuffer.slice().copyTo(sharedIndexBuffer.slice(), indexCount);
				}

				this.currentIndexType = indexType;
				if (this.currentBuffer != null)
					this.currentBuffer.close();
				this.currentBuffer = sharedIndexBuffer;
				this.currentIndexCount = vertexCount;
			}

		}

		private final Info quadsInfo = new Info(new int[] { 0, 1, 2, 2, 3, 0 });
		private final Info linesInfo = new Info(new int[] { 0, 1, 2, 3, 2, 1 });

		@Override
		public void close() {
			this.quadsInfo.close();
			this.linesInfo.close();
		}

		private Info getInfo(PrimitiveType primitiveType) {
			return switch (primitiveType.indexPattern) {
				case QUADS -> this.quadsInfo;
				case LINES -> this.linesInfo;
			};
		}

		@SuppressWarnings("resource")
		public VertexFormat.IndexType getIndexType(PrimitiveType primitiveType) {
			return getInfo(primitiveType).currentIndexType;
		}

		public GlBuffer getIndexBuffer(PrimitiveType primitiveType, int vertexCount) {
			final var info = getInfo(primitiveType);
			info.updateBufferIfNeeded(primitiveType, vertexCount);
			return info.currentBuffer;
		}

	}

}
