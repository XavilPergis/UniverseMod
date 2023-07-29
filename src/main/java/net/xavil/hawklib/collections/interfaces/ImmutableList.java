package net.xavil.hawklib.collections.interfaces;

import java.lang.reflect.Array;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.iterator.IntoIterator;

public interface ImmutableList<T> extends ImmutableCollection, IntoIterator<T> {

	T get(int index);

	default Maybe<T> last() {
		return isEmpty() ? Maybe.none() : Maybe.some(get(size() - 1));
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

}
