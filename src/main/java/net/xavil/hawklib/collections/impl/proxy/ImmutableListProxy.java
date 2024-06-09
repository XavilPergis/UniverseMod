package net.xavil.hawklib.collections.impl.proxy;

import java.util.List;

import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.collections.iterator.Iterators;

public final class ImmutableListProxy<T> implements ImmutableList<T> {

	private final List<T> wrapped;

	public ImmutableListProxy(List<T> wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public int size() {
		return this.wrapped.size();
	}

	@Override
	public Iterator<T> iter() {
		return Iterators.fromJava(this.wrapped.iterator());
	}

	@Override
	public T get(int index) {
		return this.wrapped.get(index);
	}

}
