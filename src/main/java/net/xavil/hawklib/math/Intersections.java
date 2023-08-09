package net.xavil.hawklib.math;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public final class Intersections {

	private Intersections() {
	}

	// https://stackoverflow.com/questions/28343716/sphere-intersection-test-of-aabb
	public static boolean sphereAabb(Vec3Access sphereCenter, double sphereRadius,
			Vec3Access aabbMin, Vec3Access aabbMax) {
		double d = 0.0;
		if (sphereCenter.x() < aabbMin.x())
			d += Mth.square(sphereCenter.x() - aabbMin.x());
		else if (sphereCenter.x() > aabbMax.x())
			d += Mth.square(sphereCenter.x() - aabbMax.x());
		if (sphereCenter.y() < aabbMin.y())
			d += Mth.square(sphereCenter.y() - aabbMin.y());
		else if (sphereCenter.y() > aabbMax.y())
			d += Mth.square(sphereCenter.y() - aabbMax.y());
		if (sphereCenter.z() < aabbMin.z())
			d += Mth.square(sphereCenter.z() - aabbMin.z());
		else if (sphereCenter.z() > aabbMax.z())
			d += Mth.square(sphereCenter.z() - aabbMax.z());
		return d <= sphereRadius * sphereRadius;
	}

	public static boolean raySphere(Vec3Access origin, Vec3Access dir,
			Vec3Access center, double radius) {
		double px = center.x(), py = center.y(), pz = center.z();
		px -= origin.x();
		py -= origin.y();
		pz -= origin.z();
		if (px * dir.x() + py * dir.y() + pz * dir.z() < 0.0)
			return false;
		double bx = dir.x(), by = dir.y(), bz = dir.z();
		final double bl = Math.sqrt(bx * bx + by * by + bz * bz);
		bx /= bl;
		by /= bl;
		bz /= bl;
		final var d = px * bx + py * by + pz * bz;
		bx *= d;
		by *= d;
		bz *= d;
		final var p2 = px * px + py * py + pz * pz;
		final var a2 = bx * bx + by * by + bz * bz;
		return p2 - a2 <= radius * radius;
	}

	public static boolean rayAabb(Vec3Access origin, Vec3Access dir,
			Vec3Access aabbMin, Vec3Access aabbMax) {
		// r.dir is unit direction vector of ray
		final double dirfracx = 1.0 / dir.x();
		final double dirfracy = 1.0 / dir.y();
		final double dirfracz = 1.0 / dir.z();
		// lb is the corner of AABB with minimal coordinates - left bottom, rt is
		// maximal corner
		// r.org is origin of ray
		final double t1 = (aabbMin.x() - origin.x()) * dirfracx;
		final double t2 = (aabbMax.x() - origin.x()) * dirfracx;
		final double t3 = (aabbMin.y() - origin.y()) * dirfracy;
		final double t4 = (aabbMax.y() - origin.y()) * dirfracy;
		final double t5 = (aabbMin.z() - origin.z()) * dirfracz;
		final double t6 = (aabbMax.z() - origin.z()) * dirfracz;

		final double tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
		final double tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

		// if tmax < 0, ray (line) is intersecting AABB, but the whole AABB is behind us
		// if tmin > tmax, ray doesn't intersect AABB
		if (tmax < 0 || tmin > tmax) {
			return false;
		}

		return true;
	}

}
