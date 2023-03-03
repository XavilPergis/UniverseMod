package net.xavil.universal.common;

import net.xavil.universal.common.universe.Quat;
import net.xavil.universal.common.universe.Vec3;
import net.xavil.universal.common.universe.system.OrbitalPlane;
import net.xavil.universal.common.universe.system.OrbitalShape;

public record Ellipse(Vec3 center, Vec3 right, Vec3 up) {

	public static Ellipse fromOrbit(Vec3 focus, OrbitalPlane plane, OrbitalShape shape) {
		return fromOrbit(focus, plane, shape, true);
	}

	public static Ellipse fromOrbit(Vec3 focus, OrbitalPlane plane, OrbitalShape shape, boolean rightFocus) {
		var flipRight = rightFocus ? -1.0 : 1.0;
		var rightDir = plane.rotationFromReference().transform(Vec3.XP).mul(flipRight);
		var upDir = plane.rotationFromReference().transform(Vec3.ZP);

		var semiMajor = shape.semiMajor();
		var semiMinor = shape.semiMinor();

		var center = focus.add(rightDir.mul(-shape.focalDistance()));

		return new Ellipse(center, rightDir.mul(semiMajor), upDir.mul(semiMinor));
	}

	public static double focalDistance(double semiMajor, double semiMinor) {
		if (semiMinor > semiMajor)
			semiMinor = semiMajor;
		return Math.sqrt(semiMajor * semiMajor - semiMinor * semiMinor);
	}


	public static double eccentricity(double semiMajor, double semiMinor) {
		if (semiMinor > semiMajor)
			semiMinor = semiMajor;
		return Math.sqrt(1 - (semiMinor * semiMinor) / (semiMajor * semiMajor));
	}

	public Vec3 pointFromTrueAnomaly(double angle) {
		// https://en.wikipedia.org/wiki/True_anomaly#Radius_from_true_anomaly
		var e = eccentricity(right.length(), up.length());
		var r = right.length() * (1 - e * e) / (1 + e * Math.cos(angle));

		var rn = this.right.normalize();
		var rotation = Quat.axisAngle(rn.cross(this.up).normalize(), angle);

		var focalDistance = focalDistance(right.length(), up.length());

		return this.center.add(rn.mul(focalDistance)).add(rotation.transform(rn).mul(r));
	}

}
