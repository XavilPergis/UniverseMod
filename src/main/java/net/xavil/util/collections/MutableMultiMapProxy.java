package net.xavil.util.collections;

import java.util.function.Supplier;

import net.xavil.util.Option;
import net.xavil.util.collections.interfaces.ImmutableSet;
import net.xavil.util.collections.interfaces.MutableMap;
import net.xavil.util.collections.interfaces.MutableMultiMap;
import net.xavil.util.collections.interfaces.MutableSet;
import net.xavil.util.iterator.Iterator;

public final class MutableMultiMapProxy<K, V> implements MutableMultiMap<K, V> {

	private final MutableMap<K, MutableSet<V>> proxy;
	private final Supplier<MutableMap<K, MutableSet<V>>> proxyFactory;
	private final Supplier<MutableSet<V>> setFactory;

	public MutableMultiMapProxy(Supplier<MutableMap<K, MutableSet<V>>> proxyFactory, Supplier<MutableSet<V>> setFactory) {
		this(proxyFactory.get(), proxyFactory, setFactory);
	}

	private MutableMultiMapProxy(MutableMap<K, MutableSet<V>> proxy, Supplier<MutableMap<K, MutableSet<V>>> proxyFactory, Supplier<MutableSet<V>> setFactory) {
		this.proxy = proxy;
		this.proxyFactory = proxyFactory;
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
	public Option<ImmutableSet<V>> get(K key) {
		return this.proxy.get(key).map(set -> set);
	}

	@Override
	public Iterator<K> keys() {
		return this.proxy.keys();
	}

	@Override
	public boolean insert(K key, V value) {
		final var set = this.proxy.entry(key).orInsertWith(k -> this.setFactory.get());
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
	public Option<MutableSet<V>> remove(K key) {
		return this.proxy.remove(key);
	}

}
