package net.xavil.hawklib.collections.interfaces;

import java.lang.reflect.Array;

import javax.annotation.Nullable;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.impl.ImmutableListArray;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.iterator.IntoIterator;

public interface ImmutableList<T> extends ImmutableCollection, IntoIterator<T> {

	/**
	 * Gets the element at the given index.
	 * 
	 * @param index The index of the element to get
	 * @return The value that is contained in the element at the given index
	 */
	T get(int index);

	default Maybe<T> last() {
		return isEmpty() ? Maybe.none() : Maybe.some(get(size() - 1));
	}

	@Nullable
	default T lastOrNull() {
		return isEmpty() ? null : this.get(this.size() - 1);
	}

	static <T> ImmutableList<T> copyOf(ImmutableList<T> other) {
		final var res = new Vector<T>(other.size());
		res.extend(other);
		return res;
	}

	default T[] toArray(Class<T> innerType) {
		@SuppressWarnings("unchecked")
		final T[] elems = (T[]) Array.newInstance(innerType, this.size());
		for (int i = 0; i < this.size(); ++i)
			elems[i] = this.get(i);
		return elems;
	}

	@SafeVarargs
	static <T> ImmutableList<T> of(T... args) {
		return new ImmutableListArray<>(args);
	}

}
