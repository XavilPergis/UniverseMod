package net.xavil.util.collections;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.xavil.util.collections.interfaces.MutableSet;
import net.xavil.util.iterator.IntoIterator;
import net.xavil.util.iterator.Iterator;

public class MutableSetProxy<T> implements MutableSet<T> {

	public final @Nonnull Set<T> proxy;

	public MutableSetProxy(@Nonnull Set<T> wrapped) {
		this.proxy = wrapped;
	}

	@Override
	public void clear() {
		this.proxy.clear();
	}

	@Override
	public void retain(Predicate<T> predicate) {
		this.proxy.removeIf(predicate.negate());
	}

	@Override
	public void extend(IntoIterator<T> elements) {
		if (elements instanceof MutableSetProxy<T> other) {
			this.proxy.addAll(other.proxy);
		} else {
			elements.forEach(this.proxy::add);
		}
	}

	@Override
	public int size() {
		return this.proxy.size();
	}

	@Override
	public boolean isEmpty() {
		return this.proxy.isEmpty();
	}

	@Override
	public boolean contains(T value) {
		return this.proxy.contains(value);
	}

	@Override
	public boolean insert(T value) {
		return this.proxy.add(value);
	}

	@Override
	public boolean remove(T value) {
		return this.proxy.remove(value);
	}

	@Override
	public void forEach(Consumer<? super T> consumer) {
		this.proxy.forEach(consumer);
	}

	@Override
	public Iterator<T> iter() {
		return Iterator.fromJava(this.proxy.iterator());
	}
	
}
