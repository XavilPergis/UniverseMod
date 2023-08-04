package net.xavil.ultraviolet.common.universe;

import java.util.function.DoubleBinaryOperator;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public interface DoubleField3 {

	/**
	 * @param position The point to sample the field at.
	 */
	default double sample(Vec3Access pos) {
		return sample(pos.x(), pos.y(), pos.z());
	}

	double sample(double x, double y, double z);

	default DoubleField3 optimize() {
		return this;
	}

	static DoubleField3 uniform(double value) {
		return new UniformField(value);
	}

	final class UniformField implements DoubleField3 {
		public final double value;

		public UniformField(double value) {
			this.value = value;
		}

		@Override
		public double sample(double x, double y, double z) {
			return this.value;
		}

		@Override
		public DoubleField3 add(DoubleField3 b) {
			if (b instanceof UniformField other)
				return new UniformField(this.value + other.value);
			return DoubleField3.super.add(b);
		}

		@Override
		public DoubleField3 sub(DoubleField3 b) {
			if (b instanceof UniformField other)
				return new UniformField(this.value - other.value);
			return DoubleField3.super.sub(b);
		}

		@Override
		public DoubleField3 mul(DoubleField3 b) {
			if (b instanceof UniformField other)
				return new UniformField(this.value * other.value);
			return DoubleField3.super.mul(b);
		}

		@Override
		public DoubleField3 withNumerator(DoubleField3 b) {
			if (b instanceof UniformField other)
				return new UniformField(other.value / this.value);
			return DoubleField3.super.withNumerator(b);
		}

		@Override
		public DoubleField3 withDenominator(DoubleField3 b) {
			if (b instanceof UniformField other)
				return new UniformField(this.value / other.value);
			return DoubleField3.super.withDenominator(b);
		}

		@Override
		public DoubleField3 mulPos(Vec3 s) {
			return this;
		}

	}

	static DoubleField3 simplexNoise(double seed) {
		return new SimplexNoiseField(1.0, 1.0, 0.0, seed);
	}

	final class SimplexNoiseField implements DoubleField3 {
		public final double scale;
		public final double amplitude;
		public final double offset;
		public final double seed;

		public SimplexNoiseField(double scale, double amplitude, double offset, double seed) {
			this.scale = scale;
			this.amplitude = amplitude;
			this.offset = offset;
			this.seed = seed;
		}

		@Override
		public double sample(double x, double y, double z) {
			return this.offset + this.amplitude * SimplexNoise.noise(
					this.scale * x, this.scale * y, this.scale * z, this.seed);
		}

		@Override
		public DoubleField3 add(DoubleField3 b) {
			if (b instanceof UniformField other)
				return new SimplexNoiseField(scale, amplitude, offset + other.value, seed);
			return DoubleField3.super.add(b);
		}

		@Override
		public DoubleField3 sub(DoubleField3 b) {
			if (b instanceof UniformField other)
				return new SimplexNoiseField(scale, amplitude, offset - other.value, seed);
			return DoubleField3.super.sub(b);
		}

		@Override
		public DoubleField3 mul(DoubleField3 b) {
			if (b instanceof UniformField other)
				return new SimplexNoiseField(scale, amplitude * other.value, offset * other.value, seed);
			return DoubleField3.super.mul(b);
		}

		@Override
		public DoubleField3 withDenominator(DoubleField3 b) {
			if (b instanceof UniformField other)
				return new SimplexNoiseField(scale, amplitude / other.value, offset / other.value, seed);
			return DoubleField3.super.withDenominator(b);
		}

		@Override
		public DoubleField3 mulPos(double s) {
			return new SimplexNoiseField(scale * s, amplitude, offset, seed);
		}
	}

	default DoubleField3 add(double value) {
		return add(uniform(value));
	}

	default DoubleField3 add(DoubleField3 b) {
		if (b instanceof UniformField ub && ub.value == 0.0)
			return this;
		return new Add(this, b);
	}

	final class Add implements DoubleField3 {
		public final DoubleField3 a, b;

		public Add(DoubleField3 a, DoubleField3 b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public double sample(double x, double y, double z) {
			return a.sample(x, y, z) + b.sample(x, y, z);
		}

		@Override
		public DoubleField3 optimize() {
			final var na = this.a.optimize();
			final var nb = this.b.optimize();

			// @formatter:off
			if (na instanceof UniformField ua && na instanceof UniformField ub) {
				return uniform(ua.value + ub.value);
			}

			// add(U0, U1) will already be folded into a new uniform, either by directly
			// calling UniformField::add, or via the previous calls to optimize, so we don't
			// need to consider those cases.

			// add(add(a, b), c)
			if (this.a instanceof Add other && !(this.b instanceof Add)) {
				final DoubleField3 a = other.a, b = other.b, c = this.b;
				if (a instanceof UniformField u0 && c instanceof UniformField u1)
					return new Add(b, uniform(u0.value + u1.value));
				if (b instanceof UniformField u0 && c instanceof UniformField u1)
					return new Add(a, uniform(u0.value + u1.value));
			}

			// add(sub(a, b), c)
			if (this.a instanceof Sub other && !(this.b instanceof Add)) {
				final DoubleField3 a = other.a, b = other.b, c = this.b;
				if (a instanceof UniformField u0 && c instanceof UniformField u1)
					return new Sub(uniform(u0.value + u1.value), b);
				if (b instanceof UniformField u0 && c instanceof UniformField u1)
					return new Add(a, uniform(u1.value - u0.value));
			}

			// add(a, add(b, c))
			if (this.b instanceof Add other && !(this.a instanceof Add)) {
				final DoubleField3 a = this.a, b = other.a, c = other.b;
				if (a instanceof UniformField u0 && b instanceof UniformField u1)
					return new Add(c, uniform(u0.value + u1.value));
				if (a instanceof UniformField u0 && c instanceof UniformField u1)
					return new Add(b, uniform(u0.value + u1.value));
			}

			// add(a, sub(b, c))
			if (this.b instanceof Sub other && !(this.a instanceof Add)) {
				final DoubleField3 a = this.a, b = other.a, c = other.b;
				if (a instanceof UniformField u0 && b instanceof UniformField u1)
					return new Sub(uniform(u0.value + u1.value), c);
				if (a instanceof UniformField u0 && c instanceof UniformField u1)
					return new Add(b, uniform(u0.value - u1.value));
			}

			// add(add(a, b), add(c, d))
			if (this.b instanceof Add a && this.b instanceof Add b) {
				if (a.a instanceof UniformField u0 && b.a instanceof UniformField u1)
					return a.b.add(b.b).add(u0.add(u1));
				if (a.a instanceof UniformField u0 && b.b instanceof UniformField u1)
					return a.b.add(b.a).add(u0.add(u1));
				if (a.b instanceof UniformField u0 && b.a instanceof UniformField u1)
					return a.a.add(b.b).add(u0.add(u1));
				if (a.b instanceof UniformField u0 && b.b instanceof UniformField u1)
					return a.a.add(b.a).add(u0.add(u1));
			}

			// add(sub(u0, b), add(u1, d)) => add(sub( d,  b), add(u0, u1))
			// add(sub(u0, b), add(c, u1)) => add(sub( c,  b), add(u0, u1))
			// add(sub(a, u0), add(u1, d)) => add(sub(u1, u0), add( a,  d))
			// add(sub(a, u0), add(c, u1)) => add(sub(u1, u0), add( a,  c))
			if (this.b instanceof Sub a && this.b instanceof Add b) {
				if (a.a instanceof UniformField u0 && b.a instanceof UniformField u1)
					return b.b.sub(a.b).add(u0.add(u1));
				if (a.a instanceof UniformField u0 && b.b instanceof UniformField u1)
					return b.a.sub(a.b).add(u0.add(u1));
				if (a.b instanceof UniformField u0 && b.a instanceof UniformField u1)
					return u1.sub(u0).add(a.a.add(b.b));
				if (a.b instanceof UniformField u0 && b.b instanceof UniformField u1)
					return u1.sub(u0).add(a.a.add(b.a));
			}

			// add(add(u0, b), sub(u1, d)) => add(sub( b,  d), add(u0, u1))
			// add(add(u0, b), sub(c, u1)) => add(sub(u0, u1), add( b,  c))
			// add(add(a, u0), sub(u1, d)) => add(sub( a,  d), add(u0, u1))
			// add(add(a, u0), sub(c, u1)) => add(sub(u0, u1), add( a,  c))
			if (this.b instanceof Add a && this.b instanceof Sub b) {
				if (a.a instanceof UniformField u0 && b.a instanceof UniformField u1)
					return a.b.sub(b.b).add(u0.add(u1));
				if (a.a instanceof UniformField u0 && b.b instanceof UniformField u1)
					return u0.sub(u1).add(a.b.add(b.a));
				if (a.b instanceof UniformField u0 && b.a instanceof UniformField u1)
					return a.a.sub(b.b).add(u0.add(u1));
				if (a.b instanceof UniformField u0 && b.b instanceof UniformField u1)
					return u0.sub(u1).add(a.a.add(b.a));
			}

			// add(sub(u0, b), sub(u1, d)) => sub(add(u0, u1), add( b,  d))
			// add(sub(u0, b), sub(c, u1)) => add(sub(u0, u1), sub( c,  b))
			// add(sub(a, u0), sub(u1, d)) => add(sub( a,  d), sub(u1, u0))
			// add(sub(a, u0), sub(c, u1)) => sub(add( a,  c), add(u0, u1))
			if (this.b instanceof Sub a && this.b instanceof Sub b) {
				if (a.a instanceof UniformField u0 && b.a instanceof UniformField u1)
					return u0.add(u1).sub(a.b.add(b.b));
				if (a.a instanceof UniformField u0 && b.b instanceof UniformField u1)
					return u0.sub(u1).add(b.a.sub(a.b));
				if (a.b instanceof UniformField u0 && b.a instanceof UniformField u1)
					return a.a.sub(b.b).add(u1.add(u0));
				if (a.b instanceof UniformField u0 && b.b instanceof UniformField u1)
					return a.a.add(b.a).sub(u0.add(u1));
			}

			// add(a, sub(0, b)) | add(sub(0, b), a) => sub(a, b)
			if (this.b instanceof Sub s && s.a instanceof UniformField u && u.value == 0)
				return this.a.sub(s.b);
			if (this.a instanceof Sub s && s.a instanceof UniformField u && u.value == 0)
				return this.b.sub(s.b);
			// @formatter:on

			return new Add(na, nb);
		}
	}

	default DoubleField3 sub(double value) {
		return sub(uniform(value));
	}

	default DoubleField3 sub(DoubleField3 b) {
		if (b instanceof UniformField ub && ub.value == 0.0)
			return this;
		return new Sub(this, b);
	}

	final class Sub implements DoubleField3 {
		public final DoubleField3 a, b;

		public Sub(DoubleField3 a, DoubleField3 b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public double sample(double x, double y, double z) {
			return a.sample(x, y, z) - b.sample(x, y, z);
		}

		@Override
		public DoubleField3 optimize() {
			// turn (a - b) into (a + (0 - b)) because Add's optimize can deal with this
			// case. This prevents code duplication here.
			return this.a.add(uniform(0).sub(this.b)).optimize();
		}
	}

	default DoubleField3 mul(double value) {
		return mul(uniform(value));
	}

	default DoubleField3 mul(DoubleField3 b) {
		if (b instanceof UniformField ub && ub.value == 1.0)
			return this;
		return new Mul(this, b);
	}

	final class Mul implements DoubleField3 {
		public final DoubleField3 a, b;

		public Mul(DoubleField3 a, DoubleField3 b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public double sample(double x, double y, double z) {
			return a.sample(x, y, z) * b.sample(x, y, z);
		}

		@Override
		public DoubleField3 optimize() {
			final var na = this.a.optimize();
			final var nb = this.b.optimize();
			if (na instanceof UniformField ua && na instanceof UniformField ub) {
				return uniform(ua.value * ub.value);
			}
			// TODO: n^a * n^b = n^(a + b)
			return new Mul(na, nb);
		}
	}

	default DoubleField3 withDenominator(double value) {
		return withDenominator(uniform(value));
	}

	default DoubleField3 withDenominator(DoubleField3 b) {
		if (b instanceof UniformField ub && ub.value == 1.0)
			return this;
		return new Div(this, b);
	}

	default DoubleField3 withNumerator(double value) {
		return withNumerator(uniform(value));
	}

	default DoubleField3 withNumerator(DoubleField3 b) {
		if (b instanceof UniformField ub && ub.value == 1.0)
			return this;
		return new Div(b, this);
	}

	final class Div implements DoubleField3 {
		public final DoubleField3 a, b;

		public Div(DoubleField3 a, DoubleField3 b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public double sample(double x, double y, double z) {
			return a.sample(x, y, z) / b.sample(x, y, z);
		}

		@Override
		public DoubleField3 optimize() {
			final var na = this.a.optimize();
			final var nb = this.b.optimize();
			if (na instanceof UniformField ua && na instanceof UniformField ub) {
				return uniform(ua.value / ub.value);
			}
			return new Div(na, nb);
		}
	}

	default DoubleField3 withBase(double b) {
		return withBase(uniform(b));
	}

	default DoubleField3 withBase(DoubleField3 b) {
		if (b instanceof UniformField ub && ub.value == 0.0)
			return uniform(0);
		if (b instanceof UniformField ub && ub.value == 1.0)
			return uniform(1);
		return new Pow(b, this);
	}

	default DoubleField3 withExponent(double b) {
		return withExponent(uniform(b));
	}

	default DoubleField3 withExponent(DoubleField3 b) {
		if (b instanceof UniformField ub && ub.value == 0.0)
			return uniform(1.0);
		if (b instanceof UniformField ub && ub.value == 1.0)
			return this;
		return new Pow(this, b);
	}

	final class Pow implements DoubleField3 {
		public final DoubleField3 base, exponent;

		public Pow(DoubleField3 base, DoubleField3 exponent) {
			this.base = base;
			this.exponent = exponent;
		}

		@Override
		public double sample(double x, double y, double z) {
			return Math.pow(this.base.sample(x, y, z), this.exponent.sample(x, y, z));
		}

		@Override
		public DoubleField3 optimize() {
			final var na = this.base.optimize();
			final var nb = this.exponent.optimize();
			if (na instanceof UniformField ua && na instanceof UniformField ub) {
				return uniform(ua.value * ub.value);
			}
			return new Pow(na, nb);
		}
	}

	default DoubleField3 mulPos(double value) {
		return mulPos(Vec3.broadcast(value));
	}

	default DoubleField3 mulPos(Vec3 s) {
		return new MulPos(this, s);
	}

	final class MulPos implements DoubleField3 {
		public final DoubleField3 a;
		public final Vec3 s;

		public MulPos(DoubleField3 a, Vec3 s) {
			this.a = a;
			this.s = s;
		}

		@Override
		public double sample(double x, double y, double z) {
			return a.sample(this.s.x * x, this.s.y * y, this.s.z * z);
		}

		@Override
		public DoubleField3 mulPos(Vec3 s) {
			return new MulPos(this.a, this.s.mul(s));
		}
	}

	default DoubleField3 min(double value) {
		return min(uniform(value));
	}

	default DoubleField3 min(DoubleField3 value) {
		return clamp(uniform(Double.NEGATIVE_INFINITY), value);
	}

	default DoubleField3 max(double value) {
		return max(uniform(value));
	}

	default DoubleField3 max(DoubleField3 value) {
		return clamp(value, uniform(Double.POSITIVE_INFINITY));
	}

	default DoubleField3 clamp() {
		return clamp(0, 1);
	}

	default DoubleField3 clamp(double min, double max) {
		return clamp(uniform(min), uniform(max));
	}

	default DoubleField3 clamp(DoubleField3 min, DoubleField3 max) {
		return new Clamp(this, min, max);
	}

	static class Clamp implements DoubleField3 {
		public DoubleField3 a, min, max;

		public Clamp(DoubleField3 a, DoubleField3 min, DoubleField3 max) {
			this.a = a;
			this.min = min;
			this.max = max;
		}

		@Override
		public double sample(double x, double y, double z) {
			final var value = this.a.sample(x, y, z);
			final var min = this.min.sample(x, y, z);
			final var max = this.max.sample(x, y, z);
			return value > max ? max : value < min ? min : value;
		}

	}

	default DoubleField3 lerp(double min, double max) {
		return lerp(uniform(min), uniform(max));
	}

	default DoubleField3 lerp(DoubleField3 min, DoubleField3 max) {
		return new Lerp(this, min, max);
	}

	final class Lerp implements DoubleField3 {
		public final DoubleField3 t, a, b;

		public Lerp(DoubleField3 t, DoubleField3 a, DoubleField3 b) {
			this.t = t;
			this.a = a;
			this.b = b;
		}

		@Override
		public double sample(double x, double y, double z) {
			final var t = this.t.sample(x, y, z);
			final var a = this.a.sample(x, y, z);
			final var b = this.b.sample(x, y, z);
			return a + t * (b - a);
		}

		@Override
		public DoubleField3 optimize() {
			final var nt = this.t.optimize();
			final var na = this.a.optimize();
			final var nb = this.b.optimize();
			if (na instanceof UniformField ua && ua.value == 0.0)
				return new Mul(nt, nb);
			return new Lerp(nt, na, nb);
		}
	}

	default DoubleField3 neg() {
		return uniform(0).sub(this);
	}

	static DoubleField3 spokes(double spokeCount, double spokeCurveExponent) {
		return (x, y, z) -> {
			final var pos = new Vec3(x, y, z);
			final var angleFromCenter = Math.atan2(pos.x, pos.z);
			final var t = Math.abs(Math.cos(angleFromCenter * spokeCount));
			return Math.pow(t, spokeCurveExponent);
		};
	}

	default DoubleField3 spiralAboutY(double spiralFactor, double radius) {
		return (x, y, z) -> {
			final var len = Math.sqrt(x * x + z * z) / radius;
			final var angle = 2.0 * Math.PI * spiralFactor * Math.pow(1.0 - len, 2.0);

			final double c = Math.cos(angle), s = Math.sin(angle);
			final var nx = x * c + z * s;
			final var nz = z * c - x * s;

			return this.sample(nx, y, nz);
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
		return (x, y, z) -> {
			final var length = Math.sqrt(x * x + y * y + z * z);
			return (double) Math.max(0, 1 - length / radius);
		};
	}

	// static DoubleField3 lineSegment(Vec3Access start, Vec3Access end, double
	// radius) {
	// return (x, y, z) -> {
	// return 0;
	// };
	// }

	static DoubleField3 sdfPoint(Vec3Access P) {
		return (x, y, z) -> {
			final var dx = Math.pow(P.x() - x, 2.0);
			final var dy = Math.pow(P.y() - y, 2.0);
			final var dz = Math.pow(P.z() - z, 2.0);
			return Math.sqrt(dx + dy + dz);
		};
	}

	// static DoubleField3 sdfLineSegment(Vec3Access A, Vec3Access B) {
	// final var BsubA = new Vec3(B).sub(A);
	// final var BsubAlen2 = BsubA.lengthSquared();
	// return (x, y, z) -> {
	// final double PsubAx = x - A.x(), PsubAy = y - A.y(), PsubAz = z - A.z();
	// final var h = Mth.clamp((PsubAx * BsubA.x + PsubAy * BsubA.y + PsubAz *
	// BsubA.z) / BsubAlen2, 0, 1);
	// final var dx = PsubAx - BsubA.x * h;
	// final var dy = PsubAy - BsubA.y * h;
	// final var dz = PsubAz - BsubA.z * h;
	// return Math.sqrt(dx * dx + dy * dy + dz * dz);
	// };
	// }

	static DoubleField3 sdfLineSegment(final Vec3 a, final Vec3 b) {
		return (x, y, z) -> {
			final var v = new Vec3(x, y, z);
			final var ab = b.sub(a);
			final var av = v.sub(a);

			if (av.dot(ab) <= 0.0) // Point is lagging behind start of the segment, so perpendicular distance is
									// not viable.
				return av.length(); // Use distance to start of segment instead.

			final var bv = v.sub(b);

			if (bv.dot(ab) >= 0.0) // Point is advanced past the end of the segment, so perpendicular distance is
									// not viable.
				return bv.length(); // Use distance to end of the segment instead.

			return (ab.cross(av)).length() / ab.length(); // Perpendicular distance of point to segment.
		};
	}

	// static DoubleField3 sdfLineSegment(Vec3Access A, Vec3Access B) {
	// return (x, y, z) -> {
	// final var P = new Vec3(x, y, z);
	// final var BsubA = new Vec3(B).sub(A);
	// // final var PsubA = P.sub(A);
	// final var h = Mth.clamp(P.projectOnto(BsubA).length(), 0, 1);
	// final var Q = Vec3.lerp(h, A, B);
	// return P.distanceTo(Q);
	// // return PsubA;
	// };
	// }

	/**
	 * This method turns an SDF into a "cloud". The surface and inside of the SDF is
	 * mapped to 1, and {@code r} units from the surface is mapped to 0, linearly
	 * interpolating for intermediate values.
	 * 
	 * @param r The maximum distance from the surface of the SDF that his cloud will
	 *          be greater than 0.
	 * @return The remapped field.
	 */
	default DoubleField3 sdfCloud(double r) {
		return (x, y, z) -> {
			final var n = this.sample(x, y, z);
			return 1.0 - Mth.clamp(n / r, 0, 1);
		};
	}

	static DoubleField3 sphereMask(double radius) {
		final var r2 = radius * radius;
		return (x, y, z) -> x * x + y * y + z * z > r2 ? 0 : 1;
	}

	// larger values for falloff mean a less prominent falloff. a falloff of 1 means
	// that the falloff starts at the origin.
	static DoubleField3 verticalDisc(double radius, double height, double falloffRadius) {
		final var rFalloff = radius / falloffRadius;

		final double densityAtFalloffH = 0.01;
		final double k = height / Math.log(1.0 / densityAtFalloffH);

		return (x, y, z) -> {
			final var projectedLen = Math.sqrt(x * x + z * z);
			var hf = Math.exp(-Math.abs(y) / k);
			var rf = Mth.clamp((radius - projectedLen) / rFalloff, 0, 1);
			return rf * hf;
		};
	}

	static DoubleField3 cylinderMask(double radius, double height) {
		final var r2 = radius * radius;
		return (x, y, z) -> x * x + z * z > r2 || y > height || y < -height ? 0 : 1;
	}
}
