package net.xavil.hawklib;

import java.util.function.Consumer;

import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;

public interface Disposable extends AutoCloseable {

	@Override
	void close();

	static Multi scope() {
		return new Multi();
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
		public void close() {
			this.disposer.accept(this.value);
		}
	}

	static final class Multi implements Disposable {
		private final MutableList<Disposable> children = new Vector<>();

		public <T extends Disposable> T attach(T value) {
			this.children.push(value);
			return value;
		}

		public <T extends Disposable> T detach(T value) {
			for (int i = 0; i < this.children.size(); ++i) {
				if (this.children.get(i) != value)
					continue;
				this.children.remove(i);
			}
			return value;
		}

		@Override
		public void close() {
			this.children.forEach(Disposable::close);
			this.children.clear();
		}
	}

}
