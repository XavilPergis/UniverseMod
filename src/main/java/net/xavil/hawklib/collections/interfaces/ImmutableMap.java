package net.xavil.hawklib.collections.interfaces;

import java.util.Map;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.impl.proxy.ImmutableMapProxy;
import net.xavil.hawklib.collections.iterator.Iterator;

public interface ImmutableMap<K, V> extends ImmutableCollection {

	@Nonnull
	Maybe<V> get(K key);

	@Nullable
	default V getOrNull(K key) {
		return get(key).unwrapOrNull();
	}

	default V getOrThrow(K key) {
		final var res = get(key);
		if (res.isNone())
			throw new IllegalArgumentException(String.format(
					"Key '%s' does not exist in map",
					key));
		return res.unwrap();
	}

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

	static <K, V> ImmutableMapProxy<K, V> proxy(Map<K, V> map) {
		return new ImmutableMapProxy<>(map);
	}

	static <K, V> boolean mapsEqual(ImmutableMap<K, V> a, ImmutableMap<K, V> b) {
		for (final var key : a.keys().iterable()) {
			if (!b.containsKey(key))
				return false;
			if (!Objects.equals(a.getOrThrow(key), b.getOrThrow(key)))
				return false;
		}
		return true;
	}

	abstract class Entry<K, V> {
		public final K key;

		public Entry(K key) {
			this.key = key;
		}

		public abstract Maybe<V> get();

		@Nullable
		public abstract V getOrNull();

		@Nonnull
		public abstract V getOrThrow();

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
			public Maybe<V> get() {
				return this.map.get(this.key);
			}

			@Override
			@Nullable
			public V getOrNull() {
				return this.map.getOrNull(this.key);
			}

			@Override
			@Nonnull
			public V getOrThrow() {
				return this.map.getOrThrow(this.key);
			}

			@Override
			public boolean exists() {
				return this.map.containsKey(this.key);
			}
		}
	}

}
