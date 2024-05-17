package net.xavil.hawklib.collections.impl;

import java.util.Arrays;

import javax.annotation.Nonnull;

import it.unimi.dsi.fastutil.ints.IntConsumer;
import it.unimi.dsi.fastutil.ints.IntPredicate;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.collections.CollectionHint;
import net.xavil.hawklib.collections.interfaces.ImmutableListInt;
import net.xavil.hawklib.collections.interfaces.MutableListInt;
import net.xavil.hawklib.collections.iterator.IteratorInt;
import net.xavil.hawklib.collections.iterator.IntoIteratorInt;
import net.xavil.hawklib.collections.iterator.SizeHint;

public final class VectorInt implements MutableListInt {

	private int[] elements = {};
	private int size = 0;
	private boolean unordered = false;

	public VectorInt() {
	}

	public VectorInt(ImmutableListInt elements) {
		try {
			this.elements = new int[elements.size()];
			this.size = elements.size();
			if (elements instanceof VectorInt src) {
				System.arraycopy(src.elements, 0, this.elements, 0, this.size);
			} else {
				final var iter = elements.iter();
				int i = 0;
				while (iter.hasNext())
					this.elements[i++] = iter.next();
				Assert.isEqual(i, elements.size());
			}
		} catch (Throwable t) {
			this.elements = new int[0];
			this.size = 0;
			throw t;
		}
	}

	public VectorInt(int initialCapacity) {
		this.elements = new int[initialCapacity];
	}

	public static VectorInt fromElements(int... initialElements) {
		return new VectorInt(initialElements, initialElements.length);
	}

