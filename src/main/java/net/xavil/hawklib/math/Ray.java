package net.xavil.hawklib.math;

import com.mojang.math.Matrix4f;

import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public record Ray(Vec3 origin, Vec3 dir) {

	public Vec3 stepBy(double t) {
		return this.origin.add(this.dir.mul(t));
	}

	public Ray transformBy(Matrix4f matrix) {
		final var newOrigin = this.origin.transformBy(matrix, 1);
		final var newDir = this.dir.transformBy(matrix, 1).normalize();
		return new Ray(newOrigin, newDir);
	}

	public boolean intersectsSphere(Vec3Access center, double radius) {
		double px = center.x(), py = center.y(), pz = center.z();
		px -= this.origin.x;
		py -= this.origin.y;
		pz -= this.origin.z;
		if (px * this.dir.x + py * this.dir.y + pz * this.dir.z < 0.0)
			return false;
		double bx = this.dir.x, by = this.dir.y, bz = this.dir.z;
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

	public boolean intersectAABB(Vec3 boxMin, Vec3 boxMax) {

		// r.dir is unit direction vector of ray
		final double dirfracx = 1.0 / this.dir.x;
		final double dirfracy = 1.0 / this.dir.y;
		final double dirfracz = 1.0 / this.dir.z;
		// lb is the corner of AABB with minimal coordinates - left bottom, rt is
		// maximal corner
		// r.org is origin of ray
		final double t1 = (boxMin.x - this.origin.x) * dirfracx;
		final double t2 = (boxMax.x - this.origin.x) * dirfracx;
		final double t3 = (boxMin.y - this.origin.y) * dirfracy;
		final double t4 = (boxMax.y - this.origin.y) * dirfracy;
		final double t5 = (boxMin.z - this.origin.z) * dirfracz;
		final double t6 = (boxMax.z - this.origin.z) * dirfracz;

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
