package net.xavil.util.collections;

import java.util.Set;

import net.xavil.util.collections.interfaces.ImmutableSet;
import net.xavil.util.iterator.Iterator;

public final class ImmutableSetProxy<T> implements ImmutableSet<T> {

	private final Set<T> proxy;

	public ImmutableSetProxy(Set<T> proxy) {
		this.proxy = proxy;
	}

	@Override
	public int size() {
		return this.proxy.size();
	}

	@Override
	public Iterator<T> iter() {
		return Iterator.fromJava(this.proxy.iterator());
	}

	@Override
	public boolean contains(T value) {
		return this.proxy.contains(value);
	}
	
}
