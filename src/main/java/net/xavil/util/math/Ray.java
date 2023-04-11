package net.xavil.util.math;

import com.mojang.math.Matrix4f;

public record Ray(Vec3 origin, Vec3 dir) {

	public Vec3 stepBy(double t) {
		return this.origin.add(this.dir.mul(t));
	}

	public Ray transformBy(Matrix4f matrix) {
		final var newOrigin = this.origin.transformBy(matrix, 1);
		final var newDir = this.dir.transformBy(matrix, 0);
		return new Ray(newOrigin, newDir);
	}

	public boolean intersectsSphere(Vec3 center, double radius) {
		final var L = center.sub(this.origin);
		final var tc = L.dot(this.dir);
		if (tc < 0.0)
			return false;
		final var d2 = (tc * tc) - L.lengthSquared();
		return d2 <= radius * radius;
	}

	public boolean intersectAABB(Vec3 boxMin, Vec3 boxMax) {
		final var invDir = this.dir.recip();
		final var t1 = (boxMin.x - this.origin.x) * invDir.x;
		final var t2 = (boxMax.x - this.origin.x) * invDir.x;
		final var t3 = (boxMin.y - this.origin.y) * invDir.y;
		final var t4 = (boxMax.y - this.origin.y) * invDir.y;
		final var t5 = (boxMin.z - this.origin.z) * invDir.z;
		final var t6 = (boxMax.z - this.origin.z) * invDir.z;

		final var tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
		final var tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

		return tmax >= 0 && tmin <= tmax;
	}

}
