package net.xavil.hawklib.collections.iterator;

import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.ints.IntUnaryOperator;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.ints.IntPredicate;
import net.xavil.hawklib.MaybeInt;
import net.xavil.hawklib.collections.interfaces.MutableListInt;

/**
 * A lazily-evaluated arbitrary sequence of values. Roughly analagous to
 * {@link java.util.Iterator}, but with a more ergonomic interface.
 */
public interface IteratorInt extends IntoIteratorInt {

	/**
	 * Returns whether the iterator has more elements to yield.
	 * 
	 * <p>
	 * It is valid for implementations to update their internal state when this
	 * method is called. Subsequent calls to this method must not return
	 * {@code false} if they previously returned {@code true}, unless
	 * {@link #next()} was called in between. If an iterator is fused (i.e., if
	 * {@link #hasProperty()} returns {@code true} when called with
	 * {@link #PROPERTY_FUSED}), then this method cannot ever return anything other
	 * than {@code false} after a {@code false} was previously yielded.
	 * </p>
	 * 
	 * @return {@code true} if the iterator has more elements to yield.
	 */
	boolean hasNext();

	/**
	 * Advances the iterator, and returns the element that was just advanced past.
	 * {@link #hasNext()} must be called and return true before a call to this
	 * method is valid.
	 * 
	 * @return The next element in the iterator.
	 */
	int next();

	/**
	 * To allow both {@link Iterator}s and other things like collections or ranges
	 * to be used directly as arguments for certain operations, {@link Iterator}
	 * itself implements {@link IntoIterator}.
	 * 
	 * @return {@code this}.
	 */
	@Override
	default IteratorInt iter() {
		return this;
	}

	/**
	 * Returns a size hint for this iterator. This iterator must yield at least as
	 * many elements as specified by {@link SizeHint#lowerBound}. If
	 * {@link SizeHint#upperBound} is empty, this iterator may yield an infinite
	 * number of elements. If it is not, then this iterator must yield at most that
	 * many.
	 * 
	 * <p>
	 * Each subsequent call must produce either the same size hint as the previous
	 * call, or a new size hint that is <i>more restrictive</i> than the previous
	 * one.
	 * </p>
	 * 
	 * <p>
	 * The default implementation of this method returns a size hint that is valid
	 * for all iterators.
	 * </p>
	 * 
	 * @return The size hint for this iterator.
	 */
	default SizeHint sizeHint() {
		return SizeHint.UNKNOWN;
	}

	default boolean hasProperties(int propertyMask) {
		return (properties() & propertyMask) == propertyMask;
	}

	default int properties() {
		return 0;
	}

	default int fillArray(int[] array) {
		return fillArray(array, 0, array.length);
	}

	/**
	 * Writes as many elements yielded from this iterator as can fit in the slice of
	 * the given array from start (inclusive) to end (exclusive). This method may
	 * only partially fill the given array.
	 * 
	 * @param array The array to write elements to
	 * @param start The start index (inclusive)
	 * @param end   The end index (exclusive)
	 * @return The amount of elements that were written into the array
	 * 
	 * @throws IllegalArgumentException when the end index is greater than the array
	 *                                  length, the start index is greater than the
	 *                                  end index, or the start or end index is less
	 *                                  than than 0.
	 * @throws NullPointerException     when the array is null.
	 */
	default int fillArray(@Nonnull int[] array, int start, int end) {
		if (end > array.length || start > end || start < 0 || end < 0)
			throw new IllegalArgumentException(String.format(
					"Invalid range bounds of [%d, %d) for array of length %d",
					start, end, array.length));
		int i = start;
		while (i < end && hasNext())
			array[i++] = next();
		return i - start;
	}

	default <C extends MutableListInt> C collectTo(Supplier<C> listFactory) {
		final var res = listFactory.get();
		res.extend(this);
		return res;
	}

