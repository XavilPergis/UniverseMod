package net.xavil.hawklib.collections.impl.proxy;

import java.util.Map;
import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.iterator.Iterator;

public final class MutableMapProxy<K, V> implements MutableMap<K, V> {

	private final Map<K, V> wrapped;

	public MutableMapProxy(Map<K, V> wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void clear() {
		this.wrapped.clear();
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
		return Iterator.fromJava(this.wrapped.keySet().iterator());
	}

	@Override
	public Maybe<V> insert(K key, V value) {
		final Maybe<V> res = this.wrapped.containsKey(key) ? Maybe.some(this.wrapped.get(key)) : Maybe.none();
		this.wrapped.put(key, value);
		return res;
	}

	@Override
	public Maybe<V> remove(K key) {
		final Maybe<V> res = this.wrapped.containsKey(key) ? Maybe.some(this.wrapped.get(key)) : Maybe.none();
		this.wrapped.remove(key);
		return res;
	}

	@Override
	public void retain(BiPredicate<K, V> predicate) {
		this.wrapped.entrySet().removeIf(entry -> !predicate.test(entry.getKey(), entry.getValue()));
	}

}
