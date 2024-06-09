package net.xavil.hawklib.collections.impl;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.collections.iterator.SizeHint;

public final class ArrayIterator<T> implements Iterator<T> {

	private T[] elements;
	private int currentIndex, endIndex;

	public ArrayIterator(T[] elements, int startIndex, int endIndex) {
		this.elements = elements;
		this.currentIndex = startIndex;
		this.endIndex = endIndex;
	}

	public ArrayIterator(T[] elements) {
		this(elements, 0, elements.length);
	}

	@Override
	public boolean hasNext() {
		return this.currentIndex < this.endIndex;
	}

	@Override
	public T next() {
		return this.elements[this.currentIndex++];
	}

	@Override
	public SizeHint sizeHint() {
		return SizeHint.exactly(this.endIndex - this.currentIndex);
	}

	@Override
	public int fillArray(@NotNull T[] array, int start, int end) {
		if (end > array.length || start > end || start < 0 || end < 0)
			throw new IllegalArgumentException(String.format(
					"Invalid range bounds of [%d, %d) for array of length %d",
					start, end, array.length));

		final var copiedLen = Math.min(end - start, this.endIndex - this.currentIndex);
		System.arraycopy(this.elements, this.currentIndex, array, start, copiedLen);

		this.currentIndex += copiedLen;
		return copiedLen;
	}

	@Override
	public int count() {
		return this.endIndex - this.currentIndex;
	}

	@Override
	public void forEach(Consumer<? super T> consumer) {
		for (; this.currentIndex < this.endIndex; ++this.currentIndex)
		consumer.accept(this.elements[this.currentIndex]);
	}

	@Override
	public int properties() {
		return PROPERTY_FUSED;
	}

	@Override
	public Iterator<T> skip(int amount) {
		this.currentIndex += amount;
		if (this.currentIndex > this.endIndex)
			this.currentIndex = this.endIndex;
		return this;
	}

	@Override
	public Iterator<T> limit(int limit) {
		this.endIndex = Math.min(this.endIndex, this.currentIndex + limit);
		return this;
	}

}