	default MaybeInt find(IntPredicate predicate) {
		while (hasNext()) {
			final var value = next();
			if (predicate.test(value))
				return MaybeInt.some(value);
		}
		return MaybeInt.none();
	}

	default int indexOf(IntPredicate predicate) {
		for (int i = 0; hasNext(); ++i) {
			final var value = next();
			if (predicate.test(value))
				return i;
		}
		return -1;
	}

	default boolean any(IntPredicate predicate) {
		while (hasNext()) {
			final var value = next();
			if (predicate.test(value))
				return true;
		}
		return false;
	}

	default boolean all(IntPredicate predicate) {
		while (hasNext()) {
			final var value = next();
			if (!predicate.test(value))
				return false;
		}
		return true;
	}

	default MaybeInt min(IntComparator comparator) {
		boolean foundAny = hasNext();
		if (!foundAny)
			return MaybeInt.none();
		int minValue = next();
		while (hasNext()) {
			final var value = next();
			if (comparator.compare(minValue, value) >= 0) {
				minValue = value;
			}
		}
		return MaybeInt.some(minValue);
	}

	default MaybeInt max(IntComparator comparator) {
		boolean foundAny = hasNext();
		if (!foundAny)
			return MaybeInt.none();
		int maxValue = next();
		while (hasNext()) {
			final var value = next();
			if (comparator.compare(maxValue, value) <= 0) {
				maxValue = value;
			}
		}
		return MaybeInt.some(maxValue);
	}

	default int count() {
		int counter = 0;
		while (hasNext()) {
			next();
			counter += 1;
		}
		return counter;
	}

	/**
	 * Returns an iterator that never yields any values.
	 * 
	 * @param <T> The type of elements yielded by the iterator. Meaningless for this
	 *            method, but needed to get Java to allow composing this into other
	 *            iterators.
	 * @return The empty iterator instance.
	 */
	static IteratorInt empty() {
		// This iterator never produces any elements so this cast is ok.
		return Empty.INSTANCE;
	}

	final class Empty implements IteratorInt {
		public static Empty INSTANCE = new Empty();

		private Empty() {
		}

		@Override
		public SizeHint sizeHint() {
			return SizeHint.ZERO;
		}

		@Override
		public int properties() {
			return Iterator.PROPERTY_FUSED;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public int next() {
			return 0;
		}
	}

	static IteratorInt once(int value) {
		return new Once(value);
	}

	final class Once implements IteratorInt {
		private final int value;
		private boolean emitted = false;

		private Once(int value) {
			this.value = value;
		}

		@Override
		public SizeHint sizeHint() {
			return SizeHint.exactly(1);
		}

		@Override
		public int properties() {
			return Iterator.PROPERTY_FUSED;
		}

		@Override
		public boolean hasNext() {
			return !this.emitted;
		}

		@Override
		public int next() {
			this.emitted = true;
			return this.value;
		}
	}

	static IteratorInt repeat(int value) {
		return new Repeat(value, -1);
	}

	static IteratorInt repeat(int value, int limit) {
		return new Repeat(value, limit);
	}

	final class Repeat implements IteratorInt {
		private final int value;
		private int limit = -1;

		private Repeat(int value, int limit) {
			this.value = value;
			this.limit = limit;
		}

		@Override
		public int properties() {
			int props = Iterator.PROPERTY_FUSED;
			if (this.limit < 0)
				props |= Iterator.PROPERTY_INFINITE;
			return props;
		}

		@Override
		public boolean hasNext() {
			return this.limit > 0;
		}

		@Override
		public int next() {
			this.limit -= 1;
			return this.value;
		}

		@Override
		public int fillArray(@Nonnull int[] array, int start, int end) {
			if (end > array.length || start > end || start < 0 || end < 0)
				throw new IllegalArgumentException(String.format(
						"Invalid range bounds of [%d, %d) for array of length %d",
						start, end, array.length));
			if (this.limit < 0) {
				Arrays.fill(array, start, end, this.value);
				return end - start;
			} else {
				final var copyAmount = Math.min(end - start, this.limit);
				Arrays.fill(array, start, start + copyAmount, this.value);
				this.limit -= copyAmount;
				return copyAmount;
			}
		}

