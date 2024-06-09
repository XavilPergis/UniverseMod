package net.xavil.hawklib.collections.impl;

import java.util.Arrays;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.xavil.hawklib.collections.interfaces.ImmutableListFloat;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.collections.iterator.IteratorFloat;
import net.xavil.hawklib.collections.iterator.SizeHint;

public final class ImmutableListArrayFloat implements ImmutableListFloat {

	private final float[] elements;

	public ImmutableListArrayFloat(float[] elements) {
		this.elements = Arrays.copyOf(elements, elements.length);
	}

	@Override
	public int size() {
		return this.elements.length;
	}

	@Override
	public IteratorFloat iter() {
		return new Iter(this);
	}

	@Override
	public float get(int index) {
		return this.elements[index];
	}

	@Override
	public void forEach(FloatConsumer consumer) {
		for (int i = 0; i < this.elements.length; ++i)
			consumer.accept(this.elements[i]);
	}

	@Override
	public String toString() {
		return ListUtil.asString(this);
	}

	public static class Iter implements IteratorFloat {
		private final ImmutableListArrayFloat list;
		private int cur;

		public Iter(ImmutableListArrayFloat list) {
			this(list, 0);
		}

		private Iter(ImmutableListArrayFloat list, int cur) {
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
		public float next() {
			return this.list.elements[this.cur++];
		}

		@Override
		public void forEach(FloatConsumer consumer) {
			for (; this.cur < this.list.elements.length; ++this.cur) {
				consumer.accept(this.list.elements[this.cur]);
			}
		}

		@Override
		public int properties() {
			return Iterator.PROPERTY_FUSED;
		}

		@Override
		public IteratorFloat skip(int amount) {
			final var newCur = Math.min(this.cur + amount, this.list.elements.length);
			return new Iter(this.list, newCur);
		}
	}

}
