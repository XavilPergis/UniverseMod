package net.xavil.util.collections.interfaces;

import net.xavil.util.Option;
import net.xavil.util.iterator.IntoIterator;

public interface ImmutableList<T> extends ImmutableCollection, IntoIterator<T> {

	T get(int index);

	default Option<T> last() {
		return isEmpty() ? Option.none() : Option.some(get(size() - 1));
	}

}
