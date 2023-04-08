package net.xavil.util.collections.interfaces;

import net.xavil.util.Option;
import net.xavil.util.iterator.Iterator;

public interface ImmutableMap<K, V> extends ImmutableCollection {

	Option<V> get(K key);

	Iterator<K> keys();

	default boolean containsKey(K key) {
		return this.get(key).isSome();
	}

	default Entry<K, V> entry(K key) {
		return new Entry.Impl<>(this, key);
	}

	default Iterator<? extends Entry<K, V>> entries() {
		return this.keys().map(this::entry);
	}

	default Iterator<V> values() {
		return this.keys().map(key -> this.get(key).unwrap());
	}

	abstract class Entry<K, V> {
		public final K key;

		public Entry(K key) {
			this.key = key;
		}

		public abstract Option<V> get();

		public boolean exists() {
			return this.get().isSome();
		}

		private static final class Impl<K, V> extends Entry<K, V> {
			private final ImmutableMap<K, V> map;

			public Impl(ImmutableMap<K, V> map, K key) {
				super(key);
				this.map = map;
			}

			@Override
			public Option<V> get() {
				return this.map.get(this.key);
			}

			@Override
			public boolean exists() {
				return this.map.containsKey(this.key);
			}
		}
	}

}
