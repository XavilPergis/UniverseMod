package net.xavil.universal.common.universe;

import java.util.Random;
import java.util.function.DoubleUnaryOperator;

import net.minecraft.util.Mth;
import net.xavil.util.FastHasher;
import net.xavil.util.math.Vec3;

public interface DoubleField3 {

	/**
	 * @param position The point to sample the density field at.
	 * @return density in range of [0,), in Tm^-3 (amount per Terameter^3)
	 */
	double sampleDensity(Vec3 position);

	default DoubleField3 add(DoubleField3 b) {
		return pos -> this.sampleDensity(pos) + b.sampleDensity(pos);
	}

	default DoubleField3 sub(DoubleField3 b) {
		return pos -> Math.max(0, this.sampleDensity(pos) - b.sampleDensity(pos));
	}

	default DoubleField3 mul(DoubleField3 b) {
		return pos -> this.sampleDensity(pos) * b.sampleDensity(pos);
	}

	default DoubleField3 add(double uniform) {
		return pos -> this.sampleDensity(pos) + uniform;
	}

	default DoubleField3 sub(double uniform) {
		return pos -> Math.max(0, this.sampleDensity(pos) - uniform);
	}

	default DoubleField3 mul(double uniform) {
		return pos -> this.sampleDensity(pos) * uniform;
	}

	static DoubleField3 min(DoubleField3 a, DoubleField3 b) {
		return pos -> Math.min(a.sampleDensity(pos), b.sampleDensity(pos));
	}

	static DoubleField3 max(DoubleField3 a, DoubleField3 b) {
		return pos -> Math.max(a.sampleDensity(pos), b.sampleDensity(pos));
	}

	default DoubleField3 lerp(double min, double max) {
		return pos -> Mth.lerp(this.sampleDensity(pos), min, max);
	}

	static DoubleField3 uniform(double density) {
		return pos -> density;
	}

	static DoubleField3 random() {
		return pos -> new Random(FastHasher.hash(pos)).nextDouble();
	}

	static DoubleField3 spokes(double spokeCount, double spokeCurveExponent) {
		return pos -> {
			var angleFromCenter = Math.atan2(pos.x, pos.z);
			return Math.pow(Math.abs(Math.cos(angleFromCenter * spokeCount)), spokeCurveExponent);
		};
	}

	default DoubleField3 spiralAboutY(double twistFactor) {
		final var f = 2 * Math.PI * twistFactor;
		return pos -> {
			final var projectedLen = pos.x * pos.x + pos.z * pos.z;
			final var twistedPos = pos.rotateY(f * projectedLen);
			return this.sampleDensity(twistedPos);
		};
	}

	/**
	 * A sphere with a density of 1 in its center, and 0 at its radius and beyond.
	 * 
	 * @param radius The distance from the center of the sphere at which density
	 *               becomes 0.
	 * @return
	 */
	static DoubleField3 sphereCloud(double radius) {
		return pos -> (double) Math.max(0, 1 - pos.length() / radius);
	}

	static DoubleField3 simplexNoise(double w) {
		return pos -> SimplexNoise.noise(pos.x, pos.y, pos.z, w);
	}

	static DoubleField3 sphereMask(double radius) {
		final var r2 = radius * radius;
		return pos -> pos.lengthSquared() > r2 ? 0 : 1;
	}

	// larger values for falloff mean a less prominent falloff. a falloff of 1 means
	// that the falloff starts at the origin.
	static DoubleField3 verticalDisc(double radius, double height,
			double falloffRadius) {
		final var rFalloff = radius / falloffRadius;

		final double densityAtFalloffH = 0.01;
		final double k = height / Math.log(1.0 / densityAtFalloffH);

		return pos -> {
			final var projectedLen = Math.sqrt(pos.x * pos.x + pos.z * pos.z);
			var hf = Math.exp(-Math.abs(pos.y) / k);
			var rf = Mth.clamp((radius - projectedLen) / rFalloff, 0, 1);
			return rf * hf;
		};
	}

	static DoubleField3 cylinderMask(double radius, double height) {
		final var r2 = radius * radius;
		return pos -> pos.x * pos.x + pos.z * pos.z > r2 || pos.y > height || pos.y < -height ? 0 : 1;
	}

	default DoubleField3 translate(Vec3 newOrigin) {
		return pos -> this.sampleDensity(newOrigin.add(pos));
	}

	default DoubleField3 scale(double scale) {
		return pos -> this.sampleDensity(pos.mul(scale));
	}

	default DoubleField3 scale(double xScale, double yScale, double zScale) {
		return pos -> this.sampleDensity(pos.mul(Vec3.from(xScale, yScale, zScale)));
	}

	default DoubleField3 scale(Vec3 scale) {
		return pos -> this.sampleDensity(pos.mul(scale));
	}

	default DoubleField3 rotate(Vec3 axis, double angle) {
		return pos -> this.sampleDensity(rotateVector(axis, angle, pos));
	}

	static Vec3 rotateVector(Vec3 axis, double angle, Vec3 v) {
		// https://en.wikipedia.org/wiki/Rodrigues%27_rotation_formula
		return v.mul(Math.cos(angle))
				.add(axis.cross(v).mul(Math.sin(angle)))
				.add(axis.mul(axis.dot(v) * (1 - Math.cos(angle))));
	}

	default DoubleField3 map(DoubleUnaryOperator mapper) {
		return pos -> mapper.applyAsDouble(this.sampleDensity(pos));
	}

	default DoubleField3 curveExp(double base) {
		return pos -> (Math.pow(base, this.sampleDensity(pos)) - 1) / (base - 1);
	}

	default DoubleField3 curvePoly(double exponent) {
		return pos -> Math.pow(this.sampleDensity(pos), exponent);
	}

}
