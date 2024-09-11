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

	public final BufferLayoutSet asLayoutSet;
	private final long computedHash;

	private BufferLayout(ImmutableList<BuiltElement> elements, int byteStride) {
		this.elements = elements;
		this.byteStride = byteStride;

		final var hasher = new FastHasher();
		hasher.appendInt(this.byteStride);
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
		return hashToInt();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof BufferLayout other) {
			return this.byteStride == other.byteStride
					&& this.elements.equals(other.elements);
		}
		return false;
	}

	public static Builder builder() {
		return new Builder();
	}

	public interface LayoutFinalizer {
		BufferLayout finalizeElements(MutableList<ElementWithUsage> builder);
	}

	public static LayoutFinalizer alignedFinalizer(int alignment) {
		return elements -> {
			int currentOffset = 0;
			final var dst = new Vector<BuiltElement>();
			for (int i = 0; i < elements.size(); ++i) {
				final var elem = elements.get(i);
				dst.push(new BuiltElement(elem.attrib, currentOffset, elem.element));
				currentOffset += elem.element.byteSize;
				currentOffset = Mth.roundToward(currentOffset, alignment);
			}
			dst.optimize();
			return new BufferLayout(dst, currentOffset);
		};
	}

	private static final int[] VECTOR_ALIGNMENT_TABLE = { 1, 2, 4, 4 };

	// this finalizer is written with the assumption that each element describes a
	// member of a structure
	public static final LayoutFinalizer FINALIZER_STD430 = elements -> {
		final var dst = new Vector<BuiltElement>();
		// OpenGL spec section 7.6.2.2 describes std140 and std 430

		int currentBaseOffset = 0, structureAlignment = 0;
		for (int i = 0; i < elements.size(); ++i) {
			final var elem = elements.get(i);
			final int fieldSize, fieldAlign;

			// i think the spec says that the field size for Nvec3 is 3*sizeof(N), but has
			// an alignment of 4*N. but since an array's layout sets the stride to be the
			// alignment if its element type, each vec3 in an array (and therefor a matrix),
			// each Nvec3 takes up 4*N bytes. This also seems to mean that packing a byte
			// after a vec3 is theoretically possible.
			if (elem.element.attribSlotCount == 1) {
				// scalar
				fieldAlign = elem.element.type.byteSize * VECTOR_ALIGNMENT_TABLE[elem.element.componentCount - 1];
				fieldSize = elem.element.type.byteSize * elem.element.componentCount;
			} else {
				// matrix (treated as an array of vectors, and as such needs special handling)
				//
				// std140 would require that we round up the align of each array element to the
				// alignment of a vec4, but we dont have to do it here.
				fieldAlign = elem.element.type.byteSize * VECTOR_ALIGNMENT_TABLE[elem.element.componentCount - 1];
				fieldSize = fieldAlign * elem.element.attribSlotCount;
			}

			structureAlignment = Math.max(structureAlignment, fieldAlign);
			final var alignedOffset = Mth.roundToward(currentBaseOffset, fieldAlign);
			currentBaseOffset = alignedOffset + fieldSize;
			dst.push(new BuiltElement(elem.attrib, alignedOffset, elem.element));
		}
		final var stride = Mth.roundToward(currentBaseOffset, structureAlignment);
		dst.optimize();
		return new BufferLayout(dst, stride);
	};

	public static final class ElementWithUsage {
		public final Element element;
		public final Attribute attrib;

		public ElementWithUsage(Element element, Attribute attrib) {
			this.element = element;
			this.attrib = attrib;
		}
	}

	public static final class Builder {

		public final MutableList<ElementWithUsage> elements = new Vector<>();

		private Builder() {
		}

		public Builder element(Element element, Attribute usage) {
			this.elements.push(new ElementWithUsage(element, usage));
			return this;
		}

		public BufferLayout build() {
			return build(alignedFinalizer(4));
		}

		public BufferLayout build(LayoutFinalizer finalizer) {
			return finalizer.finalizeElements(this.elements);
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

	public static final class BuiltElement implements Hashable {
		// what sort of data this element represents.
		public final Attribute attribute;

		public final int componentCount;
		// the amount of attributes that this element takes up - usually 1, but may be
		// more for matrices. for example, for a mat4, this would be 4, and for a mat3,
		// this would be 3.
		public final int attribSlotCount;

		public final ComponentType type;
		public final int elementCount;
		public final int byteSize;
		public final int byteOffset;

		private final long computedHash;

		public BuiltElement(Attribute usage,
				int byteOffset, Element element) {
			this.attribute = usage;
			this.byteOffset = byteOffset;
			this.componentCount = element.componentCount;
			this.attribSlotCount = element.attribSlotCount;
			this.type = element.type;
			this.byteSize = element.byteSize;
			this.elementCount = this.componentCount * this.attribSlotCount;

			final var hasher = new FastHasher();
			hasher.appendInt(this.attribute.hashCode());
			hasher.appendInt(this.byteOffset);
			hasher.appendInt(this.componentCount);
			hasher.appendInt(this.attribSlotCount);
			hasher.append(this.type);
			hasher.appendInt(this.byteSize);
			this.computedHash = hasher.currentHash();
		}

		@Override
		public String toString() {
			return String.format(
					"\"%s\" [components=%d, type=%s, size=%d, offset=%d]",
					this.attribute, this.componentCount, this.type, this.byteSize, this.byteOffset);
		}

		@Override
		public void appendHash(Hasher hasher) {
			hasher.appendLong(this.computedHash);
		}

		@Override
		public int hashCode() {
			return hashToInt();
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

		public final int byteSize;

		public Element(ComponentType type, int componentCount, int attribSlotCount) {
			this.type = type;
			this.componentCount = componentCount;
			this.attribSlotCount = attribSlotCount;

			this.byteSize = type.byteSize * componentCount * attribSlotCount;
		}
	}

	// @formatter:off
	// unnormalized integers (interpret directly as integers)
	public static final Element
			ELEMENT_BYTE1 = new Element(ComponentType.BYTE, 1, 1),
			ELEMENT_BYTE2 = new Element(ComponentType.BYTE, 2, 1),
			ELEMENT_BYTE3 = new Element(ComponentType.BYTE, 3, 1),
			ELEMENT_BYTE4 = new Element(ComponentType.BYTE, 4, 1),
			ELEMENT_UBYTE1 = new Element(ComponentType.UBYTE, 1, 1),
			ELEMENT_UBYTE2 = new Element(ComponentType.UBYTE, 2, 1),
			ELEMENT_UBYTE3 = new Element(ComponentType.UBYTE, 3, 1),
			ELEMENT_UBYTE4 = new Element(ComponentType.UBYTE, 4, 1),
			ELEMENT_SHORT1 = new Element(ComponentType.SHORT, 1, 1),
			ELEMENT_SHORT2 = new Element(ComponentType.SHORT, 2, 1),
			ELEMENT_SHORT3 = new Element(ComponentType.SHORT, 3, 1),
			ELEMENT_SHORT4 = new Element(ComponentType.SHORT, 4, 1),
			ELEMENT_USHORT1 = new Element(ComponentType.USHORT, 1, 1),
			ELEMENT_USHORT2 = new Element(ComponentType.USHORT, 2, 1),
			ELEMENT_USHORT3 = new Element(ComponentType.USHORT, 3, 1),
			ELEMENT_USHORT4 = new Element(ComponentType.USHORT, 4, 1),
			ELEMENT_INT1 = new Element(ComponentType.INT, 1, 1),
			ELEMENT_INT2 = new Element(ComponentType.INT, 2, 1),
			ELEMENT_INT3 = new Element(ComponentType.INT, 3, 1),
			ELEMENT_INT4 = new Element(ComponentType.INT, 4, 1),
			ELEMENT_UINT1 = new Element(ComponentType.UINT, 1, 1),
			ELEMENT_UINT2 = new Element(ComponentType.UINT, 2, 1),
			ELEMENT_UINT3 = new Element(ComponentType.UINT, 3, 1),
			ELEMENT_UINT4 = new Element(ComponentType.UINT, 4, 1);

	// normalized integers (convert to floats)
	public static final Element
			ELEMENT_FLOAT_BYTE_NORM1 = new Element(ComponentType.BYTE_NORM, 1, 1),
			ELEMENT_FLOAT_BYTE_NORM2 = new Element(ComponentType.BYTE_NORM, 2, 1),
			ELEMENT_FLOAT_BYTE_NORM3 = new Element(ComponentType.BYTE_NORM, 3, 1),
			ELEMENT_FLOAT_BYTE_NORM4 = new Element(ComponentType.BYTE_NORM, 4, 1),
			ELEMENT_FLOAT_UBYTE_NORM1 = new Element(ComponentType.UBYTE_NORM, 1, 1),
			ELEMENT_FLOAT_UBYTE_NORM2 = new Element(ComponentType.UBYTE_NORM, 2, 1),
			ELEMENT_FLOAT_UBYTE_NORM3 = new Element(ComponentType.UBYTE_NORM, 3, 1),
			ELEMENT_FLOAT_UBYTE_NORM4 = new Element(ComponentType.UBYTE_NORM, 4, 1),
			ELEMENT_FLOAT_SHORT_NORM1 = new Element(ComponentType.SHORT_NORM, 1, 1),
			ELEMENT_FLOAT_SHORT_NORM2 = new Element(ComponentType.SHORT_NORM, 2, 1),
			ELEMENT_FLOAT_SHORT_NORM3 = new Element(ComponentType.SHORT_NORM, 3, 1),
			ELEMENT_FLOAT_SHORT_NORM4 = new Element(ComponentType.SHORT_NORM, 4, 1),
			ELEMENT_FLOAT_USHORT_NORM1 = new Element(ComponentType.USHORT_NORM, 1, 1),
			ELEMENT_FLOAT_USHORT_NORM2 = new Element(ComponentType.USHORT_NORM, 2, 1),
			ELEMENT_FLOAT_USHORT_NORM3 = new Element(ComponentType.USHORT_NORM, 3, 1),
			ELEMENT_FLOAT_USHORT_NORM4 = new Element(ComponentType.USHORT_NORM, 4, 1);

	// unnormalized integers (convert to floats)
	public static final Element
			ELEMENT_FLOAT_BYTE1 = new Element(ComponentType.BYTE, 1, 1),
			ELEMENT_FLOAT_BYTE2 = new Element(ComponentType.BYTE, 2, 1),
			ELEMENT_FLOAT_BYTE3 = new Element(ComponentType.BYTE, 3, 1),
			ELEMENT_FLOAT_BYTE4 = new Element(ComponentType.BYTE, 4, 1),
			ELEMENT_FLOAT_UBYTE1 = new Element(ComponentType.UBYTE, 1, 1),
			ELEMENT_FLOAT_UBYTE2 = new Element(ComponentType.UBYTE, 2, 1),
			ELEMENT_FLOAT_UBYTE3 = new Element(ComponentType.UBYTE, 3, 1),
			ELEMENT_FLOAT_UBYTE4 = new Element(ComponentType.UBYTE, 4, 1),
			ELEMENT_FLOAT_SHORT1 = new Element(ComponentType.SHORT, 1, 1),
			ELEMENT_FLOAT_SHORT2 = new Element(ComponentType.SHORT, 2, 1),
			ELEMENT_FLOAT_SHORT3 = new Element(ComponentType.SHORT, 3, 1),
			ELEMENT_FLOAT_SHORT4 = new Element(ComponentType.SHORT, 4, 1),
			ELEMENT_FLOAT_USHORT1 = new Element(ComponentType.USHORT, 1, 1),
			ELEMENT_FLOAT_USHORT2 = new Element(ComponentType.USHORT, 2, 1),
			ELEMENT_FLOAT_USHORT3 = new Element(ComponentType.USHORT, 3, 1),
			ELEMENT_FLOAT_USHORT4 = new Element(ComponentType.USHORT, 4, 1),
			ELEMENT_FLOAT_INT1 = new Element(ComponentType.INT, 1, 1),
			ELEMENT_FLOAT_INT2 = new Element(ComponentType.INT, 2, 1),
			ELEMENT_FLOAT_INT3 = new Element(ComponentType.INT, 3, 1),
			ELEMENT_FLOAT_INT4 = new Element(ComponentType.INT, 4, 1),
			ELEMENT_FLOAT_UINT1 = new Element(ComponentType.UINT, 1, 1),
			ELEMENT_FLOAT_UINT2 = new Element(ComponentType.UINT, 2, 1),
			ELEMENT_FLOAT_UINT3 = new Element(ComponentType.UINT, 3, 1),
			ELEMENT_FLOAT_UINT4 = new Element(ComponentType.UINT, 4, 1);

	// floats
	public static final Element ELEMENT_FLOAT1 = new Element(ComponentType.FLOAT, 1, 1);
	public static final Element ELEMENT_FLOAT2 = new Element(ComponentType.FLOAT, 2, 1);
	public static final Element ELEMENT_FLOAT3 = new Element(ComponentType.FLOAT, 3, 1);
	public static final Element ELEMENT_FLOAT4 = new Element(ComponentType.FLOAT, 4, 1);

	// columnx x rows, mirroring GLSL
	public static final Element ELEMENT_MAT2X2 = new Element(ComponentType.FLOAT, 2, 2);
	public static final Element ELEMENT_MAT2X3 = new Element(ComponentType.FLOAT, 2, 3);
	public static final Element ELEMENT_MAT2X4 = new Element(ComponentType.FLOAT, 2, 4);
	public static final Element ELEMENT_MAT3X2 = new Element(ComponentType.FLOAT, 3, 2);
	public static final Element ELEMENT_MAT3X3 = new Element(ComponentType.FLOAT, 3, 3);
	public static final Element ELEMENT_MAT3X4 = new Element(ComponentType.FLOAT, 3, 4);
	public static final Element ELEMENT_MAT4X2 = new Element(ComponentType.FLOAT, 4, 2);
	public static final Element ELEMENT_MAT4X3 = new Element(ComponentType.FLOAT, 4, 3);
	public static final Element ELEMENT_MAT4X4 = new Element(ComponentType.FLOAT, 4, 4);
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
			.element(ELEMENT_FLOAT_USHORT_NORM2, Attribute.UV0)
			.element(ELEMENT_FLOAT_UBYTE_NORM4, Attribute.COLOR)
			.element(ELEMENT_FLOAT3, Attribute.NORMAL)
			.build();
	public static final BufferLayout POSITION_TEX = builder()
			.element(ELEMENT_FLOAT3, Attribute.POSITION)
			.element(ELEMENT_FLOAT_USHORT_NORM2, Attribute.UV0)
			.build();
	public static final BufferLayout POSITION_COLOR_TEX = builder()
			.element(ELEMENT_FLOAT3, Attribute.POSITION)
			.element(ELEMENT_FLOAT_UBYTE_NORM4, Attribute.COLOR)
			.element(ELEMENT_FLOAT_USHORT_NORM2, Attribute.UV0)
			.build();
	public static final BufferLayout POSITION_COLOR_TEX_LIGHTMAP = builder()
			.element(ELEMENT_FLOAT3, Attribute.POSITION)
			.element(ELEMENT_FLOAT_UBYTE_NORM4, Attribute.COLOR)
			.element(ELEMENT_FLOAT_USHORT_NORM2, Attribute.UV0)
			.element(ELEMENT_SHORT2, Attribute.UV2)
			.build();

}
