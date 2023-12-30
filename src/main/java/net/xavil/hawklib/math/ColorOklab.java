package net.xavil.hawklib.math;

public final class ColorOklab {

	public float l, a, b, alpha;

	public ColorOklab(float l, float a, float b, float alpha) {
		this.l = l;
		this.a = a;
		this.b = b;
		this.alpha = alpha;
	}

	public ColorOklab(float l, float a, float b) {
		this(l, a, b, 1f);
	}

	// https://bottosson.github.io/posts/oklab/
	public static ColorOklab fromLinearSrgb(ColorRgba c) {
		return fromLinearSrgb(c.r, c.g, c.b, c.a);
	}

	public static ColorOklab fromLinearSrgb(float r, float g, float b, float alpha) {
		float l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b;
		float m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b;
		float s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b;
		l = (float) Math.cbrt(l);
		m = (float) Math.cbrt(m);
		s = (float) Math.cbrt(s);

		return new ColorOklab(
				0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
				1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
				0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
				alpha);
	}

	public static ColorRgba toLinearSrgb(ColorOklab c) {
		return toLinearSrgb(c.l, c.a, c.b, c.alpha);
	}

	public static ColorRgba toLinearSrgb(float L, float a, float b, float alpha) {
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

	public static void toLinearSrgb(ColorRgba.Mutable out, ColorOklab c) {
		toLinearSrgb(out, c.l, c.a, c.b, c.alpha);
	}

	public static void toLinearSrgb(ColorRgba.Mutable out, float L, float a, float b, float alpha) {
		float l = L + 0.3963377774f * a + 0.2158037573f * b;
		float m = L - 0.1055613458f * a - 0.0638541728f * b;
		float s = L - 0.0894841775f * a - 1.2914855480f * b;
		l = l * l * l;
		m = m * m * m;
		s = s * s * s;

		out.r = +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
		out.g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
		out.b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;
		out.a = alpha;
	}

}
