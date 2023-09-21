package net.xavil.hawklib.math;

import net.minecraft.util.Mth;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;

public final class Color implements ColorAccess, Hashable {

	public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	public static final Color BLACK = new Color(0, 0, 0, 1);
	public static final Color RED = new Color(1, 0, 0, 1);
	public static final Color GREEN = new Color(0, 1, 0, 1);
	public static final Color BLUE = new Color(0, 0, 1, 1);
	public static final Color CYAN = new Color(0, 1, 1, 1);
	public static final Color MAGENTA = new Color(1, 0, 1, 1);
	public static final Color YELLOW = new Color(1, 1, 0, 1);
	public static final Color WHITE = new Color(1, 1, 1, 1);

	public final float r, g, b, a;

	public Color(float r, float g, float b, float a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	public Color(float r, float g, float b) {
		this(r, g, b, 1f);
	}

	public static Color fromDoubles(double r, double g, double b, double a) {
		return new Color((float) r, (float) g, (float) b, (float) a);
	}

	public static Color fromDoubles(double r, double g, double b) {
		return new Color((float) r, (float) g, (float) b, 1f);
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

	public static Color fromPackedRgb(int rgb) {
		float b = (rgb & 0xff) / 255f;
		rgb >>>= 8;
		float g = (rgb & 0xff) / 255f;
		rgb >>>= 8;
		float r = (rgb & 0xff) / 255f;
		rgb >>>= 8;
		return new Color(r, g, b, 1f);
	}

	public static Color fromPackedRgba(int rgba) {
		float a = (rgba & 0xff) / 255f;
		rgba >>>= 8;
		float b = (rgba & 0xff) / 255f;
		rgba >>>= 8;
		float g = (rgba & 0xff) / 255f;
		rgba >>>= 8;
		float r = (rgba & 0xff) / 255f;
		rgba >>>= 8;
		return new Color(r, g, b, a);
	}

	// https://www.cs.rit.edu/~ncs/color/t_convert.html
	public static Color fromHsva(float h, float s, float v, float a) {
		if (s == 0) {
			// achromatic (grey)
			return new Color(v, v, v, a);
		}

		h /= 60; // sector 0 to 5
		final int i = Mth.floor(h);
		final float f = h - i; // fractional part of h
		final float p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));

		switch (i) {
			// @formatter:off
			case 0:  return new Color(v, t, p, a);
			case 1:  return new Color(q, v, p, a);
			case 2:  return new Color(p, v, t, a);
			case 3:  return new Color(p, q, v, a);
			case 4:  return new Color(t, p, v, a);
			default: return new Color(v, p, q, a);
			// @formatter:on
		}
	}

	public Color withR(float r) {
		return new Color(r, g, b, a);
	}

	public Color withG(float g) {
		return new Color(r, g, b, a);
	}

	public Color withB(float b) {
		return new Color(r, g, b, a);
	}

	public Color withA(float a) {
		return new Color(r, g, b, a);
	}

	public Color mul(float b) {
		return new Color(this.r * b, this.g * b, this.b * b, this.a * b);
	}

	public Color mul(Color b) {
		return new Color(this.r * b.r, this.g * b.g, this.b * b.b, this.a * b.a);
	}

	public Color add(Color b) {
		return new Color(this.r + b.r, this.g + b.g, this.b + b.b, this.a + b.a);
	}

	public Color sub(Color b) {
		return new Color(this.r - b.r, this.g - b.g, this.b - b.b, this.a - b.a);
	}

	public static Color min(Color a, Color b) {
		return new Color(
				Math.min(a.r, b.r),
				Math.min(a.g, b.g),
				Math.min(a.b, b.b),
				Math.min(a.a, b.a));
	}

	public static Color max(Color a, Color b) {
		return new Color(
				Math.max(a.r, b.r),
				Math.max(a.g, b.g),
				Math.max(a.b, b.b),
				Math.max(a.a, b.a));
	}

	public static Color lerp(float t, Color a, Color b) {
		return new Color(
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
