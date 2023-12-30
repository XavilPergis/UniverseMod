package net.xavil.hawklib.math;

import net.minecraft.util.Mth;

public final class ColorHsva {

	public float h, s, v, a;

	public ColorHsva(float h, float s, float v, float a) {
		this.h = h;
		this.s = s;
		this.v = v;
		this.a = a;
	}

	public ColorHsva(float h, float s, float v) {
		this(h, s, v, 1f);
	}

	public static ColorHsva toRgba(ColorRgba c) {
		return fromRgba(c.r, c.g, c.b, c.a);
	}

	// https://www.cs.rit.edu/~ncs/color/t_convert.html
	public static ColorHsva fromRgba(float r, float g, float b, float a) {
		final float min = Math.min(r, Math.min(g, b));
		final float max = Math.max(r, Math.max(g, b));

		if (max == 0)
			return new ColorHsva(0, 0, 0, a);

		final float delta = max - min;
		final float s = delta / max;

		float h;
		if (r == max)
			h = (g - b) / delta;
		else if (g == max)
			h = 2 + (b - r) / delta;
		else
			h = 4 + (r - g) / delta;

		h *= 60;
		h = h < 0 ? h + 360 : h;

		final float v = max;
		return new ColorHsva(h, s, v, a);
	}

	public static ColorRgba toRgba(ColorHsva c) {
		return toRgba(c.h, c.s, c.v, c.a);
	}

	// https://www.cs.rit.edu/~ncs/color/t_convert.html
	public static ColorRgba toRgba(float h, float s, float v, float a) {
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

}