	private VectorInt(int[] elements, int size) {
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
	public int[] backingStorage() {
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
		final int[] newElements = new int[newCapacity];
		final int[] oldElements = this.elements;
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
	public void retain(IntPredicate predicate) {
		int src = 0, dst = 0;
		while (src < this.size) {
			try {
				final var elem = this.elements[src++];
				if (predicate.test(elem))
					this.elements[dst++] = elem;
			} catch (Throwable throwable) {
				src -= 1;
				System.arraycopy(this.elements, src, this.elements, dst, this.size - src);
				this.size -= src - dst;
				throw throwable;
			}
		}
		this.size = dst;
	}

	@Override
	public void extend(int index, IntoIteratorInt elements) {
		if (this.unordered) {
			// honestly, this doesn't make a whole lot of sense. If the collection is
			// unordered, then what is the point of inserting at a specific index?
			swapExtend(index, elements);
		} else {
			shiftExtend(index, elements);
		}
	}

	private interface InsertFunction {
		void insert(int index, int element);
	}

	private void extendPreamble(int index, IntoIteratorInt elements, InsertFunction func) {
		ListUtil.checkBounds(index, this.size, false);

		final var iter = elements.iter();
		final var minElementCount = iter.sizeHint().lowerBound();

		// reserve enough space so we know we can do the arraycopy
		reserve(minElementCount);

		// if a lower bound is reported, we can fill at least that many elements
		// directly, instead of inserting one-at-a-time. This is especially advantageous
		// for order-preserving insertions, since each insertion must shift all elements
		// after the insertion point. Since we use Iterator::fillArray, extending a
		// vector from another boils down to a simple array copy.
		if (minElementCount > 0) {
			final var copyCount = Math.min(minElementCount, this.size - index);
			System.arraycopy(this.elements, index, this.elements, index + minElementCount, copyCount);
			iter.fillArray(this.elements, index, index + minElementCount);
			this.size += minElementCount;
		}

		// any stragglers are inserted the naive way, since we cant preallocate space
		// for them.
		for (int i = index + minElementCount; iter.hasNext(); ++i)
			func.insert(i, iter.next());
	}

	public void shiftExtend(int index, IntoIteratorInt elements) {
		extendPreamble(index, elements, this::shiftInsert);
	}

	public void swapExtend(int index, IntoIteratorInt elements) {
		extendPreamble(index, elements, this::swapInsert);
	}

	@Override
	public IteratorInt iter() {
		return new Iter(this);
	}

	@Override
	public int get(int index) {
		ListUtil.checkBounds(index, this.size, true);
		return this.elements[index];
	}

	@Override
	public void insert(int index, int value) {
		if (this.unordered) {
			// honestly, this doesn't make a whole lot of sense. If the collection is
			// unordered, then what is the point of inserting at a specific index?
			swapInsert(index, value);
		} else {
			shiftInsert(index, value);
		}
	}

	public void shiftInsert(int index, int value) {
		ListUtil.checkBounds(index, this.size, false);
		reserve(1);
		final var copyLen = this.size - index;
		System.arraycopy(this.elements, index, this.elements, index + 1, copyLen);
		this.elements[index] = value;
		this.size += 1;
	}

	public void swapInsert(int index, int value) {
		ListUtil.checkBounds(index, this.size, false);
		reserve(1);
		this.elements[this.size] = this.elements[index];
		this.elements[index] = value;
		this.size += 1;
	}

	@Override
	public int remove(int index) {
		if (this.unordered) {
			return swapRemove(index);
		} else {
			return shiftRemove(index);
		}
	}

	public int shiftRemove(int index) {
		ListUtil.checkBounds(index, this.size, true);
		final int old = this.elements[index];
		final var copyLen = this.size - index - 1;
		System.arraycopy(this.elements, index + 1, this.elements, index, copyLen);
		this.size -= 1;
		return old;
	}

	public int swapRemove(int index) {
		ListUtil.checkBounds(index, this.size, true);
		final int old = this.elements[index];
		this.elements[index] = this.elements[this.size - 1];
		// this.elements[this.size - 1] = null;
		this.size -= 1;
		return old;
	}

	@Override
	public int set(int index, int value) {
		ListUtil.checkBounds(index, this.size, true);
		final var old = this.elements[index];
		this.elements[index] = value;
		return old;
	}

	@Override
	public void swap(int indexA, int indexB) {
		ListUtil.checkBounds(indexA, this.size, true);
		ListUtil.checkBounds(indexB, this.size, true);
		final int tmp = this.elements[indexA];
		this.elements[indexA] = this.elements[indexB];
		this.elements[indexB] = tmp;
	}

	@Override
	public void optimize() {
		if (this.elements.length == this.size)
			return;
		final int[] newElements = new int[this.size];
		final int[] oldElements = this.elements;
		this.elements = newElements;
		System.arraycopy(oldElements, 0, newElements, 0, this.size);
	}

	@Override
	public VectorInt hint(CollectionHint hint) {
		this.unordered |= hint == CollectionHint.UNORDERED;
		return this;
	}

	@Override
	public void forEach(IntConsumer consumer) {
		for (int i = 0; i < this.size; ++i)
			consumer.accept(this.elements[i]);
	}

	@Override
	public int[] toArray() {
		return Arrays.copyOf(this.elements, this.size);
	}

	@Override
	public String toString() {
		return ListUtil.asString(this);
	}

	public VectorInt copy() {
		final int[] newElements = new int[this.elements.length];
		System.arraycopy(this.elements, 0, newElements, 0, this.size);
		return new VectorInt(newElements, this.size);
	}

	public static class Iter implements IteratorInt {
		private final VectorInt vector;
		private int cur, end;

		public Iter(VectorInt vector) {
			this(vector, 0);
		}

		private Iter(VectorInt vector, int cur) {
			this.vector = vector;
			this.cur = cur;
			this.end = vector.size;
		}

		@Override
		public SizeHint sizeHint() {
			return SizeHint.exactly(this.end - this.cur);
		}

		@Override
		public boolean hasNext() {
			return this.cur < this.end;
		}

		@Override
		public int next() {
			return this.vector.elements[this.cur++];
		}

		@Override
		public int fillArray(@Nonnull int[] array, int start, int end) {
			if (end > array.length || start > end || start < 0 || end < 0)
				throw new IllegalArgumentException(String.format(
						"Invalid range bounds of [%d, %d) for array of length %d",
						start, end, array.length));
			final var copyLength = Math.min(start - end, this.end - this.cur);
			System.arraycopy(this.vector.elements, this.cur, array, start, copyLength);
			return copyLength;
		}

		@Override
		public void forEach(IntConsumer consumer) {
			for (; this.cur < this.end; ++this.cur) {
				consumer.accept(this.vector.elements[this.cur]);
			}
		}

		@Override
		public boolean isFused() {
			return true;
		}

		@Override
		public IteratorInt skip(int amount) {
			this.cur = Math.min(this.end, this.cur + amount);
			return this;
		}

		@Override
		public IteratorInt limit(int limit) {
			this.end = Math.min(this.end, this.cur + limit);
			return this;
		}
	}

}
