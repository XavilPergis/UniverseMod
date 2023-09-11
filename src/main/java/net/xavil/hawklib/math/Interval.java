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

	public double size() {
		return this.higher - this.lower;
	}

	public Interval intersection(Interval other) {
		if (!this.intersects(other))
			return ZERO;
		return new Interval(
				Math.max(this.lower, other.lower),
				Math.min(this.higher, other.higher));
	}

	public boolean intersects(Interval other) {
		return !(this.lower >= other.higher || this.higher < other.lower);
	}

	public boolean contains(Interval other) {
		return this.lower <= other.lower && this.higher >= other.higher;
	}

	public boolean contains(double value) {
		return value >= this.lower && value < this.higher;
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
