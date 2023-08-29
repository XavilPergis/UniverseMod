package net.xavil.hawklib.client.flexible;

import org.lwjgl.opengl.GL45C;

import net.minecraft.util.Mth;
import net.xavil.hawklib.client.gl.GlVertexArray;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.MutableList;

public final class BufferLayout {

	public final ImmutableList<BuiltElement> elements;
	public final int byteStride;
	public final int totalAttribCount;

	private BufferLayout(ImmutableList<BuiltElement> elements, int byteStride, int totalAttribCount) {
		this.elements = elements;
		this.byteStride = byteStride;
		this.totalAttribCount = totalAttribCount;
	}

	public void setupVertexState(GlVertexArray vertexArray, int baseAttribIndex, int bindingIndex, int instanceRate) {
		for (final var element : this.elements.iterable()) {
			for (int i = 0; i < element.attribSlotCount; ++i) {
				final var attribIndex = baseAttribIndex + element.attribSlotOffset + i;
				GL45C.glEnableVertexArrayAttrib(vertexArray.id, attribIndex);
				switch (element.attribType) {
					case FLOAT -> GL45C.glVertexArrayAttribFormat(vertexArray.id,
							attribIndex,
							element.componentCount, element.type.gl, element.isNormalized,
							element.byteOffset);
					case INT -> GL45C.glVertexArrayAttribIFormat(vertexArray.id,
							attribIndex,
							element.componentCount, element.type.gl,
							element.byteOffset);
				}
				GL45C.glVertexArrayAttribBinding(vertexArray.id, attribIndex, bindingIndex);	
				GL45C.glVertexArrayBindingDivisor(vertexArray.id, attribIndex, instanceRate);
			}
		}
	}

