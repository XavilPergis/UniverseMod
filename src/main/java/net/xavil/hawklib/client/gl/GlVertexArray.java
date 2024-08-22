package net.xavil.hawklib.client.gl;

import java.lang.ref.WeakReference;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;

import com.mojang.blaze3d.vertex.VertexFormat.IndexType;

import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferLayoutSet;
import net.xavil.hawklib.client.gl.shader.ShaderAttributeSet;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.ultraviolet.Mod;

public final class GlVertexArray extends GlObject {

	private static final MutableMap<ShaderAttributeSet, MutableMap<BufferLayoutSet, GlVertexArray>> VAO_CACHE = MutableMap
			.hashMap();

	private boolean isFrozen;
	private ShaderAttributeSet attribSet;
	private BufferLayoutSet layoutSet;

	private static final class BoundVertexBuffer {
		public WeakReference<GlBuffer> bufferRef;
		public long offset, size;
		public int stride;

		public void set(GlBuffer.Slice buffer, int stride) {
			this.bufferRef = new WeakReference<>(buffer.buffer);
			this.offset = buffer.offset;
			this.size = buffer.size;
			this.stride = stride;
		}
	}

	private BoundVertexBuffer[] boundVertexBuffers;
	private WeakReference<GlBuffer> boundElementBuffer;

	public GlVertexArray(int id, boolean owned) {
		super(ObjectType.VERTEX_ARRAY, id, owned);
	}

	public GlVertexArray() {
		super(ObjectType.VERTEX_ARRAY, GL45C.glCreateVertexArrays(), true);
	}

	private void assertUnfrozen() {
		if (this.isFrozen)
			throw new IllegalStateException(String.format(
					"{} may not have its format changed, as it is frozen.",
					debugDescription()));
	}

	public void bindVertexBuffer(GlBuffer.Slice buffer, int bindingIndex, int stride) {
		final var slot = this.boundVertexBuffers[bindingIndex];
		boolean shouldUpdate = false;
		shouldUpdate |= slot.bufferRef == null || !slot.bufferRef.refersTo(buffer.buffer);
		shouldUpdate |= slot.offset != buffer.offset;
		shouldUpdate |= slot.size != buffer.size;
		shouldUpdate |= slot.stride != stride;
		if (shouldUpdate) {
			GL45C.glVertexArrayVertexBuffer(this.id, bindingIndex, buffer.buffer.id, buffer.offset, stride);
			slot.set(buffer, stride);
		}
	}

	// this doesnt quite work like it should for me. i seem to need the index buffer
	// bound when using glDrawElements, instead of it using VAO state like i would
	// expect.
	public void bindElementBuffer(@Nullable GlBuffer buffer) {
		if (this.boundElementBuffer == null || !this.boundElementBuffer.refersTo(buffer)) {
			GL45C.glVertexArrayElementBuffer(this.id, buffer == null ? 0 : buffer.id);
			this.boundElementBuffer = new WeakReference<>(buffer);
		}
	}

	public void enableAttribute(int attribIndex, boolean enabled) {
		assertUnfrozen();
		if (enabled)
			GL45C.glEnableVertexArrayAttrib(this.id, attribIndex);
		else
			GL45C.glDisableVertexArrayAttrib(this.id, attribIndex);
	}

	private static boolean isBufferAlive(WeakReference<GlBuffer> buffer) {
		return buffer != null
				&& !buffer.refersTo(null)
				&& !buffer.get().isDestroyed();
	}

	public void verifyForDrawArrays(int elementCount, int instanceCount) {
		for (final var attrib : attribSet.attributes.iterable()) {
			final var attribSourceRef = layoutSet.attributeSources.getOrThrow(attrib.attrib);

			final var info = this.boundVertexBuffers[attribSourceRef.bufferIndex];
			final var bufRef = info.bufferRef;
			if (!isBufferAlive(bufRef))
				throw new IllegalStateException(String.format(
						"%s: vertex buffer in slot %d not bound for draw",
						debugDescription(), attribSourceRef.bufferIndex));

			final var buf = bufRef.get();
			if (attrib.instanceRate == ShaderAttributeSet.InstanceRate.PER_VERTEX) {
				final var bytesDrawn = elementCount * info.stride;
				if (buf.size() < bytesDrawn)
					throw new IllegalStateException(String.format(
							"%s: vertex buffer of size %d not big enough for draw arrays with vertex count of %d and stride %d (%d bytes total)",
							debugDescription(), buf.size(), elementCount, info.stride, bytesDrawn));
			} else {
				final var bytesDrawn = instanceCount * info.stride;
				if (buf.size() < bytesDrawn)
					throw new IllegalStateException(String.format(
							"%s: per-instance buffer of size %d not big enough for instanced draw arrays with instance count of %d and stride %d (%d bytes total)",
							debugDescription(), buf.size(), instanceCount, info.stride, bytesDrawn));
			}
		}
	}

