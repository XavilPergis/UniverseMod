package net.xavil.hawklib.collections.impl;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.collections.iterator.SizeHint;

public final class Vector<T> implements MutableList<T> {

	private T[] elements = makeArray(0);
	private int size = 0;
	private boolean unordered = false;

	public Vector() {
	}

	public Vector(ImmutableList<T> elements) {
		try {
			this.elements = makeArray(elements.size());
			this.size = elements.size();
			if (elements instanceof Vector<T> src) {
				System.arraycopy(src.elements, 0, this.elements, 0, this.size);
			} else {
				final var iter = elements.iter();
				int i = 0;
				while (iter.hasNext())
					this.elements[i++] = iter.next();
				Assert.isEqual(i, elements.size());
			}
		} catch (Throwable t) {
			this.elements = makeArray(0);
			this.size = 0;
			throw t;
		}
	}

	public Vector(int initialCapacity) {
		this.elements = makeArray(initialCapacity);
	}

	@SafeVarargs
	public static <T> Vector<T> fromElements(T... initialElements) {
		return new Vector<>(initialElements, initialElements.length);
	}

	private Vector(T[] elements, int size) {
		this.elements = elements;
		this.size = size;
	}

	@Override
	public int size() {
		return this.size;
	}

	public int capacity() {
		return this.elements.length;
	}

	/**
	 * This method allows for direct access into the internal state of this vector.
	 * Caution should be exercised when using this, as all accesses through this are
	 * unchecked. Additionally, this array will no longer point into the vector's
	 * backing storage the next time the vector resises.
	 * 
	 * @return A reference to this vector's backing storage.
	 */
	public T[] backingStorage() {
		return this.elements;
	}

	@SuppressWarnings("unchecked")
	private static <T> T[] makeArray(int length) {
		return (T[]) new Object[length];
	}

	private static int nextCapacity(int cur) {
		if (cur == 0)
			return 16;
		if (cur < 16384)
			return (cur * 3 + 1) / 2;
		return cur * 2;
	}

	private void resizeStorage(int newCapacity) {
		final T[] newElements = makeArray(newCapacity);
		final T[] oldElements = this.elements;
		this.elements = newElements;
		System.arraycopy(oldElements, 0, newElements, 0, this.size);
	}

	public void reserve(int additional) {
		if (this.elements.length >= this.size + additional)
			return;
		int cap = this.elements.length;
		while (cap < this.size + additional) {
			final var prevCap = cap;
			cap = nextCapacity(cap);
			Assert.isTrue(cap > prevCap);
		}
		resizeStorage(cap);
	}

	public void reserveExact(int additional) {
		if (this.elements.length >= this.size + additional)
			return;
		resizeStorage(this.size + additional);
	}

	@Override
	public void clear() {
		for (int i = 0; i < this.size; ++i)
			this.elements[i] = null;
		this.size = 0;
	}

	// Linear time in element count on average
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
	public void extend(IntoIterator<? extends T> elements) {
		final var iter = elements.iter();
		final var minCount = iter.sizeHint().lowerBound();
		final var lowerBoundEnd = this.size + minCount;
		reserve(minCount);
		for (int i = this.size; i < lowerBoundEnd; ++i) {
			iter.hasNext();
			this.elements[i] = iter.next();
			this.size += 1;
		}

		while (iter.hasNext()) {
			reserve(1);
			for (int i = this.size; i < this.elements.length; ++i) {
				if (!iter.hasNext())
					break;
				this.elements[i] = iter.next();
				this.size += 1;
			}
		}
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
		reserve(1);
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
	public void swap(int indexA, int indexB) {
		Assert.isTrue(indexA < this.size && indexB < this.size);
		final T tmp = this.elements[indexA];
		this.elements[indexA] = this.elements[indexB];
		this.elements[indexB] = tmp;
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
	@SuppressWarnings("unchecked")
	public T[] toArray(Class<T> innerType) {
		return Arrays.copyOf(this.elements, this.size, (Class<T[]>) innerType.arrayType());
	}

	@Override
	public String toString() {
		return ListUtil.asString(this);
	}

	public Vector<T> copy() {
		final T[] newElements = makeArray(this.elements.length);
		System.arraycopy(this.elements, 0, newElements, 0, this.size);
		return new Vector<>(newElements, this.size);
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
