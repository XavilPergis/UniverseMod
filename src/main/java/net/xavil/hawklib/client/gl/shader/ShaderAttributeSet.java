package net.xavil.hawklib.client.gl.shader;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.gl.GlLimits;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.ImmutableMap;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;
import net.xavil.ultraviolet.mixin.accessor.VertexFormatAccessor;

public final class ShaderAttributeSet implements Hashable {

	private static final MutableMap<VertexFormat, ShaderAttributeSet> VANILLA_CACHE = MutableMap.identityHashMap();

	// @formatter:off
	public static final AttributeType
			FLOAT1 = new AttributeType(BufferLayout.AttributeType.FLOAT, 1, 1),
			FLOAT2 = new AttributeType(BufferLayout.AttributeType.FLOAT, 2, 1),
			FLOAT3 = new AttributeType(BufferLayout.AttributeType.FLOAT, 3, 1),
			FLOAT4 = new AttributeType(BufferLayout.AttributeType.FLOAT, 4, 1),
			MAT2 = new AttributeType(BufferLayout.AttributeType.FLOAT, 2, 2),
			MAT3 = new AttributeType(BufferLayout.AttributeType.FLOAT, 3, 3),
			MAT4 = new AttributeType(BufferLayout.AttributeType.FLOAT, 4, 4),
			INT1 = new AttributeType(BufferLayout.AttributeType.INT, 1, 1),
			INT2 = new AttributeType(BufferLayout.AttributeType.INT, 2, 1),
			INT3 = new AttributeType(BufferLayout.AttributeType.INT, 3, 1),
			INT4 = new AttributeType(BufferLayout.AttributeType.INT, 4, 1);
	// @formatter:on

	public static final ShaderAttributeSet EMPTY = builder().build();
	public static final ShaderAttributeSet POSITION = builder()
			.attrib("aPos", BufferLayout.Attribute.POSITION, FLOAT3, InstanceRate.PER_VERTEX)
			.build();
	public static final ShaderAttributeSet POSITION_COLOR = builder()
			.attrib("aPos", BufferLayout.Attribute.POSITION, FLOAT3, InstanceRate.PER_VERTEX)
			.attrib("aColor", BufferLayout.Attribute.COLOR, FLOAT4, InstanceRate.PER_VERTEX)
			.build();
	public static final ShaderAttributeSet POSITION_COLOR_NORMAL = builder()
			.attrib("aPos", BufferLayout.Attribute.POSITION, FLOAT3, InstanceRate.PER_VERTEX)
			.attrib("aColor", BufferLayout.Attribute.COLOR, FLOAT4, InstanceRate.PER_VERTEX)
			.attrib("aNormal", BufferLayout.Attribute.NORMAL, FLOAT3, InstanceRate.PER_VERTEX)
			.build();
	public static final ShaderAttributeSet POSITION_TEX_COLOR_NORMAL = builder()
			.attrib("aPos", BufferLayout.Attribute.POSITION, FLOAT3, InstanceRate.PER_VERTEX)
			.attrib("aTexCoord0", BufferLayout.Attribute.UV0, FLOAT2, InstanceRate.PER_VERTEX)
			.attrib("aColor", BufferLayout.Attribute.COLOR, FLOAT4, InstanceRate.PER_VERTEX)
			.attrib("aNormal", BufferLayout.Attribute.NORMAL, FLOAT3, InstanceRate.PER_VERTEX)
			.build();
	public static final ShaderAttributeSet POSITION_TEX = builder()
			.attrib("aPos", BufferLayout.Attribute.POSITION, FLOAT3, InstanceRate.PER_VERTEX)
			.attrib("aTexCoord0", BufferLayout.Attribute.UV0, FLOAT2, InstanceRate.PER_VERTEX)
			.build();
	public static final ShaderAttributeSet POSITION_COLOR_TEX = builder()
			.attrib("aPos", BufferLayout.Attribute.POSITION, FLOAT3, InstanceRate.PER_VERTEX)
			.attrib("aColor", BufferLayout.Attribute.COLOR, FLOAT4, InstanceRate.PER_VERTEX)
			.attrib("aTexCoord0", BufferLayout.Attribute.UV0, FLOAT2, InstanceRate.PER_VERTEX)
			.build();

