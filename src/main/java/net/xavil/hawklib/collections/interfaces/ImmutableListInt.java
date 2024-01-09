package net.xavil.hawklib.collections.interfaces;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.MaybeInt;
import net.xavil.hawklib.collections.impl.ImmutableListArray;
import net.xavil.hawklib.collections.impl.ListUtil;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.iterator.IntoIteratorInt;

public interface ImmutableListInt extends ImmutableCollection, IntoIteratorInt {

	/**
	 * Gets the element at the given index.
	 * 
	 * @param index The index of the element to get
	 * @return The value that is contained in the element at the given index
	 */
	int get(int index);

	// this is the only method that really need be overridden to optimize the whole
	// family of "last" methods.
	default int lastOrThrow() {
		ListUtil.checkBounds(0, size(), true);
		return this.get(this.size() - 1);
	}

	default MaybeInt last() {
		return isEmpty() ? MaybeInt.none() : MaybeInt.some(lastOrThrow());
	}

	static <T> ImmutableList<T> copyOf(ImmutableList<T> other) {
		final var res = new Vector<T>(other.size());
		res.extend(other);
		return res;
	}

	default int[] toArray() {
		final int[] elems = new int[this.size()];
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
