package net.xavil.hawklib.collections.interfaces;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.impl.proxy.MutableListProxy;
import net.xavil.hawklib.collections.iterator.IntoIterator;

public interface MutableList<T> extends MutableCollection, ImmutableList<T> {

	void retain(Predicate<T> predicate);

	void extend(IntoIterator<T> elements);

	T set(int index, T value);

	default void swap(int indexA, int indexB) {
		if (indexA != indexB) {
			this.set(indexA, this.set(indexB, this.get(indexA)));
		}
	}

	void insert(int index, T value);

	T remove(int index);

	default void push(T value) {
		this.insert(this.size(), value);
	}

	default Maybe<T> pop() {
		return this.isEmpty() ? Maybe.none() : Maybe.some(this.remove(this.size() - 1));
	}

	default void shuffle(Rng rng) {
		if (this.size() <= 1)
			return;
		// TODO: allow implementations to hint if they're random-access or not. It would
		// be very very slow to do this on a big linked list.
		for (int i = 0; i < this.size(); ++i) {
			this.swap(i, rng.uniformInt(0, this.size()));
		}
	}

	default void sort(Comparator<? super T> comparator) {
		final T[] elems = this.toArray();
		Arrays.sort(elems, comparator);
		for (int i = 0; i < elems.length; ++i)
			this.set(i, elems[i]);
	}

	@Override
	default MutableList<T> hint(CollectionHint hint) {
		return this;
	}

	default T[] toArray() {
		@SuppressWarnings("unchecked")
		final T[] elems = (T[]) new Object[this.size()];
		for (int i = 0; i < this.size(); ++i)
			elems[i] = this.get(i);
		return elems;
	}

	static <T> MutableList<T> proxy(List<T> list) {
		return new MutableListProxy<>(list);
	}

	static <T> MutableList<T> copyOf(ImmutableList<T> other) {
		final var res = new Vector<T>(other.size());
		res.extend(other);
		return res;
	}

}
