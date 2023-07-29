package net.xavil.universegen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.ultraviolet.Mod;

public class LinearSpline {

	private record ControlPoint(double t, double value) {
	}

	private final List<ControlPoint> controlPoints = new ArrayList<>();

	public void addControlPoint(double t, double valueAtPoint) {
		this.controlPoints.add(new ControlPoint(t, valueAtPoint));
		this.controlPoints.sort(Comparator.comparingDouble(cp -> cp.t));
		for (int i = 1; i < this.controlPoints.size(); ++i) {
			final var lo = this.controlPoints.get(i - 1);
			final var hi = this.controlPoints.get(i);
			if (lo.t == hi.t)
				throw new IllegalArgumentException(String.format(
						"Duplicate control point: T-value '%d' is already occupied!",
						lo.t));
		}
	}

	public double sample(double t) {
		Assert.isTrue(this.controlPoints.size() >= 2);
		for (var i = 1; i < this.controlPoints.size(); ++i) {
			var lo = this.controlPoints.get(i - 1);
			var hi = this.controlPoints.get(i);
			if (t >= lo.t && t < hi.t) {
				return Mth.lerp(Mth.inverseLerp(t, lo.t, hi.t), lo.value, hi.value);
			}
		}
		return 0;
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
