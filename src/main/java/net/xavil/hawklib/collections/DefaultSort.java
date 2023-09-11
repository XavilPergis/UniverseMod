package net.xavil.hawklib.collections;

import java.util.Arrays;
import java.util.Comparator;

import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;

public final class DefaultSort<T> implements SortingStrategy<T> {

	private static final DefaultSort<?> INSTANCE = new DefaultSort<>();

	private DefaultSort() {}

	@SuppressWarnings("unchecked")
	public static <T> DefaultSort<T> instance() {
		return (DefaultSort<T>) INSTANCE;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void sort(MutableList<T> list, Comparator<? super T> cmp) {
		if (list instanceof Vector<T> vec) {
			Arrays.sort(vec.backingStorage(), 0, vec.size(), cmp);
		} else {
			final Object[] elems = new Object[list.size()];
			for (int i = 0; i < list.size(); ++i)
				elems[i] = list.get(i);
			Arrays.sort((T[]) elems, cmp);
			for (int i = 0; i < elems.length; ++i)
				list.set(i, (T) elems[i]);
		}
	}
	
}