	// @formatter:off
	public static final ShaderAttributeSet VANILLA_BLIT_SCREEN = fromVanilla(DefaultVertexFormat.BLIT_SCREEN);
	public static final ShaderAttributeSet VANILLA_BLOCK = fromVanilla(DefaultVertexFormat.BLOCK);
	public static final ShaderAttributeSet VANILLA_NEW_ENTITY = fromVanilla(DefaultVertexFormat.NEW_ENTITY);
	public static final ShaderAttributeSet VANILLA_PARTICLE = fromVanilla(DefaultVertexFormat.PARTICLE);
	public static final ShaderAttributeSet VANILLA_POSITION = fromVanilla(DefaultVertexFormat.POSITION);
	public static final ShaderAttributeSet VANILLA_POSITION_COLOR = fromVanilla(DefaultVertexFormat.POSITION_COLOR);
	public static final ShaderAttributeSet VANILLA_POSITION_COLOR_NORMAL = fromVanilla(DefaultVertexFormat.POSITION_COLOR_NORMAL);
	public static final ShaderAttributeSet VANILLA_POSITION_COLOR_LIGHTMAP = fromVanilla(DefaultVertexFormat.POSITION_COLOR_LIGHTMAP);
	public static final ShaderAttributeSet VANILLA_POSITION_TEX = fromVanilla(DefaultVertexFormat.POSITION_TEX);
	public static final ShaderAttributeSet VANILLA_POSITION_COLOR_TEX = fromVanilla(DefaultVertexFormat.POSITION_COLOR_TEX);
	public static final ShaderAttributeSet VANILLA_POSITION_TEX_COLOR = fromVanilla(DefaultVertexFormat.POSITION_TEX_COLOR);
	public static final ShaderAttributeSet VANILLA_POSITION_COLOR_TEX_LIGHTMAP = fromVanilla(DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);
	public static final ShaderAttributeSet VANILLA_POSITION_TEX_LIGHTMAP_COLOR = fromVanilla(DefaultVertexFormat.POSITION_TEX_LIGHTMAP_COLOR);
	public static final ShaderAttributeSet VANILLA_POSITION_TEX_COLOR_NORMAL = fromVanilla(DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
	// @formatter:on

	public final ImmutableList<BuiltAttribute> attributes;
	public final ImmutableMap<String, BuiltAttribute> attributesByName;
	private final long computedHash;

	private ShaderAttributeSet(ImmutableList<BuiltAttribute> attributes) {
		this.attributes = attributes;

		final var mappings = MutableMap.<String, BuiltAttribute>hashMap();
		attributes.forEach(attrib -> mappings.insertAndGet(attrib.name, attrib));
		mappings.optimize();
		this.attributesByName = mappings;

		final var hasher = new FastHasher();
		hasher.appendInt(this.attributes.size());
		for (final var attrib : this.attributes.iterable()) {
			hasher.appendString(attrib.name);
			hasher.append(attrib.attribType);
			hasher.appendInt(attrib.attrib.hashCode());
		}
		this.computedHash = hasher.currentHashInt();
	}

	public static ShaderAttributeSet fromVanilla(VertexFormat format) {
		if (!VANILLA_CACHE.containsKey(format)) {
			VANILLA_CACHE.insert(format, new ShaderAttributeSet(buildAttribsFromVanilla(format)));
		}
		return VANILLA_CACHE.getOrThrow(format);
	}

	private static ImmutableList<BuiltAttribute> buildAttribsFromVanilla(VertexFormat format) {
		final var output = new Vector<BuiltAttribute>();

		// vanilla is a little bit weird with this, it numbers vertex attribs via
		// iterating over this map's keySet(). I have no fucking clue as to why they did
		// it like this.
		final var elementMapping = VertexFormatAccessor.getElementMapping(format);

		int i = 0;
		for (final var attribName : elementMapping.keys().iterable()) {
			final var elem = elementMapping.getOrThrow(attribName);
			// count up the slots even if we don't add the attribut to the output! This is
			// what vanilla does in ShaderInstance's constructor!
			final var attribIndex = i++;

			final var attribute = switch (elem.getUsage()) {
				case COLOR -> BufferLayout.Attribute.COLOR;
				case NORMAL -> BufferLayout.Attribute.NORMAL;
				case POSITION -> BufferLayout.Attribute.POSITION;
				case UV -> switch (elem.getIndex()) {
					case 0 -> BufferLayout.Attribute.UV0;
					case 1 -> BufferLayout.Attribute.UV1;
					case 2 -> BufferLayout.Attribute.UV2;
					default -> null;
				};
				default -> null;
			};

			final var attribType = switch (elem.getCount()) {
				case 1 -> FLOAT1;
				case 2 -> FLOAT2;
				case 3 -> FLOAT3;
				case 4 -> FLOAT4;
				default -> null;
			};

			// attribType being null is actually an error, but vanilla doesn't have anything
			// that would cause it to *be* null so im not gonna worry too much.
			// attribute being null is expected, though, for things like (unused) generic
			// vertex elements and padding.
			if (attribute == null || attribType == null)
				continue;

			output.push(new BuiltAttribute(attribName, attribIndex, attribute, attribType, InstanceRate.PER_VERTEX));
		}

		return output;
	}

	public static Builder builder() {
		return new Builder();
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
		if (obj instanceof ShaderAttributeSet other) {
			return other.computedHash == this.computedHash;
		}
		return false;
	}

	public static enum InstanceRate {
		PER_VERTEX, PER_INSTANCE;
	}

	public static final class BuiltAttribute {
		public final String name;
		public final int attribIndex;
		public final BufferLayout.Attribute attrib;
		public final AttributeType attribType;
		public final InstanceRate instanceRate;

		public BuiltAttribute(String name, int attribIndex, BufferLayout.Attribute attrib, AttributeType attribType,
				InstanceRate instanceRate) {
			this.name = name;
			this.attribIndex = attribIndex;
			this.attrib = attrib;
			this.attribType = attribType;
			this.instanceRate = instanceRate;
		}
	}

	public static final class AttributeType implements Hashable {
		public final BufferLayout.AttributeType attribType;
		public final int componentCount;
		public final int attribSlotCount;

		private final long computedHash;

		public AttributeType(BufferLayout.AttributeType attribType, int componentCount, int attribSlotCount) {
			this.attribType = attribType;
			this.componentCount = componentCount;
			this.attribSlotCount = attribSlotCount;

			final var hasher = new FastHasher();
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
			if (obj instanceof AttributeType other) {
				return other.computedHash == this.computedHash;
			}
			return false;
		}

	}

	public static final class Builder {
		private int currentAttribIndex = 0;
		private final MutableList<BuiltAttribute> attributes = new Vector<>();

		private Builder() {
		}

		public Builder attrib(String name, BufferLayout.Attribute attrib, AttributeType attribType,
				InstanceRate instanceRate) {
			this.attributes.push(new BuiltAttribute(name, this.currentAttribIndex, attrib, attribType, instanceRate));
			this.currentAttribIndex += attribType.attribSlotCount;
			return this;
		}

		public ShaderAttributeSet build() {
			if (this.currentAttribIndex > GlLimits.MAX_VERTEX_ATTRIBS)
				throw new IllegalArgumentException(String.format(
						"attrib slot usage of %d slots exceeds limit of %d.",
						this.currentAttribIndex, GlLimits.MAX_VERTEX_ATTRIBS));
			this.attributes.optimize();
			return new ShaderAttributeSet(attributes);
		}
	}

}
