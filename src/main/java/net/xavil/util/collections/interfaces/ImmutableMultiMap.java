package net.xavil.util.collections.interfaces;

import net.xavil.util.Option;
import net.xavil.util.iterator.IntoIterator;
import net.xavil.util.iterator.Iterator;

public interface ImmutableMultiMap<K, V> extends ImmutableCollection {

	Option<ImmutableSet<V>> get(K key);

	Iterator<K> keys();

	default boolean containsKey(K key) {
		return this.get(key).isSome();
	}

	default boolean contains(K key, V value) {
		return this.get(key).unwrapOr(ImmutableSet.of()).contains(value);
	}

	default Entry<K, V> entry(K key) {
		return new Entry.Impl<>(this, key);
	}

	default Iterator<? extends Entry<K, V>> entries() {
		return this.keys().map(this::entry);
	}

	default Iterator<ImmutableSet<V>> values() {
		return this.keys().map(key -> this.get(key).unwrap());
	}

	abstract class Entry<K, V> implements IntoIterator<V> {
		public final K key;

		public Entry(K key) {
			this.key = key;
		}

		public abstract Option<ImmutableSet<V>> get();

		public abstract boolean contains(V value);

		public boolean exists() {
			return this.get().isSome();
		}

		private static final class Impl<K, V> extends Entry<K, V> {
			private final ImmutableMultiMap<K, V> map;

			public Impl(ImmutableMultiMap<K, V> map, K key) {
				super(key);
				this.map = map;
			}

			@Override
			public Option<ImmutableSet<V>> get() {
				return this.map.get(this.key);
			}

			@Override
			public Iterator<V> iter() {
				return get().unwrapOr(ImmutableSet.of()).iter();
			}

			@Override
			public boolean contains(V value) {
				return this.map.contains(this.key, value);
			}

			@Override
			public boolean exists() {
				return this.map.containsKey(this.key);
			}
		}
	}

}
