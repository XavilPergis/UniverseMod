package net.xavil.universal.common.universe.system.gen;

// inner bound is inclusive, outer bound is exclusive
public record Interval(double inner, double outer) {

	public static final Interval ZERO = new Interval(0, 0);

	public double size() {
		return this.outer - this.inner;
	}

	public Interval intersection(Interval other) {
		if (!this.intersectsWith(other))
			return ZERO;
		return new Interval(Math.max(this.inner, other.inner), Math.min(this.outer, other.outer));
	}

	public boolean intersectsWith(Interval other) {
		return this.contains(other.inner) || this.contains(other.outer)
				|| other.contains(this.inner) || other.contains(this.outer);
	}

	public boolean contains(Interval other) {
		return this.inner <= other.inner && this.outer >= other.outer;
	}

	public boolean contains(double value) {
		return value >= this.inner && value <= this.outer;
	}

}
