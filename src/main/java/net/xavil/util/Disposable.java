package net.xavil.util;

import java.util.function.Consumer;
import java.util.function.Function;

import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.MutableList;

public interface Disposable {

	void dispose();

	static void scope(Consumer<Multi> consumer) {
		final var multi = new Multi();
		try {
			consumer.accept(multi);
		} finally {
			multi.dispose();
		}
	}

	static <T> T scope(Function<Multi, T> func) {
		final var multi = new Multi();
		try {
			return func.apply(multi);
		} finally {
			multi.dispose();
		}
	}

	static <T> Wrapped<T> wrapped(T value, Consumer<T> disposer) {
		return new Wrapped<>(value, disposer);
	}

	static final class Wrapped<T> implements Disposable {

		public final T value;
		public final Consumer<T> disposer;

		public Wrapped(T value, Consumer<T> disposer) {
			this.value = value;
			this.disposer = disposer;
		}

		@Override
		public void dispose() {
			this.disposer.accept(this.value);
		}
	}

	static final class Multi implements Disposable {
		private final MutableList<Disposable> children = new Vector<>();

		public <T extends Disposable> T attach(T value) {
			this.children.push(value);
			return value;
		}

		@Override
		public void dispose() {
			this.children.forEach(Disposable::dispose);
		}
	}

}
