package net.xavil.hawklib;

import java.util.Comparator;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;

public class LinearSpline {

	private record ControlPoint(double t, double value) {
	}

	private double minT = Double.POSITIVE_INFINITY, maxT = Double.NEGATIVE_INFINITY;
	private final MutableList<ControlPoint> controlPoints = new Vector<>();

	public void addControlPoint(double t, double valueAtPoint) {
		this.controlPoints.push(new ControlPoint(t, valueAtPoint));
		this.controlPoints.sort(Comparator.comparingDouble(cp -> cp.t));
		this.minT = Math.min(this.minT, t);
		this.maxT = Math.max(this.maxT, t);
		for (int i = 1; i < this.controlPoints.size(); ++i) {
			final var lo = this.controlPoints.get(i - 1);
			final var hi = this.controlPoints.get(i);
			if (lo.t == hi.t)
				throw new IllegalArgumentException(String.format(
						"Duplicate control point: T-value '%d' is already occupied!",
						lo.t));
		}
	}

	public LinearSpline inverse() {
		final var res = new LinearSpline();
		this.controlPoints.forEach(cp -> res.addControlPoint(cp.value, cp.t));
		return res;
	}

	public double sample(double t) {
		for (var i = 1; i < this.controlPoints.size(); ++i) {
			final var lo = this.controlPoints.get(i - 1);
			final var hi = this.controlPoints.get(i);
			if (t >= lo.t && t < hi.t) {
				final var st = Mth.inverseLerp(t, lo.t, hi.t);
				return Mth.lerp(st, lo.value, hi.value);
			}
		}
		if (this.controlPoints.isEmpty())
			return 0;
		final var lo = this.controlPoints.get(0);
		final var hi = this.controlPoints.get(this.controlPoints.size() - 1);
		return t <= lo.t ? lo.value : hi.value;
	}

	public double areaUnder(double a, double b) {
		if (a > b)
			throw new IllegalArgumentException(String.format(
					"lower bound of %f is creater than upper bound of %f",
					a, b));

		if (this.controlPoints.isEmpty())
			return 0;
		if (this.controlPoints.size() == 1)
			return (b - a) * this.controlPoints.get(0).value;

		double area = 0;
		for (int i = 1; i < this.controlPoints.size(); ++i) {
			final var lo = this.controlPoints.get(i - 1);
			final var hi = this.controlPoints.get(i);

			if (b <= lo.t)
				break;
			if (a >= hi.t)
				continue;

			final var ta = Mth.inverseLerp(Math.max(a, lo.t), lo.t, hi.t);
			final var tb = Mth.inverseLerp(Math.min(b, hi.t), lo.t, hi.t);
			final var va = Mth.lerp(ta, lo.value, hi.value);
			final var vb = Mth.lerp(tb, lo.value, hi.value);
			final var h = (tb - ta) * (hi.t - lo.t);
			area += 0.5 * (va + vb) * h;
		}
		return area;
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder("LinearSpline[");
		if (this.controlPoints.size() > 0) {
			builder.append("(");
			builder.append(this.controlPoints.get(0).t);
			builder.append(" -> ");
			builder.append(this.controlPoints.get(0).value);
			builder.append(")");
			for (int i = 1; i < this.controlPoints.size(); ++i) {
				builder.append(", (");
				builder.append(this.controlPoints.get(i).t);
				builder.append(" -> ");
				builder.append(this.controlPoints.get(i).value);
				builder.append(")");
			}
		}
		builder.append("]");
		return builder.toString();
	}

}
