package net.xavil.ultraviolet.common.universe;

import net.minecraft.util.Mth;
import net.xavil.hawklib.SimplexNoise;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.interfaces.Vec2Access;

public interface ScalarField2 {

	/**
	 * @param position The point to sample the field at.
	 */
	default double sample(Vec2Access pos) {
		return sample(pos.x(), pos.y());
	}

	double sample(double x, double y);

	default ScalarField2 optimize() {
		return this;
	}

	static ScalarField2 uniform(double value) {
		return new UniformField(value);
	}

	final class UniformField implements ScalarField2 {
		public final double value;

		public UniformField(double value) {
			this.value = value;
		}

		@Override
		public double sample(double x, double y) {
			return this.value;
		}

		@Override
		public ScalarField2 add(ScalarField2 b) {
			if (b instanceof UniformField other)
				return new UniformField(this.value + other.value);
			return ScalarField2.super.add(b);
		}

		@Override
		public ScalarField2 sub(ScalarField2 b) {
			if (b instanceof UniformField other)
				return new UniformField(this.value - other.value);
			return ScalarField2.super.sub(b);
		}

		@Override
		public ScalarField2 mul(ScalarField2 b) {
			if (b instanceof UniformField other)
				return new UniformField(this.value * other.value);
			return ScalarField2.super.mul(b);
		}

		@Override
		public ScalarField2 withNumerator(ScalarField2 b) {
			if (b instanceof UniformField other)
				return new UniformField(other.value / this.value);
			return ScalarField2.super.withNumerator(b);
		}

		@Override
		public ScalarField2 withDenominator(ScalarField2 b) {
			if (b instanceof UniformField other)
				return new UniformField(this.value / other.value);
			return ScalarField2.super.withDenominator(b);
		}

		@Override
		public ScalarField2 mulPos(Vec2 s) {
			return this;
		}

	}

	static ScalarField2 simplexNoise(double seed) {
		return new SimplexNoiseField(1.0, 1.0, 0.0, seed);
	}

	static ScalarField2 fractalSimplexNoise(double seed, int layers,
			double scaleBase, double scaleMultiplier,
			double amplitudeBase, double amplitudeMultiplier) {
		return new FractalSimplexNoiseField(seed, layers,
				scaleBase, scaleMultiplier, amplitudeBase, amplitudeMultiplier);
	}

	final class SimplexNoiseField implements ScalarField2 {
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
		public double sample(double x, double y) {
			return this.offset + this.amplitude * SimplexNoise.noise(
					this.scale * x, this.scale * y, this.seed);
		}

		@Override
		public ScalarField2 add(ScalarField2 b) {
			if (b instanceof UniformField other)
				return new SimplexNoiseField(scale, amplitude, offset + other.value, seed);
			return ScalarField2.super.add(b);
		}

		@Override
		public ScalarField2 sub(ScalarField2 b) {
			if (b instanceof UniformField other)
				return new SimplexNoiseField(scale, amplitude, offset - other.value, seed);
			return ScalarField2.super.sub(b);
		}

		@Override
		public ScalarField2 mul(ScalarField2 b) {
			if (b instanceof UniformField other)
				return new SimplexNoiseField(scale, amplitude * other.value, offset * other.value, seed);
			return ScalarField2.super.mul(b);
		}

		@Override
		public ScalarField2 withDenominator(ScalarField2 b) {
			if (b instanceof UniformField other)
				return new SimplexNoiseField(scale, amplitude / other.value, offset / other.value, seed);
			return ScalarField2.super.withDenominator(b);
		}

		@Override
		public ScalarField2 mulPos(double s) {
			return new SimplexNoiseField(scale * s, amplitude, offset, seed);
		}
	}

	final class FractalSimplexNoiseField implements ScalarField2 {
		public final int layers;
		public final double maxAmplitude;
		public final double scaleBase, scaleMultiplier;
		public final double amplitudeBase, amplitudeMultiplier;
		public final double seed;

