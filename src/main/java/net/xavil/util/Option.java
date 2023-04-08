package net.xavil.util;

import java.util.Objects;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.util.iterator.IntoIterator;
import net.xavil.util.iterator.Iterator;

public abstract sealed class Option<T> implements IntoIterator<T>, Hashable {

	@SuppressWarnings("unchecked")
	public static <T> Option.None<T> none() {
		return (Option.None<T>) None.INSTANCE;
	}

	public static <T> Option.Some<T> some(T value) {
		return new Option.Some<>(value);
	}

	public static <T> Option<@Nonnull T> fromNullable(@Nullable T value) {
		return value == null ? none() : new Option.Some<>(value);
	}

	public final boolean isSome() {
		return this instanceof Some;
	}

	public final boolean isNone() {
		return !isSome();
	}

	public abstract <U> Option<U> map(Function<? super T, ? extends U> mapper);

	public abstract <U> Option<U> flatMap(Function<? super T, Option<? extends U>> mapper);

	public abstract Option<T> filter(Predicate<? super T> predicate);

	public abstract T unwrap();

	public abstract T unwrapOr(T defaultValue);
	
	public abstract T unwrapOrElse(Supplier<T> defaultValue);

	public abstract @Nullable T unwrapOrNull();

	public abstract Option<T> or(Option<T> other);

	public abstract Option<T> orElse(Supplier<Option<T>> other);

	public abstract void ifSome(Consumer<T> consumer);

	public abstract void ifNone(Runnable runnable);

	public abstract boolean innerEquals(T value);

	public static final class None<T> extends Option<T> {

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
			return Option.none();
		}

		@Override
		public <U> None<U> flatMap(Function<? super T, Option<? extends U>> mapper) {
			return Option.none();
		}

		@Override
		public None<T> filter(Predicate<? super T> predicate) {
			return Option.none();
		}

		@Override
		public T unwrap() {
			throw Assert.isUnreachable();
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
		public Option<T> or(Option<T> other) {
			return other;
		}

		@Override
		public Option<T> orElse(Supplier<Option<T>> other) {
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

	public static final class Some<T> extends Option<T> {
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
		public <U> Option<U> flatMap(Function<? super T, Option<? extends U>> mapper) {
			return (Option<U>) mapper.apply(this.value);
		}

		@Override
		public Option<T> filter(Predicate<? super T> predicate) {
			return predicate.test(this.value) ? this : Option.none();
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
		public Option<T> or(Option<T> other) {
			return this;
		}

		@Override
		public Option<T> orElse(Supplier<Option<T>> other) {
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
