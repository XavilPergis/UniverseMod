package net.xavil.hawklib.client.flexible;

import java.util.Comparator;

import net.xavil.hawklib.client.flexible.BufferLayout.Attribute;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.ImmutableMap;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;
import net.xavil.hawklib.tuple.Tuple2;

public final class BufferLayoutSet implements Hashable {

	public static final BufferLayoutSet EMPTY = fromLayouts(ImmutableList.of());

	private static final MutableMap<BufferLayout, BufferLayoutSet> SINGLE_SET_CACHE = MutableMap.hashMap();

	public final ImmutableList<BufferLayout> layouts;
	public final ImmutableMap<BufferLayout.Attribute, AttributeSourceRef> attributeSources;
	private final long computedHash;

	public BufferLayoutSet(ImmutableList<BufferLayout> layouts,
			ImmutableMap<Attribute, AttributeSourceRef> attributeSources) {
		this.layouts = layouts;
		this.attributeSources = attributeSources;

		// iteration order for maps is definitely not consistent for different
		// instances, even if they have the same logical contents.
		final var sourceList = this.attributeSources.entries()
				.map(entry -> new Tuple2<>(entry.key, entry.getOrThrow()))
				.collectTo(Vector::new);
		sourceList.sort(Comparator.comparingInt(tuple -> tuple.a.hashCode()));

		final var hasher = new FastHasher();
		this.layouts.forEach(hasher::append);
		sourceList.forEach(hasher::append);
		this.computedHash = hasher.currentHash();
	}

	public static BufferLayoutSet fromLayouts(ImmutableList<BufferLayout> layouts) {
		if (layouts.size() == 0)
			return EMPTY;
		if (layouts.size() == 1 && SINGLE_SET_CACHE.containsKey(layouts.get(0)))
			return SINGLE_SET_CACHE.getOrThrow(layouts.get(0));

		final var attributeSources = MutableMap.<Attribute, AttributeSourceRef>hashMap();

		for (int i = 0; i < layouts.size(); ++i) {
			final var layout = layouts.get(i);
			for (int j = 0; j < layout.elements.size(); ++j) {
				final var element = layout.elements.get(j);
				final var sourceRef = new AttributeSourceRef(i, j);
				final var prev = attributeSources.insertAndGet(element.attribute, sourceRef);
				if (prev.isSome()) {
					throw new IllegalArgumentException(String.format(
							"automatic attribute source assignment failed: encountered duplicate attribute '{}' declared in buffer {} element {} and buffer {} element {}",
							element.attribute,
							prev.unwrap().bufferIndex, prev.unwrap().elementIndex,
							i, j));
				}
			}
		}

		final var res = new BufferLayoutSet(layouts, attributeSources);
		if (layouts.size() == 1)
			SINGLE_SET_CACHE.insert(layouts.get(0), res);
		return res;
	}

	public static BufferLayoutSet fromSingle(BufferLayout layout) {
		return SINGLE_SET_CACHE.entry(layout).orInsertWithKey(k -> fromLayouts(ImmutableList.of(layout)));
	}

	public int size() {
		return this.layouts.size();
	}

	public BufferLayout get(int bufferIndex) {
		return this.layouts.get(bufferIndex);
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
		if (obj instanceof BufferLayoutSet other) {
			return this.computedHash == other.computedHash
					&& this.layouts.equals(other.layouts)
					&& ImmutableMap.mapsEqual(this.attributeSources, other.attributeSources);
		}
		return false;
	}

	public String debugDescription() {
		String res = "";
		for (int i = 0; i < this.layouts.size(); ++i) {
			final var layout = this.layouts.get(i);
			res += String.format("\n- Buffer %d stride=%d", i, layout.byteStride);
			for (int j = 0; j < layout.elements.size(); ++j) {
				// find the vertex attributes provided by this element, if any. Our
				// representation (and probably OpenGL itself) allow multiple attributes to
				// refer to the same buffer elements, so we have to use a list here.
				final var associatedAttribs = new Vector<BufferLayout.Attribute>();
				for (final var attrib : this.attributeSources.keys().iterable()) {
					final var srcRef = this.attributeSources.getOrThrow(attrib);
					if (srcRef.bufferIndex == i && srcRef.elementIndex == j)
						associatedAttribs.push(attrib);
				}

				final var element = layout.elements.get(j);
				res += String.format("\n\t- %dx%d %s size=%d offset=%d",
						element.componentCount, element.attribSlotCount,
						element.type,
						element.byteSize, element.byteOffset);
				for (int k = 0; k < associatedAttribs.size(); ++k)
					res += String.format(" [%s]", associatedAttribs.get(k));
			}
		}
		return res;
	}

	public static final class AttributeSourceRef {
		// which buffer is this attribute sourced from?
		public final int bufferIndex;
		// which element inside the referenced buffer is the attribute sourced from?
		public final int elementIndex;

		public AttributeSourceRef(int bufferIndex, int elementIndex) {
			this.bufferIndex = bufferIndex;
			this.elementIndex = elementIndex;
		}
	}

}