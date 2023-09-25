package net.xavil.hawklib.collections.iterator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.interfaces.MutableList;

/**
 * A lazily-evaluated arbitrary sequence of values. Roughly analagous to
 * {@link java.util.Iterator}, but with a more ergonomic interface.
 * 
 * @param <T> The type of element yielded by this iterator.
 */
public interface Iterator<T> extends IntoIterator<T> {

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
	T next();

	/**
	 * To allow both {@link Iterator}s and other things like collections or ranges
	 * to be used directly as arguments for certain operations, {@link Iterator}
	 * itself implements {@link IntoIterator}.
	 * 
	 * @return {@code this}.
	 */
	@Override
	default Iterator<T> iter() {
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

	/**
	 * Adapts an iterator to a Java standard library iterator.
	 * 
	 * @param <T>  The type that the iterator yields.
	 * @param iter The source iterator.
	 * @return The adapted iterator.
	 */
	static <T> java.util.Iterator<T> asJava(Iterator<T> iter) {
		// @formatter:off
		return new java.util.Iterator<T>() {
			@Override public boolean hasNext() { return iter.hasNext(); }
			@Override public T next()          { return iter.next(); }
			@Override public void forEachRemaining(Consumer<? super T> consumer) { iter.forEach(consumer); }
		};
		// @formatter:on
	}

	static <T> Iterator<T> fromJava(java.util.Iterator<T> iter) {
		// @formatter:off
		return new Iterator<T>() {
			@Override public boolean hasNext() { return iter.hasNext(); }
			@Override public T next()          { return iter.next(); }
			@Override public void forEach(Consumer<? super T> consumer) { iter.forEachRemaining(consumer); }
		};
		// @formatter:on
	}

	default <C extends MutableList<T>> C collectTo(Supplier<C> listFactory) {
		final var res = listFactory.get();
		res.extend(this);
		return res;
	}

	default Maybe<T> find(Predicate<? super T> predicate) {
		while (hasNext()) {
			final var value = next();
			if (predicate.test(value))
				return Maybe.some(value);
		}
		return Maybe.none();
	}

	default int indexOf(Predicate<? super T> predicate) {
		for (int i = 0; hasNext(); ++i) {
			final var value = next();
			if (predicate.test(value))
				return i;
		}
		return -1;
	}

	default boolean any(Predicate<? super T> predicate) {
		while (hasNext()) {
			final var value = next();
			if (predicate.test(value))
				return true;
		}
		return false;
	}

	default boolean all(Predicate<? super T> predicate) {
		while (hasNext()) {
			final var value = next();
			if (!predicate.test(value))
				return false;
		}
		return true;
	}

	default Maybe<T> min(Comparator<? super T> comparator) {
		T minValue = null;
		while (hasNext()) {
			final var value = next();
			if (minValue == null || comparator.compare(minValue, value) >= 0) {
				minValue = value;
			}
		}
		return Maybe.fromNullable(minValue);
	}

	default Maybe<T> max(Comparator<? super T> comparator) {
		T maxValue = null;
		while (hasNext()) {
			final var value = next();
			if (maxValue == null || comparator.compare(maxValue, value) <= 0) {
				maxValue = value;
			}
		}
		return Maybe.fromNullable(maxValue);
	}

	default int count() {
		int counter = 0;
		while (hasNext()) {
			next();
			counter += 1;
		}
		return counter;
	}

	default <U> U fold(U acc, BiFunction<? super U, ? super T, ? extends U> updater) {
		while (hasNext())
			acc = updater.apply(acc, next());
		return acc;
	}

	/**
	 * Returns an iterator that never yields any values.
	 * 
	 * @param <T> The type of elements yielded by the iterator. Meaningless for this
	 *            method, but needed to get Java to allow composing this into other
	 *            iterators.
	 * @return The empty iterator instance.
	 */
	@SuppressWarnings("unchecked")
	static <T> Iterator<T> empty() {
		// This iterator never produces any elements so this cast is ok.
		return (Iterator<T>) Empty.INSTANCE;
	}

	final class Empty<T> implements Iterator<T> {
		public static Empty<?> INSTANCE = new Empty<>();

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
		public T next() {
			return null;
		}
	}

	static <T> Iterator<T> once(T value) {
		return new Once<>(value);
	}

	final class Once<T> implements Iterator<T> {
		private final T value;
		private boolean emitted = false;

		private Once(T value) {
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
		public T next() {
			this.emitted = true;
			return this.value;
		}
	}

	default Iterator<T> inspect(Consumer<? super T> callback) {
		return new Inspect<>(this, callback);
	}

	final class Inspect<T> implements Iterator<T> {
		private final Iterator<T> source;
		private final Consumer<? super T> callback;

		public Inspect(Iterator<T> source, Consumer<? super T> callback) {
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
		public T next() {
			final var value = this.source.next();
			this.callback.accept(value);
			return value;
		}

		@Override
		public void forEach(Consumer<? super T> consumer) {
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

	default Iterator<Enumerate.Item<T>> enumerate() {
		return new Enumerate<>(this);
	}

	final class Enumerate<T> implements Iterator<Enumerate.Item<T>> {
		private final Iterator<T> source;
		private final Item<T> item = new Item<>();
		private int currentIndex = 0;

		public static final class Item<T> {
			public int index;
			public T item;
		}

		public Enumerate(Iterator<T> source) {
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
		public Item<T> next() {
			this.item.index = this.currentIndex++;
			this.item.item = this.source.next();
			return this.item;
		}

		@Override
		public void forEach(Consumer<? super Item<T>> consumer) {
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

	default Iterator<T> filter(Predicate<? super T> predicate) {
		return new Filter<>(this) {
			@Override
			protected boolean test(T value) {
				return predicate.test(value);
			}
		};
	}

	default Iterator<T> filterNull() {
		if (hasProperties(PROPERTY_NONNULL))
			return this;
		return new Filter<T>(this) {
			@Override
			protected boolean test(T value) {
				return value != null;
			}

			@Override
			public int properties() {
				return super.properties() | PROPERTY_NONNULL;
			}
		};
	}

	abstract class Filter<T> implements Iterator<T> {
		private final Iterator<T> source;
		private boolean hasNext = false;
		private T current = null;

		public Filter(Iterator<T> source) {
			this.source = source;
		}

		@Override
		public SizeHint sizeHint() {
			return this.source.sizeHint().withLowerBound(0);
		}

		protected abstract boolean test(T value);

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
		public T next() {
			loadNext();
			this.hasNext = false;
			return this.current;
		}

		@Override
		public void forEach(Consumer<? super T> consumer) {
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

	default <U> Iterator<U> map(Function<? super T, ? extends U> mapper) {
		return new Map<>(this, mapper);
	}

	final class Map<T, U> implements Iterator<U> {
		private final Iterator<T> source;
		private final Function<? super T, ? extends U> mapper;

		private Map(Iterator<T> source, Function<? super T, ? extends U> mapper) {
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
			return this.mapper.apply(this.source.next());
		}

		@Override
		public void forEach(Consumer<? super U> consumer) {
			this.source.forEach(value -> consumer.accept(this.mapper.apply(value)));
		}

		@Override
		public int properties() {
			return this.source.properties() & ~PROPERTY_NONNULL;
		}

		@Override
		public <V> Iterator<V> map(Function<? super U, ? extends V> mapper) {
			return new Map<>(this.source, this.mapper.andThen(mapper));
		}

		@Override
		public int count() {
			return this.source.count();
		}
	}

	@SuppressWarnings("unchecked")
	default <U> Iterator<U> filterCast(Class<U> clazz) {
		return map(x -> x != null && clazz.isInstance(x) ? (U) x : null).filterNull();
	}

	default <U> Iterator<U> flatMap(Function<? super T, ? extends IntoIterator<? extends U>> mapper) {
		return new FlatMap<>(this, mapper);
	}

	final class FlatMap<T, U> implements Iterator<U> {
		private final Iterator<T> source;
		private final Function<? super T, ? extends IntoIterator<? extends U>> mapper;

		private Iterator<? extends U> currentIter;
		private U current = null;
		private boolean hasNext = false;

		public FlatMap(Iterator<T> source, Function<? super T, ? extends IntoIterator<? extends U>> mapper) {
			this.source = source;
			this.mapper = mapper;
		}

		private void loadNext() {
			if (this.hasNext)
				return;
			while (this.currentIter == null || !this.currentIter.hasNext()) {
				if (!this.source.hasNext())
					return;
				this.currentIter = this.mapper.apply(this.source.next()).iter();
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
			this.source.forEach(value -> this.mapper.apply(value).forEach(consumer));
		}

		@Override
		public int properties() {
			return this.source.properties() & ~PROPERTY_NONNULL;
		}

		@Override
		public int count() {
			int count = 0;
			while (this.source.hasNext()) {
				final var mapped = this.mapper.apply(this.source.next()).iter();
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
	default Iterator<T> chain(IntoIterator<T> other) {
		return new Chain<>(this, other.iter());
	}

	final class Chain<T> implements Iterator<T> {
		private final Iterator<T>[] sources;
		private int currentSource = 0;
		private boolean hasNext = false;

		@SafeVarargs
		public Chain(Iterator<T>... sources) {
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
		public T next() {
			loadNext();
			this.hasNext = false;
			return this.sources[this.currentSource].next();
		}

		@Override
		public void forEach(Consumer<? super T> consumer) {
			for (final var source : this.sources)
				source.forEach(consumer);
		}

		@Override
		@SuppressWarnings("unchecked")
		public Iterator<T> chain(IntoIterator<T> other) {
			if (other instanceof Chain<T> otherChain) {
				final var selfLen = this.sources.length - this.currentSource;
				final var otherLen = otherChain.sources.length - otherChain.currentSource;
				final var newSources = (Iterator<T>[]) new Iterator[selfLen + otherLen];
				System.arraycopy(this.sources, this.currentSource, newSources, 0,
						this.sources.length - this.currentSource);
				System.arraycopy(otherChain.sources, otherChain.currentSource, newSources, selfLen,
						this.sources.length - this.currentSource);
				return new Chain<>(newSources);
			}
			return Iterator.super.chain(other);
		}

		@Override
		public Iterator<T> limit(int limit) {
			if (this.sources.length > 0) {
				if (limit <= this.sources[0].sizeHint().lowerBound()) {
					return this.sources[0];
				}
			}
			return Iterator.super.limit(limit);
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

	default Iterator<T> limit(int limit) {
		if (limit == 0)
			return empty();
		return new Limit<>(this, limit);
	}

	final class Limit<T> implements Iterator<T> {
		private final Iterator<T> source;
		private final int limit;
		private int taken = 0;

		public Limit(Iterator<T> source, int limit) {
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
		public T next() {
			this.taken += 1;
			return this.source.next();
		}

		@Override
		public Iterator<T> limit(int limit) {
			if (limit >= this.limit)
				return this;
			return new Limit<>(this.source, limit);
		}

		@Override
		public int properties() {
			return this.source.properties();
		}
	}

	default Iterator<T> skip(int amount) {
		if (amount == 0)
			return this;
		return new Skip<>(this, amount);
	}

	final class Skip<T> implements Iterator<T> {
		private final Iterator<T> source;
		private int remaining;

		public Skip(Iterator<T> source, int amount) {
			this.source = source;
			this.remaining = amount;
		}

		@Override
		public SizeHint sizeHint() {
			final var hint = this.source.sizeHint();
			final var min = Math.max(0, hint.lowerBound() - this.remaining);
			final var max = hint.upperBound().isEmpty() ? OptionalInt.empty()
					: OptionalInt.of(Math.max(0, hint.upperBound().getAsInt() - this.remaining));
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
		public T next() {
			skipIfNeeded();
			return this.source.next();
		}

		@Override
		public void forEach(Consumer<? super T> consumer) {
			while (this.remaining > 0 && this.source.hasNext()) {
				this.source.next();
				this.remaining -= 1;
			}
			this.source.forEach(consumer);
		}

		@Override
		public Iterator<T> skip(int amount) {
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

	default Iterator<T> fused() {
		if (this.isFused())
			return this;
		return new Fused<>(this);
	}

	final class Fused<T> implements Iterator<T> {
		private final Iterator<T> source;
		private boolean canYield = true;

		public Fused(Iterator<T> source) {
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
		public T next() {
			return this.source.next();
		}

		@Override
		public void forEach(Consumer<? super T> consumer) {
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