		@Override
		public <U> Iterator<U> map(Int2ObjectFunction<? extends U> mapper) {
			return Iterator.repeat(mapper.get(this.value), this.limit);
		}

		@Override
		public IteratorInt mapToInt(IntUnaryOperator mapper) {
			return new Repeat(mapper.apply(this.value), this.limit);
		}

		@Override
		public IteratorInt skip(int amount) {
			if (this.limit >= 0)
				this.limit = Math.max(0, this.limit - amount);
			return this;
		}

		@Override
		public IteratorInt limit(int limit) {
			this.limit = Math.min(limit, this.limit);
			return this;
		}
	}

	static IteratorInt fromBuffer(IntBuffer buffer) {
		return new IntBufferIterator(buffer);
	}

	final class IntBufferIterator implements IteratorInt {
		private final IntBuffer buffer;

		private IntBufferIterator(IntBuffer buffer) {
			this.buffer = buffer.duplicate();
		}

		@Override
		public boolean hasNext() {
			return this.buffer.hasRemaining();
		}

		@Override
		public int next() {
			return this.buffer.get();
		}

		@Override
		public SizeHint sizeHint() {
			return SizeHint.exactly(this.buffer.remaining());
		}

		@Override
		public int fillArray(@Nonnull int[] array, int start, int end) {
			if (end > array.length || start > end || start < 0 || end < 0)
				throw new IllegalArgumentException(String.format(
						"Invalid range bounds of [%d, %d) for array of length %d",
						start, end, array.length));
			final var position = this.buffer.position();
			this.buffer.get(array, start, Math.min(end - start, this.buffer.limit() - position));
			return this.buffer.position() - position;
		}

		@Override
		public IteratorInt skip(int amount) {
			this.buffer.position(Math.min(this.buffer.position() + amount, this.buffer.limit()));
			return this;
		}

		@Override
		public IteratorInt limit(int limit) {
			this.buffer.limit(Math.min(this.buffer.position() + limit, this.buffer.limit()));
			return this;
		}
	}

	default IteratorInt inspect(IntConsumer callback) {
		return new Inspect<>(this, callback);
	}

	final class Inspect<T> implements IteratorInt {
		private final IteratorInt source;
		private final IntConsumer callback;

		public Inspect(IteratorInt source, IntConsumer callback) {
			this.source = source;
			this.callback = callback;
		}

		@Override
		public SizeHint sizeHint() {
			return this.source.sizeHint();
		}

		@Override
		public boolean hasNext() {
			return this.source.hasNext();
		}

		@Override
		public int next() {
			final var value = this.source.next();
			this.callback.accept(value);
			return value;
		}

		@Override
		public void forEach(IntConsumer consumer) {
			this.source.forEach(value -> {
				this.callback.accept(value);
				consumer.accept(value);
			});
		}

		@Override
		public int properties() {
			return this.source.properties();
		}

		@Override
		public int count() {
			return this.source.count();
		}

	}

	default Iterator<Enumerate.Item> enumerate() {
		return new Enumerate(this);
	}

	final class Enumerate implements Iterator<Enumerate.Item> {
		private final IteratorInt source;
		private final Item item = new Item();
		private int currentIndex = 0;

		public static final class Item {
			public int index;
			public int item;
		}

		public Enumerate(IteratorInt source) {
			this.source = source;
		}

		@Override
		public SizeHint sizeHint() {
			return this.source.sizeHint();
		}

		@Override
		public boolean hasNext() {
			return this.source.hasNext();
		}

		@Override
		public Item next() {
			this.item.index = this.currentIndex++;
			this.item.item = this.source.next();
			return this.item;
		}

