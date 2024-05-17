package net.xavil.hawklib.client.flexible;

import org.lwjgl.opengl.GL45C;
import java.nio.ByteBuffer;

import com.mojang.blaze3d.vertex.VertexFormat;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.GlVertexArray;
import net.xavil.hawklib.client.gl.shader.AttributeSet;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.ultraviolet.Mod;

public class Mesh implements Disposable {

	private static final SequentialIndexBufferPool INDEX_BUFFER_POOL = new SequentialIndexBufferPool();

	// multiple vertex buffers allow for deinterleaved attributes - important for
	// e.g. instanced rendering.
	// The "primary" vertex buffer generally contains per-vertex data, and the
	// secondary buffers contain per-instance data.
	private VertexBufferData primaryVertexData = null;
	private MutableList<VertexBufferData> secondaryVertexData = new Vector<>();

	private boolean ownsIndexBuffer = false;
	private GlBuffer indexBuffer = null;
	private GlVertexArray vertexArray = null;

	private PrimitiveType primitiveType;
	private VertexFormat.IndexType indexType;
	private int indexCount = 0;
	// instancing is disabled by default
	private int instanceCount = -1;

	private static final class VertexBufferData implements Disposable {
		public final GlBuffer buffer = new GlBuffer();
		public BufferLayout layout;
		public int instanceRate = 0;

		public int baseAttribIndex = -1;
		// public int bindingIndex = -1;

		public void clearVertexArrayState(GlVertexArray vertexArray) {
			if (this.layout == null)
				return;
			vertexArray.clearVertexState(this.layout, this.baseAttribIndex);
		}

		public void setupVertexArrayState(GlVertexArray vertexArray, BufferLayout layout, int baseAttribIndex,
				int bindingIndex) {
			this.baseAttribIndex = baseAttribIndex;
			// this.bindingIndex = bindingIndex;
			this.layout = layout;
			vertexArray.setupVertexState(this.layout, baseAttribIndex, bindingIndex, this.instanceRate);
			vertexArray.bindVertexBuffer(this.buffer.slice(), bindingIndex, this.layout.byteStride);
		}

		@Override
		public void close() {
			this.buffer.close();
		}
	}

	private static final class SequentialIndexBufferPool implements Disposable {
		private static final class Info implements Disposable {
			private final int[] indexPattern;

			private int currentIndexCount;
			private VertexFormat.IndexType currentIndexType;
			private GlBuffer currentBuffer;
			// set of meshes that use the shared quads buffer. we could probably switch to
			// using vertex pulling and avoid even allocating this sort of buffer, but i
			// dont wanna do that right now lol
			private final MutableSet<Mesh> attachedMeshes = MutableSet.identityHashSet();

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

				for (final var mesh : this.attachedMeshes.iterable()) {
					mesh.indexType = indexType;
					mesh.indexBuffer = sharedIndexBuffer;
					mesh.vertexArray.bindElementBuffer(mesh.indexBuffer);
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

		public void removeMesh(Mesh mesh) {
			this.quadsInfo.attachedMeshes.remove(mesh);
			this.linesInfo.attachedMeshes.remove(mesh);
		}
	}

	private void createIfNeeded() {
		if (this.primaryVertexData == null) {
			this.primaryVertexData = new VertexBufferData();
			this.primaryVertexData.instanceRate = 0;
		}
		if (this.vertexArray == null)
			this.vertexArray = new GlVertexArray();
	}

	public void upload(VertexBuilder.BuiltBuffer buffer) {
		upload(buffer, GlBuffer.UsageHint.STATIC_DRAW);
	}

	private void resetVertexArray() {
		this.primaryVertexData.clearVertexArrayState(this.vertexArray);
		for (final var vd : this.secondaryVertexData.iterable())
			vd.clearVertexArrayState(this.vertexArray);
	}

	public void upload(VertexBuilder.BuiltBuffer buffer, GlBuffer.UsageHint usage) {
		createIfNeeded();

		this.primitiveType = buffer.primitiveType;
		this.indexCount = buffer.indexCount;

		buffer.vertexData.rewind();
		this.primaryVertexData.buffer.allocateMutableStorage(buffer.vertexData, usage);

		// we have to reset all attribs before
		resetVertexArray();

		setupIndexBuffer(buffer, usage);
		this.primaryVertexData.setupVertexArrayState(this.vertexArray, buffer.layout, 0, 0);

		// signals to the VertexBuilder that this buffer came from that it may reuse
		// this buffer's memory.
		buffer.close();
	}

