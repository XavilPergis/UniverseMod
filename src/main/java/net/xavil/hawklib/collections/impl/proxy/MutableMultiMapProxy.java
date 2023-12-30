package net.xavil.hawklib.collections.impl.proxy;

import java.util.function.Supplier;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.interfaces.ImmutableSet;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.interfaces.MutableMultiMap;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.collections.iterator.Iterator;

public final class MutableMultiMapProxy<K, V> implements MutableMultiMap<K, V> {

	private final MutableMap<K, MutableSet<V>> proxy;
	private final Supplier<MutableSet<V>> setFactory;

	public MutableMultiMapProxy(MutableMap<K, MutableSet<V>> proxy, Supplier<MutableSet<V>> setFactory) {
		this.proxy = proxy;
		this.setFactory = setFactory;
	}

	@Override
	public int size() {
		return this.proxy.size();
	}

	@Override
	public void clear() {
		this.proxy.clear();
	}

	@Override
	public void optimize() {
		this.proxy.optimize();
		this.proxy.values().forEach(set -> set.optimize());
	}

	@Override
	public boolean containsKey(K key) {
		return this.proxy.containsKey(key);
	}

	@Override
	public boolean contains(K key, V value) {
		return this.proxy.get(key).map(set -> set.contains(value)).unwrapOr(false);
	}

	@Override
	public Maybe<ImmutableSet<V>> get(K key) {
		return this.proxy.get(key).map(set -> set);
	}

	@Override
	public Iterator<K> keys() {
		return this.proxy.keys();
	}

	@Override
	public boolean insert(K key, V value) {
		final var set = this.proxy.entry(key).orInsertWithKey(k -> this.setFactory.get());
		return set.insert(value);
	}

	@Override
	public boolean remove(K key, V value) {
		return this.proxy.get(key).map(set -> {
			final var removed = set.remove(value);
			if (set.isEmpty()) {
				this.proxy.remove(key);
			}
			return removed;
		}).unwrapOr(false);
	}

	@Override
	public Maybe<MutableSet<V>> remove(K key) {
		return this.proxy.remove(key);
	}

}
