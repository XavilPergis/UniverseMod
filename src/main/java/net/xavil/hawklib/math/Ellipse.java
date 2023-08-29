package net.xavil.hawklib.math;

import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public record Ellipse(Vec3 center, Vec3 right, Vec3 up) {

	public static Ellipse fromOrbit(Vec3Access focus, OrbitalPlane plane, OrbitalShape shape, double precessionAngle) {
		return fromOrbit(focus, plane, shape, precessionAngle, true);
	}

	public static Ellipse fromOrbit(Vec3Access focus, OrbitalPlane plane, OrbitalShape shape, double precessionAngle, boolean rightFocus) {
		// var rotation = plane.rotationFromReference();
		// rotation = Quat.axisAngle(plane.normal(), precessionAngle).hamiltonProduct(rotation);
		var rotation = plane.rotationFromReference().hamiltonProduct(Quat.axisAngle(Vec3.YP, precessionAngle));

		final var flipRight = rightFocus ? -1.0 : 1.0;
		final var rightDir = rotation.transform(Vec3.XP).mul(flipRight);
		final var upDir = rotation.transform(Vec3.ZP);

		final var semiMajor = shape.semiMajor();
		final var semiMinor = shape.semiMinor();

		final var center = rightDir.mul(-shape.focalDistance()).add(focus);

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

	public Vec3.Mutable pointFromTrueAnomaly(Vec3.Mutable out, double angle) {
		return Vec3.set(out, pointFromTrueAnomaly(angle));
	}

}
