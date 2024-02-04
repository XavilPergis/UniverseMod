package net.xavil.hawklib;

import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.iterator.IntoIterator;
import net.xavil.hawklib.collections.iterator.Iterator;

public final class WeightedList<T> implements MutableList<WeightedList.Entry<T>> {
	public static final class Entry<T> {
		public final double weight;
		public final T value;

		public Entry(double weight, T value) {
			this.weight = weight;
			this.value = value;
		}

		public Entry<T> withWeight(double weight) {
			return new Entry<>(weight, value);
		}

		public Entry<T> withValue(T value) {
			return new Entry<>(weight, value);
		}
	}

	private final Vector<Entry<T>> entries;
	private double totalWeight;

	public WeightedList() {
		this(new Vector<>(), 0);
	}

	public WeightedList(IntoIterator<Entry<T>> entries) {
		this(entries.iter().collectTo(Vector::new), 0);
		this.entries.forEach(e -> this.totalWeight += e.weight);
	}

	private WeightedList(Vector<Entry<T>> entries, double totalWeight) {
		this.entries = entries;
		this.totalWeight = totalWeight;
	}

	public void push(double weight, T value) {
		this.entries.push(new Entry<T>(weight, value));
		this.totalWeight += weight;
	}

	// t in [0, 1)
	public int pickIndex(double t) {
		double inertia = t * this.totalWeight;
		for (int i = 0; i < this.entries.size(); ++i) {
			final var elem = this.entries.get(i);
			if (elem.weight > inertia)
				return i;
			inertia -= elem.weight;
		}
		// this just happens to return the sentinel -1 when the list is empty.
		return this.entries.size() - 1;
	}
	public T pick(double t) {
		double inertia = t * this.totalWeight;
		for (int i = 0; i < this.entries.size(); ++i) {
			final var elem = this.entries.get(i);
			if (elem.weight > inertia)
				return elem.value;
			inertia -= elem.weight;
		}
		return this.entries.last().map(e -> e.value).unwrapOrNull();
	}

	public void remove(T value) {
		final int i = this.entries.indexOf(e -> e.value == value);
		final var elem = this.entries.remove(i);
		this.totalWeight -= elem.weight;
	}

	public double totalWeight() {
		return this.totalWeight;
	}

	@Override
	protected WeightedList<T> clone() {
		return new WeightedList<>(new Vector<>(this.entries), this.totalWeight);
	}

	@Override
	public Iterator<Entry<T>> iter() {
		return this.entries.iter();
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
	public Entry<T> set(int index, Entry<T> value) {
		final var res = this.entries.set(index, value);
		this.totalWeight += value.weight - res.weight;
		return res;
	}

	@Override
	public void insert(int index, Entry<T> value) {
		this.entries.insert(index, value);
		this.totalWeight += value.weight;
	}

	@Override
	public Entry<T> remove(int index) {
		final var res = this.entries.remove(index);
		this.totalWeight -= res.weight;
		return res;
	}
}

