package net.xavil.hawklib.collections.iterator;

import it.unimi.dsi.fastutil.ints.IntConsumer;

/**
 * Essentally like Java's {@link Iterable}
 */
public interface IntoIteratorInt {

	/**
	 * Creates an iterator from this value.
	 * 
	 * @return The iterator.
	 */
	IteratorInt iter();

	/**
	 * Yields all remaining elements in this iterator into {@code consumer}.
	 * 
	 * @param consumer The consumer that accepts the remaining elements.
	 */
	default void forEach(IntConsumer consumer) {
		final var iter = iter();
		while (iter.hasNext()) {
			consumer.accept(iter.next());
		}
	}

}