		@Override
		public void forEach(Consumer<? super Item> consumer) {
			this.source.forEach(value -> {
				this.item.index = this.currentIndex++;
				this.item.item = value;
				consumer.accept(this.item);
			});
		}

		@Override
		public int properties() {
			return this.source.properties();
		}

		@Override
		public int count() {
			return this.source.count();
		}

	}

	default IteratorInt filter(IntPredicate predicate) {
		return new Filter(this) {
			@Override
			protected boolean test(int value) {
				return predicate.test(value);
			}
		};
	}

	abstract class Filter implements IteratorInt {
		private final IteratorInt source;
		private boolean hasNext = false;
		private int current;

		public Filter(IteratorInt source) {
			this.source = source;
		}

		@Override
		public SizeHint sizeHint() {
			return this.source.sizeHint().withLowerBound(0);
		}

		protected abstract boolean test(int value);

		private void loadNext() {
			if (this.hasNext)
				return;
			while (this.source.hasNext()) {
				this.current = this.source.next();
				if (test(this.current)) {
					this.hasNext = true;
					break;
				}
			}
		}

		@Override
		public boolean hasNext() {
			loadNext();
			return this.hasNext;
		}

		@Override
		public int next() {
			loadNext();
			this.hasNext = false;
			return this.current;
		}

		@Override
		public void forEach(IntConsumer consumer) {
			this.source.forEach(value -> {
				if (test(value))
					consumer.accept(value);
			});
		}

		@Override
		public int properties() {
			return this.source.properties();
		}
	}

	default <U> Iterator<U> map(Int2ObjectFunction<? extends U> mapper) {
		return new Map<>(this, mapper);
	}

	default IteratorInt mapToInt(IntUnaryOperator mapper) {
		return new MapInt(this, mapper);
	}

	final class Map<U> implements Iterator<U> {
		private final IteratorInt source;
		private final Int2ObjectFunction<? extends U> mapper;

		private Map(IteratorInt source, Int2ObjectFunction<? extends U> mapper) {
			this.source = source;
			this.mapper = mapper;
		}

		@Override
		public SizeHint sizeHint() {
			return this.source.sizeHint();
		}

		@Override
		public boolean hasNext() {
			return this.source.hasNext();
		}

		@Override
		public U next() {
			return this.mapper.get(this.source.next());
		}

		@Override
		public void forEach(Consumer<? super U> consumer) {
			this.source.forEach(value -> consumer.accept(this.mapper.get(value)));
		}

		@Override
		public int properties() {
			return this.source.properties();
		}

		@Override
		public <V> Iterator<V> map(Function<? super U, ? extends V> mapper) {
			return new IteratorInt.Map<>(this.source, x -> mapper.apply(this.mapper.get(x)));
		}

		@Override
		public int count() {
			return this.source.count();
		}
	}

	final class MapInt implements IteratorInt {
		private final IteratorInt source;
		private final IntUnaryOperator mapper;

		private MapInt(IteratorInt source, IntUnaryOperator mapper) {
			this.source = source;
			this.mapper = mapper;
		}

		@Override
		public SizeHint sizeHint() {
			return this.source.sizeHint();
		}

		@Override
		public boolean hasNext() {
			return this.source.hasNext();
		}

		@Override
		public int next() {
			return this.mapper.apply(this.source.next());
		}

		@Override
		public void forEach(IntConsumer consumer) {
			this.source.forEach(value -> consumer.accept(this.mapper.apply(value)));
		}

		@Override
		public int properties() {
			return this.source.properties();
		}

		@Override
		public <U> Iterator<U> map(Int2ObjectFunction<? extends U> mapper) {
			return new Map<>(this.source, x -> mapper.get(this.mapper.apply(x)));
		}

		@Override
		public MapInt mapToInt(IntUnaryOperator mapper) {
			return new MapInt(this.source, x -> mapper.apply(this.mapper.apply(x)));
		}

		@Override
		public int count() {
			return this.source.count();
		}
	}