	public void clearVertexState(GlVertexArray vertexArray, int baseAttribIndex) {
		for (final var element : this.elements.iterable()) {
			for (int i = 0; i < element.attribSlotCount; ++i) {
				final var attribIndex = baseAttribIndex + element.attribSlotOffset + i;
				GL45C.glDisableVertexArrayAttrib(vertexArray.id, attribIndex);
			}
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private int alignment = 4;
		private int currentOffset = 0;
		private int currentAttribOffset = 0;
		private final MutableList<BuiltElement> elements = new Vector<>();

		private Builder() {
		}

		public Builder alignedTo(int alignment) {
			this.alignment = alignment;
			return this;
		}

		public Builder element(String name, Element element, Usage usage, int index) {
			this.elements.push(new BuiltElement(name, usage, index, this.currentOffset, this.currentAttribOffset, element));
			this.currentAttribOffset += element.attribSlotCount;
			this.currentOffset += element.byteSize;
			if (this.alignment > 0)
				this.currentOffset = Mth.roundToward(this.currentOffset, this.alignment);
			return this;
		}

		public BufferLayout build() {
			this.elements.optimize();
			return new BufferLayout(elements, this.currentOffset, this.currentAttribOffset);
		}
	}

	public enum ComponentType {
		FLOAT(GL45C.GL_FLOAT, 4, "Float"),
		INT(GL45C.GL_INT, 4, "Int"),
		UINT(GL45C.GL_UNSIGNED_INT, 4, "Unsigned Int"),
		SHORT(GL45C.GL_SHORT, 2, "Short"),
		USHORT(GL45C.GL_UNSIGNED_SHORT, 2, "Unsigned Short"),
		BYTE(GL45C.GL_BYTE, 1, "Byte"),
		UBYTE(GL45C.GL_UNSIGNED_BYTE, 1, "Unsigned Byte");

		public final int gl;
		public final int byteSize;
		public final String name;

		private ComponentType(int gl, int byteSize, String name) {
			this.gl = gl;
			this.byteSize = byteSize;
			this.name = name;
		}
	}

	public enum Usage {
		GENERIC,
		POSITION,
		NORMAL,
		UV,
		COLOR,
	}

	public enum AttributeType {
		/**
		 * Used for any attribute that has floating-point components. This means things
		 * such as "float", "vec4" and "mat4".
		 */
		FLOAT("Float"),
		/**
		 * Used for any attribute that has integer components. This means things such as
		 * "int" or "ivec4".
		 */
		INT("Int");

		public final String name;

		private AttributeType(String name) {
			this.name = name;
		}
	}

	public static final class BuiltElement {
		public final String name;

		// what sort of data this element represents.
		public final Usage usage;
		// does not correspond to any opengl state, this is for differentiating
		// different UV sets.
		public final int index;

		public final int componentCount;
		// the amount of attributes that this element takes up - usually 1, but may be
		// more for matrices. for example, for a mat4, this would be 4, and for a mat3,
		// this would be 3.
		public final int attribSlotCount;
		public final AttributeType attribType;
		public final int attribSlotOffset;

		public final ComponentType type;
		public final int byteSize;
		public final int byteOffset;
		public final boolean isNormalized;

		public final Element asElement;

		public BuiltElement(String name, Usage usage, int index,
				int byteOffset, int attribSlotOffset,
				Element element) {
			this.name = name;
			this.usage = usage;
			this.index = index;
			this.byteOffset = byteOffset;
			this.attribType = element.attribType;
			this.componentCount = element.componentCount;
			this.attribSlotCount = element.attribSlotCount;
			this.attribSlotOffset = attribSlotOffset;
			this.type = element.type;
			this.byteSize = element.byteSize;
			this.isNormalized = element.isNormalized;

			this.asElement = element;
		}
	}

	public static final class Element {

		public final ComponentType type;
		public final int componentCount;
		public final int attribSlotCount;
		public final AttributeType attribType;
		public final boolean isNormalized;

		public final int byteSize;

		public Element(ComponentType type, int componentCount, int attribSlotCount, AttributeType attribType,
				boolean isNormalized) {
			this.type = type;
			this.componentCount = componentCount;
			this.attribSlotCount = attribSlotCount;
			this.attribType = attribType;
			this.isNormalized = isNormalized;

			this.byteSize = type.byteSize * componentCount * attribSlotCount;
		}
	}

	// @formatter:off
	// unnormalized integers (interpret directly as integers)
	public static final Element
			ELEMENT_BYTE1 = new Element(ComponentType.BYTE, 1, 1, AttributeType.INT, false),
			ELEMENT_BYTE2 = new Element(ComponentType.BYTE, 2, 1, AttributeType.INT, false),
			ELEMENT_BYTE3 = new Element(ComponentType.BYTE, 3, 1, AttributeType.INT, false),
			ELEMENT_BYTE4 = new Element(ComponentType.BYTE, 4, 1, AttributeType.INT, false),
			ELEMENT_UBYTE1 = new Element(ComponentType.UBYTE, 1, 1, AttributeType.INT, false),
			ELEMENT_UBYTE2 = new Element(ComponentType.UBYTE, 2, 1, AttributeType.INT, false),
			ELEMENT_UBYTE3 = new Element(ComponentType.UBYTE, 3, 1, AttributeType.INT, false),
			ELEMENT_UBYTE4 = new Element(ComponentType.UBYTE, 4, 1, AttributeType.INT, false),
			ELEMENT_SHORT1 = new Element(ComponentType.SHORT, 1, 1, AttributeType.INT, false),
			ELEMENT_SHORT2 = new Element(ComponentType.SHORT, 2, 1, AttributeType.INT, false),
			ELEMENT_SHORT3 = new Element(ComponentType.SHORT, 3, 1, AttributeType.INT, false),
			ELEMENT_SHORT4 = new Element(ComponentType.SHORT, 4, 1, AttributeType.INT, false),
			ELEMENT_USHORT1 = new Element(ComponentType.USHORT, 1, 1, AttributeType.INT, false),
			ELEMENT_USHORT2 = new Element(ComponentType.USHORT, 2, 1, AttributeType.INT, false),
			ELEMENT_USHORT3 = new Element(ComponentType.USHORT, 3, 1, AttributeType.INT, false),
			ELEMENT_USHORT4 = new Element(ComponentType.USHORT, 4, 1, AttributeType.INT, false),
			ELEMENT_INT1 = new Element(ComponentType.INT, 1, 1, AttributeType.INT, false),
			ELEMENT_INT2 = new Element(ComponentType.INT, 2, 1, AttributeType.INT, false),
			ELEMENT_INT3 = new Element(ComponentType.INT, 3, 1, AttributeType.INT, false),
			ELEMENT_INT4 = new Element(ComponentType.INT, 4, 1, AttributeType.INT, false),
			ELEMENT_UINT1 = new Element(ComponentType.UINT, 1, 1, AttributeType.INT, false),
			ELEMENT_UINT2 = new Element(ComponentType.UINT, 2, 1, AttributeType.INT, false),
			ELEMENT_UINT3 = new Element(ComponentType.UINT, 3, 1, AttributeType.INT, false),
			ELEMENT_UINT4 = new Element(ComponentType.UINT, 4, 1, AttributeType.INT, false);

	// normalized integers (convert to floats)
	public static final Element
			ELEMENT_FLOAT_BYTE_NORM1 = new Element(ComponentType.BYTE, 1, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_BYTE_NORM2 = new Element(ComponentType.BYTE, 2, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_BYTE_NORM3 = new Element(ComponentType.BYTE, 3, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_BYTE_NORM4 = new Element(ComponentType.BYTE, 4, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_UBYTE_NORM1 = new Element(ComponentType.UBYTE, 1, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_UBYTE_NORM2 = new Element(ComponentType.UBYTE, 2, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_UBYTE_NORM3 = new Element(ComponentType.UBYTE, 3, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_UBYTE_NORM4 = new Element(ComponentType.UBYTE, 4, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_SHORT_NORM1 = new Element(ComponentType.SHORT, 1, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_SHORT_NORM2 = new Element(ComponentType.SHORT, 2, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_SHORT_NORM3 = new Element(ComponentType.SHORT, 3, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_SHORT_NORM4 = new Element(ComponentType.SHORT, 4, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_USHORT_NORM1 = new Element(ComponentType.USHORT, 1, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_USHORT_NORM2 = new Element(ComponentType.USHORT, 2, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_USHORT_NORM3 = new Element(ComponentType.USHORT, 3, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_USHORT_NORM4 = new Element(ComponentType.USHORT, 4, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_INT_NORM1 = new Element(ComponentType.INT, 1, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_INT_NORM2 = new Element(ComponentType.INT, 2, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_INT_NORM3 = new Element(ComponentType.INT, 3, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_INT_NORM4 = new Element(ComponentType.INT, 4, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_UINT_NORM1 = new Element(ComponentType.UINT, 1, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_UINT_NORM2 = new Element(ComponentType.UINT, 2, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_UINT_NORM3 = new Element(ComponentType.UINT, 3, 1, AttributeType.FLOAT, true),
			ELEMENT_FLOAT_UINT_NORM4 = new Element(ComponentType.UINT, 4, 1, AttributeType.FLOAT, true);

	// unnormalized integers (convert to floats)
	public static final Element
			ELEMENT_FLOAT_BYTE1 = new Element(ComponentType.BYTE, 1, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_BYTE2 = new Element(ComponentType.BYTE, 2, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_BYTE3 = new Element(ComponentType.BYTE, 3, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_BYTE4 = new Element(ComponentType.BYTE, 4, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_UBYTE1 = new Element(ComponentType.UBYTE, 1, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_UBYTE2 = new Element(ComponentType.UBYTE, 2, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_UBYTE3 = new Element(ComponentType.UBYTE, 3, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_UBYTE4 = new Element(ComponentType.UBYTE, 4, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_SHORT1 = new Element(ComponentType.SHORT, 1, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_SHORT2 = new Element(ComponentType.SHORT, 2, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_SHORT3 = new Element(ComponentType.SHORT, 3, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_SHORT4 = new Element(ComponentType.SHORT, 4, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_USHORT1 = new Element(ComponentType.USHORT, 1, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_USHORT2 = new Element(ComponentType.USHORT, 2, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_USHORT3 = new Element(ComponentType.USHORT, 3, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_USHORT4 = new Element(ComponentType.USHORT, 4, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_INT1 = new Element(ComponentType.INT, 1, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_INT2 = new Element(ComponentType.INT, 2, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_INT3 = new Element(ComponentType.INT, 3, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_INT4 = new Element(ComponentType.INT, 4, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_UINT1 = new Element(ComponentType.UINT, 1, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_UINT2 = new Element(ComponentType.UINT, 2, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_UINT3 = new Element(ComponentType.UINT, 3, 1, AttributeType.FLOAT, false),
			ELEMENT_FLOAT_UINT4 = new Element(ComponentType.UINT, 4, 1, AttributeType.FLOAT, false);

	// floats
	public static final Element ELEMENT_FLOAT1 = new Element(ComponentType.FLOAT, 1, 1, AttributeType.FLOAT, false);
	public static final Element ELEMENT_FLOAT2 = new Element(ComponentType.FLOAT, 2, 1, AttributeType.FLOAT, false);
	public static final Element ELEMENT_FLOAT3 = new Element(ComponentType.FLOAT, 3, 1, AttributeType.FLOAT, false);
	public static final Element ELEMENT_FLOAT4 = new Element(ComponentType.FLOAT, 4, 1, AttributeType.FLOAT, false);

	public static final Element ELEMENT_MAT2 = new Element(ComponentType.FLOAT, 2, 2, AttributeType.FLOAT, false);
	public static final Element ELEMENT_MAT3 = new Element(ComponentType.FLOAT, 3, 3, AttributeType.FLOAT, false);
	public static final Element ELEMENT_MAT4 = new Element(ComponentType.FLOAT, 4, 4, AttributeType.FLOAT, false);
	// @formatter:on

	public static final BufferLayout POSITION = builder()
			.element("aPos", ELEMENT_FLOAT3, Usage.POSITION, 0)
			.build();
	public static final BufferLayout POSITION_COLOR = builder()
			.element("aPos", ELEMENT_FLOAT3, Usage.POSITION, 0)
			.element("aColor", ELEMENT_FLOAT_UBYTE_NORM4, Usage.COLOR, 0)
			.build();
	public static final BufferLayout POSITION_COLOR_NORMAL = builder()
			.element("aPos", ELEMENT_FLOAT3, Usage.POSITION, 0)
			.element("aColor", ELEMENT_FLOAT_UBYTE_NORM4, Usage.COLOR, 0)
			.element("aNormal", ELEMENT_FLOAT3, Usage.NORMAL, 0)
			.build();
	public static final BufferLayout POSITION_TEX_COLOR_NORMAL = builder()
			.element("aPos", ELEMENT_FLOAT3, Usage.POSITION, 0)
			.element("aTexCoord0", ELEMENT_FLOAT_SHORT_NORM2, Usage.UV, 0)
			.element("aColor", ELEMENT_FLOAT_UBYTE_NORM4, Usage.COLOR, 0)
			.element("aNormal", ELEMENT_FLOAT3, Usage.NORMAL, 0)
			.build();
	public static final BufferLayout POSITION_TEX = builder()
			.element("aPos", ELEMENT_FLOAT3, Usage.POSITION, 0)
			.element("aTexCoord0", ELEMENT_FLOAT_SHORT_NORM2, Usage.UV, 0)
			.build();
	public static final BufferLayout POSITION_COLOR_TEX = builder()
			.element("aPos", ELEMENT_FLOAT3, Usage.POSITION, 0)
			.element("aColor", ELEMENT_FLOAT_UBYTE_NORM4, Usage.COLOR, 0)
			.element("aTexCoord0", ELEMENT_FLOAT_SHORT_NORM2, Usage.UV, 0)
			.build();

}
