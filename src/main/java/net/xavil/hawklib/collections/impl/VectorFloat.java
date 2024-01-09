package net.xavil.hawklib.collections.impl;

import java.util.Arrays;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import it.unimi.dsi.fastutil.floats.FloatPredicate;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.interfaces.ImmutableListFloat;
import net.xavil.hawklib.collections.interfaces.MutableListFloat;
import net.xavil.hawklib.collections.iterator.IteratorFloat;
import net.xavil.hawklib.collections.iterator.IntoIteratorFloat;
import net.xavil.hawklib.collections.iterator.SizeHint;

public final class VectorFloat implements MutableListFloat {

	private float[] elements = {};
	private int size = 0;
	private boolean unordered = false;

	public VectorFloat() {
	}

	public VectorFloat(ImmutableListFloat elements) {
		try {
			this.elements = new float[elements.size()];
			this.size = elements.size();
			if (elements instanceof VectorFloat src) {
				System.arraycopy(src.elements, 0, this.elements, 0, this.size);
			} else {
				final var iter = elements.iter();
				int i = 0;
				while (iter.hasNext())
					this.elements[i++] = iter.next();
				Assert.isEqual(i, elements.size());
			}
		} catch (Throwable t) {
			this.elements = new float[0];
			this.size = 0;
			throw t;
		}
	}

	public VectorFloat(int initialCapacity) {
		this.elements = new float[initialCapacity];
	}

	public static VectorFloat fromElements(float... initialElements) {
		return new VectorFloat(initialElements, initialElements.length);
	}