		public FractalSimplexNoiseField(double seed, int layers,
				double scaleBase, double scaleMultiplier,
				double amplitudeBase, double amplitudeMultiplier) {
			this.layers = layers;
			this.scaleBase = scaleBase;
			this.scaleMultiplier = scaleMultiplier;
			this.amplitudeBase = amplitudeBase;
			this.amplitudeMultiplier = amplitudeMultiplier;
			this.seed = seed;

			double curAmp = this.amplitudeBase, maxAmp = 0;
			for (int i = 0; i < this.layers; ++i) {
				maxAmp += curAmp;
				curAmp *= this.amplitudeMultiplier;
			}

			this.maxAmplitude = maxAmp;
		}

		@Override
		public double sample(double x, double y) {
			double res = 0.0;
			double cA = this.amplitudeBase, cS = this.scaleBase;
			for (int i = 0; i < this.layers; ++i) {
				res += cA * SimplexNoise.noise(cS * x, cS * y, this.seed + i);
				cA *= this.amplitudeMultiplier;
				cS *= this.scaleMultiplier;
			}
			return res / this.maxAmplitude;
		}
	}

	default ScalarField2 add(double value) {
		return add(uniform(value));
	}

	default ScalarField2 add(ScalarField2 b) {
		if (b instanceof UniformField ub && ub.value == 0.0)
			return this;
		return new Add(this, b);
	}

	final class Add implements ScalarField2 {
		public final ScalarField2 a, b;

