package net.xavil.util.collections.interfaces;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.xavil.util.Option;
import net.xavil.util.collections.MutableMapProxy;
import net.xavil.util.iterator.Iterator;

public interface MutableMap<K, V> extends MutableCollection, ImmutableMap<K, V> {

	Option<V> insert(K key, V value);

	Option<V> remove(K key);

	void retain(BiPredicate<K, V> predicate);

	// void extend(IntoIterator<T> elements);

	default EntryMut<K, V> entry(K key) {
		return new EntryMut.Impl<>(this, key);
	}

	@Override
	default Iterator<? extends EntryMut<K, V>> entries() {
		return this.keys().map(this::entry);
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

		public abstract Option<V> insert(V value);

		public abstract Option<V> remove();

		public abstract V orInsertWith(Function<K, V> supplier);

		private static final class Impl<K, V> extends EntryMut<K, V> {
			private final MutableMap<K, V> map;
			
			public Impl(MutableMap<K, V> map, K key) {
				super(key);
				this.map = map;
			}

			@Override
			public Option<V> insert(V value) {
				return this.map.insert(this.key, value);
			}

			@Override
			public Option<V> remove() {
				return this.map.remove(this.key);
			}

			@Override
			public Option<V> get() {
				return this.map.get(this.key);
			}

			@Override
			public V orInsertWith(Function<K, V> supplier) {
				return this.map.get(this.key).unwrapOrElse(() -> {
					final var value = supplier.apply(this.key);
					this.map.insert(this.key, value);
					return value;
				});
			}

		}
	}

}
