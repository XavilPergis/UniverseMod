package net.xavil.hawklib.collections.impl;

import java.util.Arrays;
import java.util.function.Consumer;

import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.collections.iterator.SizeHint;

public final class ImmutableListArray<T> implements ImmutableList<T> {

	private final T[] elements;

	public ImmutableListArray(T[] elements) {
		this.elements = Arrays.copyOf(elements, elements.length);
	}

	@Override
	public int size() {
		return this.elements.length;
	}

	@Override
	public Iterator<T> iter() {
		return new Iter<>(this);
	}

	@Override
	public T get(int index) {
		return this.elements[index];
	}

	@Override
	public void forEach(Consumer<? super T> consumer) {
		for (int i = 0; i < this.elements.length; ++i)
			consumer.accept(this.elements[i]);
	}

	@Override
	public T[] toArray(Class<T> innerType) {
		return Arrays.copyOf(this.elements, this.elements.length);
	}

	@Override
	public String toString() {
		return ListUtil.asString(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof ImmutableListArray<?> other) {
			return Arrays.equals(this.elements, other.elements);
		} else if (obj instanceof ImmutableList<?> other) {
			return ListUtil.genericEquals(this, other);
		}
		return false;
	}

	public static class Iter<T> implements Iterator<T> {
		private final ImmutableListArray<T> list;
		private int cur;

		public Iter(ImmutableListArray<T> list) {
			this(list, 0);
		}

		private Iter(ImmutableListArray<T> list, int cur) {
			this.list = list;
			this.cur = cur;
		}

		@Override
		public SizeHint sizeHint() {
			return SizeHint.exactly(this.list.elements.length);
		}

		@Override
		public boolean hasNext() {
			return this.cur < this.list.elements.length;
		}

		@Override
		public T next() {
			return this.list.elements[this.cur++];
		}

		@Override
		public void forEach(Consumer<? super T> consumer) {
			for (; this.cur < this.list.elements.length; ++this.cur) {
				consumer.accept(this.list.elements[this.cur]);
			}
		}

		@Override
		public int properties() {
			return Iterator.PROPERTY_FUSED;
		}

		@Override
		public Iterator<T> skip(int amount) {
			final var newCur = Math.min(this.cur + amount, this.list.elements.length);
			return new Iter<>(this.list, newCur);
		}
	}

}
