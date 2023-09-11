package net.xavil.hawklib.collections;

import java.util.Comparator;

import net.xavil.hawklib.collections.interfaces.MutableList;

public interface SortingStrategy<T> {

	void sort(MutableList<T> list, Comparator<? super T> cmp);

	/**
	 * This flag communicates a relaxation of the properties otherwise guaranteed
	 * for sorts. If set, the relative ordering of equal elements need not be
	 * preserved.
	 */
	static int UNSTABLE = (1 << 0);

	/**
	 * This flag is a hint, and will not affect the outcome of a sorting operation.
	 * If misued, it may cause the sorting operation to take much more time to
	 * complete than it otherwise would.
	 * 
	 * This flag should be set if the elements being sorted are already near the
	 * indices they will end up at once the sorting operation is complete.
	 * 
	 * @implNote This currently uses a simple insertion sort, which, for a length n
	 *           collection where each element is at most k elements away from its
	 *           sorted position, means the worst case time complexity is
	 *           {@code O(k*n)}.
	 */
	static int ALMOST_SORTED = (1 << 1);

	static <T> void sort(MutableList<T> list, int flags, Comparator<? super T> cmp) {
		SortingStrategy<T> sorter = DefaultSort.instance();
		if ((flags & ALMOST_SORTED) != 0)
			sorter = InsertionSort.instance();
		sorter.sort(list, cmp);
	}

}
