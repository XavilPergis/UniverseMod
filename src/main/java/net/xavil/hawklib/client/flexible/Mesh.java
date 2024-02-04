package net.xavil.hawklib.client.flexible;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.vertex.VertexFormat;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlBuffer;
import net.xavil.hawklib.client.gl.GlManager;
import net.xavil.hawklib.client.gl.GlVertexArray;
import net.xavil.hawklib.client.gl.shader.AttributeSet;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.ultraviolet.Mod;

public class Mesh implements Disposable {
	// multiple vertex buffers allow for deinterleaved attributes - important for
	// e.g. instanced rendering.
	// The "primary" vertex buffer generally contains per-vertex data, and the
	// secondary buffers contain per-instance data.
	private VertexBufferData primaryVertexData = null;
	private MutableList<VertexBufferData> secondaryVertexData = new Vector<>();

	private static final class VertexBufferData implements Disposable {
		public final GlBuffer buffer = new GlBuffer();
		public BufferLayout layout;
		public int instanceRate = 0;

		public int baseAttribIndex = -1;
		// public int bindingIndex = -1;

		public void clearVertexArrayState(GlVertexArray vertexArray) {
			if (this.layout == null)
				return;
			this.layout.clearVertexState(vertexArray, this.baseAttribIndex);
		}

		public void setupVertexArrayState(GlVertexArray vertexArray, BufferLayout layout, int baseAttribIndex,
				int bindingIndex) {
			this.baseAttribIndex = baseAttribIndex;
			// this.bindingIndex = bindingIndex;
			this.layout = layout;
			this.layout.setupVertexState(vertexArray, baseAttribIndex, bindingIndex, this.instanceRate);
			GL45C.glVertexArrayVertexBuffer(vertexArray.id, bindingIndex, this.buffer.id, 0, this.layout.byteStride);
		}

		@Override
		public void close() {
			this.buffer.close();
		}
	}

	private GlBuffer indexBuffer = null;
	private GlVertexArray vertexArray = null;

	private PrimitiveType primitiveType;
	private VertexFormat.IndexType indexType;
	private int indexCount = 0;
	// instancing is disabled by default
	private int instanceCount = -1;

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
		this.primaryVertexData.buffer.bufferData(buffer.vertexData, usage);

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
			if (this.indexBuffer == null)
				this.indexBuffer = new GlBuffer();
			this.indexBuffer.bufferData(buffer.indexData, usage);
			this.indexType = buffer.indexType;
			GL45C.glVertexArrayElementBuffer(this.vertexArray.id, this.indexBuffer.id);
		} else {
			final var sequentialIndexBuffer = buffer.primitiveType.getSequentialBuffer(buffer.indexCount);
			final var indexBuffer = GlBuffer.importFromAutoStorage(sequentialIndexBuffer);
			this.indexType = sequentialIndexBuffer.type();
			GL45C.glVertexArrayElementBuffer(this.vertexArray.id, indexBuffer.id);
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
		drawState.apply(GlManager.currentState());
		shader.bind();
		GlManager.bindVertexArray(this.vertexArray.id);
		if (this.instanceCount >= 0) {
			GL45C.glDrawElementsInstanced(this.primitiveType.gl, this.indexCount, this.indexType.asGLType, 0L,
					this.instanceCount);
		} else {
			GL45C.glDrawElements(this.primitiveType.gl, this.indexCount, this.indexType.asGLType, 0L);
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
		if (this.indexBuffer != null)
			this.indexBuffer.close();
		this.indexBuffer = null;
		if (this.vertexArray != null)
			this.vertexArray.close();
		this.vertexArray = null;
	}

}
