package net.xavil.hawklib.math;

import net.minecraft.util.Mth;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

public final class ColorRgba implements ColorAccess, Hashable {

	public static final ColorRgba TRANSPARENT = new ColorRgba(0, 0, 0, 0);
	public static final ColorRgba BLACK = new ColorRgba(0, 0, 0, 1);
	public static final ColorRgba RED = new ColorRgba(1, 0, 0, 1);
	public static final ColorRgba GREEN = new ColorRgba(0, 1, 0, 1);
	public static final ColorRgba BLUE = new ColorRgba(0, 0, 1, 1);
	public static final ColorRgba CYAN = new ColorRgba(0, 1, 1, 1);
	public static final ColorRgba MAGENTA = new ColorRgba(1, 0, 1, 1);
	public static final ColorRgba YELLOW = new ColorRgba(1, 1, 0, 1);
	public static final ColorRgba WHITE = new ColorRgba(1, 1, 1, 1);

	public final float r, g, b, a;

	public ColorRgba(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	public ColorRgba(float r, float g, float b) {
		this(r, g, b, 1f);
	}

	public static ColorRgba fromDoubles(double r, double g, double b, double a) {
		return new ColorRgba((float) r, (float) g, (float) b, (float) a);
	}

	public static ColorRgba fromDoubles(double r, double g, double b) {
		return new ColorRgba((float) r, (float) g, (float) b, 1f);
	}

	// @override:off
	@Override public float r() { return this.r; }
	@Override public float g() { return this.g; }
	@Override public float b() { return this.b; }
	@Override public float a() { return this.a; }
	// @override:on

	public static final class Mutable {
		public float r, g, b, a;

		public Mutable() {}

		public Mutable(float r, float g, float b, float a) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
		}

		public Mutable(float r, float g, float b) {
			this(r, g, b, 1f);
		}

	}

	public static Mutable set(Mutable r, ColorAccess c) {
		r.r = c.r();
		r.g = c.g();
		r.b = c.b();
		r.a = c.a();
		return r;
	}

	public static ColorRgba fromPackedRgb(int rgb) {
		float b = (rgb & 0xff) / 255f;
		rgb >>>= 8;
		float g = (rgb & 0xff) / 255f;
		rgb >>>= 8;
		float r = (rgb & 0xff) / 255f;
		rgb >>>= 8;
		return new ColorRgba(r, g, b, 1f);
	}

	public static ColorRgba fromPackedRgba(int rgba) {
		float a = (rgba & 0xff) / 255f;
		rgba >>>= 8;
		float b = (rgba & 0xff) / 255f;
		rgba >>>= 8;
		float g = (rgba & 0xff) / 255f;
		rgba >>>= 8;
		float r = (rgba & 0xff) / 255f;
		rgba >>>= 8;
		return new ColorRgba(r, g, b, a);
	}

	// https://www.cs.rit.edu/~ncs/color/t_convert.html
	public static ColorRgba fromHsva(float h, float s, float v, float a) {
		if (s == 0) {
			// achromatic (grey)
			return new ColorRgba(v, v, v, a);
		}

		h /= 60; // sector 0 to 5
		final int i = Mth.floor(h);
		final float f = h - i; // fractional part of h
		final float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));

		switch (i) {
			// @formatter:off
			case 0:  return new ColorRgba(v, t, p, a);
			case 1:  return new ColorRgba(q, v, p, a);
			case 2:  return new ColorRgba(p, v, t, a);
			case 3:  return new ColorRgba(p, q, v, a);
			case 4:  return new ColorRgba(t, p, v, a);
			default: return new ColorRgba(v, p, q, a);
			// @formatter:on
		}
	}

	public ColorHsva toHsva() {
		return ColorHsva.fromRgba(r, g, b, a);
	}

	public ColorRgba withR(float r) {
		return new ColorRgba(r, g, b, a);
	}

	public ColorRgba withG(float g) {
		return new ColorRgba(r, g, b, a);
	}

	public ColorRgba withB(float b) {
		return new ColorRgba(r, g, b, a);
	}

	public ColorRgba withA(float a) {
		return new ColorRgba(r, g, b, a);
	}

	public ColorRgba mul(float b) {
		return new ColorRgba(this.r * b, this.g * b, this.b * b, this.a * b);
	}

	public ColorRgba mul(ColorRgba b) {
		return new ColorRgba(this.r * b.r, this.g * b.g, this.b * b.b, this.a * b.a);
	}

	public ColorRgba add(ColorRgba b) {
		return new ColorRgba(this.r + b.r, this.g + b.g, this.b + b.b, this.a + b.a);
	}

	public ColorRgba sub(ColorRgba b) {
		return new ColorRgba(this.r - b.r, this.g - b.g, this.b - b.b, this.a - b.a);
	}

	public static ColorRgba min(ColorRgba a, ColorRgba b) {
		return new ColorRgba(
				Math.min(a.r, b.r),
				Math.min(a.g, b.g),
				Math.min(a.b, b.b),
				Math.min(a.a, b.a));
	}

	public static ColorRgba max(ColorRgba a, ColorRgba b) {
		return new ColorRgba(
				Math.max(a.r, b.r),
				Math.max(a.g, b.g),
				Math.max(a.b, b.b),
				Math.max(a.a, b.a));
	}

	public static ColorRgba lerp(float t, ColorRgba a, ColorRgba b) {
		return new ColorRgba(
				Mth.lerp(t, a.r, b.r),
				Mth.lerp(t, a.g, b.g),
				Mth.lerp(t, a.b, b.b),
				Mth.lerp(t, a.a, b.a));
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendFloat(this.r).appendFloat(this.g).appendFloat(this.b).appendFloat(this.a);
	}

}
