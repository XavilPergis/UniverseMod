package net.xavil.universegen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.xavil.util.Assert;

public class LinearSpline {
	
	private record ControlPoint(double t, double value) {}

	private final List<ControlPoint> controlPoints = new ArrayList<>();

	public void addControlPoint(double t, double valueAtPoint) {
		this.controlPoints.add(new ControlPoint(t, valueAtPoint));
		this.controlPoints.sort(Comparator.comparingDouble(cp -> cp.t));
	}

	public double sample(double t) {
		Assert.isTrue(this.controlPoints.size() >= 2);
		var lo = this.controlPoints.get(0);
		for (var i = 1; i < this.controlPoints.size(); ++i) {
			if (t >= lo.t) {
				var hi = this.controlPoints.get(i);
				final var p = (t - lo.t) / (hi.t - lo.t);
				return lo.value + p * hi.value;
			}
		}
		return 0;
	}

}
