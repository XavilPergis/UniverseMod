package net.xavil.hawklib.collections.interfaces;

import java.lang.reflect.Array;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import javax.annotation.Nullable;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.impl.ImmutableListArray;
import net.xavil.hawklib.collections.impl.ListUtil;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.impl.proxy.ImmutableListProxy;
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

	@Nullable
	default <U> int binarySearch(U value, Function<T, U> keyExtractor, Comparator<U> comparator) {
		int lo = 0, hi = this.size();
		while (lo < hi) {
			final var mid = (lo + hi) / 2;
			final var cmp = comparator.compare(value, keyExtractor.apply(this.get(mid)));
			if (cmp > 0) {
				lo = mid + 1;
			} else if (cmp < 0) {
				hi = mid;
			} else {
				return mid;
			}
		}

		// by the time we get here, we definitely haven't found our result, so all
		// returned indices will be negative.

		final var loValue = keyExtractor.apply(this.get(lo));
		if (comparator.compare(value, loValue) > 0) {
			return -(lo + 1);
		}

		return -lo;
	}

	@Nullable
	default int binarySearchByDouble(double value, ToDoubleFunction<T> keyExtractor) {
		int lo = 0, hi = this.size();
		while (lo < hi) {
			final var mid = (lo + hi) / 2;
			final var midValue = keyExtractor.applyAsDouble(this.get(mid));
			if (midValue > mid) {
				lo = mid + 1;
			} else if (midValue < mid) {
				hi = mid;
			} else {
				return mid;
			}
		}

		// by the time we get here, we definitely haven't found our result, so all
		// returned indices will be negative.

		final var loValue = keyExtractor.applyAsDouble(this.get(lo));
		if (value > loValue) {
			return -(lo + 1);
		}

		return -lo;
	}

	static <T> ImmutableList<T> copyOf(ImmutableList<T> other) {
		final var res = new Vector<T>(other.size());
		res.extend(other);
		return res;
	}

	static <T> ImmutableListProxy<T> proxy(List<T> list) {
		return new ImmutableListProxy<>(list);
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
