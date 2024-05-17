package net.xavil.hawklib.client.gl;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL45C;

import net.xavil.hawklib.client.flexible.BufferLayout;

public final class GlVertexArray extends GlObject {

	public GlVertexArray(int id, boolean owned) {
		super(ObjectType.VERTEX_ARRAY, id, owned);
	}

	public GlVertexArray() {
		super(ObjectType.VERTEX_ARRAY, GL45C.glCreateVertexArrays(), true);
	}

	public void enableAttribute(int attribIndex, boolean enabled) {
		if (enabled)
			GL45C.glEnableVertexArrayAttrib(this.id, attribIndex);
		else
			GL45C.glDisableVertexArrayAttrib(this.id, attribIndex);
	}

	public void bindVertexBuffer(GlBuffer.Slice buffer, int bindingIndex, int stride) {
		GL45C.glVertexArrayVertexBuffer(this.id, bindingIndex, buffer.buffer.id, buffer.offset, stride);
	}

	public void bindElementBuffer(@Nullable GlBuffer buffer) {
		GL45C.glVertexArrayElementBuffer(this.id, buffer == null ? 0 : buffer.id);
	}

	public void setupVertexState(BufferLayout layout, int baseAttribIndex, int bindingIndex, int instanceRate) {
		for (final var element : layout.elements.iterable()) {
			for (int i = 0; i < element.attribSlotCount; ++i) {
				final var attribIndex = baseAttribIndex + element.attribSlotOffset + i;
				enableAttribute(attribIndex, true);
				switch (element.attribType) {
					case FLOAT -> GL45C.glVertexArrayAttribFormat(this.id,
							attribIndex,
							element.componentCount, element.type.gl, element.isNormalized,
							element.byteOffset);
					case INT -> GL45C.glVertexArrayAttribIFormat(this.id,
							attribIndex,
							element.componentCount, element.type.gl,
							element.byteOffset);
				}
				GL45C.glVertexArrayAttribBinding(this.id, attribIndex, bindingIndex);
				GL45C.glVertexArrayBindingDivisor(this.id, attribIndex, instanceRate);
			}
		}
	}

	public void clearVertexState(BufferLayout layout, int baseAttribIndex) {
		for (final var element : layout.elements.iterable()) {
			for (int i = 0; i < element.attribSlotCount; ++i) {
				final var attribIndex = baseAttribIndex + element.attribSlotOffset + i;
				enableAttribute(attribIndex, false);
			}
		}
		bindElementBuffer(null);
	}

}
