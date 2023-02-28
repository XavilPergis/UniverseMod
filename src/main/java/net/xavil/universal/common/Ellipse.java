package net.xavil.universal.common;

import net.xavil.universal.common.universe.Vec3;
import net.xavil.universal.common.universe.system.OrbitalPlane;
import net.xavil.universal.common.universe.system.OrbitalShape;

public record Ellipse(Vec3 center, Vec3 right, Vec3 up) {

	public static Ellipse fromOrbit(Vec3 focus, OrbitalPlane plane, OrbitalShape shape) {
		var rightDir = plane.rotationFromReference().transform(Vec3.XP);
		var upDir = plane.rotationFromReference().transform(Vec3.ZP);

		var semiMajor = shape.semimajorAxisTm();
		var semiMinor = shape.semiminorAxisTm();

		var distanceToCenter = Math.sqrt(semiMajor * semiMajor - semiMinor * semiMinor);
		var center = focus.add(rightDir.mul(distanceToCenter).neg());

		return new Ellipse(center, rightDir.mul(semiMajor), upDir.mul(semiMinor));
	}

	public Vec3 pointFromAngle(double angle) {
		return this.center.add(this.right.mul(Math.cos(angle))).add(this.up.mul(Math.sin(angle)));
	}

}
