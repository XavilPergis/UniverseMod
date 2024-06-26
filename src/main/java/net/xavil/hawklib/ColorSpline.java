package net.xavil.hawklib;

import java.util.Comparator;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.ColorOklab;
import net.xavil.hawklib.math.ColorRgba;

public final class ColorSpline {

	private record ControlPoint(float t, ColorOklab color) {
	}

	private float minT = Float.POSITIVE_INFINITY, maxT = Float.NEGATIVE_INFINITY;
	private final MutableList<ColorSpline.ControlPoint> controlPoints = new Vector<>();

	public void addControlPoint(float t, ColorRgba color) {
		addControlPoint(t, ColorOklab.fromLinearSrgb(color));
	}

	public void addControlPoint(float t, ColorOklab color) {
		this.minT = Math.min(this.minT, t);
		this.maxT = Math.max(this.maxT, t);
		this.controlPoints.push(new ControlPoint(t, color));
		this.controlPoints.sort(Comparator.comparingDouble(cp -> cp.t));
		for (int i = 1; i < this.controlPoints.size(); ++i) {
			final var lo = this.controlPoints.get(i - 1);
			final var hi = this.controlPoints.get(i);
			if (lo.t == hi.t)
				throw new IllegalArgumentException(String.format(
						"Duplicate control point: T-value '%f' is already occupied!",
						lo.t));
		}
	}

	public void clear() {
		this.minT = Float.POSITIVE_INFINITY;
		this.maxT = Float.NEGATIVE_INFINITY;
		this.controlPoints.clear();
	}

	public ColorRgba sample(float t) {
		for (var i = 1; i < this.controlPoints.size(); ++i) {
			final var lo = this.controlPoints.get(i - 1);
			final var hi = this.controlPoints.get(i);
			if (t >= lo.t && t < hi.t) {
				final var st = Mth.inverseLerp(t, lo.t, hi.t);
				final var l = Mth.lerp(st, lo.color.l, hi.color.l);
				final var a = Mth.lerp(st, lo.color.a, hi.color.a);
				final var b = Mth.lerp(st, lo.color.b, hi.color.b);
				final var alpha = Mth.lerp(st, lo.color.alpha, hi.color.alpha);
				return ColorOklab.toLinearSrgb(l, a, b, alpha);
			}
		}
		if (this.controlPoints.isEmpty())
			return ColorRgba.TRANSPARENT;
		final var lo = this.controlPoints.get(0);
		final var hi = this.controlPoints.get(this.controlPoints.size() - 1);
		return t <= lo.t ? ColorOklab.toLinearSrgb(lo.color) : ColorOklab.toLinearSrgb(hi.color);
	}

	public interface ColorConsumer {
		void accept(int i, float r, float g, float b, float a);
	}

	public void sample(float startT, float endT, int subdivisions, ColorSpline.ColorConsumer colorConsumer) {
		if (this.controlPoints.isEmpty()) {
			for (int i = 0; i < subdivisions; ++i)
				colorConsumer.accept(i, 0f, 0f, 0f, 0f);
			return;
		}

		final var min = this.controlPoints.get(0);
		final var max = this.controlPoints.get(this.controlPoints.size() - 1);

		final var rgba = new ColorRgba.Mutable();

		int currentControlPoint = 0;
		for (int i = 0; i < subdivisions; ++i) {
			final var t = Mth.lerp(i / (float) subdivisions, startT, endT);
			if (t <= this.minT) {
				ColorOklab.toLinearSrgb(rgba, min.color);
			} else if (t >= this.maxT) {
				ColorOklab.toLinearSrgb(rgba, max.color);
			} else {
				while (t > this.controlPoints.get(currentControlPoint + 1).t) {
					currentControlPoint += 1;
				}

				final var lo = this.controlPoints.get(currentControlPoint);
				final var hi = this.controlPoints.get(currentControlPoint + 1);

				final var st = Mth.inverseLerp(t, lo.t, hi.t);
				final var l = Mth.lerp(st, lo.color.l, hi.color.l);
				final var a = Mth.lerp(st, lo.color.a, hi.color.a);
				final var b = Mth.lerp(st, lo.color.b, hi.color.b);
				final var alpha = Mth.lerp(st, lo.color.alpha, hi.color.alpha);
				ColorOklab.toLinearSrgb(rgba, l, a, b, alpha);
			}
			colorConsumer.accept(i, rgba.r, rgba.g, rgba.b, rgba.a);
		}
	}

}