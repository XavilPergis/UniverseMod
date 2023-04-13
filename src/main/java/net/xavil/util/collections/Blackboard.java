package net.xavil.util.collections;

import net.xavil.util.Option;
import net.xavil.util.collections.interfaces.MutableMap;

public final class Blackboard {

	public static final class Key<T> {
		public final String name;

		private Key(String name) {
			this.name = name;
		}

		public static <T> Key<T> create(String name) {
			return new Key<>(name);
		}

		public boolean exists(Blackboard blackboard) {
			return blackboard.exists(this);
		}
		public Option<T> insert(Blackboard blackboard, T value) {
			return blackboard.insert(this, value);
		}
		public Option<T> get(Blackboard blackboard) {
			return blackboard.get(this);
		}
		public Option<T> remove(Blackboard blackboard) {
			return blackboard.remove(this);
		}
	}

	private final MutableMap<Key<Object>, Object> values = MutableMap.identityHashMap();

	@SuppressWarnings("unchecked")
	public boolean exists(Key<?> key) {
		return this.values.containsKey((Key<Object>) key);
	}
	

	@SuppressWarnings("unchecked")
	public <T> Option<T> insert(Key<T> key, T value) {
		final var prev = this.values.insert((Key<Object>) key, value);
		return prev.map(obj -> (T) obj);
	}
	
	@SuppressWarnings("unchecked")
	public <T> Option<T> get(Key<T> key) {
		return this.values.get((Key<Object>) key).map(obj -> (T) obj);
	}

	@SuppressWarnings("unchecked")
	public <T> Option<T> remove(Key<T> key) {
		return this.values.remove((Key<Object>) key).map(obj -> (T) obj);
	}

}
