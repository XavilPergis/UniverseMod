package net.xavil.hawklib.collections.interfaces;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.MaybeFloat;
import net.xavil.hawklib.collections.impl.ImmutableListArray;
import net.xavil.hawklib.collections.impl.ListUtil;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.iterator.IntoIteratorFloat;

public interface ImmutableListFloat extends ImmutableCollection, IntoIteratorFloat {

	/**
	 * Gets the element at the given index.
	 * 
	 * @param index The index of the element to get
	 * @return The value that is contained in the element at the given index
	 */
	float get(int index);

	// this is the only method that really need be overridden to optimize the whole
	// family of "last" methods.
	default float lastOrThrow() {
		ListUtil.checkBounds(0, size(), true);
		return this.get(this.size() - 1);
	}

	default MaybeFloat last() {
		return isEmpty() ? MaybeFloat.none() : MaybeFloat.some(lastOrThrow());
	}

	static <T> ImmutableList<T> copyOf(ImmutableList<T> other) {
		final var res = new Vector<T>(other.size());
		res.extend(other);
		return res;
	}

	default float[] toArray() {
		final float[] elems = new float[this.size()];
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
