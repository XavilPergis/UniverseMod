package net.xavil.hawklib.collections.impl.proxy;

import java.util.Map;

import javax.annotation.Nullable;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.interfaces.ImmutableMap;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.collections.iterator.Iterators;

public final class ImmutableMapProxy<K, V> implements ImmutableMap<K, V> {

	private final Map<K, V> wrapped;

	public ImmutableMapProxy(Map<K, V> wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public int size() {
		return this.wrapped.size();
	}

	@Override
	public Maybe<V> get(K key) {
		return this.wrapped.containsKey(key) ? Maybe.some(this.wrapped.get(key)) : Maybe.none();
	}

	@Override
	@Nullable
	public V getOrNull(K key) {
		return this.wrapped.get(key);
	}

	@Override
	public Iterator<K> keys() {
		return Iterators.fromJava(this.wrapped.keySet().iterator());
	}

}