	public void verifyForDrawElements(IndexType indexType,
			int elementCount, int instanceCount) {
		if (!isBufferAlive(this.boundElementBuffer))
			throw new IllegalStateException(String.format(
					"%s: element buffer not bound for indexed draw",
					debugDescription()));

		final var ebo = this.boundElementBuffer.get();
		final var bytesDrawn = elementCount * indexType.bytes;
		if (bytesDrawn > ebo.size())
			throw new IllegalStateException(String.format(
					"%s: element buffer of size %d not big enough for draw elements with element count of %d and stride %d (%d bytes total)",
					debugDescription(), ebo.size(), elementCount, indexType.bytes, bytesDrawn));

		for (final var attrib : attribSet.attributes.iterable()) {
			final var attribSourceRef = layoutSet.attributeSources.getOrThrow(attrib.attrib);

			final var info = this.boundVertexBuffers[attribSourceRef.bufferIndex];
			final var bufRef = info.bufferRef;
			if (!isBufferAlive(bufRef))
				throw new IllegalStateException(String.format(
						"%s: vertex buffer in slot %d not bound for draw",
						debugDescription(), attribSourceRef.bufferIndex));

			final var vbo = bufRef.get();
			if (attrib.instanceRate == ShaderAttributeSet.InstanceRate.PER_INSTANCE) {
				final var bytesDrawnVtx = instanceCount * info.stride;
				if (vbo.size() < bytesDrawnVtx)
					throw new IllegalStateException(String.format(
							"%s: per-instance buffer of size %d not big enough for instanced draw arrays with instance count of %d and stride %d (%d bytes total)",
							debugDescription(), vbo.size(), instanceCount, info.stride, bytesDrawnVtx));
			}
		}
	}

	public void setInstanceRate(int attribIndex, int instanceRate) {
		assertUnfrozen();
		GL45C.glVertexArrayBindingDivisor(this.id, attribIndex, instanceRate);
	}

	public void setInstanceRate(int attribIndex, ShaderAttributeSet.InstanceRate instanceRate) {
		setInstanceRate(attribIndex, switch (instanceRate) {
			case PER_VERTEX -> 0;
			case PER_INSTANCE -> 1;
		});
	}

	public void setAttribFormat(int attribIndex, int componentCount,
			ComponentType.InterpretAs attribType,
			ComponentType componentType, int byteOffset) {
		assertUnfrozen();
		switch (attribType) {
			case FLOAT -> GL45C.glVertexArrayAttribFormat(this.id,
					attribIndex,
					componentCount, componentType.gl,
					componentType.interpretAs == ComponentType.InterpretAs.FLOAT,
					byteOffset);
			case INT -> GL45C.glVertexArrayAttribIFormat(this.id,
					attribIndex,
					componentCount, componentType.gl,
					byteOffset);
		}
	}

	public void setAttribBinding(int attribIndex, int bindingIndex) {
		assertUnfrozen();
		GL45C.glVertexArrayAttribBinding(this.id, attribIndex, bindingIndex);
	}

	private static void compatCheckFailed(ShaderAttributeSet attribSet, BufferLayoutSet layoutSet) {
		Mod.LOGGER.error("Attribute set was incompatible with buffer layout set.");
		Mod.LOGGER.error("Attributes needed:");
		Mod.LOGGER.error("{}", attribSet.debugDescription());
		Mod.LOGGER.error("Attributes provided:");
		Mod.LOGGER.error("{}", layoutSet.debugDescription());
		throw new IllegalArgumentException("buffer layout set was incompatible with shader attribute set.");
	}

