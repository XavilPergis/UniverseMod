package net.xavil.hawklib.collections.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.collections.iterator.SizeHint;

public final class Vector<T> implements MutableList<T> {

	private T[] elements = makeArray(0);
	private int size = 0;
	private float growthFactor = 2.0f;
	private boolean unordered = false;

	public Vector() {
	}

	public Vector(int initialCapacity) {
		this.elements = makeArray(initialCapacity);
	}

	private Vector(T[] elements, int size, float growthFactor) {
		this.elements = elements;
		this.size = size;
		this.growthFactor = growthFactor;
	}

	public void setGrowthFactor(float growthFactor) {
		Assert.isTrue(growthFactor > 1.0f);
		this.growthFactor = growthFactor;
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] makeArray(int length) {
		return (T[]) new Object[length];
	}

	private void growToFit(int newCapacity) {
		if (this.elements.length >= newCapacity)
			return;
		int cap = this.elements.length;
		if (cap == 0)
			cap = 16;
		while (cap < newCapacity) {
			final var prevCap = cap;
			cap *= this.growthFactor;
			Assert.isTrue(cap > prevCap);
		}
		T[] newElements = makeArray(newCapacity);
		T[] oldElements = this.elements;
		this.elements = newElements;
		System.arraycopy(oldElements, 0, newElements, 0, this.size);
	}

	@Override
	public void clear() {
		for (int i = 0; i < this.size; ++i)
			this.elements[i] = null;
		this.size = 0;
	}

	@Override
	public void retain(Predicate<T> predicate) {
		int src = 0, dst = 0;
		while (src < this.size) {
			try {
				final var elem = this.elements[src++];
				if (predicate.test(elem))
					this.elements[dst++] = elem;
			} catch (Throwable throwable) {
				src -= 1;
				System.arraycopy(this.elements, src, this.elements, dst, this.size - src);
				final var oldSize = this.size;
				this.size -= src - dst;
				for (int i = this.size; i < oldSize; ++i)
					this.elements[i] = null;
				throw throwable;
			}
		}
		for (int i = dst; i < this.size; ++i) {
			this.elements[i] = null;
		}
		this.size = dst;
	}

	@Override
	public void extend(IntoIterator<T> elements) {
		final var iter = elements.iter();
		final var minCount = iter.sizeHint().lowerBound();
		final var lowerBoundEnd = this.size + minCount;
		growToFit(lowerBoundEnd);
		for (int i = this.size; i < lowerBoundEnd; ++i) {
			iter.hasNext();
			this.elements[i] = iter.next();
			this.size += 1;
		}

		while (iter.hasNext()) {
			growToFit(this.size + 1);
			for (int i = this.size; i < this.elements.length; ++i) {
				if (!iter.hasNext())
					break;
				this.elements[i] = iter.next();
				this.size += 1;
			}
		}
	}

	@Override
	public int size() {
		return this.size;
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
	public void insert(int index, T value) {
		Assert.isTrue(index <= this.size);
		growToFit(this.size + 1);
		if (this.unordered) {
			// honestly, this doesn't make a whole lot of sense. If the collection is
			// unordered, then what is the point of inserting at a specific index?
			this.elements[this.size] = this.elements[index];
			this.elements[index] = value;
			this.size += 1;
		} else {
			final var copyLen = this.size - index;
			System.arraycopy(this.elements, index, this.elements, index + 1, copyLen);
			this.elements[index] = value;
			this.size += 1;
		}
	}

	@Override
	public T remove(int index) {
		Assert.isTrue(index < this.size);
		T old = this.elements[index];
		if (this.unordered) {
			this.elements[index] = this.elements[this.size - 1];
			this.elements[this.size - 1] = null;
			this.size -= 1;
		} else {
			final var copyLen = this.size - index - 1;
			System.arraycopy(this.elements, index + 1, this.elements, index, copyLen);
			this.size -= 1;
		}
		return old;
	}

	@Override
	public T set(int index, T value) {
		Assert.isTrue(index < this.size);
		final var old = this.elements[index];
		this.elements[index] = value;
		return old;
	}

	@Override
	public void optimize() {
		if (this.elements.length == this.size)
			return;
		T[] newElements = makeArray(this.size);
		T[] oldElements = this.elements;
		this.elements = newElements;
		System.arraycopy(oldElements, 0, newElements, 0, this.size);
	}

	@Override
	public Vector<T> hint(CollectionHint hint) {
		this.unordered |= hint == CollectionHint.UNORDERED;
		return this;
	}

	@Override
	public void forEach(Consumer<? super T> consumer) {
		for (int i = 0; i < this.size; ++i)
			consumer.accept(this.elements[i]);
	}

	@Override
	public void sort(Comparator<? super T> comparator) {
		Arrays.sort(this.elements, comparator);
	}

	@Override
	public T[] toArray() {
		return Arrays.copyOf(this.elements, this.size);
	}

	public Vector<T> copy() {
		final T[] newElements = makeArray(this.elements.length);
		System.arraycopy(this.elements, 0, newElements, 0, this.size);
		return new Vector<>(newElements, this.size, this.growthFactor);
	}

	public static class Iter<T> implements Iterator<T> {
		private final Vector<T> vector;
		private int cur;

		public Iter(Vector<T> vector) {
			this(vector, 0);
		}

		private Iter(Vector<T> vector, int cur) {
			this.vector = vector;
			this.cur = cur;
		}

		@Override
		public SizeHint sizeHint() {
			return SizeHint.exactly(this.vector.size);
		}

		@Override
		public boolean hasNext() {
			return this.cur < this.vector.size;
		}

		@Override
		public T next() {
			return this.vector.elements[this.cur++];
		}

		@Override
		public void forEach(Consumer<? super T> consumer) {
			for (; this.cur < this.vector.size; ++this.cur) {
				consumer.accept(this.vector.elements[this.cur]);
			}
		}

		@Override
		public boolean isFused() {
			return true;
		}

		@Override
		public Iterator<T> skip(int amount) {
			final var newCur = Math.min(this.cur + amount, this.vector.size);
			return new Iter<>(this.vector, newCur);
		}
	}

}
