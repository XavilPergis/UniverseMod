package net.xavil.hawklib.math;

import net.minecraft.util.Mth;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

// inner bound is inclusive, outer bound is exclusive
public final class Interval implements Hashable {

	public static final Interval ZERO = new Interval(0, 0);
	public static final Interval ONE = new Interval(1, 1);
	public static final Interval UNIPOLAR = new Interval(0, 1);
	public static final Interval BIPOLAR = new Interval(-1, 1);

	public final double min, max;

	public Interval(double min, double max) {
		this.min = min;
		this.max = max;
	}

	public Interval(double n) {
		this(n, n);
	}

	public static double size(double l, double h) {
		return Math.abs(h - l);
	}

	public double size() {
		return size(this.min, this.max);
	}

	public Interval intersection(Interval other) {
		if (!this.intersects(other))
			return ZERO;
		return new Interval(
				Math.max(this.min, other.min),
				Math.min(this.max, other.max));
	}

	public static boolean intersects(double l1, double h1, double l2, double h2) {
		return !(l1 >= h2 || h1 < l2);
	}

	public boolean intersects(Interval other) {
		return intersects(this.min, this.max, other.min, other.max);
	}

	public boolean intersects(double otherL, double otherH) {
		return intersects(this.min, this.max, otherL, otherH);
	}

	public static boolean contains(double l1, double h1, double l2, double h2) {
		return l1 <= l2 && h1 >= h2;
	}

	public boolean contains(Interval other) {
		return contains(this.min, this.max, other.min, other.max);
	}

	public static boolean contains(double l, double h, double n) {
		return n >= l && n < h;
	}

	public boolean contains(double value) {
		return contains(this.min, this.max, value);
	}

	public Interval mul(double scale) {
		return new Interval(scale * this.min, scale * this.max);
	}

	public Interval add(double shift) {
		return new Interval(this.min + shift, this.max + shift);
	}

	public double lerp(double t) {
		return Mth.lerp(t, this.min, this.max);
	}

	public double inverseLerp(double v) {
		return Mth.inverseLerp(v, this.min, this.max);
	}

	public Interval expand(double n) {
		double min = this.min - n, max = this.max + n;
		if (min > max)
			min = max = (this.min + this.max) / 2;
		return new Interval(min, max);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendDouble(this.min);
		hasher.appendDouble(this.max);
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Interval other && this.min == other.min && this.max == other.max;
	}

	@Override
	public String toString() {
		return String.format("[%f, %f)", this.min, this.max);
	}

}
