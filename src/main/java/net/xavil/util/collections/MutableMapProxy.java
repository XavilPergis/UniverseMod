package net.xavil.util.collections;

import java.util.Map;
import java.util.function.BiPredicate;

import net.xavil.util.Option;
import net.xavil.util.collections.interfaces.MutableMap;
import net.xavil.util.iterator.Iterator;

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
	public Option<V> get(K key) {
		return this.wrapped.containsKey(key) ? Option.some(this.wrapped.get(key)) : Option.none();
	}

	@Override
	public Iterator<K> keys() {
		return Iterator.fromJava(this.wrapped.keySet().iterator());
	}

	@Override
	public Option<V> insert(K key, V value) {
		final Option<V> res = this.wrapped.containsKey(key) ? Option.some(this.wrapped.get(key)) : Option.none();
		this.wrapped.put(key, value);
		return res;
	}

	@Override
	public Option<V> remove(K key) {
		final Option<V> res = this.wrapped.containsKey(key) ? Option.some(this.wrapped.get(key)) : Option.none();
		this.wrapped.remove(key);
		return res;
	}

	@Override
	public void retain(BiPredicate<K, V> predicate) {
		this.wrapped.entrySet().removeIf(entry -> !predicate.test(entry.getKey(), entry.getValue()));
	}

}
