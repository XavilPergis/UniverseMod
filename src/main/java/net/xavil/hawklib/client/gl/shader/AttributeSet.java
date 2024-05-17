package net.xavil.hawklib.client.gl.shader;

import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.ImmutableMap;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

public final class AttributeSet implements Hashable {

	public static final class Attribute implements Hashable {
		public final BufferLayout.AttributeType attribType;
		public final int componentCount;
		public final int attribSlotCount;

		private final long computedHash;

		public Attribute(BufferLayout.AttributeType attribType, int componentCount, int attribSlotCount) {
			this.attribType = attribType;
			this.componentCount = componentCount;
			this.attribSlotCount = attribSlotCount;

			final var hasher = FastHasher.create();
			hasher.appendInt(this.attribType.ordinal());
			hasher.appendInt(this.componentCount);
			hasher.appendInt(this.attribSlotCount);
			this.computedHash = hasher.currentHash();
		}

		@Override
		public int hashCode() {
			return FastHasher.hashToInt(this);
		}

		@Override
		public void appendHash(Hasher hasher) {
			hasher.appendLong(this.computedHash);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Attribute other) {
				return other.computedHash == this.computedHash;
			}
			return false;
		}

	}

	// @formatter:off
	public static final Attribute
			FLOAT1 = new Attribute(BufferLayout.AttributeType.FLOAT, 1, 1),
			FLOAT2 = new Attribute(BufferLayout.AttributeType.FLOAT, 2, 1),
			FLOAT3 = new Attribute(BufferLayout.AttributeType.FLOAT, 3, 1),
			FLOAT4 = new Attribute(BufferLayout.AttributeType.FLOAT, 4, 1),
			MAT2 = new Attribute(BufferLayout.AttributeType.FLOAT, 2, 2),
			MAT3 = new Attribute(BufferLayout.AttributeType.FLOAT, 3, 3),
			MAT4 = new Attribute(BufferLayout.AttributeType.FLOAT, 4, 4),
			INT1 = new Attribute(BufferLayout.AttributeType.INT, 1, 1),
			INT2 = new Attribute(BufferLayout.AttributeType.INT, 2, 1),
			INT3 = new Attribute(BufferLayout.AttributeType.INT, 3, 1),
			INT4 = new Attribute(BufferLayout.AttributeType.INT, 4, 1);
	// @formatter:on

	public static final AttributeSet EMPTY = builder().build();
	public static final AttributeSet POSITION = builder()
			.attrib("aPos", FLOAT3)
			.build();
	public static final AttributeSet POSITION_COLOR = builder()
			.attrib("aPos", FLOAT3)
			.attrib("aColor", FLOAT4)
			.build();
	public static final AttributeSet POSITION_COLOR_NORMAL = builder()
			.attrib("aPos", FLOAT3)
			.attrib("aColor", FLOAT4)
			.attrib("aNormal", FLOAT3)
			.build();
	public static final AttributeSet POSITION_TEX_COLOR_NORMAL = builder()
			.attrib("aPos", FLOAT3)
			.attrib("aTexCoord0", FLOAT2)
			.attrib("aColor", FLOAT4)
			.attrib("aNormal", FLOAT3)
			.build();
	public static final AttributeSet POSITION_TEX = builder()
			.attrib("aPos", FLOAT3)
			.attrib("aTexCoord0", FLOAT2)
			.build();
	public static final AttributeSet POSITION_COLOR_TEX = builder()
			.attrib("aPos", FLOAT3)
			.attrib("aColor", FLOAT4)
			.attrib("aTexCoord0", FLOAT2)
			.build();

	public static final class BuiltAttribute {
		public final String name;
		public final Attribute attrib;

		public BuiltAttribute(String name, Attribute attrib) {
			this.name = name;
			this.attrib = attrib;
		}
	}

	public final ImmutableList<BuiltAttribute> attributes;
	public final ImmutableMap<String, Attribute> attributeMappings;
	private final long computedHash;

	private AttributeSet(ImmutableList<BuiltAttribute> attributes) {
		this.attributes = attributes;

		final var mappings = MutableMap.<String, Attribute>hashMap();
		attributes.forEach(attrib -> mappings.insert(attrib.name, attrib.attrib));
		mappings.optimize();
		this.attributeMappings = mappings;

		final var hasher = FastHasher.create();
		hasher.appendInt(this.attributes.size());
		for (final var attrib : this.attributes.iterable()) {
			hasher.appendString(attrib.name);
			hasher.append(attrib.attrib);
		}
		this.computedHash = hasher.currentHashInt();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final MutableList<BuiltAttribute> attributes = new Vector<>();

		private Builder() {
		}

		public Builder attrib(String name, Attribute attrib) {
			this.attributes.push(new BuiltAttribute(name, attrib));
			return this;
		}

		public AttributeSet build() {
			this.attributes.optimize();
			return new AttributeSet(attributes);
		}
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendLong(this.computedHash);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AttributeSet other) {
			return other.computedHash == this.computedHash;
		}
		return false;
	}

}
