package net.xavil.hawklib.collections.interfaces;

import java.util.Set;

import net.xavil.hawklib.collections.impl.proxy.MutableSetProxy;
import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.hawklib.collections.iterator.Iterator;

public interface ImmutableSet<T> extends ImmutableCollection, IntoIterator<T> {

	boolean contains(T value);

	// ImmutableSet<T> difference(ImmutableSet<T> other);

	// ImmutableSet<T> intersection(ImmutableSet<T> other);

	// ImmutableSet<T> union(ImmutableSet<T> other);

	// default ImmutableSet<T> symmetricDifference(ImmutableSet<T> other) {
	// 	return this.difference(other).union(other.difference(this));
	// }

	static <T> ImmutableSet<T> of() {
		return new ImmutableSet<T>() {
			@Override
			public int size() {
				return 0;
			}

			@Override
			public Iterator<T> iter() {
				return Iterator.empty();
			}

			@Override
			public boolean contains(T value) {
				return false;
			}
		};
	}

	static <T> ImmutableSet<T> of(T e0) {
		return new MutableSetProxy<>(Set.of(e0));
	}
	static <T> ImmutableSet<T> of(T e0, T e1) {
		return new MutableSetProxy<>(Set.of(e0, e1));
	}
	static <T> ImmutableSet<T> of(T e0, T e1, T e2) {
		return new MutableSetProxy<>(Set.of(e0, e1, e2));
	}
	static <T> ImmutableSet<T> of(T e0, T e1, T e2, T e3) {
		return new MutableSetProxy<>(Set.of(e0, e1, e2, e3));
	}
	static <T> ImmutableSet<T> of(T e0, T e1, T e2, T e3, T e4) {
		return new MutableSetProxy<>(Set.of(e0, e1, e2, e3, e4));
	}
	static <T> ImmutableSet<T> of(T e0, T e1, T e2, T e3, T e4, T e5) {
		return new MutableSetProxy<>(Set.of(e0, e1, e2, e3, e4, e5));
	}
	@SafeVarargs
	static <T> ImmutableSet<T> of(T... elements) {
		return new MutableSetProxy<>(Set.of(elements));
	}

}
