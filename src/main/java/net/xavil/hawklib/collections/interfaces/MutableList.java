package net.xavil.hawklib.collections.interfaces;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.SortingStrategy;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.impl.proxy.MutableListProxy;
import net.xavil.hawklib.collections.iterator.IntoIterator;

public interface MutableList<T> extends MutableCollection, ImmutableList<T> {

	/**
	 * Sets the element at the given index.
	 * 
	 * @param index The index of the element to set
	 * @param value The value to be stored at the given index
	 * @return The value that was previously contained in the element at the given
	 *         index
	 * 
	 * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
	 */
	T set(int index, T value);

	/**
	 * Inserts an element before the given index. If the index points one after the
	 * end of the list, then the value will be inserted at the end.
	 * 
	 * @param index The index before which the value will be inserted
	 * @param value The value to be inserted
	 * 
	 * @throws IndexOutOfBoundsException if {@code index < 0 || index > size()}
	 */
	void insert(int index, T value);

	/**
	 * Removes the element at the given index.
	 * 
	 * @param index The index of the element to remove
	 * @return The value of the element that was removed
	 * 
	 * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
	 */
	T remove(int index);

	/**
	 * Tests all elements in this list, removing any that failed the predicate.
	 * 
	 * @param predicate The predicate to test all the elements against
	 */
	default void retain(Predicate<T> predicate) {
		for (int i = 0; i < this.size();) {
			if (predicate.test(this.get(i))) {
				i += 1;
			} else {
				this.remove(i);
			}
		}
	}

	/**
	 * Inserts an element at the end of the list.
	 * 
	 * @param value The value to be inserted
	 * @see #insert(int, T)
	 */
	default void push(T value) {
		this.insert(this.size(), value);
	}

	default void pushFront(T value) {
		this.insert(0, value);
	}

	default Maybe<T> pop() {
		return this.isEmpty() ? Maybe.none() : Maybe.some(this.remove(this.size() - 1));
	}

	default Maybe<T> popFront() {
		return this.isEmpty() ? Maybe.none() : Maybe.some(this.remove(0));
	}

	/**
	 * Removes the element at the end of the list, doing nothing if the list is
	 * empty.
	 * 
	 * @return The value of the element that was removed, or {@code null} if the
	 *         list was empty
	 */
	default T popOrNull() {
		return this.isEmpty() ? null : this.remove(this.size() - 1);
	}

	default T popFrontOrNull() {
		return this.isEmpty() ? null : this.remove(0);
	}

	/**
	 * Inserts all the elements yielded by the given iterator before the element at
	 * the given index. This is generally preferable to inserting elements one-by
	 * one for bulk operations.
	 * 
	 * @param index    The index before which the values will be inserted
	 * @param elements The elements to be inserted
	 * @see #insert(int, T)
	 */
	default void extend(int index, IntoIterator<? extends T> elements) {
		final var iter = elements.iter();
		for (int i = index; iter.hasNext(); ++i)
			insert(i, iter.next());
	}

	/**
	 * Inserts all the elements yielded by the given iterator at the end of the
	 * list.
	 * 
	 * @param elements The elements to be inserted
	 * @see #extend(int, IntoIterator)
	 */
	default void extend(IntoIterator<? extends T> elements) {
		extend(this.size(), elements);
	}

	default void swap(int indexA, int indexB) {
		if (indexA != indexB) {
			this.set(indexA, this.set(indexB, this.get(indexA)));
		}
	}

	/**
	 * Returns the index of the first element that satisfies the given predicate.
	 * 
	 * @param predicate The predicate to test elements with
	 * @return The index of the element, or {@code -1} if no element was found
	 */
	default int indexOf(Predicate<? super T> predicate) {
		return this.iter().indexOf(predicate);
	}

	default int indexOf(T value) {
		return this.indexOf(n -> Objects.equals(n, value));
	}

	default void reverse() {
		for (int i = 0; i < this.size() / 2; ++i)
			this.swap(i, this.size() - i - 1);
	}

	/**
	 * Removes all elements whose indices are greater than or equal to {@code size}.
	 * 
	 * @param size The new size of the list
	 */
	default void truncate(int size) {
		while (this.size() > size) {
			this.pop();
		}
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

	default void sort(int flags, Comparator<? super T> comparator) {
		SortingStrategy.sort(this, flags, comparator);
	}

	default void sort(Comparator<? super T> comparator) {
		sort(0, comparator);
	}

	@Override
	default MutableList<T> hint(CollectionHint hint) {
		return this;
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
