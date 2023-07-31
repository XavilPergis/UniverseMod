package net.xavil.hawklib.collections.interfaces;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.impl.proxy.MutableMultiMapProxy;

public interface MutableMultiMap<K, V> extends MutableCollection, ImmutableMultiMap<K, V> {

	boolean insert(K key, V value);

	boolean remove(K key, V value);

	Maybe<MutableSet<V>> remove(K key);

	@Override
	default MutableMultiMap<K, V> hint(CollectionHint hint) {
		return this;
	}

	static <K, V> MutableMultiMapProxy<K, V> hashMultiMap() {
		return new MutableMultiMapProxy<>(
				MutableMap.proxy(new Object2ObjectOpenHashMap<>()),
				() -> MutableSet.proxy(new ObjectOpenHashSet<>()));
	}
}
