package net.xavil.hawklib.collections.iterator;

import it.unimi.dsi.fastutil.floats.FloatConsumer;

/**
 * Essentally like Java's {@link Iterable}
 */
public interface IntoIteratorFloat {

	/**
	 * Creates an iterator from this value.
	 * 
	 * @return The iterator.
	 */
	IteratorFloat iter();

	/**
	 * Yields all remaining elements in this iterator into {@code consumer}.
	 * 
	 * @param consumer The consumer that accepts the remaining elements.
	 */
	default void forEach(FloatConsumer consumer) {
		final var iter = iter();
		while (iter.hasNext()) {
			consumer.accept(iter.next());
		}
	}

}
