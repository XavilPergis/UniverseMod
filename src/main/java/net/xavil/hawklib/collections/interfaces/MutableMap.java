package net.xavil.hawklib.collections.interfaces;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.impl.proxy.MutableMapProxy;
import net.xavil.hawklib.collections.iterator.Iterator;

public interface MutableMap<K, V> extends MutableCollection, ImmutableMap<K, V> {

	/**
	 * Insert an entry into this map.
	 * 
	 * @param key   The key of the entry that will be inserted.
	 * @param value The value of the entry that will be inserted.
	 * @return {@code true} if the set of keys changed as a result of this operation.
	 */
	boolean insert(K key, V value);

	default Maybe<V> insertAndGet(K key, V value) {
		final var old = get(key);
		insert(key, value);
		return old;
	}

	/**
	 * Remove an entry from this map.
	 * 
	 * @param key The key of the entry that will be removed.
	 * @return {@code true} if the set of keys changed as a result of this operation.
	 */
	boolean remove(K key);

	default Maybe<V> removeAndGet(K key) {
		final var old = get(key);
		remove(key);
		return old;
	}

	void retain(BiPredicate<K, V> predicate);

	default EntryMut<K, V> entry(K key) {
		return new EntryMut.Impl<>(this, key);
	}

	@Override
	default Iterator<? extends EntryMut<K, V>> entries() {
		return this.keys().map(this::entry);
	}

	@Override
	default MutableMap<K, V> hint(CollectionHint hint) {
		return this;
	}

	static <K, V> MutableMapProxy<K, V> proxy(Map<K, V> map) {
		return new MutableMapProxy<>(map);
	}

	static <K, V> MutableMap<K, V> hashMap() {
		return new MutableMapProxy<>(new Object2ObjectOpenHashMap<>());
	}

	static <K, V> MutableMap<K, V> identityHashMap() {
		return new MutableMapProxy<>(new IdentityHashMap<>());
	}

	public abstract class EntryMut<K, V> extends Entry<K, V> {
		public EntryMut(K key) {
			super(key);
		}

		public abstract Maybe<V> insert(V value);

		public abstract Maybe<V> remove();

		public abstract V orInsertWithKey(Function<K, V> supplier);

		public abstract V orInsertWith(Supplier<V> supplier);

		private static final class Impl<K, V> extends EntryMut<K, V> {
			private final MutableMap<K, V> map;

			public Impl(MutableMap<K, V> map, K key) {
				super(key);
				this.map = map;
			}

			@Override
			public Maybe<V> insert(V value) {
				return this.map.insertAndGet(this.key, value);
			}

			@Override
			public Maybe<V> remove() {
				return this.map.removeAndGet(this.key);
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
			public V orInsertWithKey(Function<K, V> supplier) {
				return orInsertWith(() -> supplier.apply(this.key));
			}

			@Override
			public V orInsertWith(Supplier<V> supplier) {
				return this.map.get(this.key).unwrapOrElse(() -> {
					final var value = supplier.get();
					this.map.insertAndGet(this.key, value);
					return value;
				});
			}

		}
	}

}
