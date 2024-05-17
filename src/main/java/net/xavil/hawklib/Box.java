package net.xavil.hawklib;

import java.util.function.Supplier;

public final class Box<T> implements Supplier<T> {

	private T value;

	public Box() {
		this(null);
	}

	public Box(T value) {
		this.value = value;
	}

	@Override
	public T get() {
		return this.value;
	}

	public T set(T value) {
		final T old = this.value;
		this.value = value;
		return old;
	}

	@Override
	public String toString() {
		return "Box(" + this.value.toString() + ")";
	}

}
