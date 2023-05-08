package net.xavil.util.collections;

import net.xavil.util.Option;
import net.xavil.util.collections.interfaces.MutableMap;

public final class Blackboard<K> {

	public static final class Key<K, T> {
		public final K key;
		public final T defaultValue;

		private Key(K key, T defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
		}

		public static <K, T> Key<K, T> create(K key) {
			return new Key<>(key, null);
		}
		public static <K, T> Key<K, T> create(K key, T defaultValue) {
			return new Key<>(key, defaultValue);
		}

		public boolean exists(Blackboard<K> blackboard) {
			return blackboard.exists(this);
		}
		public Option<T> insert(Blackboard<K> blackboard, T value) {
			return blackboard.insert(this, value);
		}
		public Option<T> get(Blackboard<K> blackboard) {
			return blackboard.get(this);
		}
		public Option<T> remove(Blackboard<K> blackboard) {
			return blackboard.remove(this);
		}

		public T getOrDefault(Blackboard<K> blackboard) {
			return blackboard.get(this).unwrapOr(this.defaultValue);
		}
	}

	private final MutableMap<Key<K, Object>, Object> values = MutableMap.identityHashMap();

	@SuppressWarnings("unchecked")
	public boolean exists(Key<K, ?> key) {
		return this.values.containsKey((Key<K, Object>) key);
	}
	

	@SuppressWarnings("unchecked")
	public <T> Option<T> insert(Key<K, T> key, T value) {
		final var prev = this.values.insert((Key<K, Object>) key, value);
		return prev.map(obj -> (T) obj);
	}
	
	@SuppressWarnings("unchecked")
	public <T> Option<T> get(Key<K, T> key) {
		return this.values.get((Key<K, Object>) key).map(obj -> (T) obj);
	}

	@SuppressWarnings("unchecked")
	public <T> Option<T> remove(Key<K, T> key) {
		return this.values.remove((Key<K, Object>) key).map(obj -> (T) obj);
	}

}
