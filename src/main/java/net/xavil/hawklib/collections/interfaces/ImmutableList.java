package net.xavil.hawklib.collections.interfaces;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.iterator.IntoIterator;

public interface ImmutableList<T> extends ImmutableCollection, IntoIterator<T> {

	T get(int index);

	default Maybe<T> last() {
		return isEmpty() ? Maybe.none() : Maybe.some(get(size() - 1));
	}

}
