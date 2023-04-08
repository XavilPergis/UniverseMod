package net.xavil.util.collections.interfaces;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import net.xavil.util.collections.MutableListProxy;
import net.xavil.util.iterator.IntoIterator;

public interface MutableList<T> extends MutableCollection, ImmutableList<T> {

	void retain(Predicate<T> predicate);

	void extend(IntoIterator<T> elements);

	void insert(int index, T value);

	T remove(int index);

	default void push(T value) {
		this.insert(this.size(), value);
	}

	default Optional<T> pop() {
		return this.isEmpty() ? Optional.empty() : Optional.of(this.remove(this.size() - 1));
	}

	static <T> MutableList<T> proxy(List<T> list) {
		return new MutableListProxy<>(list);
	}

}
