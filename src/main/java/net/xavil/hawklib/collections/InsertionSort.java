package net.xavil.hawklib.collections;

import java.util.Comparator;

import net.xavil.hawklib.collections.interfaces.MutableList;

public final class InsertionSort<T> implements SortingStrategy<T> {

	private static final InsertionSort<?> INSTANCE = new InsertionSort<>();

	private InsertionSort() {}

	@SuppressWarnings("unchecked")
	public static <T> InsertionSort<T> instance() {
		return (InsertionSort<T>) INSTANCE;
	}

	@Override
	public void sort(MutableList<T> list, Comparator<? super T> cmp) {
		for (int i = 0; i < list.size(); ++i) {
			final T value = list.get(i);
			for (int j = i - 1; j >= 0; --j) {
				if (cmp.compare(value, list.get(j)) >= 0)
					break;
				list.swap(j, j + 1);
			}
		}
	}

}
