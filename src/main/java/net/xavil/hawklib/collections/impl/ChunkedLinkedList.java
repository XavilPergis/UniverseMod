package net.xavil.hawklib.collections.impl;

import java.util.function.Consumer;

import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.collections.iterator.SizeHint;

public final class ChunkedLinkedList<T> implements MutableList<T> {

	private static final class Node<T> {
		public final T[] elements;
		public int size = 0;
		public Node<T> prev, next;

		public Node(int chunkSize) {
			this.elements = ListUtil.makeTypedObjectArray(chunkSize);
		}
	}

	private final int chunkSize;
	private int size = 0;
	private Node<T> headNode = null, tailNode = null;

	public ChunkedLinkedList(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	public ChunkedLinkedList() {
		this(16);
	}

	@Override
	public void clear() {
		this.headNode = this.tailNode = null;
		this.size = 0;
	}

	@Override
	public int size() {
		return this.size;
	}

	@Override
	public T get(int index) {
		ListUtil.checkBounds(index, this.size, true);

		Node<T> currentNode = this.headNode;
		int baseIndex = 0;
		for (;;) {
			final var nextIndex = baseIndex + currentNode.size;
			if (nextIndex > index)
				break;
			baseIndex = nextIndex;
			currentNode = currentNode.next;
		}

		return currentNode.elements[index - baseIndex];
	}

	@Override
	public Iterator<T> iter() {
		return new Iter<>(this);
	}

	@Override
	public T set(int index, T value) {
		ListUtil.checkBounds(index, this.size, true);

		Node<T> currentNode = this.headNode;
		int baseIndex = 0;
		for (;;) {
			final var nextIndex = baseIndex + currentNode.size;
			if (nextIndex > index)
				break;
			baseIndex = nextIndex;
			currentNode = currentNode.next;
		}

		final T old = currentNode.elements[index - baseIndex];
		currentNode.elements[index - baseIndex] = value;
		return old;
	}

	// tail insertions are fast
	@Override
	public void insert(int index, T value) {
		ListUtil.checkBounds(index, this.size, false);

		if (index == this.size) {
			// node is at capacity, need to make a new one!
			if (this.tailNode.size == this.tailNode.elements.length) {
				final var prev = this.tailNode;
				this.tailNode = new Node<>(this.chunkSize);
				this.tailNode.prev = prev;
				prev.next = this.tailNode;
			}

			// the if block above ensures that we have spare capacity in the tail node
			this.tailNode.elements[this.tailNode.size] = value;
			this.tailNode.size += 1;
			return;
		}

		Node<T> currentNode = this.headNode;
		int baseIndex = 0;
		for (;;) {
			final var nextIndex = baseIndex + currentNode.size;
			if (nextIndex > index)
				break;
			baseIndex = nextIndex;
			currentNode = currentNode.next;
		}

		if (currentNode.size == currentNode.elements.length) {
			final var prev = currentNode;
			currentNode = new Node<>(this.chunkSize);
			currentNode.prev = prev;
			prev.next = currentNode;
		}

		final var nodeIndex = index - baseIndex;
		System.arraycopy(currentNode.elements, nodeIndex, currentNode.elements, nodeIndex + 1, 1);
		currentNode.elements[nodeIndex] = value;
		currentNode.size += 1;
	}

	@Override
	public T remove(int index) {
		ListUtil.checkBounds(index, this.size, true);

		Node<T> currentNode = this.headNode;
		int baseIndex = 0;
		for (;;) {
			final var nextIndex = baseIndex + currentNode.size;
			if (nextIndex > index)
				break;
			baseIndex = nextIndex;
			currentNode = currentNode.next;
		}

		final var nodeIndex = index - baseIndex;
		final var old = currentNode.elements[nodeIndex];
		System.arraycopy(currentNode.elements, nodeIndex + 1, currentNode.elements, nodeIndex, 1);
		currentNode.size -= 1;
		return old;
	}

	@Override
	public void extend(int index, IntoIterator<? extends T> elements) {
		ListUtil.checkBounds(index, this.size, true);

		final var iter = elements.iter();
		final var sizeHint = iter.sizeHint();

		Node<T> currentNode = this.headNode;
		int baseIndex = 0;
		for (;;) {
			final var nextIndex = baseIndex + currentNode.size;
			if (nextIndex > index)
				break;
			baseIndex = nextIndex;
			currentNode = currentNode.next;
		}

		// currentNode.size - (index - baseIndex)
		// 2 - (4 - 3) -> 2 - 1 -> 1
		// final var initialNodeRemaining = currentNode.size - (index - baseIndex);
		// for (int i = 0; i < initialNodeRemaining; ++i) {
		// 	currentNode.elements[i + index - baseIndex] = value;
		// }
	}

	@Override
	public void forEach(Consumer<? super T> consumer) {
		Node<T> currentNode = this.headNode;
		while (currentNode != null) {
			for (int i = 0; i < currentNode.size; ++i)
				consumer.accept(currentNode.elements[i]);
			currentNode = currentNode.next;
		}
	}

	public static final class Iter<T> implements Iterator<T> {
		private final ChunkedLinkedList<T> list;
		private Node<T> currentNode;
		private int currentNodeIndex;

		private int consumed = 0;
		private boolean hasNext = false;

		public Iter(ChunkedLinkedList<T> list) {
			this.list = list;
			this.currentNode = list.headNode;
		}

		private void advanceIfNeeded() {
			if (this.hasNext)
				return;
			while (this.currentNodeIndex >= this.currentNode.size && this.currentNode != null) {
				this.currentNode = this.currentNode.next;
				this.currentNodeIndex = 0;
			}
			this.hasNext = this.currentNode != null;
		}

		@Override
		public boolean hasNext() {
			advanceIfNeeded();
			return this.hasNext;
		}

		@Override
		public T next() {
			advanceIfNeeded();
			this.hasNext = false;
			this.consumed += 1;
			return this.currentNode.elements[this.currentNodeIndex++];
		}

		@Override
		public SizeHint sizeHint() {
			return SizeHint.exactly(this.list.size - this.consumed);
		}

		@Override
		public int properties() {
			return PROPERTY_FUSED;
		}

		@Override
		public void forEach(Consumer<? super T> consumer) {
			while (this.currentNode != null) {
				for (; this.currentNodeIndex < this.currentNode.size; ++this.currentNodeIndex) {
					consumer.accept(this.currentNode.elements[this.currentNodeIndex]);
				}
				this.currentNodeIndex = 0;
				this.currentNode = this.currentNode.next;
			}
		}

	}

}