	private VectorFloat(float[] elements, int size) {
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
	public float[] backingStorage() {
		return this.elements;
	}

	private static int nextCapacity(int cur) {
		if (cur == 0)
			return 16;
		if (cur < 16384)
			return (cur * 3 + 1) / 2;
		return cur * 2;
	}

	private void resizeStorage(int newCapacity) {
		final float[] newElements = new float[newCapacity];
		final float[] oldElements = this.elements;
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
		this.size = 0;
	}

	@Override
	public void truncate(int size) {
		if (size >= this.size)
			return;
		this.size = size;
	}

	// Linear time in element count on average
	@Override
	public void retain(FloatPredicate predicate) {
		int src = 0, dst = 0;
		while (src < this.size) {
			try {
				final var elem = this.elements[src++];
				if (predicate.test(elem))
					this.elements[dst++] = elem;
			} catch (Throwable throwable) {
				src -= 1;
				System.arraycopy(this.elements, src, this.elements, dst, this.size - src);
				// final var oldSize = this.size;
				this.size -= src - dst;
				// for (int i = this.size; i < oldSize; ++i)
				// 	this.elements[i] = null;
				throw throwable;
			}
		}
		// for (int i = dst; i < this.size; ++i) {
		// 	this.elements[i] = null;
		// }
		this.size = dst;
	}

	@Override
	public void extend(int index, IntoIteratorFloat elements) {
		if (this.unordered) {
			// honestly, this doesn't make a whole lot of sense. If the collection is
			// unordered, then what is the point of inserting at a specific index?
			swapExtend(index, elements);
		} else {
			shiftExtend(index, elements);
		}
	}

	public void shiftExtend(int index, IntoIteratorFloat elements) {
		ListUtil.checkBounds(index, this.size, false);

		final var iter = elements.iter();
		final var minElementCount = iter.sizeHint().lowerBound();

		// reserve enough space so we know we can do the arraycopy
		reserve(minElementCount);

		// batch shifts together for lower bound - we won't hit the slow path for simple
		// copies from one collection to another, but will if we eg filter.
		if (minElementCount > 0) {
			final var copyCount = Math.min(minElementCount, this.size - index);
			System.arraycopy(this.elements, index, this.elements, index + minElementCount, copyCount);

			for (int i = index; i < index + minElementCount; ++i) {
				this.elements[i] = iter.next();
			}
			this.size += minElementCount;
		}

		// do it the slow way :(
		for (int i = index + minElementCount; iter.hasNext(); ++i)
			shiftInsert(i, iter.next());
	}

	public void swapExtend(int index, IntoIteratorFloat elements) {
		ListUtil.checkBounds(index, this.size, false);

		final var iter = elements.iter();
		reserve(iter.sizeHint().lowerBound());

		for (int i = index; iter.hasNext(); ++i)
			swapInsert(i, iter.next());
	}

	@Override
	public IteratorFloat iter() {
		return new Iter(this);
	}

	@Override
	public float get(int index) {
		ListUtil.checkBounds(index, this.size, true);
		return this.elements[index];
	}

	@Override
	public void insert(int index, float value) {
		if (this.unordered) {
			// honestly, this doesn't make a whole lot of sense. If the collection is
			// unordered, then what is the point of inserting at a specific index?
			swapInsert(index, value);
		} else {
			shiftInsert(index, value);
		}
	}

	public void shiftInsert(int index, float value) {
		ListUtil.checkBounds(index, this.size, false);
		reserve(1);
		final var copyLen = this.size - index;
		System.arraycopy(this.elements, index, this.elements, index + 1, copyLen);
		this.elements[index] = value;
		this.size += 1;
	}

	public void swapInsert(int index, float value) {
		ListUtil.checkBounds(index, this.size, false);
		reserve(1);
		this.elements[this.size] = this.elements[index];
		this.elements[index] = value;
		this.size += 1;
	}

	@Override
	public float remove(int index) {
		if (this.unordered) {
			return swapRemove(index);
		} else {
			return shiftRemove(index);
		}
	}

	public float shiftRemove(int index) {
		ListUtil.checkBounds(index, this.size, true);
		final float old = this.elements[index];
		final var copyLen = this.size - index - 1;
		System.arraycopy(this.elements, index + 1, this.elements, index, copyLen);
		this.size -= 1;
		return old;
	}

	public float swapRemove(int index) {
		ListUtil.checkBounds(index, this.size, true);
		final float old = this.elements[index];
		this.elements[index] = this.elements[this.size - 1];
		// this.elements[this.size - 1] = null;
		this.size -= 1;
		return old;
	}

	@Override
	public float set(int index, float value) {
		ListUtil.checkBounds(index, this.size, true);
		final var old = this.elements[index];
		this.elements[index] = value;
		return old;
	}

	@Override
	public void swap(int indexA, int indexB) {
		ListUtil.checkBounds(indexA, this.size, true);
		ListUtil.checkBounds(indexB, this.size, true);
		final float tmp = this.elements[indexA];
		this.elements[indexA] = this.elements[indexB];
		this.elements[indexB] = tmp;
	}

	@Override
	public void optimize() {
		if (this.elements.length == this.size)
			return;
		final float[] newElements = new float[this.size];
		final float[] oldElements = this.elements;
		this.elements = newElements;
		System.arraycopy(oldElements, 0, newElements, 0, this.size);
	}

	@Override
	public VectorFloat hint(CollectionHint hint) {
		this.unordered |= hint == CollectionHint.UNORDERED;
		return this;
	}

	@Override
	public void forEach(FloatConsumer consumer) {
		for (int i = 0; i < this.size; ++i)
			consumer.accept(this.elements[i]);
	}

	@Override
	public float[] toArray() {
		return Arrays.copyOf(this.elements, this.size);
	}

	@Override
	public String toString() {
		return ListUtil.asString(this);
	}

	public VectorFloat copy() {
		final float[] newElements = new float[this.elements.length];
		System.arraycopy(this.elements, 0, newElements, 0, this.size);
		return new VectorFloat(newElements, this.size);
	}

	public static class Iter implements IteratorFloat {
		private final VectorFloat vector;
		private int cur;

		public Iter(VectorFloat vector) {
			this(vector, 0);
		}

		private Iter(VectorFloat vector, int cur) {
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
		public float next() {
			return this.vector.elements[this.cur++];
		}

		@Override
		public void forEach(FloatConsumer consumer) {
			for (; this.cur < this.vector.size; ++this.cur) {
				consumer.accept(this.vector.elements[this.cur]);
			}
		}

		@Override
		public boolean isFused() {
			return true;
		}

		@Override
		public IteratorFloat skip(int amount) {
			final var newCur = Math.min(this.cur + amount, this.vector.size);
			return new Iter(this.vector, newCur);
		}
	}

}
