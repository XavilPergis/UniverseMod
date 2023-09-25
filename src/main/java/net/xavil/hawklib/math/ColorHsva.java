package net.xavil.hawklib.math;

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

	public ColorRgba toRgba() {
		return ColorRgba.fromHsva(h, s, v, a);
	}

}
