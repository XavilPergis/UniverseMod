package net.xavil.hawklib.collections.impl;

import net.xavil.hawklib.collections.interfaces.ImmutableList;

public final class ListUtil {

	@SuppressWarnings("unchecked")
	public static <T> T[] makeTypedObjectArray(int length) {
		return (T[]) new Object[length];
	}

	public static void checkBounds(int index, int size, boolean exclusive) {
		if (index < 0 || index > size || (exclusive && index == size)) {
			throw new IndexOutOfBoundsException(String.format(
					"Index %d is out of bounds for list of length %d",
					index, size));
		}
	}

	private static final int BIG_ARRAY_CUTOFF = 32;

	public static <T> String asString(ImmutableList<T> list) {
		return asString(list, BIG_ARRAY_CUTOFF);
	}

	public static <T> String asString(ImmutableList<T> list, int cutoff) {
		final var builder = new StringBuilder();
		builder.append("[");
		if (list.size() > 0) {
			builder.append(list.get(0));
			final var limit = Math.min(list.size(), cutoff);
			for (int i = 1; i < limit; ++i) {
				builder.append(", ");
				builder.append(list.get(i));
			}
			if (list.size() > cutoff) {
				builder.append(", ... <");
				builder.append(list.size() - cutoff);
				builder.append(" more>");
			}
		}
		builder.append("]");
		return builder.toString();
	}

}
