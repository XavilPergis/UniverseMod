package net.xavil.hawklib.collections.iterator;

import java.util.function.Consumer;

/**
 * Essentally like Java's {@link Iterable}
 */
public interface IntoIterator<T> {

	/**
	 * Creates an iterator from this value.
	 * 
	 * @return The iterator.
	 */
	Iterator<T> iter();

	/**
	 * Yields all remaining elements in this iterator into {@code consumer}.
	 * 
	 * @param consumer The consumer that accepts the remaining elements.
	 */
	default void forEach(Consumer<? super T> consumer) {
		final var iter = iter();
		while (iter.hasNext()) {
			consumer.accept(iter.next());
		}
	}

	/**
	 * Creates an {@link Iterable} from this value, allowing its use in enhanced
	 * {@code for} loops.
	 * 
	 * @return an {@link Iterable}.
	 */
	default Iterable<T> iterable() {
		return new Iterable<T>() {
			@Override
			public java.util.Iterator<T> iterator() {
				return Iterators.asJava(IntoIterator.this.iter());
			}

			@Override
			public void forEach(Consumer<? super T> action) {
				IntoIterator.this.forEach(action);
			}
		};
	}

}