	private static boolean isCompatible(
			ShaderAttributeSet.BuiltAttribute attrib,
			BufferLayout.BuiltElement element) {
		boolean compatible = true;
		compatible &= attrib.attrib == element.attribute;
		compatible &= attrib.attribType.componentCount == element.componentCount;
		compatible &= attrib.attribType.attribSlotCount == element.attribSlotCount;
		return compatible;
	}

	public void setup(ShaderAttributeSet attribSet, BufferLayoutSet layoutSet) {
		assertUnfrozen();

		this.attribSet = attribSet;
		this.layoutSet = layoutSet;
		this.boundVertexBuffers = new BoundVertexBuffer[GlLimits.MAX_VERTEX_ATTRIBS];
		for (int i = 0; i < this.boundVertexBuffers.length; ++i)
			this.boundVertexBuffers[i] = new BoundVertexBuffer();

		for (final var attrib : attribSet.attributes.iterable()) {
			final var attribSourceRef = layoutSet.attributeSources.getOrThrow(attrib.attrib);

			final var sourceLayout = layoutSet.layouts.get(attribSourceRef.bufferIndex);
			final var sourceElement = sourceLayout.elements.get(attribSourceRef.elementIndex);

			if (!isCompatible(attrib, sourceElement))
				compatCheckFailed(attribSet, layoutSet);

			for (int i = 0; i < attrib.attribType.attribSlotCount; ++i) {
				final var attribIndex = attrib.attribIndex + i;
				enableAttribute(attribIndex, true);
				setAttribFormat(attribIndex,
						sourceElement.componentCount,
						attrib.attribType.interpretAs,
						sourceElement.type,
						sourceElement.byteOffset);
				setAttribBinding(attribIndex, attribSourceRef.bufferIndex);
				setInstanceRate(attribIndex, attrib.instanceRate);
			}
		}
	}

	/**
	 * Freeze or unfreeze this VAO. The VAO's layout may not be changed while
	 * frozen, but new buffers may be bound to existing buffer slots.
	 * 
	 * @param frozen The new frozen status of this VAO.
	 */
	public void setFrozen(boolean frozen) {
		this.isFrozen = frozen;
	}

	public static GlVertexArray cachedVertexArray(ShaderAttributeSet attribSet, BufferLayoutSet layoutSet) {
		final var layoutSets = VAO_CACHE.entry(attribSet).orInsertWith(MutableMap::hashMap);
		if (!layoutSets.containsKey(layoutSet)) {
			final var vao = new GlVertexArray();
			vao.setup(attribSet, layoutSet);
			vao.setFrozen(true);
			layoutSets.insert(layoutSet, vao);
			Mod.LOGGER.debug("Created VAO for attrib + buffer combination");
			Mod.LOGGER.debug("Attributes:");
			Mod.LOGGER.debug("{}", attribSet.debugDescription());
			Mod.LOGGER.debug("Buffer Layouts:");
			Mod.LOGGER.debug("{}", layoutSet.debugDescription());
		}
		return layoutSets.getOrThrow(layoutSet);
	}

	public void bind() {
		GlManager.bindVertexArray(this.id);
	}

	public String dump() {
		String res = debugDescription();
		res += String.format("Attributes:%s",
				this.attribSet != null ? this.attribSet.debugDescription() : "(null)");
		res += String.format("\nBuffer Layouts:%s",
				this.layoutSet != null ? this.layoutSet.debugDescription() : "(null)");
		final var ebo = this.boundElementBuffer != null ? this.boundElementBuffer.get() : null;
		if (ebo != null) {
			res += String.format("\nBound Element Buffer: %d size=%d", ebo.id, ebo.size());
		}
		for (int i = 0; i < this.boundVertexBuffers.length; ++i) {
			final var info = this.boundVertexBuffers[i];
			final var vbo = info.bufferRef != null ? info.bufferRef.get() : null;
			if (vbo != null) {
				res += String.format("\nBound Vertex Buffer[%d]: %d size=%d sliceOffset=%d sliceSize=%d stride=%d", i,
						vbo.id, vbo.size(), info.offset, info.size, info.stride);
			}
		}
		return res;
	}

}
