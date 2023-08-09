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
		return Intersections.raySphere(this.origin, this.dir, center, radius);
	}

	public boolean intersectAABB(Vec3 aabbMin, Vec3 aabbMax) {
		return Intersections.rayAabb(this.origin, this.dir, aabbMin, aabbMax);
	}

}
