package net.xavil.universal.common.universe;

import java.util.function.DoubleUnaryOperator;

import net.minecraft.world.phys.Vec3;

public interface DensityField3 {

	/**
	 * @param position The point to sample the density field at.
	 * @return density in range of [0,), in Tm^-3 (amount per Terameter^3)
	 */
	double sampleDensity(Vec3 position);

	static DensityField3 add(DensityField3 a, DensityField3 b) {
		return pos -> a.sampleDensity(pos) + b.sampleDensity(pos);
	}

	static DensityField3 sub(DensityField3 a, DensityField3 b) {
		return pos -> Math.max(0, a.sampleDensity(pos) - b.sampleDensity(pos));
	}

	static DensityField3 mul(DensityField3 a, DensityField3 b) {
		return pos -> a.sampleDensity(pos) * b.sampleDensity(pos);
	}

	static DensityField3 min(DensityField3 a, DensityField3 b) {
		return pos -> Math.min(a.sampleDensity(pos), b.sampleDensity(pos));
	}

	static DensityField3 max(DensityField3 a, DensityField3 b) {
		return pos -> Math.max(a.sampleDensity(pos), b.sampleDensity(pos));
	}

	static DensityField3 uniform(double density) {
		return pos -> density;
	}

	/**
	 * A sphere with a density of 1 in its center, and 0 at its radius and beyond.
	 * 
	 * @param radius The distance from the center of the sphere at which density
	 *               becomes 0.
	 * @return
	 */
	static DensityField3 sphereCloud(double radius) {
		return pos -> (double) Math.max(0, 1 - pos.length() / radius);
	}

	static DensityField3 simplexNoise(double w) {
		return pos -> SimplexNoise.noise(pos.x, pos.y, pos.z, w);
	}

	static DensityField3 sphereMask(double radius) {
		final var r2 = radius * radius;
		return pos -> pos.lengthSqr() > r2 ? 0 : 1;
	}

	static DensityField3 cylinderMask(double radius, double height) {
		final var r2 = radius * radius;
		return pos -> pos.x * pos.x + pos.z * pos.z > r2 || pos.y > height || pos.y < -height ? 0 : 1;
	}

	default DensityField3 translate(Vec3 newOrigin) {
		return pos -> this.sampleDensity(newOrigin.add(pos));
	}

	default DensityField3 scale(double scale) {
		return pos -> this.sampleDensity(pos.scale(scale));
	}

	default DensityField3 scale(double xScale, double yScale, double zScale) {
		return pos -> this.sampleDensity(new Vec3(xScale, yScale, zScale));
	}

	default DensityField3 scale(Vec3 scale) {
		return pos -> this.sampleDensity(pos.multiply(scale));
	}

	default DensityField3 rotate(Vec3 axis, double angle) {
		return pos -> this.sampleDensity(rotateVector(axis, angle, pos));
	}

	static Vec3 rotateVector(Vec3 axis, double angle, Vec3 v) {
		// https://en.wikipedia.org/wiki/Rodrigues%27_rotation_formula
		return v.scale(Math.cos(angle))
				.add(axis.cross(v).scale(Math.sin(angle)))
				.add(axis.scale(axis.dot(v) * (1 - Math.cos(angle))));
	}

	default DensityField3 map(DoubleUnaryOperator mapper) {
		return pos -> mapper.applyAsDouble(this.sampleDensity(pos));
	}

	default DensityField3 curveExp(double base) {
		return pos -> (Math.pow(base, this.sampleDensity(pos)) - 1) / (base - 1);
	}

	default DensityField3 curvePoly(double exponent) {
		return pos -> Math.pow(this.sampleDensity(pos), exponent);
	}

}
