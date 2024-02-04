package net.xavil.hawklib.collections.iterator;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import it.unimi.dsi.fastutil.floats.Float2ObjectFunction;
import it.unimi.dsi.fastutil.floats.FloatComparator;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import it.unimi.dsi.fastutil.floats.FloatPredicate;
import net.xavil.hawklib.MaybeFloat;
import net.xavil.hawklib.collections.interfaces.MutableListFloat;

/**
 * A lazily-evaluated arbitrary sequence of values. Roughly analagous to
 * {@link java.util.Iterator}, but with a more ergonomic interface.
 */
public interface IteratorFloat extends IntoIteratorFloat {

	/**
	 * Returns whether the iterator has more elements to yield.
	 * 
	 * <p>
	 * It is valid for implementations to update their internal state when this
	 * method is called. Subsequent calls to this method must not return
	 * {@code false} if they previously returned {@code true}, unless
	 * {@link #next()} was called in between. If an iterator is fused (i.e., if
	 * {@link #isFused()} returns {@code true}), then this method cannot ever return
	 * anything other than {@code false} after a {@code false} was previously
	 * yielded.
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
	float next();

	/**
	 * To allow both {@link Iterator}s and other things like collections or ranges
	 * to be used directly as arguments for certain operations, {@link Iterator}
	 * itself implements {@link IntoIterator}.
	 * 
	 * @return {@code this}.
	 */
	@Override
	default IteratorFloat iter() {
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

	/**
	 * Returns whether this iterator is known to definitely be fused.
	 * 
	 * <p>
	 * Subsequent calls to this method <i>must</i> return the same value as the
	 * previous. i.e., if an iterator reports that it is fused, it must
	 * <i>always</i> report that it is fused.
	 * </p>
	 * 
	 * @return {@code true} if this iterator is definitely fused.
	 */
	default boolean isFused() {
		return hasProperties(PROPERTY_FUSED);
	}

	static final int PROPERTY_FUSED = 1 << 0;
	static final int PROPERTY_NONNULL = 1 << 1;

	default boolean hasProperties(int propertyMask) {
		return (properties() & propertyMask) == propertyMask;
	}

	default int properties() {
		return 0;
	}

	default int fillArray(float[] array) {
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
	default int fillArray(@NotNull float[] array, int start, int end) {
		if (end > array.length || start > end || start < 0 || end < 0)
			throw new IllegalArgumentException(String.format(
					"Invalid range bounds of [%d, %d) for array of length %d",
					start, end, array.length));
		int i = start;
		while (i < end && hasNext())
			array[i++] = next();
		return i - start;
	}

	default <C extends MutableListFloat> C collectTo(Supplier<C> listFactory) {
		final var res = listFactory.get();
		res.extend(this);
		return res;
	}

	default MaybeFloat find(FloatPredicate predicate) {
		while (hasNext()) {
			final var value = next();
			if (predicate.test(value))
				return MaybeFloat.some(value);
		}
		return MaybeFloat.none();
	}

	default int indexOf(FloatPredicate predicate) {
		for (int i = 0; hasNext(); ++i) {
			final var value = next();
			if (predicate.test(value))
				return i;
		}
		return -1;
	}

	default boolean any(FloatPredicate predicate) {
		while (hasNext()) {
			final var value = next();
			if (predicate.test(value))
				return true;
		}
		return false;
	}

	default boolean all(FloatPredicate predicate) {
		while (hasNext()) {
			final var value = next();
			if (!predicate.test(value))
				return false;
		}
		return true;
	}

	default MaybeFloat min(FloatComparator comparator) {
		boolean foundAny = hasNext();
		if (!foundAny) 
			return MaybeFloat.none();
		float minValue = next();
		while (hasNext()) {
			final var value = next();
			if (comparator.compare(minValue, value) >= 0) {
				minValue = value;
			}
		}
		return MaybeFloat.some(minValue);
	}

	default MaybeFloat max(FloatComparator comparator) {
		boolean foundAny = hasNext();
		if (!foundAny) 
			return MaybeFloat.none();
		float maxValue = next();
		while (hasNext()) {
			final var value = next();
			if (comparator.compare(maxValue, value) <= 0) {
				maxValue = value;
			}
		}
		return MaybeFloat.some(maxValue);
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
	static IteratorFloat empty() {
		// This iterator never produces any elements so this cast is ok.
		return Empty.INSTANCE;
	}

	final class Empty implements IteratorFloat {
		public static Empty INSTANCE = new Empty();

		private Empty() {
		}

		@Override
		public SizeHint sizeHint() {
			return SizeHint.ZERO;
		}

		@Override
		public int properties() {
			return PROPERTY_FUSED | PROPERTY_NONNULL;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public float next() {
			return 0;
		}
	}

	static IteratorFloat once(float value) {
		return new Once(value);
	}

	final class Once implements IteratorFloat {
		private final float value;
		private boolean emitted = false;

		private Once(float value) {
			this.value = value;
		}

		@Override
		public SizeHint sizeHint() {
			return SizeHint.exactly(1);
		}

		@Override
		public int properties() {
			return PROPERTY_FUSED;
		}

		@Override
		public boolean hasNext() {
			return !this.emitted;
		}

		@Override
		public float next() {
			this.emitted = true;
			return this.value;
		}
	}

	default IteratorFloat inspect(FloatConsumer callback) {
		return new Inspect<>(this, callback);
	}

	final class Inspect<T> implements IteratorFloat {
		private final IteratorFloat source;
		private final FloatConsumer callback;

		public Inspect(IteratorFloat source, FloatConsumer callback) {
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
		public float next() {
			final var value = this.source.next();
			this.callback.accept(value);
			return value;
		}

		@Override
		public void forEach(FloatConsumer consumer) {
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
		private final IteratorFloat source;
		private final Item item = new Item();
		private int currentIndex = 0;

		public static final class Item {
			public int index;
			public float item;
		}

		public Enumerate(IteratorFloat source) {
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

	default IteratorFloat filter(FloatPredicate predicate) {
		return new Filter(this) {
			@Override
			protected boolean test(float value) {
				return predicate.test(value);
			}
		};
	}

	// filter out NaNs
	default IteratorFloat filterNan() {
		if (hasProperties(PROPERTY_NONNULL))
			return this;
		return new Filter(this) {
			@Override
			protected boolean test(float value) {
				return !Float.isNaN(value);
			}

			@Override
			public int properties() {
				return super.properties() | PROPERTY_NONNULL;
			}
		};
	}

	// filter out infinities
	default IteratorFloat filterInfinity() {
		if (hasProperties(PROPERTY_NONNULL))
			return this;
		return new Filter(this) {
			@Override
			protected boolean test(float value) {
				return !Float.isInfinite(value);
			}

			@Override
			public int properties() {
				return super.properties() | PROPERTY_NONNULL;
			}
		};
	}

	// filter out NaNs and infinities
	default IteratorFloat filterNonFinite() {
		if (hasProperties(PROPERTY_NONNULL))
			return this;
		return new Filter(this) {
			@Override
			protected boolean test(float value) {
				return Float.isFinite(value);
			}

			@Override
			public int properties() {
				return super.properties() | PROPERTY_NONNULL;
			}
		};
	}

	abstract class Filter implements IteratorFloat {
		private final IteratorFloat source;
		private boolean hasNext = false;
		private float current;

		public Filter(IteratorFloat source) {
			this.source = source;
		}

		@Override
		public SizeHint sizeHint() {
			return this.source.sizeHint().withLowerBound(0);
		}

		protected abstract boolean test(float value);

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
		public float next() {
			loadNext();
			this.hasNext = false;
			return this.current;
		}

		@Override
		public void forEach(FloatConsumer consumer) {
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

	default <U> Iterator<U> map(Float2ObjectFunction<? extends U> mapper) {
		return new Map<>(this, mapper);
	}

	default IteratorFloat mapToFloat(FloatUnaryOperator mapper) {
		return new MapFloat(this, mapper);
	}

	final class Map<U> implements Iterator<U> {
		private final IteratorFloat source;
		private final Float2ObjectFunction<? extends U> mapper;

		private Map(IteratorFloat source, Float2ObjectFunction<? extends U> mapper) {
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
			return this.source.properties() & ~PROPERTY_NONNULL;
		}

		@Override
		public <V> Iterator<V> map(Function<? super U, ? extends V> mapper) {
			return new IteratorFloat.Map<>(this.source, x -> mapper.apply(this.mapper.get(x)));
		}

		@Override
		public int count() {
			return this.source.count();
		}
	}

	final class MapFloat implements IteratorFloat {
		private final IteratorFloat source;
		private final FloatUnaryOperator mapper;

		private MapFloat(IteratorFloat source, FloatUnaryOperator mapper) {
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
		public float next() {
			return this.mapper.apply(this.source.next());
		}

		@Override
		public void forEach(FloatConsumer consumer) {
			this.source.forEach(value -> consumer.accept(this.mapper.apply(value)));
		}

		@Override
		public int properties() {
			return this.source.properties() & ~PROPERTY_NONNULL;
		}

		@Override
		public <U> Iterator<U> map(Float2ObjectFunction<? extends U> mapper) {
			return new Map<>(this.source, x -> mapper.get(this.mapper.apply(x)));
		}

		@Override
		public MapFloat mapToFloat(FloatUnaryOperator mapper) {
			return new MapFloat(this.source, x -> mapper.apply(this.mapper.apply(x)));
		}

		@Override
		public int count() {
			return this.source.count();
		}
	}

	default <U> Iterator<U> flatMap(Float2ObjectFunction<? extends IntoIterator<? extends U>> mapper) {
		return new FlatMap<>(this, mapper);
	}

	final class FlatMap<U> implements Iterator<U> {
		private final IteratorFloat source;
		private final Float2ObjectFunction<? extends IntoIterator<? extends U>> mapper;

		private Iterator<? extends U> currentIter;
		private U current = null;
		private boolean hasNext = false;

		public FlatMap(IteratorFloat source, Float2ObjectFunction<? extends IntoIterator<? extends U>> mapper) {
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
			return this.source.properties() & ~PROPERTY_NONNULL;
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
	default IteratorFloat chain(IntoIteratorFloat other) {
		return new Chain(this, other.iter());
	}

	final class Chain implements IteratorFloat {
		private final IteratorFloat[] sources;
		private int currentSource = 0;
		private boolean hasNext = false;

		@SafeVarargs
		public Chain(IteratorFloat... sources) {
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
		public float next() {
			loadNext();
			this.hasNext = false;
			return this.sources[this.currentSource].next();
		}

		@Override
		public void forEach(FloatConsumer consumer) {
			for (final var source : this.sources)
				source.forEach(consumer);
		}

		@Override
		public IteratorFloat chain(IntoIteratorFloat other) {
			if (other instanceof Chain otherChain) {
				final var selfLen = this.sources.length - this.currentSource;
				final var otherLen = otherChain.sources.length - otherChain.currentSource;
				final var newSources = (IteratorFloat[]) new Iterator[selfLen + otherLen];
				System.arraycopy(this.sources, this.currentSource, newSources, 0,
						this.sources.length - this.currentSource);
				System.arraycopy(otherChain.sources, otherChain.currentSource, newSources, selfLen,
						this.sources.length - this.currentSource);
				return new Chain(newSources);
			}
			return IteratorFloat.super.chain(other);
		}

		@Override
		public IteratorFloat limit(int limit) {
			if (this.sources.length > 0) {
				if (limit <= this.sources[0].sizeHint().lowerBound()) {
					return this.sources[0];
				}
			}
			return IteratorFloat.super.limit(limit);
		}

		@Override
		public int properties() {
			int props = PROPERTY_FUSED | PROPERTY_NONNULL;
			for (final var source : this.sources) {
				final var childProps = source.properties();
				if ((childProps & PROPERTY_NONNULL) == 0)
					props = props & ~PROPERTY_NONNULL;
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

	default IteratorFloat limit(int limit) {
		if (limit == 0)
			return empty();
		return new Limit<>(this, limit);
	}

	final class Limit<T> implements IteratorFloat {
		private final IteratorFloat source;
		private final int limit;
		private int taken = 0;

		public Limit(IteratorFloat source, int limit) {
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
		public float next() {
			this.taken += 1;
			return this.source.next();
		}

		@Override
		public IteratorFloat limit(int limit) {
			if (limit >= this.limit)
				return this;
			return new Limit<>(this.source, limit);
		}

		@Override
		public int properties() {
			return this.source.properties();
		}
	}

	default IteratorFloat skip(int amount) {
		if (amount == 0)
			return this;
		return new Skip<>(this, amount);
	}

	final class Skip<T> implements IteratorFloat {
		private final IteratorFloat source;
		private int remaining;

		public Skip(IteratorFloat source, int amount) {
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
		public float next() {
			skipIfNeeded();
			return this.source.next();
		}

		@Override
		public void forEach(FloatConsumer consumer) {
			while (this.remaining > 0 && this.source.hasNext()) {
				this.source.next();
				this.remaining -= 1;
			}
			this.source.forEach(consumer);
		}

		@Override
		public IteratorFloat skip(int amount) {
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

	default IteratorFloat fused() {
		if (this.isFused())
			return this;
		return new Fused(this);
	}

	final class Fused implements IteratorFloat {
		private final IteratorFloat source;
		private boolean canYield = true;

		public Fused(IteratorFloat source) {
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
		public float next() {
			return this.source.next();
		}

		@Override
		public void forEach(FloatConsumer consumer) {
			this.source.forEach(consumer);
		}

		@Override
		public int properties() {
			return this.source.properties() | PROPERTY_FUSED;
		}

		@Override
		public int count() {
			return this.source.count();
		}
	}

}
