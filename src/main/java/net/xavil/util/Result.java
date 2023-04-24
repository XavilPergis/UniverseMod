package net.xavil.util;

public abstract sealed class Result<T, E> {

	public interface Wrap<T, E extends Throwable> {
		T get() throws E;
	}

	public static <T> Result<T, Throwable> wrap(Wrap<T, Throwable> wrap) {
		try {
			return new Ok<>(wrap.get());
		} catch (Throwable ex) {
			return new Err<>(ex);
		}
	}

	public static <T, E extends Throwable> Result<T, E> wrapGeneric(Class<E> exType, Wrap<T, E> wrap) {
		try {
			return new Ok<>(wrap.get());
		} catch (Throwable ex) {
			if (exType.isInstance(ex)) {
				return new Err<>((E) ex);
			}
			throw Assert.isUnreachable();
		}
	}

	public static final class Ok<T, E extends Throwable> extends Result<T, E> {
		public final T value;

		public Ok(T value) {
			this.value = value;
		}
	}

	public static final class Err<T, E> extends Result<T, E> {
		public final E value;

		public Err(E value) {
			this.value = value;
		}
	}

}