	default <U> Iterator<U> flatMap(Int2ObjectFunction<? extends IntoIterator<? extends U>> mapper) {
		return new FlatMap<>(this, mapper);
	}

	final class FlatMap<U> implements Iterator<U> {
		private final IteratorInt source;
		private final Int2ObjectFunction<? extends IntoIterator<? extends U>> mapper;

		private Iterator<? extends U> currentIter;
		private U current = null;
		private boolean hasNext = false;

		public FlatMap(IteratorInt source, Int2ObjectFunction<? extends IntoIterator<? extends U>> mapper) {
			this.source = source;
			this.mapper = mapper;
		}

		private void loadNext() {
			if (this.hasNext)
				return;
			while (this.currentIter == null || !this.currentIter.hasNext()) {
				if (!this.source.hasNext())
					return;
				this.currentIter = this.mapper.get(this.source.next()).iter();
			}

			this.current = this.currentIter.next();
			this.hasNext = true;
		}

		@Override
		public boolean hasNext() {
			loadNext();
			return this.hasNext;
		}

		@Override
		public U next() {
			loadNext();
			this.hasNext = false;
			return this.current;
		}

		@Override
		public void forEach(Consumer<? super U> consumer) {
			this.source.forEach(value -> this.mapper.get(value).forEach(consumer));
		}

		@Override
		public int properties() {
			return this.source.properties();
		}

		@Override
		public int count() {
			int count = 0;
			while (this.source.hasNext()) {
				final var mapped = this.mapper.get(this.source.next()).iter();
				count += mapped.count();
			}
			return count;
		}
	}

	/**
	 * Chains two iterators, such that all elements from {@code this} are yielded
	 * first, followed by all elements from {@code other}.
	 * 
	 * <p>
	 * Note that this may treat all source iterators as if they were fused by
	 * advancing to the next source after the previous one's {@link #hasNext()} has
	 * returned {@code false}.
	 * </p>
	 * 
	 * @param other The iterator whose elements are "appended" to {@code this}
	 * @return The chained iterator.
	 */
	default IteratorInt chain(IntoIteratorInt other) {
		return new Chain(this, other.iter());
	}

	final class Chain implements IteratorInt {
		private final IteratorInt[] sources;
		private int currentSource = 0;
		private boolean hasNext = false;

		@SafeVarargs
		public Chain(IteratorInt... sources) {
			this.sources = Arrays.copyOf(sources, sources.length);
		}

		private void loadNext() {
			if (this.hasNext)
				return;
			if (this.currentSource >= this.sources.length)
				return;
			this.hasNext = this.sources[this.currentSource].hasNext();
			while (!this.hasNext) {
				this.currentSource += 1;
				if (this.currentSource >= this.sources.length) {
					return;
				}
				this.hasNext = this.sources[this.currentSource].hasNext();
			}

		}

		@Override
		public boolean hasNext() {
			loadNext();
			return this.currentSource < this.sources.length;
		}

		@Override
		public int next() {
			loadNext();
			this.hasNext = false;
			return this.sources[this.currentSource].next();
		}

		@Override
		public void forEach(IntConsumer consumer) {
			for (final var source : this.sources)
				source.forEach(consumer);
		}

		@Override
		public IteratorInt chain(IntoIteratorInt other) {
			if (other instanceof Chain otherChain) {
				final var selfLen = this.sources.length - this.currentSource;
				final var otherLen = otherChain.sources.length - otherChain.currentSource;
				final var newSources = (IteratorInt[]) new Iterator[selfLen + otherLen];
				System.arraycopy(this.sources, this.currentSource, newSources, 0,
						this.sources.length - this.currentSource);
				System.arraycopy(otherChain.sources, otherChain.currentSource, newSources, selfLen,
						this.sources.length - this.currentSource);
				return new Chain(newSources);
			}
			return IteratorInt.super.chain(other);
		}

