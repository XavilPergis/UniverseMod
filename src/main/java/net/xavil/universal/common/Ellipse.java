package net.xavil.universal.common;

import net.xavil.universal.common.universe.Quat;
import net.xavil.universal.common.universe.Vec3;
import net.xavil.universal.common.universe.system.OrbitalPlane;
import net.xavil.universal.common.universe.system.OrbitalShape;

public record Ellipse(Vec3 center, Vec3 right, Vec3 up) {

	public static Ellipse fromOrbit(Vec3 focus, OrbitalPlane plane, OrbitalShape shape) {
		var rightDir = plane.rotationFromReference().transform(Vec3.XP);
		var upDir = plane.rotationFromReference().transform(Vec3.ZP);

		var semiMajor = shape.semiMajor();
		var semiMinor = shape.semiMinor();

		var distanceToCenter = Math.sqrt(semiMajor * semiMajor - semiMinor * semiMinor);
		var center = focus.add(rightDir.mul(distanceToCenter).neg());

		return new Ellipse(center, rightDir.mul(semiMajor), upDir.mul(semiMinor));
	}

	public static double eccentricity(double semiMajor, double semiMinor) {
		return Math.sqrt(1 - (semiMinor * semiMinor) / (semiMajor * semiMajor));
	}

	public Vec3 pointFromAngle(double angle) {
		return this.center.add(this.right.mul(Math.cos(angle))).add(this.up.mul(Math.sin(angle)));
	}

	public Vec3 pointFromTrueAnomaly(double angle) {
		// https://en.wikipedia.org/wiki/True_anomaly#Radius_from_true_anomaly
		var e = Math.sqrt(1 - up.lengthSquared() / right.lengthSquared());
		var r = right.lengthSquared() * (1 - e * e) / (1 + e * Math.cos(angle));

		var rn = this.right.normalize();
		var rotation = Quat.axisAngle(rn.cross(this.up).normalize(), angle);

		return rotation.transform(rn).mul(r);
	}

}
