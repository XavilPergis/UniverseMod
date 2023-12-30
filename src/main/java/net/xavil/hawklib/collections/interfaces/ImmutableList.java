package net.xavil.hawklib.collections.interfaces;

import java.lang.reflect.Array;

import javax.annotation.Nullable;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.impl.ImmutableListArray;
import net.xavil.hawklib.collections.impl.ListUtil;
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

	// this is the only method that really need be overridden to optimize the whole
	// family of "last" methods.
	default T lastOrThrow() {
		ListUtil.checkBounds(0, size(), true);
		return this.get(this.size() - 1);
	}

	default Maybe<T> last() {
		return isEmpty() ? Maybe.none() : Maybe.some(lastOrThrow());
	}

	@Nullable
	default T lastOrNull() {
		return isEmpty() ? null : lastOrThrow();
	}

	static <T> ImmutableList<T> copyOf(ImmutableList<T> other) {
		final var res = new Vector<T>(other.size());
		res.extend(other);
		return res;
	}

	default T[] toArray(Class<T> innerType) {
		@SuppressWarnings("unchecked")
		final T[] elems = (T[]) Array.newInstance(innerType, this.size());
		final var writtenCount = this.iter().fillArray(elems);
		Assert.isEqual(writtenCount, this.size(),
				"iterator did not yield the same amount of elements as the list contained!!");
		return elems;
	}

	@SafeVarargs
	static <T> ImmutableList<T> of(T... args) {
		return new ImmutableListArray<>(args);
	}

}
