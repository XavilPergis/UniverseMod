package net.xavil.hawklib;

import java.util.Objects;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.ints.IntPredicate;
import net.xavil.hawklib.collections.iterator.IntoIteratorInt;
import net.xavil.hawklib.collections.iterator.IteratorInt;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

public abstract sealed class MaybeInt implements IntoIteratorInt, Hashable {

	public static MaybeInt.None none() {
		return None.INSTANCE;
	}

	public static MaybeInt some(int value) {
		return new Some(value);
	}

	public final boolean isSome() {
		return this instanceof Some;
	}

	public final boolean isNone() {
		return !isSome();
	}

	public abstract <U> Maybe<U> map(Int2ObjectFunction<? extends U> mapper);

	public abstract <U> Maybe<U> flatMap(Int2ObjectFunction<Maybe<? extends U>> mapper);

	public abstract MaybeInt filter(IntPredicate predicate);

	public abstract int unwrap();

	public abstract int unwrapOr(int defaultValue);
	
	// public abstract int unwrapOrElse(IntSupplier defaultValue);

	public abstract MaybeInt or(MaybeInt other);

	public abstract MaybeInt orElse(Supplier<MaybeInt> other);

	public abstract void ifSome(IntConsumer consumer);

	public abstract void ifNone(Runnable runnable);

	public abstract boolean innerEquals(int value);

	public static final class None extends MaybeInt {

		private static final None INSTANCE = new None();

		@Override
		public IteratorInt iter() {
			return IteratorInt.empty();
		}

		@Override
		public void forEach(IntConsumer consumer) {
		}

		@Override
		public <U> Maybe.None<U> map(Int2ObjectFunction<? extends U> mapper) {
			return Maybe.none();
		}

		@Override
		public <U> Maybe.None<U> flatMap(Int2ObjectFunction<Maybe<? extends U>> mapper) {
			return Maybe.none();
		}

		@Override
		public None filter(IntPredicate predicate) {
			return MaybeInt.none();
		}

		@Override
		public int unwrap() {
			throw new RuntimeException("tried to unwrap an `Option` that was `None`!");
		}

		@Override
		public int unwrapOr(int defaultValue) {
			return defaultValue;
		}

		// @Override
		// public T unwrapOrElse(Supplier<T> defaultValue) {
		// 	return defaultValue.get();
		// }

		@Override
		public MaybeInt or(MaybeInt other) {
			return other;
		}

		@Override
		public MaybeInt orElse(Supplier<MaybeInt> other) {
			return other.get();
		}

		@Override
		public void ifSome(IntConsumer consumer) {
		}

		@Override
		public void ifNone(Runnable runnable) {
			runnable.run();
		}

		@Override
		public boolean innerEquals(int value) {
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof None;
		}

		@Override
		public int hashCode() {
			return FastHasher.hashToInt(this);
		}

		@Override
		public void appendHash(Hasher hasher) {
		}

		@Override
		public String toString() {
			return "None";
		}
	}

	public static final class Some extends MaybeInt {
		public final int value;

		public Some(int value) {
			this.value = value;
		}

		@Override
		public IteratorInt iter() {
			return IteratorInt.once(this.value);
		}

		@Override
		public void forEach(IntConsumer consumer) {
			consumer.accept(this.value);
		}

		@Override
		public <U> Maybe.Some<U> map(Int2ObjectFunction<? extends U> mapper) {
			return new Maybe.Some<>(mapper.get(this.value));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <U> Maybe<U> flatMap(Int2ObjectFunction<Maybe<? extends U>> mapper) {
			return (Maybe<U>) mapper.get(this.value);
		}

		@Override
		public MaybeInt filter(IntPredicate predicate) {
			return predicate.test(this.value) ? this : MaybeInt.none();
		}

		@Override
		public int unwrap() {
			return this.value;
		}

		@Override
		public int unwrapOr(int defaultValue) {
			return this.value;
		}

		// @Override
		// public int unwrapOrElse(Supplier<int> defaultValue) {
		// 	return this.value;
		// }

		@Override
		public MaybeInt or(MaybeInt other) {
			return this;
		}

		@Override
		public MaybeInt orElse(Supplier<MaybeInt> other) {
			return this;
		}

		@Override
		public void ifSome(IntConsumer consumer) {
			consumer.accept(this.value);
		}

		@Override
		public void ifNone(Runnable runnable) {
		}

		@Override
		public boolean innerEquals(int value) {
			return Objects.equals(this.value, value);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Some other ? Objects.equals(this.value, other.value) : false;
		}

		@Override
		public int hashCode() {
			return FastHasher.hashToInt(this);
		}

		@Override
		public void appendHash(Hasher hasher) {
			hasher.appendInt(this.value);
		}

		@Override
		public String toString() {
			return "Some(" + this.value + ")";
		}
	}

}
