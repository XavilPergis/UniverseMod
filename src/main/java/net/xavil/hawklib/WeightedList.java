package net.xavil.hawklib;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.impl.VectorInt;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.iterator.Iterator;

public final class WeightedList<T> {

	public static final class Builder<T> implements MutableList<Builder.Entry<T>> {
		public static final class Entry<T> {
			public T value;
			public float probability;

			public Entry(T value, float probability) {
				this.value = value;
				this.probability = probability;
			}

			public Entry<T> copy() {
				return new Entry<>(this.value, this.probability);
			}
		}

		public Builder() {
		}

		private final Vector<Entry<T>> entries = new Vector<>();

		public void push(double weight, T value) {
			this.entries.push(new Entry<>(value, (float) weight));
		}

		@Override
		public int size() {
			return this.entries.size();
		}

		@Override
		public Entry<T> get(int index) {
			return this.entries.get(index);
		}

		@Override
		public Iterator<Entry<T>> iter() {
			return this.entries.iter();
		}

		@Override
		public Entry<T> set(int index, Entry<T> value) {
			return this.entries.set(index, value);
		}

		@Override
		public void insert(int index, Entry<T> value) {
			this.entries.insert(index, value);
		}

		@Override
		public Entry<T> remove(int index) {
			return this.entries.remove(index);
		}

		public WeightedList<T> build() {
			final var res = new WeightedList<T>(this.entries.size());
			this.entries.iter().map(entry -> entry.value).fillArray(res.values);

			// final var probs = new float[this.entries.size()];
			float totalWeight = 0f;
			for (int i = 0; i < this.entries.size(); ++i) {
				totalWeight += res.probabilities[i] = this.entries.get(i).probability;
			}

			final var normFactor = this.entries.size() / totalWeight;
			final VectorInt small = new VectorInt(), large = new VectorInt();
			for (int i = 0; i < this.entries.size(); ++i) {
				((res.probabilities[i] *= normFactor) >= 1 ? large : small).push(i);
			}

			while (!large.isEmpty() && !small.isEmpty()) {
				final int hi = large.popOrThrow(), lo = small.popOrThrow();
				res.alias[lo] = hi;
				res.probabilities[hi] += res.probabilities[lo] - 1;
				(res.probabilities[hi] >= 1 ? large : small).push(hi);
			}

			while (!large.isEmpty())
				res.probabilities[large.popOrThrow()] = 1f;
			while (!small.isEmpty())
				res.probabilities[small.popOrThrow()] = 1f;

			return res;
		}
	}

	private final T[] values;
	private final int[] alias;
	private final float[] probabilities;

	@SuppressWarnings("unchecked")
	private WeightedList(int size) {
		this.values = (T[]) new Object[size];
		this.alias = new int[size];
		this.probabilities = new float[size];
	}

	public T pick(double t) {
		final var fi = t * this.values.length;
		final var i = Mth.floor(fi);

		final var useAlias = fi - i > this.probabilities[i];
		return useAlias ? this.values[this.alias[i]] : this.values[i];
	}

	public int size() {
		return this.values.length;
	}

}
