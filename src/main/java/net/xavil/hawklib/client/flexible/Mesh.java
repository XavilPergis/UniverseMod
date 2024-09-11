package net.xavil.hawklib.client.flexible;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.HawkLib;
import net.xavil.hawklib.client.flexible.vertex.FilledBuffer;
import net.xavil.hawklib.client.gl.BufferSliceable;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.GlObject;
import net.xavil.hawklib.client.gl.GlVertexArray;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.ultraviolet.Mod;

public class Mesh implements Disposable {

	public static final Mesh IMMEDIATE_SCRATCH = new Mesh();

	// if this field is null, then an auto index buffer will be used instead.
	private GlBuffer.GrowableBuffer indexBuffer = null;
	private VertexFormat.IndexType indexType = null;

	private int elementCount = 0;

	// instancing is disabled by default
	private int instanceCount = 1;

	private BufferLayoutSet layouts;
	private IndexPattern indexPattern;
	private final Vector<GlBuffer.GrowableBuffer> vertexBuffers = new Vector<>();
	private final MutableMap<String, GlBuffer.GrowableBuffer> ssboBuffers = MutableMap.hashMap();
	private final Vector<GlBuffer.GrowableBuffer> freeBuffers = new Vector<>();

	private String debugName;

	static {
		IMMEDIATE_SCRATCH.setDebugName("Immediate Scratch Mesh");
	}

	public static void draw(ShaderProgram shader, FilledBuffer buffer, DrawState drawState) {
		IMMEDIATE_SCRATCH.setupAndUpload(buffer);
		shader.setupDefaultShaderUniforms();
		IMMEDIATE_SCRATCH.draw(shader, drawState);
	}

	public Mesh() {
	}

	@Override
	public void close() {
		if (this.indexBuffer != null)
			this.indexBuffer.close();
		this.indexBuffer = null;
		this.vertexBuffers.iter().filterNull().forEach(GlBuffer.GrowableBuffer::close);
		this.freeBuffers.forEach(GlBuffer.GrowableBuffer::close);
		this.vertexBuffers.clear();
		this.freeBuffers.clear();
	}

	public void setDebugName(String name) {
		this.debugName = name;
		this.vertexBuffers.forEach(buffer -> buffer.setDebugName(this.debugName));
		this.freeBuffers.forEach(buffer -> buffer.setDebugName(this.debugName));
	}

