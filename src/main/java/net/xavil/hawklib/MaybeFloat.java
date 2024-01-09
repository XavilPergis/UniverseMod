package net.xavil.hawklib;

import java.util.Objects;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.floats.Float2ObjectFunction;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import it.unimi.dsi.fastutil.floats.FloatPredicate;
import net.xavil.hawklib.collections.iterator.IntoIteratorFloat;
import net.xavil.hawklib.collections.iterator.IteratorFloat;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

public abstract sealed class MaybeFloat implements IntoIteratorFloat, Hashable {

	public static MaybeFloat.None none() {
		return None.INSTANCE;
	}

	public static MaybeFloat some(float value) {
		return new Some(value);
	}

	public static MaybeFloat fromNanable(float value) {
		return Float.isNaN(value) ? none() : new Some(value);
	}

	public final boolean isSome() {
		return this instanceof Some;
	}

	public final boolean isNone() {
		return !isSome();
	}

	public abstract <U> Maybe<U> map(Float2ObjectFunction<? extends U> mapper);

	public abstract <U> Maybe<U> flatMap(Float2ObjectFunction<Maybe<? extends U>> mapper);

	public abstract MaybeFloat filter(FloatPredicate predicate);

	public abstract float unwrap();

	public abstract float unwrapOr(float defaultValue);
	
	// public abstract float unwrapOrElse(FloatSupplier defaultValue);

	public abstract MaybeFloat or(MaybeFloat other);

	public abstract MaybeFloat orElse(Supplier<MaybeFloat> other);

	public abstract void ifSome(FloatConsumer consumer);

	public abstract void ifNone(Runnable runnable);

	public abstract boolean innerEquals(float value);

	public static final class None extends MaybeFloat {

		private static final None INSTANCE = new None();

		@Override
		public IteratorFloat iter() {
			return IteratorFloat.empty();
		}

		@Override
		public void forEach(FloatConsumer consumer) {
		}

		@Override
		public <U> Maybe.None<U> map(Float2ObjectFunction<? extends U> mapper) {
			return Maybe.none();
		}

		@Override
		public <U> Maybe.None<U> flatMap(Float2ObjectFunction<Maybe<? extends U>> mapper) {
			return Maybe.none();
		}

		@Override
		public None filter(FloatPredicate predicate) {
			return MaybeFloat.none();
		}

		@Override
		public float unwrap() {
			throw new RuntimeException("tried to unwrap an `Option` that was `None`!");
		}

		@Override
		public float unwrapOr(float defaultValue) {
			return defaultValue;
		}

		// @Override
		// public T unwrapOrElse(Supplier<T> defaultValue) {
		// 	return defaultValue.get();
		// }

		@Override
		public MaybeFloat or(MaybeFloat other) {
			return other;
		}

		@Override
		public MaybeFloat orElse(Supplier<MaybeFloat> other) {
			return other.get();
		}

		@Override
		public void ifSome(FloatConsumer consumer) {
		}

		@Override
		public void ifNone(Runnable runnable) {
			runnable.run();
		}

		@Override
		public boolean innerEquals(float value) {
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

	public static final class Some extends MaybeFloat {
		public final float value;

		public Some(float value) {
			this.value = value;
		}

		@Override
		public IteratorFloat iter() {
			return IteratorFloat.once(this.value);
		}

		@Override
		public void forEach(FloatConsumer consumer) {
			consumer.accept(this.value);
		}

		@Override
		public <U> Maybe.Some<U> map(Float2ObjectFunction<? extends U> mapper) {
			return new Maybe.Some<>(mapper.get(this.value));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <U> Maybe<U> flatMap(Float2ObjectFunction<Maybe<? extends U>> mapper) {
			return (Maybe<U>) mapper.get(this.value);
		}

		@Override
		public MaybeFloat filter(FloatPredicate predicate) {
			return predicate.test(this.value) ? this : MaybeFloat.none();
		}

		@Override
		public float unwrap() {
			return this.value;
		}

		@Override
		public float unwrapOr(float defaultValue) {
			return this.value;
		}

		// @Override
		// public float unwrapOrElse(Supplier<float> defaultValue) {
		// 	return this.value;
		// }

		@Override
		public MaybeFloat or(MaybeFloat other) {
			return this;
		}

		@Override
		public MaybeFloat orElse(Supplier<MaybeFloat> other) {
			return this;
		}

		@Override
		public void ifSome(FloatConsumer consumer) {
			consumer.accept(this.value);
		}

		@Override
		public void ifNone(Runnable runnable) {
		}

		@Override
		public boolean innerEquals(float value) {
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
			hasher.appendFloat(this.value);
		}

		@Override
		public String toString() {
			return "Some(" + this.value + ")";
		}
	}

}
