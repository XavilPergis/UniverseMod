package net.xavil.hawklib.math;

import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

// inner bound is inclusive, outer bound is exclusive
public final class Interval implements Hashable {

	public static final Interval ZERO = new Interval(0, 0);

	public final double lower, higher;

	public Interval(double lower, double higher) {
		this.lower = lower;
		this.higher = higher;
	}

	public static double size(double l, double h) {
		return Math.abs(h - l);
	}

	public double size() {
		return size(this.lower, this.higher);
	}

	public Interval intersection(Interval other) {
		if (!this.intersects(other))
			return ZERO;
		return new Interval(
				Math.max(this.lower, other.lower),
				Math.min(this.higher, other.higher));
	}

	public static boolean intersects(double l1, double h1, double l2, double h2) {
		return !(l1 >= h2 || h1 < l2);
	}

	public boolean intersects(Interval other) {
		return intersects(this.lower, this.higher, other.lower, other.higher);
	}

	public boolean intersects(double otherL, double otherH) {
		return intersects(this.lower, this.higher, otherL, otherH);
	}

	public static boolean contains(double l1, double h1, double l2, double h2) {
		return l1 <= l2 && h1 >= h2;
	}

	public boolean contains(Interval other) {
		return contains(this.lower, this.higher, other.lower, other.higher);
	}

	public static boolean contains(double l, double h, double n) {
		return n >= l && n < h;
	}

	public boolean contains(double value) {
		return contains(this.lower, this.higher, value);
	}

	public Interval mul(double scale) {
		return new Interval(scale * this.lower, scale * this.higher);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendDouble(this.lower);
		hasher.appendDouble(this.higher);
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Interval other && this.lower == other.lower && this.higher == other.higher;
	}

	@Override
	public String toString() {
		return String.format("[%f, %f)", this.lower, this.higher);
	}

}
