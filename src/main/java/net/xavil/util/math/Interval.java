package net.xavil.util.math;

// inner bound is inclusive, outer bound is exclusive
public record Interval(double lower, double higher) {

	public static final Interval ZERO = new Interval(0, 0);

	public double size() {
		return this.higher - this.lower;
	}

	public Interval intersection(Interval other) {
		if (!this.intersects(other))
			return ZERO;
		return new Interval(Math.max(this.lower, other.lower), Math.min(this.higher, other.higher));
	}

	public boolean intersects(Interval other) {
		return this.contains(other.lower) || this.contains(other.higher)
				|| other.contains(this.lower) || other.contains(this.higher);
	}

	public boolean contains(Interval other) {
		return this.lower <= other.lower && this.higher >= other.higher;
	}

	public boolean contains(double value) {
		return value >= this.lower && value <= this.higher;
	}

}