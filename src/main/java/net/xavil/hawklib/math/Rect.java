package net.xavil.hawklib.math;

import net.xavil.hawklib.math.matrices.Vec2;

public record Rect(Vec2 min, Vec2 max) {

	public static final Rect UNIPOLAR = new Rect(new Vec2(0, 0), new Vec2(1, 1));
	public static final Rect BIPOLAR = new Rect(new Vec2(-1, -1), new Vec2(1, 1));

	public Rect(double minX, double minY, double maxX, double maxY) {
		this(new Vec2(minX, minY), new Vec2(maxX, maxY));
	}

	public static Rect fromCorners(Vec2 min, Vec2 max) {
		return new Rect(min, max);
	}

	public boolean contains(Vec2 point) {
		return point.x >= this.min.x && point.x < this.max.x
				&& point.y >= this.min.y && point.y < this.max.y;
	}

	public Vec2 pos() {
		return this.min;
	}

	public Vec2 size() {
		return this.max.sub(this.min);
	}

	public Rect scale(double scale) {
		final var center = this.min.add(this.max).mul(0.5);
		final var hs = this.size().mul(scale * 0.5);
		return new Rect(center.add(hs.neg()), center.add(hs));
	}
}