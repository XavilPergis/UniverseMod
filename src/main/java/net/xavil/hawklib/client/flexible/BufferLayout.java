package net.xavil.hawklib.client.flexible;

import net.minecraft.util.Mth;
import net.xavil.hawklib.client.gl.ComponentType;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

public final class BufferLayout implements Hashable {

	public final ImmutableList<BuiltElement> elements;
	public final int byteStride;
	public final int totalAttribCount;

	public final BufferLayoutSet asLayoutSet;
	private final long computedHash;

	private BufferLayout(ImmutableList<BuiltElement> elements, int byteStride, int totalAttribCount) {
		this.elements = elements;
		this.byteStride = byteStride;
		this.totalAttribCount = totalAttribCount;

		final var hasher = new FastHasher();
		hasher.appendInt(this.byteStride);
		hasher.appendInt(this.totalAttribCount);
		for (int i = 0; i < this.elements.size(); ++i)
			hasher.append(this.elements.get(i));
		this.computedHash = hasher.currentHash();

		this.asLayoutSet = BufferLayoutSet.fromSingle(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendLong(this.computedHash);
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof BufferLayout other) {
			return this.byteStride == other.byteStride
					&& this.totalAttribCount == other.totalAttribCount
					&& this.elements.equals(other.elements);
		}
		return false;
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

		public Builder element(Element element, Attribute usage) {
			this.elements.push(new BuiltElement(usage, this.currentOffset, this.currentAttribOffset, element));
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

	public static final class Attribute {
		public final String description;

		public Attribute(String description) {
			this.description = description;
		}

		public static final Attribute POSITION = new Attribute("Position");
		public static final Attribute NORMAL = new Attribute("Normal");
		public static final Attribute COLOR = new Attribute("Color");
		public static final Attribute UV0 = new Attribute("Texture Coordinate 0");
		public static final Attribute UV1 = new Attribute("Texture Coordinate 1");
		public static final Attribute UV2 = new Attribute("Texture Coordinate 2");
		public static final Attribute MODEL_MATRIX = new Attribute("Model Matrix");

		@Override
		public String toString() {
			return this.description;
		}

		@Override
		public boolean equals(Object obj) {
			return this == obj;
		}
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

	public static final class BuiltElement implements Hashable {
		// what sort of data this element represents.
		public final Attribute attribute;

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

		public final Element asElement;

		private final long computedHash;

		public BuiltElement(Attribute usage,
				int byteOffset, int attribSlotOffset,
				Element element) {
			this.attribute = usage;
			this.byteOffset = byteOffset;
			this.attribType = element.attribType;
			this.componentCount = element.componentCount;
			this.attribSlotCount = element.attribSlotCount;
			this.attribSlotOffset = attribSlotOffset;
			this.type = element.type;
			this.byteSize = element.byteSize;

			this.asElement = element;

			final var hasher = new FastHasher();
			hasher.appendInt(this.attribute.hashCode());
			hasher.appendInt(this.byteOffset);
			hasher.appendEnum(this.attribType);
			hasher.appendInt(this.componentCount);
			hasher.appendInt(this.attribSlotCount);
			hasher.appendInt(this.attribSlotOffset);
			hasher.appendEnum(this.type);
			hasher.appendInt(this.byteSize);
			this.computedHash = hasher.currentHash();
		}

		@Override
		public void appendHash(Hasher hasher) {
			hasher.appendLong(this.computedHash);
		}

		@Override
		public int hashCode() {
			return FastHasher.hashToInt(this);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof BuiltElement other) {
				return this.computedHash == other.computedHash
						&& this.attribute == other.attribute
						&& this.componentCount == other.componentCount
						&& this.attribSlotCount == other.attribSlotCount
						&& this.attribType == other.attribType
						&& this.attribSlotOffset == other.attribSlotOffset
						&& this.type == other.type
						&& this.byteSize == other.byteSize
						&& this.byteOffset == other.byteOffset;
			}
			return false;
		}

	}

	public static final class Element {

		public final ComponentType type;
		public final int componentCount;
		public final int attribSlotCount;
		public final AttributeType attribType;

		public final int byteSize;

		public Element(ComponentType type, int componentCount, int attribSlotCount, AttributeType attribType) {
			this.type = type;
			this.componentCount = componentCount;
			this.attribSlotCount = attribSlotCount;
			this.attribType = attribType;

			this.byteSize = type.byteSize * componentCount * attribSlotCount;
		}
	}

	// @formatter:off
	// unnormalized integers (interpret directly as integers)
	public static final Element
			ELEMENT_BYTE1 = new Element(ComponentType.BYTE, 1, 1, AttributeType.INT),
			ELEMENT_BYTE2 = new Element(ComponentType.BYTE, 2, 1, AttributeType.INT),
			ELEMENT_BYTE3 = new Element(ComponentType.BYTE, 3, 1, AttributeType.INT),
			ELEMENT_BYTE4 = new Element(ComponentType.BYTE, 4, 1, AttributeType.INT),
			ELEMENT_UBYTE1 = new Element(ComponentType.UBYTE, 1, 1, AttributeType.INT),
			ELEMENT_UBYTE2 = new Element(ComponentType.UBYTE, 2, 1, AttributeType.INT),
			ELEMENT_UBYTE3 = new Element(ComponentType.UBYTE, 3, 1, AttributeType.INT),
			ELEMENT_UBYTE4 = new Element(ComponentType.UBYTE, 4, 1, AttributeType.INT),
			ELEMENT_SHORT1 = new Element(ComponentType.SHORT, 1, 1, AttributeType.INT),
			ELEMENT_SHORT2 = new Element(ComponentType.SHORT, 2, 1, AttributeType.INT),
			ELEMENT_SHORT3 = new Element(ComponentType.SHORT, 3, 1, AttributeType.INT),
			ELEMENT_SHORT4 = new Element(ComponentType.SHORT, 4, 1, AttributeType.INT),
			ELEMENT_USHORT1 = new Element(ComponentType.USHORT, 1, 1, AttributeType.INT),
			ELEMENT_USHORT2 = new Element(ComponentType.USHORT, 2, 1, AttributeType.INT),
			ELEMENT_USHORT3 = new Element(ComponentType.USHORT, 3, 1, AttributeType.INT),
			ELEMENT_USHORT4 = new Element(ComponentType.USHORT, 4, 1, AttributeType.INT),
			ELEMENT_INT1 = new Element(ComponentType.INT, 1, 1, AttributeType.INT),
			ELEMENT_INT2 = new Element(ComponentType.INT, 2, 1, AttributeType.INT),
			ELEMENT_INT3 = new Element(ComponentType.INT, 3, 1, AttributeType.INT),
			ELEMENT_INT4 = new Element(ComponentType.INT, 4, 1, AttributeType.INT),
			ELEMENT_UINT1 = new Element(ComponentType.UINT, 1, 1, AttributeType.INT),
			ELEMENT_UINT2 = new Element(ComponentType.UINT, 2, 1, AttributeType.INT),
			ELEMENT_UINT3 = new Element(ComponentType.UINT, 3, 1, AttributeType.INT),
			ELEMENT_UINT4 = new Element(ComponentType.UINT, 4, 1, AttributeType.INT);

	// normalized integers (convert to floats)
	public static final Element
			ELEMENT_FLOAT_BYTE_NORM1 = new Element(ComponentType.BYTE_NORM, 1, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_BYTE_NORM2 = new Element(ComponentType.BYTE_NORM, 2, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_BYTE_NORM3 = new Element(ComponentType.BYTE_NORM, 3, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_BYTE_NORM4 = new Element(ComponentType.BYTE_NORM, 4, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UBYTE_NORM1 = new Element(ComponentType.UBYTE_NORM, 1, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UBYTE_NORM2 = new Element(ComponentType.UBYTE_NORM, 2, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UBYTE_NORM3 = new Element(ComponentType.UBYTE_NORM, 3, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UBYTE_NORM4 = new Element(ComponentType.UBYTE_NORM, 4, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_SHORT_NORM1 = new Element(ComponentType.SHORT_NORM, 1, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_SHORT_NORM2 = new Element(ComponentType.SHORT_NORM, 2, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_SHORT_NORM3 = new Element(ComponentType.SHORT_NORM, 3, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_SHORT_NORM4 = new Element(ComponentType.SHORT_NORM, 4, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_USHORT_NORM1 = new Element(ComponentType.USHORT_NORM, 1, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_USHORT_NORM2 = new Element(ComponentType.USHORT_NORM, 2, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_USHORT_NORM3 = new Element(ComponentType.USHORT_NORM, 3, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_USHORT_NORM4 = new Element(ComponentType.USHORT_NORM, 4, 1, AttributeType.FLOAT);

	// unnormalized integers (convert to floats)
	public static final Element
			ELEMENT_FLOAT_BYTE1 = new Element(ComponentType.BYTE, 1, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_BYTE2 = new Element(ComponentType.BYTE, 2, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_BYTE3 = new Element(ComponentType.BYTE, 3, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_BYTE4 = new Element(ComponentType.BYTE, 4, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UBYTE1 = new Element(ComponentType.UBYTE, 1, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UBYTE2 = new Element(ComponentType.UBYTE, 2, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UBYTE3 = new Element(ComponentType.UBYTE, 3, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UBYTE4 = new Element(ComponentType.UBYTE, 4, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_SHORT1 = new Element(ComponentType.SHORT, 1, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_SHORT2 = new Element(ComponentType.SHORT, 2, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_SHORT3 = new Element(ComponentType.SHORT, 3, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_SHORT4 = new Element(ComponentType.SHORT, 4, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_USHORT1 = new Element(ComponentType.USHORT, 1, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_USHORT2 = new Element(ComponentType.USHORT, 2, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_USHORT3 = new Element(ComponentType.USHORT, 3, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_USHORT4 = new Element(ComponentType.USHORT, 4, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_INT1 = new Element(ComponentType.INT, 1, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_INT2 = new Element(ComponentType.INT, 2, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_INT3 = new Element(ComponentType.INT, 3, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_INT4 = new Element(ComponentType.INT, 4, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UINT1 = new Element(ComponentType.UINT, 1, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UINT2 = new Element(ComponentType.UINT, 2, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UINT3 = new Element(ComponentType.UINT, 3, 1, AttributeType.FLOAT),
			ELEMENT_FLOAT_UINT4 = new Element(ComponentType.UINT, 4, 1, AttributeType.FLOAT);

	// floats
	public static final Element ELEMENT_FLOAT1 = new Element(ComponentType.FLOAT, 1, 1, AttributeType.FLOAT);
	public static final Element ELEMENT_FLOAT2 = new Element(ComponentType.FLOAT, 2, 1, AttributeType.FLOAT);
	public static final Element ELEMENT_FLOAT3 = new Element(ComponentType.FLOAT, 3, 1, AttributeType.FLOAT);
	public static final Element ELEMENT_FLOAT4 = new Element(ComponentType.FLOAT, 4, 1, AttributeType.FLOAT);

	public static final Element ELEMENT_MAT2 = new Element(ComponentType.FLOAT, 2, 2, AttributeType.FLOAT);
	public static final Element ELEMENT_MAT3 = new Element(ComponentType.FLOAT, 3, 3, AttributeType.FLOAT);
	public static final Element ELEMENT_MAT4 = new Element(ComponentType.FLOAT, 4, 4, AttributeType.FLOAT);
	// @formatter:on

	public static final BufferLayout POSITION = builder()
			.element(ELEMENT_FLOAT3, Attribute.POSITION)
			.build();
	public static final BufferLayout POSITION_COLOR = builder()
			.element(ELEMENT_FLOAT3, Attribute.POSITION)
			.element(ELEMENT_FLOAT_UBYTE_NORM4, Attribute.COLOR)
			.build();
	public static final BufferLayout POSITION_COLOR_NORMAL = builder()
			.element(ELEMENT_FLOAT3, Attribute.POSITION)
			.element(ELEMENT_FLOAT_UBYTE_NORM4, Attribute.COLOR)
			.element(ELEMENT_FLOAT3, Attribute.NORMAL)
			.build();
	public static final BufferLayout POSITION_TEX_COLOR_NORMAL = builder()
			.element(ELEMENT_FLOAT3, Attribute.POSITION)
			.element(ELEMENT_FLOAT_SHORT_NORM2, Attribute.UV0)
			.element(ELEMENT_FLOAT_UBYTE_NORM4, Attribute.COLOR)
			.element(ELEMENT_FLOAT3, Attribute.NORMAL)
			.build();
	public static final BufferLayout POSITION_TEX = builder()
			.element(ELEMENT_FLOAT3, Attribute.POSITION)
			.element(ELEMENT_FLOAT_SHORT_NORM2, Attribute.UV0)
			.build();
	public static final BufferLayout POSITION_COLOR_TEX = builder()
			.element(ELEMENT_FLOAT3, Attribute.POSITION)
			.element(ELEMENT_FLOAT_UBYTE_NORM4, Attribute.COLOR)
			.element(ELEMENT_FLOAT_SHORT_NORM2, Attribute.UV0)
			.build();
	public static final BufferLayout POSITION_COLOR_TEX_LIGHTMAP = builder()
			.element(ELEMENT_FLOAT3, Attribute.POSITION)
			.element(ELEMENT_FLOAT_UBYTE_NORM4, Attribute.COLOR)
			.element(ELEMENT_FLOAT_SHORT_NORM2, Attribute.UV0)
			.element(ELEMENT_SHORT2, Attribute.UV2)
			.build();

}
