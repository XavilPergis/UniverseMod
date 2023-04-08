package net.xavil.util.collections.interfaces;

import java.util.Set;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.xavil.util.collections.MutableSetProxy;
import net.xavil.util.iterator.IntoIterator;
import net.xavil.util.iterator.Iterator;

public interface MutableSet<T> extends MutableCollection, ImmutableSet<T> {

	void retain(Predicate<T> predicate);

	void extend(IntoIterator<T> elements);

	boolean insert(T value);

	boolean remove(T value);

	default Iterator<T> difference(ImmutableSet<T> other) {
		return this.iter().filter(item -> !other.contains(item));
	}

	static <T> MutableSetProxy<T> proxy(Set<T> set) {
		return new MutableSetProxy<>(set);
	}

	static <T> MutableSet<T> hashSet() {
		return new MutableSetProxy<>(new ObjectOpenHashSet<>());
	}

	static <T> MutableSet<T> hashSet(MutableSet<T> set) {
		final var newSet = new MutableSetProxy<T>(new ObjectOpenHashSet<>());
		newSet.extend(set);
		return newSet;
	}

}