		@Override
		public IteratorInt limit(int limit) {
			if (this.sources.length > 0) {
				if (limit <= this.sources[0].sizeHint().lowerBound()) {
					return this.sources[0];
				}
			}
			return IteratorInt.super.limit(limit);
		}

		@Override
		public int properties() {
			int props = Iterator.PROPERTY_FUSED;
			for (final var source : this.sources) {
				props |= source.properties() & Iterator.PROPERTY_INFINITE;
			}
			return props;
		}

		@Override
		public int count() {
			int count = 0;
			for (final var source : this.sources) {
				count += source.count();
			}
			return count;
		}
	}

	default IteratorInt limit(int limit) {
		if (limit == 0)
			return empty();
		return new Limit<>(this, limit);
	}

	final class Limit<T> implements IteratorInt {
		private final IteratorInt source;
		private final int limit;
		private int taken = 0;

		public Limit(IteratorInt source, int limit) {
			this.source = source;
			this.limit = limit;
		}

		@Override
		public SizeHint sizeHint() {
			return this.source.sizeHint().withUpperBound(this.limit);
		}

		@Override
		public boolean hasNext() {
			return this.taken < this.limit && this.source.hasNext();
		}

		@Override
		public int next() {
			this.taken += 1;
			return this.source.next();
		}

		@Override
		public IteratorInt limit(int limit) {
			if (limit >= this.limit)
				return this;
			return new Limit<>(this.source, limit);
		}

		@Override
		public int properties() {
			return this.source.properties();
		}
	}

	default IteratorInt skip(int amount) {
		if (amount == 0)
			return this;
		return new Skip<>(this, amount);
	}

	final class Skip<T> implements IteratorInt {
		private final IteratorInt source;
		private int remaining;

		public Skip(IteratorInt source, int amount) {
			this.source = source;
			this.remaining = amount;
		}

		@Override
		public SizeHint sizeHint() {
			final var hint = this.source.sizeHint();
			final var min = Math.max(0, hint.lowerBound() - this.remaining);
			final var max = hint.upperBound().mapInt(bound -> Math.max(0, bound - this.remaining));
			return new SizeHint(min, max);
		}

		private void skipIfNeeded() {
			while (this.remaining > 0 && this.source.hasNext()) {
				this.source.next();
				this.remaining -= 1;
			}
		}

		@Override
		public boolean hasNext() {
			skipIfNeeded();
			return this.source.hasNext();
		}

		@Override
		public int next() {
			skipIfNeeded();
			return this.source.next();
		}

		@Override
		public void forEach(IntConsumer consumer) {
			while (this.remaining > 0 && this.source.hasNext()) {
				this.source.next();
				this.remaining -= 1;
			}
			this.source.forEach(consumer);
		}

		@Override
		public IteratorInt skip(int amount) {
			if (amount == 0)
				return this;
			return new Skip<>(this.source, this.remaining + amount);
		}

		@Override
		public int properties() {
			return this.source.properties();
		}

		@Override
		public int count() {
			return Math.max(0, this.source.count() - this.remaining);
		}
	}

	default IteratorInt fused() {
		if (Iterator.hasProperties(this, Iterator.PROPERTY_FUSED))
			return this;
		return new Fused(this);
	}

	final class Fused implements IteratorInt {
		private final IteratorInt source;
		private boolean canYield = true;

		public Fused(IteratorInt source) {
			this.source = source;
		}

		@Override
		public SizeHint sizeHint() {
			return this.source.sizeHint();
		}

		@Override
		public boolean hasNext() {
			final var hasNext = this.source.hasNext();
			this.canYield &= hasNext;
			return this.canYield;
		}

		@Override
		public int next() {
			return this.source.next();
		}

		@Override
		public void forEach(IntConsumer consumer) {
			this.source.forEach(consumer);
		}

		@Override
		public int properties() {
			return this.source.properties() | Iterator.PROPERTY_FUSED;
		}

		@Override
		public int count() {
			return this.source.count();
		}
	}

}
