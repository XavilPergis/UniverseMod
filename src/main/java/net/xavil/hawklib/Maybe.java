package net.xavil.hawklib;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

public abstract sealed class Maybe<T> implements IntoIterator<T>, Hashable {

	@SuppressWarnings("unchecked")
	public static <T> Maybe.None<T> none() {
		return (Maybe.None<T>) None.INSTANCE;
	}

	public static <T> Maybe.Some<T> some(T value) {
		return new Maybe.Some<>(value);
	}

	public static <T> Maybe<T> fromNullable(@Nullable T value) {
		return value == null ? none() : new Maybe.Some<>(value);
	}

	public final boolean isSome() {
		return this instanceof Some;
	}

	public final boolean isNone() {
		return !isSome();
	}

	public abstract <U> Maybe<U> map(Function<? super T, ? extends U> mapper);

	public abstract <U> Maybe<U> flatMap(Function<? super T, Maybe<? extends U>> mapper);

	public abstract Maybe<T> filter(Predicate<? super T> predicate);

	public abstract T unwrap();

	public abstract T unwrapOr(T defaultValue);
	
	public abstract T unwrapOrElse(Supplier<T> defaultValue);

	public abstract @Nullable T unwrapOrNull();

	public abstract Maybe<T> or(Maybe<T> other);

	public abstract Maybe<T> orElse(Supplier<Maybe<T>> other);

	public abstract void ifSome(Consumer<T> consumer);

	public abstract void ifNone(Runnable runnable);

	public abstract boolean innerEquals(T value);

	public static final class None<T> extends Maybe<T> {

		private static final None<?> INSTANCE = new None<>();

		@Override
		public Iterator<T> iter() {
			return Iterator.empty();
		}

		@Override
		public void forEach(Consumer<? super T> consumer) {
		}

		@Override
		public <U> None<U> map(Function<? super T, ? extends U> mapper) {
			return Maybe.none();
		}

		@Override
		public <U> None<U> flatMap(Function<? super T, Maybe<? extends U>> mapper) {
			return Maybe.none();
		}

		@Override
		public None<T> filter(Predicate<? super T> predicate) {
			return Maybe.none();
		}

		@Override
		public T unwrap() {
			throw new RuntimeException("tried to unwrap an `Option` that was `None`!");
		}

		@Override
		public T unwrapOr(T defaultValue) {
			return defaultValue;
		}

		@Override
		public T unwrapOrElse(Supplier<T> defaultValue) {
			return defaultValue.get();
		}

		@Override
		public @Nullable T unwrapOrNull() {
			return null;
		}

		@Override
		public Maybe<T> or(Maybe<T> other) {
			return other;
		}

		@Override
		public Maybe<T> orElse(Supplier<Maybe<T>> other) {
			return other.get();
		}

		@Override
		public void ifSome(Consumer<T> consumer) {
		}

		@Override
		public void ifNone(Runnable runnable) {
			runnable.run();
		}

		@Override
		public boolean innerEquals(T value) {
			return false;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof None<?>;
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

	public static final class Some<T> extends Maybe<T> {
		public final T value;

		public Some(T value) {
			this.value = value;
		}

		@Override
		public Iterator<T> iter() {
			return Iterator.once(this.value);
		}

		@Override
		public void forEach(Consumer<? super T> consumer) {
			consumer.accept(this.value);
		}

		@Override
		public <U> Some<U> map(Function<? super T, ? extends U> mapper) {
			return new Some<>(mapper.apply(this.value));
		}

		@Override
		@SuppressWarnings("unchecked")
		public <U> Maybe<U> flatMap(Function<? super T, Maybe<? extends U>> mapper) {
			return (Maybe<U>) mapper.apply(this.value);
		}

		@Override
		public Maybe<T> filter(Predicate<? super T> predicate) {
			return predicate.test(this.value) ? this : Maybe.none();
		}

		@Override
		public T unwrap() {
			return this.value;
		}

		@Override
		public T unwrapOr(T defaultValue) {
			return this.value;
		}

		@Override
		public T unwrapOrElse(Supplier<T> defaultValue) {
			return this.value;
		}

		@Override
		public T unwrapOrNull() {
			return this.value;
		}

		@Override
		public Maybe<T> or(Maybe<T> other) {
			return this;
		}

		@Override
		public Maybe<T> orElse(Supplier<Maybe<T>> other) {
			return this;
		}

		@Override
		public void ifSome(Consumer<T> consumer) {
			consumer.accept(this.value);
		}

		@Override
		public void ifNone(Runnable runnable) {
		}

		@Override
		public boolean innerEquals(T value) {
			return Objects.equals(this.value, value);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Some<?> other ? Objects.equals(this.value, other.value) : false;
		}

		@Override
		public int hashCode() {
			return FastHasher.hashToInt(this);
		}

		@Override
		public void appendHash(Hasher hasher) {
			if (this.value instanceof Hashable hashable) {
				hasher.append(hashable);
			} else {
				hasher.appendInt(this.value.hashCode());
			}
		}

		@Override
		public String toString() {
			return "Some(" + this.value.toString() + ")";
		}
	}

}
