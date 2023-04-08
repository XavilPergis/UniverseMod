package net.xavil.util.collections;

import java.util.List;
import java.util.function.Predicate;

import net.xavil.util.collections.interfaces.MutableList;
import net.xavil.util.iterator.IntoIterator;
import net.xavil.util.iterator.Iterator;

public final class MutableListProxy<T> implements MutableList<T> {

	private final List<T> wrapped;

	public MutableListProxy(List<T> wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void clear() {
		this.wrapped.clear();
	}

	@Override
	public void retain(Predicate<T> predicate) {
		this.wrapped.removeIf(predicate.negate());
	}

	@Override
	public void extend(IntoIterator<T> elements) {
		elements.forEach(this.wrapped::add);
	}

	@Override
	public int size() {
		return this.wrapped.size();
	}

	@Override
	public Iterator<T> iter() {
		return Iterator.fromJava(this.wrapped.iterator());
	}

	@Override
	public T get(int index) {
		return this.wrapped.get(index);
	}

	@Override
	public void insert(int index, T value) {
		this.wrapped.add(index, value);
	}

	@Override
	public void push(T value) {
		this.wrapped.add(value);
	}

	@Override
	public T remove(int index) {
		return this.wrapped.remove(index);
	}

}
