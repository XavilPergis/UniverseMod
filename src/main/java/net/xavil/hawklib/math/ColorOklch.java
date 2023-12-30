package net.xavil.hawklib.math;

import net.minecraft.util.Mth;

public final class ColorOklch {

	public float l, c, h, alpha;

	public ColorOklch(float l, float c, float h, float alpha) {
		this.l = l;
		this.c = c;
		this.h = h;
		this.alpha = alpha;
	}

	public ColorOklch(float l, float c, float h) {
		this(l, c, h, 1f);
	}

	// https://bottosson.github.io/posts/oklab/
	public static ColorOklch fromLinearSrgb(ColorRgba c) {
		return fromLinearSrgb(c.r, c.g, c.b, c.a);
	}

	public static ColorOklch fromLinearSrgb(float red, float green, float blue, float alpha) {
		// conversion to oklab
		float l = 0.4122214708f * red + 0.5363325363f * green + 0.0514459929f * blue;
		float m = 0.2119034982f * red + 0.6806995451f * green + 0.1073969566f * blue;
		float s = 0.0883024619f * red + 0.2817188376f * green + 0.6299787005f * blue;
		l = (float) Math.cbrt(l);
		m = (float) Math.cbrt(m);
		s = (float) Math.cbrt(s);
		float L = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s;
		float a = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s;
		float b = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s;

		// conversion to oklch
		float C = (float) Math.sqrt(a * a + b * b);
		float h = (float) Math.atan2(b, a);

		return new ColorOklch(L, C, h, alpha);
	}

	public static ColorRgba toLinearSrgb(ColorOklch c) {
		return toLinearSrgb(c.l, c.c, c.h, c.alpha);
	}

	// hue is in degrees
	public static ColorRgba toLinearSrgb(float L, float C, float h, float alpha) {
		// conversion to oklab
		h *= Mth.DEG_TO_RAD;
		final float a = C * Mth.cos(h);
		final float b = C * Mth.sin(h);

		// conversion to srgb
		float l = L + 0.3963377774f * a + 0.2158037573f * b;
		float m = L - 0.1055613458f * a - 0.0638541728f * b;
		float s = L - 0.0894841775f * a - 1.2914855480f * b;
		l = l * l * l;
		m = m * m * m;
		s = s * s * s;
		return new ColorRgba(
				+4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
				-1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
				-0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s,
				alpha);
	}
}
