package net.xavil.hawklib.math;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec4;

public interface NumericOps<T> {

	/**
	 * @return The additive identity for this type.
	 */
	T zero();

	/**
	 * @return The multiplicative identity for this type.
	 */
	T one();

	T add(T a, T b);

	T sub(T a, T b);

	T mul(T a, T b);

	T mul(double a, T b);

	T div(T a, T b);

	default T lerp(double t, T a, T b) {
		return add(a, mul(t, sub(b, a)));
	}

	static NumericOps<Double> DOUBLE = new NumericOps<Double>() {
		// @formatter:off
		@Override public Double zero() { return 0.0; }
		@Override public Double one() { return 1.0; }
		@Override public Double add(Double a, Double b) { return a + b; }
		@Override public Double sub(Double a, Double b) { return a - b; }
		@Override public Double mul(Double a, Double b) { return a * b; }
		@Override public Double mul(double a, Double b) { return a * b; }
		@Override public Double div(Double a, Double b) { return a / b; }
		@Override public Double lerp(double t, Double a, Double b) { return Mth.lerp(t, a, b); }
		// @formatter:on
	};

	static NumericOps<Vec2> VEC2 = new NumericOps<Vec2>() {		
		// @formatter:off
		private static final Vec2 ONE = Vec2.broadcast(1.0);
		@Override public Vec2 zero() { return Vec2.ZERO; }
		@Override public Vec2 one() { return ONE; }
		@Override public Vec2 add(  Vec2 a, Vec2 b) { return a.add(b); }
		@Override public Vec2 sub(  Vec2 a, Vec2 b) { return a.sub(b); }
		@Override public Vec2 mul(  Vec2 a, Vec2 b) { return a.mul(b); }
		@Override public Vec2 mul(double a, Vec2 b) { return b.mul(a); }
		@Override public Vec2 div(  Vec2 a, Vec2 b) { return a.div(b); }
		// @formatter:on
	};

	static NumericOps<Vec3> VEC3 = new NumericOps<Vec3>() {
		// @formatter:off
		private static final Vec3 ONE = Vec3.broadcast(1.0);
		@Override public Vec3 zero() { return Vec3.ZERO; }
		@Override public Vec3 one() { return ONE; }
		@Override public Vec3 add(  Vec3 a, Vec3 b) { return a.add(b); }
		@Override public Vec3 sub(  Vec3 a, Vec3 b) { return a.sub(b); }
		@Override public Vec3 mul(  Vec3 a, Vec3 b) { return a.mul(b); }
		@Override public Vec3 mul(double a, Vec3 b) { return b.mul(a); }
		@Override public Vec3 div(  Vec3 a, Vec3 b) { return a.div(b); }
		// @formatter:on
	};

	static NumericOps<Vec4> VEC4 = new NumericOps<Vec4>() {
		// @formatter:off
		private static final Vec4 ZERO = Vec4.broadcast(0.0);
		private static final Vec4 ONE = Vec4.broadcast(1.0);
		@Override public Vec4 zero() { return ZERO; }
		@Override public Vec4 one() { return ONE; }
		@Override public Vec4 add(  Vec4 a, Vec4 b) { return a.add(b); }
		@Override public Vec4 sub(  Vec4 a, Vec4 b) { return a.sub(b); }
		@Override public Vec4 mul(  Vec4 a, Vec4 b) { return a.mul(b); }
		@Override public Vec4 mul(double a, Vec4 b) { return b.mul(a); }
		@Override public Vec4 div(  Vec4 a, Vec4 b) { return a.div(b); }
		// @formatter:on
	};

}
