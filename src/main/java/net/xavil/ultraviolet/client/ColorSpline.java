package net.xavil.ultraviolet.client;

import java.util.Comparator;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.ColorRgba;

public final class ColorSpline {

	private record ControlPoint(float t, ColorRgba color) {
	}

	private float minT = Float.POSITIVE_INFINITY, maxT = Float.NEGATIVE_INFINITY;
	private final MutableList<ColorSpline.ControlPoint> controlPoints = new Vector<>();

	public void addControlPoint(float t, ColorRgba color) {
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
		this.controlPoints.clear();
	}

	public ColorRgba sample(float t) {
		for (var i = 1; i < this.controlPoints.size(); ++i) {
			final var lo = this.controlPoints.get(i - 1);
			final var hi = this.controlPoints.get(i);
			if (t >= lo.t && t < hi.t) {
				final var st = Mth.inverseLerp(t, lo.t, hi.t);
				final var r = Mth.lerp(st, lo.color.r(), hi.color.r());
				final var g = Mth.lerp(st, lo.color.g(), hi.color.g());
				final var b = Mth.lerp(st, lo.color.b(), hi.color.b());
				final var a = Mth.lerp(st, lo.color.a(), hi.color.a());
				return new ColorRgba(r, g, b, a);
			}
		}
		if (this.controlPoints.isEmpty())
			return ColorRgba.TRANSPARENT;
		final var lo = this.controlPoints.get(0);
		final var hi = this.controlPoints.get(this.controlPoints.size() - 1);
		return t <= lo.t ? lo.color : hi.color;
	}

	interface ColorConsumer {
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

		int currentControlPoint = 0;
		for (int i = 0; i < subdivisions; ++i) {
			final var t = Mth.lerp(i / (float) subdivisions, startT, endT);
			if (t <= this.minT) {
				colorConsumer.accept(i, min.color.r, min.color.g, min.color.b, min.color.a);
			} else if (t >= this.maxT) {
				colorConsumer.accept(i, max.color.r, max.color.g, max.color.b, max.color.a);
			} else {
				while (t > this.controlPoints.get(currentControlPoint + 1).t) {
					currentControlPoint += 1;
				}

				final var lo = this.controlPoints.get(currentControlPoint);
				final var hi = this.controlPoints.get(currentControlPoint + 1);

				final var st = Mth.inverseLerp(t, lo.t, hi.t);
				final var r = Mth.lerp(st, lo.color.r(), hi.color.r());
				final var g = Mth.lerp(st, lo.color.g(), hi.color.g());
				final var b = Mth.lerp(st, lo.color.b(), hi.color.b());
				final var a = Mth.lerp(st, lo.color.a(), hi.color.a());
				colorConsumer.accept(i, r, g, b, a);
			}
		}
	}

}