		public Add(ScalarField2 a, ScalarField2 b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public double sample(double x, double y) {
			return a.sample(x, y) + b.sample(x, y);
		}

		@Override
		public ScalarField2 optimize() {
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
				final ScalarField2 a = other.a, b = other.b, c = this.b;
				if (a instanceof UniformField u0 && c instanceof UniformField u1)
					return new Add(b, uniform(u0.value + u1.value));
				if (b instanceof UniformField u0 && c instanceof UniformField u1)
					return new Add(a, uniform(u0.value + u1.value));
			}

			// add(sub(a, b), c)
			if (this.a instanceof Sub other && !(this.b instanceof Add)) {
				final ScalarField2 a = other.a, b = other.b, c = this.b;
				if (a instanceof UniformField u0 && c instanceof UniformField u1)
					return new Sub(uniform(u0.value + u1.value), b);
				if (b instanceof UniformField u0 && c instanceof UniformField u1)
					return new Add(a, uniform(u1.value - u0.value));
			}

			// add(a, add(b, c))
			if (this.b instanceof Add other && !(this.a instanceof Add)) {
				final ScalarField2 a = this.a, b = other.a, c = other.b;
				if (a instanceof UniformField u0 && b instanceof UniformField u1)
					return new Add(c, uniform(u0.value + u1.value));
				if (a instanceof UniformField u0 && c instanceof UniformField u1)
					return new Add(b, uniform(u0.value + u1.value));
			}

			// add(a, sub(b, c))
			if (this.b instanceof Sub other && !(this.a instanceof Add)) {
				final ScalarField2 a = this.a, b = other.a, c = other.b;
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

	default ScalarField2 sub(double value) {
		return sub(uniform(value));
	}

	default ScalarField2 sub(ScalarField2 b) {
		if (b instanceof UniformField ub && ub.value == 0.0)
			return this;
		return new Sub(this, b);
	}

	final class Sub implements ScalarField2 {
		public final ScalarField2 a, b;

		public Sub(ScalarField2 a, ScalarField2 b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public double sample(double x, double y) {
			return a.sample(x, y) - b.sample(x, y);
		}

		@Override
		public ScalarField2 optimize() {
			// turn (a - b) into (a + (0 - b)) because Add's optimize can deal with this
			// case. This prevents code duplication here.
			return this.a.add(uniform(0).sub(this.b)).optimize();
		}
	}

	default ScalarField2 mul(double value) {
		return mul(uniform(value));
	}

	default ScalarField2 mul(ScalarField2 b) {
		if (b instanceof UniformField ub && ub.value == 1.0)
			return this;
		return new Mul(this, b);
	}

	final class Mul implements ScalarField2 {
		public final ScalarField2 a, b;

		public Mul(ScalarField2 a, ScalarField2 b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public double sample(double x, double y) {
			return a.sample(x, y) * b.sample(x, y);
		}

		@Override
		public ScalarField2 optimize() {
			final var na = this.a.optimize();
			final var nb = this.b.optimize();
			if (na instanceof UniformField ua && na instanceof UniformField ub) {
				return uniform(ua.value * ub.value);
			}
			// TODO: n^a * n^b = n^(a + b)
			return new Mul(na, nb);
		}
	}

	default ScalarField2 withDenominator(double value) {
		return withDenominator(uniform(value));
	}

	default ScalarField2 withDenominator(ScalarField2 b) {
		if (b instanceof UniformField ub && ub.value == 1.0)
			return this;
		return new Div(this, b);
	}

	default ScalarField2 withNumerator(double value) {
		return withNumerator(uniform(value));
	}

	default ScalarField2 withNumerator(ScalarField2 b) {
		if (b instanceof UniformField ub && ub.value == 1.0)
			return this;
		return new Div(b, this);
	}

	final class Div implements ScalarField2 {
		public final ScalarField2 a, b;

		public Div(ScalarField2 a, ScalarField2 b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public double sample(double x, double y) {
			return a.sample(x, y) / b.sample(x, y);
		}

		@Override
		public ScalarField2 optimize() {
			final var na = this.a.optimize();
			final var nb = this.b.optimize();
			if (na instanceof UniformField ua && na instanceof UniformField ub) {
				return uniform(ua.value / ub.value);
			}
			return new Div(na, nb);
		}
	}

	default ScalarField2 withBase(double b) {
		return withBase(uniform(b));
	}

	default ScalarField2 withBase(ScalarField2 b) {
		if (b instanceof UniformField ub && ub.value == 0.0)
			return uniform(0);
		if (b instanceof UniformField ub && ub.value == 1.0)
			return uniform(1);
		return new Pow(b, this);
	}

	default ScalarField2 withExponent(double b) {
		return withExponent(uniform(b));
	}

	default ScalarField2 withExponent(ScalarField2 b) {
		if (b instanceof UniformField ub && ub.value == 0.0)
			return uniform(1.0);
		if (b instanceof UniformField ub && ub.value == 1.0)
			return this;
		return new Pow(this, b);
	}

	final class Pow implements ScalarField2 {
		public final ScalarField2 base, exponent;

		public Pow(ScalarField2 base, ScalarField2 exponent) {
			this.base = base;
			this.exponent = exponent;
		}

		@Override
		public double sample(double x, double y) {
			return Math.pow(this.base.sample(x, y), this.exponent.sample(x, y));
		}

		@Override
		public ScalarField2 optimize() {
			final var na = this.base.optimize();
			final var nb = this.exponent.optimize();
			if (na instanceof UniformField ua && na instanceof UniformField ub) {
				return uniform(ua.value * ub.value);
			}
			return new Pow(na, nb);
		}
	}

	default ScalarField2 mulPos(double value) {
		return mulPos(Vec2.broadcast(value));
	}

	default ScalarField2 mulPos(Vec2 s) {
		return new MulPos(this, s);
	}

	final class MulPos implements ScalarField2 {
		public final ScalarField2 a;
		public final Vec2 s;

		public MulPos(ScalarField2 a, Vec2 s) {
			this.a = a;
			this.s = s;
		}

		@Override
		public double sample(double x, double y) {
			return a.sample(this.s.x * x, this.s.y * y);
		}

		@Override
		public ScalarField2 mulPos(Vec2 s) {
			return new MulPos(this.a, this.s.mul(s));
		}
	}

	default ScalarField2 min(double value) {
		return min(uniform(value));
	}

	default ScalarField2 min(ScalarField2 value) {
		return clamp(uniform(Double.NEGATIVE_INFINITY), value);
	}

	default ScalarField2 max(double value) {
		return max(uniform(value));
	}

	default ScalarField2 max(ScalarField2 value) {
		return clamp(value, uniform(Double.POSITIVE_INFINITY));
	}

	default ScalarField2 clamp() {
		return clamp(0, 1);
	}

	default ScalarField2 clamp(double min, double max) {
		return clamp(uniform(min), uniform(max));
	}

	default ScalarField2 clamp(ScalarField2 min, ScalarField2 max) {
		return new Clamp(this, min, max);
	}

	static class Clamp implements ScalarField2 {
		public ScalarField2 a, min, max;

		public Clamp(ScalarField2 a, ScalarField2 min, ScalarField2 max) {
			this.a = a;
			this.min = min;
			this.max = max;
		}

		@Override
		public double sample(double x, double y) {
			final var value = this.a.sample(x, y);
			final var min = this.min.sample(x, y);
			final var max = this.max.sample(x, y);
			return value > max ? max : value < min ? min : value;
		}

	}

	default ScalarField2 lerp(double min, double max) {
		return lerp(uniform(min), uniform(max));
	}

	default ScalarField2 lerp(ScalarField2 min, ScalarField2 max) {
		return new Lerp(this, min, max);
	}

	final class Lerp implements ScalarField2 {
		public final ScalarField2 t, a, b;

		public Lerp(ScalarField2 t, ScalarField2 a, ScalarField2 b) {
			this.t = t;
			this.a = a;
			this.b = b;
		}

		@Override
		public double sample(double x, double y) {
			final var t = this.t.sample(x, y);
			final var a = this.a.sample(x, y);
			final var b = this.b.sample(x, y);
			return a + t * (b - a);
		}

		@Override
		public ScalarField2 optimize() {
			final var nt = this.t.optimize();
			final var na = this.a.optimize();
			final var nb = this.b.optimize();
			if (na instanceof UniformField ua && ua.value == 0.0)
				return new Mul(nt, nb);
			return new Lerp(nt, na, nb);
		}
	}

	default ScalarField2 neg() {
		return uniform(0).sub(this);
	}

	default ScalarField2 abs() {
		return (x, y) -> Math.abs(this.sample(x, y));
	}

	static double spokes(Vec2Access pos, double spokeCount, double spokeCurveExponent) {
		final var angleFromCenter = Math.atan2(pos.x(), pos.y());
		final var t = Math.abs(Math.cos(angleFromCenter * spokeCount));
		return Math.pow(t, spokeCurveExponent);
	}

	static Vec2 spiral(Vec2Access pos, double spiralFactor, double radius) {
		final var len = Math.sqrt(pos.x() * pos.x() + pos.y() * pos.y()) / radius;
		final var angle = 2.0 * Math.PI * spiralFactor * Math.pow(1.0 - len, 2.0);

		final double c = Math.cos(angle), s = Math.sin(angle);
		final var nx = pos.x() * c + pos.y() * s;
		final var ny = pos.y() * c - pos.x() * s;

		return new Vec2(nx, ny);
	}

	static ScalarField2 spokes(double spokeCount, double spokeCurveExponent) {
		return (x, y) -> {
			final var angleFromCenter = Math.atan2(x, y);
			final var t = Math.abs(Math.cos(angleFromCenter * spokeCount));
			return Math.pow(t, spokeCurveExponent);
		};
	}

	default ScalarField2 spiral(double spiralFactor, double radius) {
		return (x, y) -> {
			final var len = Math.sqrt(x * x + y * y) / radius;
			final var angle = 2.0 * Math.PI * spiralFactor * Math.pow(1.0 - len, 2.0);

			final double c = Math.cos(angle), s = Math.sin(angle);
			final var nx = x * c + y * s;
			final var ny = y * c - x * s;

			return this.sample(nx, ny);
		};
	}

	/**
	 * A sphere with a density of 1 in its center, and 0 at its radius and beyond.
	 *
	 * @param radius The distance from the center of the sphere at which density
	 *               becomes 0.
	 * @return
	 */
	static ScalarField2 circleCloud(double radius) {
		return (x, y) -> {
			final var length = Math.sqrt(x * x + y * y);
			return (double) Math.max(0, 1 - length / radius);
		};
	}

	static ScalarField2 sdfCircle(Vec2Access P, double r) {
		return (x, y) -> {
			final var dx = P.x() - x;
			final var dy = P.y() - y;
			return Math.sqrt(dx * dx + dy * dy) - r;
		};
	}

	static ScalarField2 sdfCapsule(final Vec2 a, final Vec2 b, double r) {
		return (x, y) -> {
			final var p = new Vec2(x, y);
			final var ba = b.sub(a);
			final var pa = p.sub(a);
			final var h = Mth.clamp(pa.dot(ba) / ba.lengthSquared(), 0.0, 1.0);
			return pa.sub(ba.mul(h)).length() - r;
		};
	}

	static ScalarField2 sdfLine(Vec2Access N) {
		return (x, y) -> x * N.x() + y * N.y();
	}

	/**
	 * This method turns an SDF into a "cloud". The surface and inside of the SDF is
	 * mapped to 1, and {@code r} units from the surface is mapped to 0, linearly
	 * interpolating for intermediate values.
	 * 
	 * @param r The maximum distance from the surface of the SDF that his cloud will
	 *          be greater than 0.
	 * @return The remapped field.
	 */
	default ScalarField2 sdfCloud(double r) {
		return (x, y) -> {
			final var n = this.sample(x, y);
			return 1.0 - Mth.clamp(n / r, 0, 1);
		};
	}

	static ScalarField2 circleMask(double radius) {
		final var r2 = radius * radius;
		return (x, y) -> x * x + y * y > r2 ? 0 : 1;
	}

	// larger values for falloff mean a less prominent falloff. a falloff of 1 means
	// that the falloff starts at the origin.
	static ScalarField2 disc(double radius, double height, double falloffRadius) {
		final var rFalloff = radius / falloffRadius;

		return (x, y) -> {
			final var projectedLen = Math.sqrt(x * x + y * y);
			var hf = Math.pow(0.01, Math.abs(y / height));
			var rf = Mth.clamp((radius - projectedLen) / rFalloff, 0, 1);
			return rf * hf;
		};
	}

	// @formatter:off
	static ScalarField2 add(ScalarField2 a, ScalarField2 b) { return a.add(b); }
	static ScalarField2 add(ScalarField2 a, double b) { return a.add(uniform(b)); }
	static ScalarField2 add(double a, ScalarField2 b) { return uniform(a).add(b); }
	static ScalarField2 add(double a, double b) { return uniform(a).add(uniform(b)); }

	static ScalarField2 sub(ScalarField2 a, ScalarField2 b) { return a.sub(b); }
	static ScalarField2 sub(ScalarField2 a, double b) { return a.sub(uniform(b)); }
	static ScalarField2 sub(double a, ScalarField2 b) { return uniform(a).sub(b); }
	static ScalarField2 sub(double a, double b) { return uniform(a).sub(uniform(b)); }

	static ScalarField2 mul(ScalarField2 a, ScalarField2 b) { return a.mul(b); }
	static ScalarField2 mul(ScalarField2 a, double b) { return a.mul(uniform(b)); }
	static ScalarField2 mul(double a, ScalarField2 b) { return uniform(a).mul(b); }
	static ScalarField2 mul(double a, double b) { return uniform(a).mul(uniform(b)); }

	static ScalarField2 div(ScalarField2 a, ScalarField2 b) { return a.withDenominator(b); }
	static ScalarField2 div(ScalarField2 a, double b) { return a.withDenominator(uniform(b)); }
	static ScalarField2 div(double a, ScalarField2 b) { return uniform(a).withDenominator(b); }
	static ScalarField2 div(double a, double b) { return uniform(a).withDenominator(uniform(b)); }

	static ScalarField2 pow(ScalarField2 a, ScalarField2 b) { return a.withExponent(b); }
	static ScalarField2 pow(ScalarField2 a, double b) { return a.withExponent(uniform(b)); }
	static ScalarField2 pow(double a, ScalarField2 b) { return uniform(a).withExponent(b); }
	static ScalarField2 pow(double a, double b) { return uniform(a).withExponent(uniform(b)); }
	// @formatter:on

}