	/**
	 * Sets the layout of this mesh, i.e., which buffers must be bound for this mesh
	 * to be drawn. This includes whether the mesh uses an index buffer or not.
	 * 
	 * @param layouts   The layout of each buffer of the mesh.
	 * @param indexType The type of index data, or null if this mesh is not indexed.
	 */
	public void setLayout(BufferLayoutSet layouts, @Nullable VertexFormat.IndexType indexType) {
		this.layouts = layouts;
		this.indexType = indexType;

		if (indexType == null && this.indexBuffer != null) {
			final var oldBuffer = this.indexBuffer;
			this.indexBuffer = null;
			this.freeBuffers.push(oldBuffer);
		}

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

	public void setIndexPattern(IndexPattern indexPattern) {
		this.indexPattern = indexPattern;
	}

	public void setElementCount(int elementCount) {
		this.elementCount = elementCount;
	}

	public void setInstanceCount(int instanceCount) {
		this.instanceCount = instanceCount;
	}

	public void setupAndUpload(FilledBuffer buffer) {
		Assert.isTrue(buffer.isValid());
		setLayout(buffer.layout.asLayoutSet, null);
		setIndexPattern(buffer.indexPattern);
		setElementCount(buffer.vertexCount);
		uploadVertexBuffer(0, buffer);
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
		buffer.finishUsing();
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
		if (!this.layouts.get(bufferIndex).equals(bufferLayout)) {
			Mod.LOGGER.error("Buffer slot {} does not match provided layout");
			Mod.LOGGER.error("Expected Layout:\n{}", this.layouts.get(bufferIndex));
			Mod.LOGGER.error("Provided Layout:\n{}", bufferLayout);
			throw new IllegalArgumentException(String.format(
					"mesh upload error: buffer slot %d does not match provided layout",
					bufferIndex));
		}

		final var prevBinding = this.vertexBuffers.get(bufferIndex);
		final var info = pickBestBuffer(prevBinding, vertexData.size);
		info.setContents(vertexData);
		this.vertexBuffers.set(bufferIndex, info);

		// if already had something bound to this buffer slot, and we're changing the
		// buffer, then put the old buffer back into the free buffer pool.
		if (prevBinding != null && prevBinding != info)
			this.freeBuffers.push(prevBinding);
	}

	public void uploadSsbo(String ssboName, FilledBuffer buffer) {
		Assert.isTrue(buffer.isValid());
		uploadSsbo(ssboName, buffer.vertexData);
		buffer.finishUsing();
	}

	public void uploadSsbo(String ssboName, GlBuffer.Slice bufferData) {
		final var prevBinding = this.ssboBuffers.getOrNull(ssboName);
		final var info = pickBestBuffer(prevBinding, bufferData.size);
		info.setContents(bufferData);
		this.ssboBuffers.insert(ssboName, info);

		if (prevBinding != null && prevBinding != info)
			this.freeBuffers.push(prevBinding);
	}

	private GlBuffer.GrowableBuffer pickBestBuffer(@Nullable GlBuffer.GrowableBuffer initialCandidate,
			long requiredSize) {
		// TODO: we could even try to pack stuff into a single buffer,,,

		// pick the buffer that has the closest capacity to the required size. There
		// could be situations where this leads to undesirable results, but we would
		// need a slightly more upfront API to deal with those.
		//
		// Essentially, you could have a situation where you have one very large buffer
		// that's free, and upload a tiny bit of data to it, and then want to upload a
		// large amount of data, but could no longer put it in the high-capacity buffer.
		// if we knew everything we wanted to bind upfront, we could allocate things
		// slightly more efficiently...
		GlBuffer.GrowableBuffer bestCandidate = initialCandidate;
		long bestSizeDiff = bestCandidate == null ? Long.MAX_VALUE
				: Math.abs(requiredSize - bestCandidate.capacity());
		for (int i = 0; i < this.freeBuffers.size(); ++i) {
			final var candidate = this.freeBuffers.get(i);
			final var sizeDiff = Math.abs(requiredSize - candidate.capacity());
			if (bestCandidate == null || sizeDiff < bestSizeDiff) {
				bestCandidate = candidate;
				bestSizeDiff = sizeDiff;
			}
		}

		if (bestCandidate != null) {
			final var candidateIndex = this.freeBuffers.indexOf(bestCandidate);
			if (candidateIndex >= 0)
				this.freeBuffers.remove(candidateIndex);
			return bestCandidate;
		} else {
			return new GlBuffer.GrowableBuffer();
		}

	}

	/**
	 * Shrink all buffer capacities to their current fill sizes, and discard all
	 * cached free buffers. This is not needed if you create a mesh and never rebind
	 * anything to it.
	 */
	public void shrinkToFit() {
		if (this.indexBuffer != null)
			this.indexBuffer.shrinkToFit();
		this.vertexBuffers.forEach(GlBuffer.GrowableBuffer::shrinkToFit);
		this.freeBuffers.forEach(GlBuffer.GrowableBuffer::close);
		this.freeBuffers.clear();
	}

	public void uploadIndexBuffer(GlBuffer.Slice indexData, VertexFormat.IndexType indexType) {
		if (this.indexBuffer == null)
			this.indexBuffer = pickBestBuffer(null, indexData.size);
		this.indexBuffer.setContents(indexData);
		this.indexType = indexType;
	}

	public void clearIndexBuffer() {
		if (this.indexBuffer == null)
			return;
		final var oldBuffer = this.indexBuffer;
		this.indexBuffer = null;
		this.freeBuffers.push(oldBuffer);
	}

	public static final class IndexBuffer implements Disposable, BufferSliceable {
		public final GlBuffer indices;
		public final int indexCount;
		public final VertexFormat.IndexType indexType;

		public IndexBuffer(GlBuffer indices, int indexCount, IndexType indexType) {
			this.indices = indices;
			this.indexCount = indexCount;
			this.indexType = indexType;
		}

		@Override
		public GlBuffer.Slice slice() {
			return this.indices.slice(0, this.indexCount * this.indexType.bytes);
		}

		@Override
		public void close() {
			if (this.indices != null)
				this.indices.close();
		}
	}

	public void draw(ShaderProgram shader, DrawState drawState) {
		// nothing to draw if we specify no instances ;p
		if (this.instanceCount < 1)
			return;

		IndexBuffer indexBuffer = null;
		if (this.indexType != null) {
			indexBuffer = new IndexBuffer(this.indexBuffer.slice().buffer, this.elementCount, this.indexType);
		} else if (this.indexPattern.indexPool != null) {
			indexBuffer = this.indexPattern.getIndexBuffer(this.elementCount);
		}

		if (this.indexPattern.indexPool != null && (indexBuffer == null || indexBuffer.indexCount <= 0))
			return;

		if (this.indexType != null && this.indexBuffer == null) {
			throw new IllegalStateException(String.format(
					"mesh render error: index slot was not bound!"));
		}

		for (int i = 0; i < this.vertexBuffers.size(); ++i) {
			if (this.vertexBuffers.get(i) == null)
				throw new IllegalStateException(String.format(
						"mesh render error: buffer slot %d was not bound!",
						i));
		}

		final var vao = GlVertexArray.cachedVertexArray(shader.attributeSet(), this.layouts);

		GlObject.assertIsAlive(vao);
		GlObject.assertIsAlive(shader);

		GlManager.pushState();
		drawState.apply();

		for (final var name : this.ssboBuffers.keys().iterable()) {
			shader.setStorageBuffer(name, this.ssboBuffers.getOrThrow(name).slice());
		}

		shader.bind();
		vao.bind();

		for (int i = 0; i < this.vertexBuffers.size(); ++i) {
			final var buffer = this.vertexBuffers.get(i).slice();
			final var layout = this.layouts.get(i);
			GlObject.assertIsAlive(buffer.buffer);
			vao.bindVertexBuffer(buffer, i, layout.byteStride);
		}

		if (indexBuffer != null) {
			GlObject.assertIsAlive(indexBuffer.indices);
			vao.bindElementBuffer(indexBuffer.indices);
		} else {
			vao.bindElementBuffer(null);
		}

		if (indexBuffer == null) {
			// FIXME: why is this here.
			GlManager.enableProgramPointSize(true);
			// basic sanity check
			vao.verifyForDrawArrays(this.elementCount, this.instanceCount);
			if (this.instanceCount == 1) {
				GL45C.glDrawArrays(this.indexPattern.primitiveType.gl,
						0, this.elementCount);
			} else if (this.instanceCount > 1) {
				GL45C.glDrawArraysInstanced(this.indexPattern.primitiveType.gl,
						0, this.elementCount, this.instanceCount);
			}
		} else {
			// basic sanity check
			vao.verifyForDrawElements(indexBuffer.indexType, indexBuffer.indexCount, this.instanceCount);
			// im probably doing something wrong, but setting the vao's index buffer with
			// `glVertexArrayElementBuffer` segfaults but binding the index buffer here like
			// this works just fine.
			GlManager.bindBuffer(GlBuffer.Type.ELEMENT, indexBuffer.indices.id);
			if (this.instanceCount == 1) {
				GL45C.glDrawElements(this.indexPattern.primitiveType.gl, indexBuffer.indexCount,
						indexBuffer.indexType.asGLType, 0L);
			} else if (this.instanceCount > 1) {
				GL45C.glDrawElementsInstanced(this.indexPattern.primitiveType.gl, indexBuffer.indexCount,
						indexBuffer.indexType.asGLType, 0L, this.instanceCount);
			}
		}

		GlManager.popState();
	}

}