	private void setupIndexBuffer(VertexBuilder.BuiltBuffer buffer, GlBuffer.UsageHint usage) {
		if (buffer.indexData != null) {
			if (this.ownsIndexBuffer && this.indexBuffer != null)
				this.indexBuffer.close();
			this.ownsIndexBuffer = true;
			this.indexBuffer = new GlBuffer();
			this.indexBuffer.allocateImmutableStorage(buffer.indexData, 0);
			this.indexType = buffer.indexType;
			this.vertexArray.bindElementBuffer(this.indexBuffer);
		} else {
			if (buffer.primitiveType.indexPattern != null) {
				if (this.ownsIndexBuffer && this.indexBuffer != null)
					this.indexBuffer.close();
				this.ownsIndexBuffer = false;
				this.indexBuffer = INDEX_BUFFER_POOL.getIndexBuffer(buffer.primitiveType, buffer.vertexCount);
				this.indexType = INDEX_BUFFER_POOL.getIndexType(buffer.primitiveType);
				this.vertexArray.bindElementBuffer(this.indexBuffer);
			} else {
				this.vertexArray.bindElementBuffer(null);
			}
		}
	}

	private void compatCheckFailed(ShaderProgram shader) {
		Mod.LOGGER.error("Mesh data was incompatible with attributes for shader '{}'", shader.debugDescription());
		Mod.LOGGER.info("Attribute set the shader was declared with:");
		for (final var attrib : shader.attributeSet().attributes.iterable()) {
			Mod.LOGGER.info("- '{}': {}x{} {}", attrib.name,
					attrib.attrib.componentCount, attrib.attrib.attribSlotCount, attrib.attrib.attribType.name);
		}
		Mod.LOGGER.info("Attributes given:");
		Mod.LOGGER.info("- Primary Vertex Data:");
		for (final var elem : this.primaryVertexData.layout.elements.iterable()) {
			Mod.LOGGER.info("  - '{}': {}x{} {}", elem.name,
					elem.componentCount, elem.attribSlotCount, elem.attribType.name);
		}
		for (final var vd : this.secondaryVertexData.iterable()) {
			Mod.LOGGER.info("- Secondary Vertex Data:");
			for (final var elem : vd.layout.elements.iterable()) {
				Mod.LOGGER.info("  - '{}': {}x{} {}", elem.name,
						elem.componentCount, elem.attribSlotCount,
						elem.attribType.name);
			}
		}
		throw new IllegalArgumentException("vertex data was incompatible with shader attributes.");
	}

	private boolean isCompatible(AttributeSet.BuiltAttribute attrib, BufferLayout.BuiltElement element) {
		boolean compatible = true;
		compatible &= attrib.name.equals(element.name);
		compatible &= attrib.attrib.attribType == element.attribType;
		compatible &= attrib.attrib.componentCount == element.componentCount;
		compatible &= attrib.attrib.attribSlotCount == element.attribSlotCount;
		return compatible;
	}

	private void assertCompatibleWith(ShaderProgram shader) {
		if (shader.attributeSet() == null)
			return;
		final var attribs = shader.attributeSet().attributes.iter();

		for (final var elem : this.primaryVertexData.layout.elements.iterable()) {
			if (!attribs.hasNext())
				return;
			if (!isCompatible(attribs.next(), elem))
				compatCheckFailed(shader);
		}
		for (final var vd : this.secondaryVertexData.iterable()) {
			for (final var elem : vd.layout.elements.iterable()) {
				if (!attribs.hasNext())
					return;
				if (!isCompatible(attribs.next(), elem))
					compatCheckFailed(shader);
			}
		}
	}

	public void draw(ShaderProgram shader, DrawState drawState) {
		if (this.indexCount <= 0)
			return;

		assertCompatibleWith(shader);

		GlManager.pushState();
		drawState.apply();
		shader.bind();
		GlManager.bindVertexArray(this.vertexArray.id);
		if (this.primitiveType.indexPattern == null) {
			GlManager.enableProgramPointSize(true);
			if (this.instanceCount >= 0) {
				GL45C.glDrawArraysInstanced(this.primitiveType.gl, 0, this.indexCount, this.instanceCount);
			} else {
				GL45C.glDrawArrays(this.primitiveType.gl, 0, this.indexCount);
			}
		} else {
			if (this.instanceCount >= 0) {
				GL45C.glDrawElementsInstanced(this.primitiveType.gl, this.indexCount, this.indexType.asGLType, 0L,
						this.instanceCount);
			} else {
				GL45C.glDrawElements(this.primitiveType.gl, this.indexCount, this.indexType.asGLType, 0L);
			}
		}
		GlManager.popState();
	}

	@Override
	public void close() {
		if (this.primaryVertexData != null)
			this.primaryVertexData.close();
		this.primaryVertexData = null;
		this.secondaryVertexData.forEach(VertexBufferData::close);
		this.secondaryVertexData.clear();
		if (this.ownsIndexBuffer && this.indexBuffer != null)
			this.indexBuffer.close();
		this.indexBuffer = null;
		if (this.vertexArray != null)
			this.vertexArray.close();
		this.vertexArray = null;
		INDEX_BUFFER_POOL.removeMesh(this);
	}

}
