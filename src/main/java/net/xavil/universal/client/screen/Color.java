package net.xavil.universal.client.screen;

import net.minecraft.util.Mth;

public record Color(float r, float g, float b, float a) {

	public static final Color TRANSPARENT = new Color(0, 0, 0, 0);
	public static final Color BLACK = new Color(0, 0, 0, 1);
	public static final Color RED = new Color(1, 0, 0, 1);
	public static final Color GREEN = new Color(0, 1, 0, 1);
	public static final Color BLUE = new Color(0, 0, 1, 1);
	public static final Color CYAN = new Color(0, 1, 1, 1);
	public static final Color MAGENTA = new Color(1, 0, 1, 1);
	public static final Color YELLOW = new Color(1, 1, 0, 1);
	public static final Color WHITE = new Color(1, 1, 1, 1);

	public Color withR(double r) {
		return new Color(r, g, b, a);
	}

	public Color withG(double g) {
		return new Color(r, g, b, a);
	}

	public Color withB(double b) {
		return new Color(r, g, b, a);
	}

	public Color withA(double a) {
		return new Color(r, g, b, a);
	}

	public Color mul(double b) {
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

	public static Color lerp(double t, Color a, Color b) {
		return new Color(
				Mth.lerp(t, a.r, b.r),
				Mth.lerp(t, a.g, b.g),
				Mth.lerp(t, a.b, b.b),
				Mth.lerp(t, a.a, b.a));
	}

	public Color(double r, double g, double b, double a) {
		this((float) r, (float) g, (float) b, (float) a);
	}

	public static Color rgb(double r, double g, double b) {
		return new Color(r, g, b, 1);
	}

}
