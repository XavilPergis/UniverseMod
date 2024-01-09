package net.xavil.hawklib.collections.interfaces;

import java.util.Objects;
import it.unimi.dsi.fastutil.floats.FloatPredicate;
import net.xavil.hawklib.MaybeFloat;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.impl.VectorFloat;
import net.xavil.hawklib.collections.impl.ListUtil;
import net.xavil.hawklib.collections.iterator.IntoIteratorFloat;

public interface MutableListFloat extends MutableCollection, ImmutableListFloat {

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
	float set(int index, float value);

	/**
	 * Inserts an element before the given index. If the index points one after the
	 * end of the list, then the value will be inserted at the end.
	 * 
	 * @param index The index before which the value will be inserted
	 * @param value The value to be inserted
	 * 
	 * @throws IndexOutOfBoundsException if {@code index < 0 || index > size()}
	 */
	void insert(int index, float value);

	/**
	 * Removes the element at the given index.
	 * 
	 * @param index The index of the element to remove
	 * @return The value of the element that was removed
	 * 
	 * @throws IndexOutOfBoundsException if {@code index < 0 || index >= size()}
	 */
	float remove(int index);

	@Override
	default void clear() {
		retain(x -> false);
	}

	/**
	 * Tests all elements in this list, removing any that failed the predicate.
	 * 
	 * @param predicate The predicate to test all the elements against
	 */
	default void retain(FloatPredicate predicate) {
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
	 * @see #insert(int, float)
	 */
	default void push(float value) {
		this.insert(this.size(), value);
	}

	default void pushFront(float value) {
		this.insert(0, value);
	}

	default float popOrThrow() {
		ListUtil.checkBounds(0, this.size(), true);
		return this.remove(this.size() - 1);
	}

	default float popFrontOrThrow() {
		ListUtil.checkBounds(0, this.size(), true);
		return this.remove(0);
	}

	default MaybeFloat pop() {
		return this.isEmpty() ? MaybeFloat.none() : MaybeFloat.some(popOrThrow());
	}

	default MaybeFloat popFront() {
		return this.isEmpty() ? MaybeFloat.none() : MaybeFloat.some(popFrontOrThrow());
	}

	/**
	 * Inserts all the elements yielded by the given iterator before the element at
	 * the given index. This is generally preferable to inserting elements one-by
	 * one for bulk operations.
	 * 
	 * @param index    The index before which the values will be inserted
	 * @param elements The elements to be inserted
	 * @see #insert(int, float)
	 */
	default void extend(int index, IntoIteratorFloat elements) {
		final var iter = elements.iter();
		for (int i = index; iter.hasNext(); ++i)
			insert(i, iter.next());
	}

	/**
	 * Inserts all the elements yielded by the given iterator at the end of the
	 * list.
	 * 
	 * @param elements The elements to be inserted
	 * @see #extend(int, IntoIteratorFloat)
	 */
	default void extend(IntoIteratorFloat elements) {
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
	default int indexOf(FloatPredicate predicate) {
		return this.iter().indexOf(predicate);
	}

	default int indexOf(float value) {
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
			this.popOrThrow();
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

	@Override
	default MutableListFloat hint(CollectionHint hint) {
		return this;
	}

	static MutableListFloat copyOf(ImmutableListFloat other) {
		final var res = new VectorFloat(other.size());
		res.extend(other);
		return res;
	}

}
