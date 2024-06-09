package net.xavil.hawklib.collections.iterator;

import java.util.function.Consumer;

import net.xavil.hawklib.collections.impl.ArrayIterator;
import net.xavil.hawklib.collections.impl.ArrayIteratorInt;

public final class Iterators {

    private Iterators() {}

    public <T> Iterator<T> wrap(T[] array) {
        return new ArrayIterator<>(array);
    }

    // start is inclusive, end is exclusive
    public <T> Iterator<T> wrap(T[] array, int startIndex, int endIndex) {
        return new ArrayIterator<>(array, startIndex, endIndex);
    }

    public IteratorInt wrap(int[] array) {
        return new ArrayIteratorInt(array);
    }

    // start is inclusive, end is exclusive
    public IteratorInt wrap(int[] array, int startIndex, int endIndex) {
        return new ArrayIteratorInt(array, startIndex, endIndex);
    }

    public static <T> Iterator<T> fromJava(java.util.Iterator<T> iter) {
        // @formatter:off
    	return new Iterator<T>() {
    		@Override public boolean hasNext() { return iter.hasNext(); }
    		@Override public T next()          { return iter.next(); }
    		@Override public void forEach(Consumer<? super T> consumer) { iter.forEachRemaining(consumer); }
    	};
    	// @formatter:on
    }

    /**
     * Adapts an iterator to a Java standard library iterator.
     * 
     * @param <T>  The type that the iterator yields.
     * @param iter The source iterator.
     * @return The adapted iterator.
     */
    public static <T> java.util.Iterator<T> asJava(Iterator<T> iter) {
    	// @formatter:off
    	return new java.util.Iterator<T>() {
    		@Override public boolean hasNext() { return iter.hasNext(); }
    		@Override public T next()          { return iter.next(); }
    		@Override public void forEachRemaining(Consumer<? super T> consumer) { iter.forEach(consumer); }
    	};
    	// @formatter:on
    }

}
