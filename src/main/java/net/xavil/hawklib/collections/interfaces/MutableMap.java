package net.xavil.hawklib.collections.interfaces;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.impl.proxy.MutableMapProxy;
import net.xavil.hawklib.collections.iterator.Iterator;

public interface MutableMap<K, V> extends MutableCollection, ImmutableMap<K, V> {

	Maybe<V> insert(K key, V value);

	Maybe<V> remove(K key);

	void retain(BiPredicate<K, V> predicate);

	// void extend(IntoIterator<T> elements);

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
				return this.map.insert(this.key, value);
			}

			@Override
			public Maybe<V> remove() {
				return this.map.remove(this.key);
			}

			@Override
			public Maybe<V> get() {
				return this.map.get(this.key);
			}

			@Override
			public V orInsertWithKey(Function<K, V> supplier) {
				return orInsertWith(() -> supplier.apply(this.key));
			}

			@Override
			public V orInsertWith(Supplier<V> supplier) {
				return this.map.get(this.key).unwrapOrElse(() -> {
					final var value = supplier.get();
					this.map.insert(this.key, value);
					return value;
				});
			}

		}
	}

}
