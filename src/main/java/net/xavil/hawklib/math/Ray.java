package net.xavil.hawklib.math;

import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.VecMath;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public record Ray(Vec3 origin, Vec3 dir) {

	public Vec3 stepBy(double t) {
		return this.origin.add(this.dir.mul(t));
	}

	public boolean intersectsSphere(Vec3Access center, double radius) {
		return Intersections.raySphere(this.origin, this.dir, center, radius);
	}

	public boolean intersectAabb(Vec3Access aabbMin, Vec3Access aabbMax) {
		return Intersections.rayAabb(this.origin, this.dir, aabbMin, aabbMax);
	}

	public static Ray transform(Ray ray, Mat4Access mat) {
		return new Ray(
				VecMath.transformPerspective(mat, ray.origin, 1),
				VecMath.transformPerspective(mat, ray.dir, 0).normalize());
	}

}
