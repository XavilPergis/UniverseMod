package net.xavil.hawklib.math.matrices;

import com.mojang.math.Matrix4f;

import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec4Access;

public final class Mat4 implements Hashable, Mat4Access {

	public final double r0c0, r0c1, r0c2, r0c3;
	public final double r1c0, r1c1, r1c2, r1c3;
	public final double r2c0, r2c1, r2c2, r2c3;
	public final double r3c0, r3c1, r3c2, r3c3;

	public Mat4(double r0c0, double r0c1, double r0c2, double r0c3,
			double r1c0, double r1c1, double r1c2, double r1c3,
			double r2c0, double r2c1, double r2c2, double r2c3,
			double r3c0, double r3c1, double r3c2, double r3c3) {
		this.r0c0 = r0c0;
		this.r0c1 = r0c1;
		this.r0c2 = r0c2;
		this.r0c3 = r0c3;
		this.r1c0 = r1c0;
		this.r1c1 = r1c1;
		this.r1c2 = r1c2;
		this.r1c3 = r1c3;
		this.r2c0 = r2c0;
		this.r2c1 = r2c1;
		this.r2c2 = r2c2;
		this.r2c3 = r2c3;
		this.r3c0 = r3c0;
		this.r3c1 = r3c1;
		this.r3c2 = r3c2;
		this.r3c3 = r3c3;
	}

	public static final Mat4 IDENTITY = diagonal(Vec4.broadcast(1.0));

	public static Mat4 fromRows(Vec4Access r0, Vec4Access r1, Vec4Access r2, Vec4Access r3) {
		return new Mat4(
				r0.x(), r0.y(), r0.z(), r0.w(),
				r1.x(), r1.y(), r1.z(), r1.w(),
				r2.x(), r2.y(), r2.z(), r2.w(),
				r3.x(), r3.y(), r3.z(), r3.w());
	}

	public static Mat4 fromColumns(Vec4Access c0, Vec4Access c1, Vec4Access c2, Vec4Access c3) {
		return new Mat4(
				c0.x(), c1.x(), c2.x(), c3.x(),
				c0.y(), c1.y(), c2.y(), c3.y(),
				c0.z(), c1.z(), c2.z(), c3.z(),
				c0.w(), c1.w(), c2.w(), c3.w());
	}

	public static Mat4 fromBases(Vec3Access x, Vec3Access y, Vec3Access z, Vec3Access pos) {
		return fromColumns(x.xyz0(), y.xyz0(), z.xyz0(), pos.xyz1());
	}

	public static Mat4 diagonal(Vec4Access diag) {
		return new Mat4(
				diag.x(), 0.0, 0.0, 0.0,
				0.0, diag.y(), 0.0, 0.0,
				0.0, 0.0, diag.z(), 0.0,
				0.0, 0.0, 0.0, diag.w());
	}

	public static Mat4 scale(double n) {
		return diagonal(Vec3.broadcast(n).xyz1());
	}

	public static Mat4 perspectiveProjection(double fovRad, double aspectRatio, double near, double far) {
		final var f = 1.0 / Math.tan(fovRad / 2.0);
		final var m00 = f / aspectRatio;
		final var m11 = f;
		final var m22 = (far + near) / (near - far);
		final var m32 = -1.0;
		final var m23 = 2.0 * far * near / (near - far);
		return new Mat4(
				m00, 0.0, 0.0, 0.0,
				0.0, m11, 0.0, 0.0,
				0.0, 0.0, m22, m23,
				0.0, 0.0, m32, 0.0);
	}

	public static Mat4 orthographicProjection(double minX, double maxX,
			double minY, double maxY,
			double minZ, double maxZ) {
		return setOrthographicProjection(new Mutable(), minX, maxX, minY, maxY, minZ, maxZ).asImmutable();
	}

	// @formatter:off
	@Override public final double r0c0() {return r0c0;}
	@Override public final double r0c1() {return r0c1;}
	@Override public final double r0c2() {return r0c2;}
	@Override public final double r0c3() {return r0c3;}
	@Override public final double r1c0() {return r1c0;}
	@Override public final double r1c1() {return r1c1;}
	@Override public final double r1c2() {return r1c2;}
	@Override public final double r1c3() {return r1c3;}
	@Override public final double r2c0() {return r2c0;}
	@Override public final double r2c1() {return r2c1;}
	@Override public final double r2c2() {return r2c2;}
	@Override public final double r2c3() {return r2c3;}
	@Override public final double r3c0() {return r3c0;}
	@Override public final double r3c1() {return r3c1;}
	@Override public final double r3c2() {return r3c2;}
	@Override public final double r3c3() {return r3c3;}
	// @formatter:on

	@Override
	public Mat4 asImmutable() {
		return this;
	}

	public static Mat4 mul(Mat4Access a, Mat4Access b) {
		final var r0c0 = (a.r0c0() * b.r0c0()) + (a.r0c1() * b.r1c0()) + (a.r0c2() * b.r2c0()) + (a.r0c3() * b.r3c0());
		final var r0c1 = (a.r0c0() * b.r0c1()) + (a.r0c1() * b.r1c1()) + (a.r0c2() * b.r2c1()) + (a.r0c3() * b.r3c1());
		final var r0c2 = (a.r0c0() * b.r0c2()) + (a.r0c1() * b.r1c2()) + (a.r0c2() * b.r2c2()) + (a.r0c3() * b.r3c2());
		final var r0c3 = (a.r0c0() * b.r0c3()) + (a.r0c1() * b.r1c3()) + (a.r0c2() * b.r2c3()) + (a.r0c3() * b.r3c3());
		final var r1c0 = (a.r1c0() * b.r0c0()) + (a.r1c1() * b.r1c0()) + (a.r1c2() * b.r2c0()) + (a.r1c3() * b.r3c0());
		final var r1c1 = (a.r1c0() * b.r0c1()) + (a.r1c1() * b.r1c1()) + (a.r1c2() * b.r2c1()) + (a.r1c3() * b.r3c1());
		final var r1c2 = (a.r1c0() * b.r0c2()) + (a.r1c1() * b.r1c2()) + (a.r1c2() * b.r2c2()) + (a.r1c3() * b.r3c2());
		final var r1c3 = (a.r1c0() * b.r0c3()) + (a.r1c1() * b.r1c3()) + (a.r1c2() * b.r2c3()) + (a.r1c3() * b.r3c3());
		final var r2c0 = (a.r2c0() * b.r0c0()) + (a.r2c1() * b.r1c0()) + (a.r2c2() * b.r2c0()) + (a.r2c3() * b.r3c0());
		final var r2c1 = (a.r2c0() * b.r0c1()) + (a.r2c1() * b.r1c1()) + (a.r2c2() * b.r2c1()) + (a.r2c3() * b.r3c1());
		final var r2c2 = (a.r2c0() * b.r0c2()) + (a.r2c1() * b.r1c2()) + (a.r2c2() * b.r2c2()) + (a.r2c3() * b.r3c2());
		final var r2c3 = (a.r2c0() * b.r0c3()) + (a.r2c1() * b.r1c3()) + (a.r2c2() * b.r2c3()) + (a.r2c3() * b.r3c3());
		final var r3c0 = (a.r3c0() * b.r0c0()) + (a.r3c1() * b.r1c0()) + (a.r3c2() * b.r2c0()) + (a.r3c3() * b.r3c0());
		final var r3c1 = (a.r3c0() * b.r0c1()) + (a.r3c1() * b.r1c1()) + (a.r3c2() * b.r2c1()) + (a.r3c3() * b.r3c1());
		final var r3c2 = (a.r3c0() * b.r0c2()) + (a.r3c1() * b.r1c2()) + (a.r3c2() * b.r2c2()) + (a.r3c3() * b.r3c2());
		final var r3c3 = (a.r3c0() * b.r0c3()) + (a.r3c1() * b.r1c3()) + (a.r3c2() * b.r2c3()) + (a.r3c3() * b.r3c3());
		return new Mat4(r0c0, r0c1, r0c2, r0c3, r1c0, r1c1, r1c2, r1c3, r2c0, r2c1, r2c2, r2c3, r3c0, r3c1, r3c2, r3c3);
	}

	public static Vec4.Mutable transform(Vec4.Mutable out, Vec4.Mutable r, Mat4Access a) {
		final var x = (a.r0c0() * r.x) + (a.r0c1() * r.y) + (a.r0c2() * r.z) + (a.r0c3() * r.w);
		final var y = (a.r1c0() * r.x) + (a.r1c1() * r.y) + (a.r1c2() * r.z) + (a.r1c3() * r.w);
		final var z = (a.r2c0() * r.x) + (a.r2c1() * r.y) + (a.r2c2() * r.z) + (a.r2c3() * r.w);
		final var w = (a.r3c0() * r.x) + (a.r3c1() * r.y) + (a.r3c2() * r.z) + (a.r3c3() * r.w);
		out.x = x;
		out.y = y;
		out.z = z;
		out.w = w;
		return out;
	}

	public static Vec3.Mutable transform(Vec3.Mutable out, Vec3.Mutable r, double ww, Mat4Access a) {
		final var x = (a.r0c0() * r.x) + (a.r0c1() * r.y) + (a.r0c2() * r.z) + (a.r0c3() * ww);
		final var y = (a.r1c0() * r.x) + (a.r1c1() * r.y) + (a.r1c2() * r.z) + (a.r1c3() * ww);
		final var z = (a.r2c0() * r.x) + (a.r2c1() * r.y) + (a.r2c2() * r.z) + (a.r2c3() * ww);
		final var w = (a.r3c0() * r.x) + (a.r3c1() * r.y) + (a.r3c2() * r.z) + (a.r3c3() * ww);
		out.x = x / w;
		out.y = y / w;
		out.z = z / w;
		return out;
	}

	public static Vec4 mul(Mat4Access a, Vec4Access b) {
		final var x = (a.r0c0() * b.x()) + (a.r0c1() * b.y()) + (a.r0c2() * b.z()) + (a.r0c3() * b.w());
		final var y = (a.r1c0() * b.x()) + (a.r1c1() * b.y()) + (a.r1c2() * b.z()) + (a.r1c3() * b.w());
		final var z = (a.r2c0() * b.x()) + (a.r2c1() * b.y()) + (a.r2c2() * b.z()) + (a.r2c3() * b.w());
		final var w = (a.r3c0() * b.x()) + (a.r3c1() * b.y()) + (a.r3c2() * b.z()) + (a.r3c3() * b.w());
		return Vec4.from(x, y, z, w);
	}

	public static Vec3 mul(Mat4Access a, Vec3Access b, double ww) {
		final var x = (a.r0c0() * b.x()) + (a.r0c1() * b.y()) + (a.r0c2() * b.z()) + (a.r0c3() * ww);
		final var y = (a.r1c0() * b.x()) + (a.r1c1() * b.y()) + (a.r1c2() * b.z()) + (a.r1c3() * ww);
		final var z = (a.r2c0() * b.x()) + (a.r2c1() * b.y()) + (a.r2c2() * b.z()) + (a.r2c3() * ww);
		final var w = (a.r3c0() * b.x()) + (a.r3c1() * b.y()) + (a.r3c2() * b.z()) + (a.r3c3() * ww);
		return new Vec3(x / w, y / w, z / w);
	}

	public static Mat4 mul(double a, Mat4Access b) {
		return new Mat4(
				a * b.r0c0(), a * b.r0c1(), a * b.r0c2(), a * b.r0c3(),
				a * b.r1c0(), a * b.r1c1(), a * b.r1c2(), a * b.r1c3(),
				a * b.r2c0(), a * b.r2c1(), a * b.r2c2(), a * b.r2c3(),
				a * b.r3c0(), a * b.r3c1(), a * b.r3c2(), a * b.r3c3());
	}

	public static Mat4.Mutable mul(Mat4.Mutable r, Mat4Access a, Mat4Access b) {
		// @formatter:off
		final var r0c0 = (a.r0c0() * b.r0c0()) + (a.r0c1() * b.r1c0()) + (a.r0c2() * b.r2c0()) + (a.r0c3() * b.r3c0());
		final var r0c1 = (a.r0c0() * b.r0c1()) + (a.r0c1() * b.r1c1()) + (a.r0c2() * b.r2c1()) + (a.r0c3() * b.r3c1());
		final var r0c2 = (a.r0c0() * b.r0c2()) + (a.r0c1() * b.r1c2()) + (a.r0c2() * b.r2c2()) + (a.r0c3() * b.r3c2());
		final var r0c3 = (a.r0c0() * b.r0c3()) + (a.r0c1() * b.r1c3()) + (a.r0c2() * b.r2c3()) + (a.r0c3() * b.r3c3());
		final var r1c0 = (a.r1c0() * b.r0c0()) + (a.r1c1() * b.r1c0()) + (a.r1c2() * b.r2c0()) + (a.r1c3() * b.r3c0());
		final var r1c1 = (a.r1c0() * b.r0c1()) + (a.r1c1() * b.r1c1()) + (a.r1c2() * b.r2c1()) + (a.r1c3() * b.r3c1());
		final var r1c2 = (a.r1c0() * b.r0c2()) + (a.r1c1() * b.r1c2()) + (a.r1c2() * b.r2c2()) + (a.r1c3() * b.r3c2());
		final var r1c3 = (a.r1c0() * b.r0c3()) + (a.r1c1() * b.r1c3()) + (a.r1c2() * b.r2c3()) + (a.r1c3() * b.r3c3());
		final var r2c0 = (a.r2c0() * b.r0c0()) + (a.r2c1() * b.r1c0()) + (a.r2c2() * b.r2c0()) + (a.r2c3() * b.r3c0());
		final var r2c1 = (a.r2c0() * b.r0c1()) + (a.r2c1() * b.r1c1()) + (a.r2c2() * b.r2c1()) + (a.r2c3() * b.r3c1());
		final var r2c2 = (a.r2c0() * b.r0c2()) + (a.r2c1() * b.r1c2()) + (a.r2c2() * b.r2c2()) + (a.r2c3() * b.r3c2());
		final var r2c3 = (a.r2c0() * b.r0c3()) + (a.r2c1() * b.r1c3()) + (a.r2c2() * b.r2c3()) + (a.r2c3() * b.r3c3());
		final var r3c0 = (a.r3c0() * b.r0c0()) + (a.r3c1() * b.r1c0()) + (a.r3c2() * b.r2c0()) + (a.r3c3() * b.r3c0());
		final var r3c1 = (a.r3c0() * b.r0c1()) + (a.r3c1() * b.r1c1()) + (a.r3c2() * b.r2c1()) + (a.r3c3() * b.r3c1());
		final var r3c2 = (a.r3c0() * b.r0c2()) + (a.r3c1() * b.r1c2()) + (a.r3c2() * b.r2c2()) + (a.r3c3() * b.r3c2());
		final var r3c3 = (a.r3c0() * b.r0c3()) + (a.r3c1() * b.r1c3()) + (a.r3c2() * b.r2c3()) + (a.r3c3() * b.r3c3());
		r.r0c0 = r0c0; r.r0c1 = r0c1; r.r0c2 = r0c2; r.r0c3 = r0c3;
		r.r1c0 = r1c0; r.r1c1 = r1c1; r.r1c2 = r1c2; r.r1c3 = r1c3;
		r.r2c0 = r2c0; r.r2c1 = r2c1; r.r2c2 = r2c2; r.r2c3 = r2c3;
		r.r3c0 = r3c0; r.r3c1 = r3c1; r.r3c2 = r3c2; r.r3c3 = r3c3;
		return r;
		// @formatter:on
	}

	public static Mat4.Mutable mulScale(Mat4.Mutable r, double s, Mat4Access b) {
		// @formatter:off
		r.r0c0 = s * b.r0c0(); r.r0c1 = s * b.r0c1(); r.r0c2 = s * b.r0c2(); r.r0c3 = s * b.r0c3();
		r.r1c0 = s * b.r1c0(); r.r1c1 = s * b.r1c1(); r.r1c2 = s * b.r1c2(); r.r1c3 = s * b.r1c3();
		r.r2c0 = s * b.r2c0(); r.r2c1 = s * b.r2c1(); r.r2c2 = s * b.r2c2(); r.r2c3 = s * b.r2c3();
		r.r3c0 = b.r3c0(); r.r3c1 = b.r3c1(); r.r3c2 = b.r3c2(); r.r3c3 = b.r3c3();
		return r;
		// @formatter:on
	}

	public static Mat4.Mutable mulScale(Mat4.Mutable r, Mat4Access a, double s) {
		// @formatter:off
		r.r0c0 = s * a.r0c0(); r.r0c1 = s * a.r0c1(); r.r0c2 = s * a.r0c2(); r.r0c3 = a.r0c3();
		r.r1c0 = s * a.r1c0(); r.r1c1 = s * a.r1c1(); r.r1c2 = s * a.r1c2(); r.r1c3 = a.r1c3();
		r.r2c0 = s * a.r2c0(); r.r2c1 = s * a.r2c1(); r.r2c2 = s * a.r2c2(); r.r2c3 = a.r2c3();
		r.r3c0 = s * a.r3c0(); r.r3c1 = s * a.r3c1(); r.r3c2 = s * a.r3c2(); r.r3c3 = a.r3c3();
		return r;
		// @formatter:on
	}

	public static Mat4.Mutable mulTranslation(Mat4.Mutable r, Vec3Access a, Mat4Access b) {
		// @formatter:off
		r.r0c0 = b.r0c0() + a.x() * b.r3c0(); r.r0c1 = b.r0c1() + a.x() * b.r3c1(); r.r0c2 = b.r0c2() + a.x() * b.r3c2(); r.r0c3 = b.r0c3() + a.x() * b.r3c3();
		r.r1c0 = b.r1c0() + a.y() * b.r3c0(); r.r1c1 = b.r1c1() + a.y() * b.r3c1(); r.r1c2 = b.r1c2() + a.y() * b.r3c2(); r.r1c3 = b.r1c3() + a.y() * b.r3c3();
		r.r2c0 = b.r2c0() + a.z() * b.r3c0(); r.r2c1 = b.r2c1() + a.z() * b.r3c1(); r.r2c2 = b.r2c2() + a.z() * b.r3c2(); r.r2c3 = b.r2c3() + a.z() * b.r3c3();
		r.r3c0 = b.r3c0();                    r.r3c1 = b.r3c1();                    r.r3c2 = b.r3c2();                    r.r3c3 = b.r3c3();
		return r;
		// @formatter:on
	}

	public static Mat4.Mutable mulTranslation(Mat4.Mutable r, Mat4Access a, Vec3Access b) {
		// @formatter:off
		r.r0c0 = a.r0c0();
		r.r0c1 = a.r0c1();
		r.r0c2 = a.r0c2();
		r.r0c3 = (a.r0c0() * b.x()) + (a.r0c1() * b.y()) + (a.r0c2() * b.z()) + (a.r0c3() * 1);
		r.r1c0 = a.r1c0();
		r.r1c1 = a.r1c1();
		r.r1c2 = a.r1c2();
		r.r1c3 = (a.r1c0() * b.x()) + (a.r1c1() * b.y()) + (a.r1c2() * b.z()) + (a.r1c3() * 1);
		r.r2c0 = a.r2c0();
		r.r2c1 = a.r2c1();
		r.r2c2 = a.r2c2();
		r.r2c3 = (a.r2c0() * b.x()) + (a.r2c1() * b.y()) + (a.r2c2() * b.z()) + (a.r2c3() * 1);
		r.r3c0 = a.r3c0();
		r.r3c1 = a.r3c1();
		r.r3c2 = a.r3c2();
		r.r3c3 = (a.r3c0() * b.x()) + (a.r3c1() * b.y()) + (a.r3c2() * b.z()) + (a.r3c3() * 1);
		return r;
		// @formatter:on
	}

	public static Mat4.Mutable mulRotation(Mat4.Mutable r, Quat a, Mat4Access b) {
		// @formatter:off
		final var ii = 2 * a.i * a.i;
		final var jj = 2 * a.j * a.j;
		final var kk = 2 * a.k * a.k;
		final var ij = 2 * a.i * a.j;
		final var jk = 2 * a.j * a.k;
		final var ki = 2 * a.k * a.i;
		final var iw = 2 * a.i * a.w;
		final var jw = 2 * a.j * a.w;
		final var kw = 2 * a.k * a.w;

		final var ar0c0 = 1 - jj - kk; final var ar0c1 = ij - kw;     final var ar0c2 = ki + jw;
		final var ar1c0 = ij + kw;     final var ar1c1 = 1 - kk - ii; final var ar1c2 = jk - iw;
		final var ar2c0 = ki - jw;     final var ar2c1 = jk + iw;     final var ar2c2 = 1 - ii - jj;

		final var r0c0 = (ar0c0 * b.r0c0()) + (ar0c1 * b.r1c0()) + (ar0c2 * b.r2c0());
		final var r0c1 = (ar0c0 * b.r0c1()) + (ar0c1 * b.r1c1()) + (ar0c2 * b.r2c1());
		final var r0c2 = (ar0c0 * b.r0c2()) + (ar0c1 * b.r1c2()) + (ar0c2 * b.r2c2());
		final var r0c3 = (ar0c0 * b.r0c3()) + (ar0c1 * b.r1c3()) + (ar0c2 * b.r2c3());
		final var r1c0 = (ar1c0 * b.r0c0()) + (ar1c1 * b.r1c0()) + (ar1c2 * b.r2c0());
		final var r1c1 = (ar1c0 * b.r0c1()) + (ar1c1 * b.r1c1()) + (ar1c2 * b.r2c1());
		final var r1c2 = (ar1c0 * b.r0c2()) + (ar1c1 * b.r1c2()) + (ar1c2 * b.r2c2());
		final var r1c3 = (ar1c0 * b.r0c3()) + (ar1c1 * b.r1c3()) + (ar1c2 * b.r2c3());
		final var r2c0 = (ar2c0 * b.r0c0()) + (ar2c1 * b.r1c0()) + (ar2c2 * b.r2c0());
		final var r2c1 = (ar2c0 * b.r0c1()) + (ar2c1 * b.r1c1()) + (ar2c2 * b.r2c1());
		final var r2c2 = (ar2c0 * b.r0c2()) + (ar2c1 * b.r1c2()) + (ar2c2 * b.r2c2());
		final var r2c3 = (ar2c0 * b.r0c3()) + (ar2c1 * b.r1c3()) + (ar2c2 * b.r2c3());
		r.r0c0 = r0c0; r.r0c1 = r0c1; r.r0c2 = r0c2; r.r0c3 = r0c3;
		r.r1c0 = r1c0; r.r1c1 = r1c1; r.r1c2 = r1c2; r.r1c3 = r1c3;
		r.r2c0 = r2c0; r.r2c1 = r2c1; r.r2c2 = r2c2; r.r2c3 = r2c3;
		r.r3c0 = b.r3c0(); r.r3c1 = b.r3c1(); r.r3c2 = b.r3c2(); r.r3c3 = b.r3c3();
		return r;
		// @formatter:on
	}

	public static Mat4.Mutable mulRotation(Mat4.Mutable r, Mat4Access a, Quat b) {
		// @formatter:off
		final var ii = 2 * b.i * b.i;
		final var jj = 2 * b.j * b.j;
		final var kk = 2 * b.k * b.k;
		final var ij = 2 * b.i * b.j;
		final var jk = 2 * b.j * b.k;
		final var ki = 2 * b.k * b.i;
		final var iw = 2 * b.i * b.w;
		final var jw = 2 * b.j * b.w;
		final var kw = 2 * b.k * b.w;

		final var br0c0 = 1 - jj - kk; final var br0c1 = ij - kw;     final var br0c2 = ki + jw;
		final var br1c0 = ij + kw;     final var br1c1 = 1 - kk - ii; final var br1c2 = jk - iw;
		final var br2c0 = ki - jw;     final var br2c1 = jk + iw;     final var br2c2 = 1 - ii - jj;

		final var r0c0 = (a.r0c0() * br0c0) + (a.r0c1() * br1c0) + (a.r0c2() * br2c0);
		final var r0c1 = (a.r0c0() * br0c1) + (a.r0c1() * br1c1) + (a.r0c2() * br2c1);
		final var r0c2 = (a.r0c0() * br0c2) + (a.r0c1() * br1c2) + (a.r0c2() * br2c2);
		final var r1c0 = (a.r1c0() * br0c0) + (a.r1c1() * br1c0) + (a.r1c2() * br2c0);
		final var r1c1 = (a.r1c0() * br0c1) + (a.r1c1() * br1c1) + (a.r1c2() * br2c1);
		final var r1c2 = (a.r1c0() * br0c2) + (a.r1c1() * br1c2) + (a.r1c2() * br2c2);
		final var r2c0 = (a.r2c0() * br0c0) + (a.r2c1() * br1c0) + (a.r2c2() * br2c0);
		final var r2c1 = (a.r2c0() * br0c1) + (a.r2c1() * br1c1) + (a.r2c2() * br2c1);
		final var r2c2 = (a.r2c0() * br0c2) + (a.r2c1() * br1c2) + (a.r2c2() * br2c2);
		final var r3c0 = (a.r3c0() * br0c0) + (a.r3c1() * br1c0) + (a.r3c2() * br2c0);
		final var r3c1 = (a.r3c0() * br0c1) + (a.r3c1() * br1c1) + (a.r3c2() * br2c1);
		final var r3c2 = (a.r3c0() * br0c2) + (a.r3c1() * br1c2) + (a.r3c2() * br2c2);
		r.r0c0 = r0c0; r.r0c1 = r0c1; r.r0c2 = r0c2; r.r0c3 = a.r0c3();
		r.r1c0 = r1c0; r.r1c1 = r1c1; r.r1c2 = r1c2; r.r1c3 = a.r1c3();
		r.r2c0 = r2c0; r.r2c1 = r2c1; r.r2c2 = r2c2; r.r2c3 = a.r2c3();
		r.r3c0 = r3c0; r.r3c1 = r3c1; r.r3c2 = r3c2; r.r3c3 = a.r3c3();
		return r;
		// @formatter:on
	}

	public static Vec4.Mutable mul(Vec4.Mutable r, Mat4Access a, Vec4Access b) {
		// @formatter:off
		final var x = (a.r0c0() * b.x()) + (a.r0c1() * b.y()) + (a.r0c2() * b.z()) + (a.r0c3() * b.w());
		final var y = (a.r1c0() * b.x()) + (a.r1c1() * b.y()) + (a.r1c2() * b.z()) + (a.r1c3() * b.w());
		final var z = (a.r2c0() * b.x()) + (a.r2c1() * b.y()) + (a.r2c2() * b.z()) + (a.r2c3() * b.w());
		final var w = (a.r3c0() * b.x()) + (a.r3c1() * b.y()) + (a.r3c2() * b.z()) + (a.r3c3() * b.w());
		r.x = x; r.y = y; r.z = z; r.w = w;
		return r;
		// @formatter:on
	}

	public static Mat4.Mutable mul(Mat4.Mutable r, double a, Mat4Access b) {
		// @formatter:off
		r.r0c0 = a * b.r0c0(); r.r0c1 = a * b.r0c1(); r.r0c2 = a * b.r0c2(); r.r0c3 = a * b.r0c3();
		r.r1c0 = a * b.r1c0(); r.r1c1 = a * b.r1c1(); r.r1c2 = a * b.r1c2(); r.r1c3 = a * b.r1c3();
		r.r2c0 = a * b.r2c0(); r.r2c1 = a * b.r2c1(); r.r2c2 = a * b.r2c2(); r.r2c3 = a * b.r2c3();
		r.r3c0 = a * b.r3c0(); r.r3c1 = a * b.r3c1(); r.r3c2 = a * b.r3c2(); r.r3c3 = a * b.r3c3();
		return r;
		// @formatter:on
	}

	public static boolean invert(Mat4.Mutable r, Mat4Access a) {
		// unique combinations of 2x2 determinants (d2)
		final var d2r12c23 = a.r1c2() * a.r2c3() - a.r1c3() * a.r2c2();
		final var d2r12c12 = a.r1c1() * a.r2c2() - a.r1c2() * a.r2c1();
		final var d2r12c01 = a.r1c0() * a.r2c1() - a.r1c1() * a.r2c0();
		final var d2r12c13 = a.r1c1() * a.r2c3() - a.r1c3() * a.r2c1();
		final var d2r12c02 = a.r1c0() * a.r2c2() - a.r1c2() * a.r2c0();
		final var d2r12c03 = a.r1c0() * a.r2c3() - a.r1c3() * a.r2c0();
		final var d2r23c23 = a.r2c2() * a.r3c3() - a.r2c3() * a.r3c2();
		final var d2r23c12 = a.r2c1() * a.r3c2() - a.r2c2() * a.r3c1();
		final var d2r23c01 = a.r2c0() * a.r3c1() - a.r2c1() * a.r3c0();
		final var d2r23c13 = a.r2c1() * a.r3c3() - a.r2c3() * a.r3c1();
		final var d2r23c02 = a.r2c0() * a.r3c2() - a.r2c2() * a.r3c0();
		final var d2r23c03 = a.r2c0() * a.r3c3() - a.r2c3() * a.r3c0();
		final var d2r13c23 = a.r1c2() * a.r3c3() - a.r1c3() * a.r3c2();
		final var d2r13c12 = a.r1c1() * a.r3c2() - a.r1c2() * a.r3c1();
		final var d2r13c01 = a.r1c0() * a.r3c1() - a.r1c1() * a.r3c0();
		final var d2r13c13 = a.r1c1() * a.r3c3() - a.r1c3() * a.r3c1();
		final var d2r13c02 = a.r1c0() * a.r3c2() - a.r1c2() * a.r3c0();
		final var d2r13c03 = a.r1c0() * a.r3c3() - a.r1c3() * a.r3c0();

		// unique combinations of 3x3 determinants (d3)
		// This is also essentially the matrix of minors for this matrix.
		final var d3r123c123 = a.r1c1() * d2r23c23 - a.r1c2() * d2r23c13 + a.r1c3() * d2r23c12;
		final var d3r123c023 = a.r1c0() * d2r23c23 - a.r1c2() * d2r23c03 + a.r1c3() * d2r23c02;
		final var d3r123c013 = a.r1c0() * d2r23c13 - a.r1c1() * d2r23c03 + a.r1c3() * d2r23c01;
		final var d3r123c012 = a.r1c0() * d2r23c12 - a.r1c1() * d2r23c02 + a.r1c2() * d2r23c01;
		final var d3r023c123 = a.r0c1() * d2r23c23 - a.r0c2() * d2r23c13 + a.r0c3() * d2r23c12;
		final var d3r023c023 = a.r0c0() * d2r23c23 - a.r0c2() * d2r23c03 + a.r0c3() * d2r23c02;
		final var d3r023c013 = a.r0c0() * d2r23c13 - a.r0c1() * d2r23c03 + a.r0c3() * d2r23c01;
		final var d3r023c012 = a.r0c0() * d2r23c12 - a.r0c1() * d2r23c02 + a.r0c2() * d2r23c01;
		final var d3r013c123 = a.r0c1() * d2r13c23 - a.r0c2() * d2r13c13 + a.r0c3() * d2r13c12;
		final var d3r013c023 = a.r0c0() * d2r13c23 - a.r0c2() * d2r13c03 + a.r0c3() * d2r13c02;
		final var d3r013c013 = a.r0c0() * d2r13c13 - a.r0c1() * d2r13c03 + a.r0c3() * d2r13c01;
		final var d3r013c012 = a.r0c0() * d2r13c12 - a.r0c1() * d2r13c02 + a.r0c2() * d2r13c01;
		final var d3r012c123 = a.r0c1() * d2r12c23 - a.r0c2() * d2r12c13 + a.r0c3() * d2r12c12;
		final var d3r012c023 = a.r0c0() * d2r12c23 - a.r0c2() * d2r12c03 + a.r0c3() * d2r12c02;
		final var d3r012c013 = a.r0c0() * d2r12c13 - a.r0c1() * d2r12c03 + a.r0c3() * d2r12c01;
		final var d3r012c012 = a.r0c0() * d2r12c12 - a.r0c1() * d2r12c02 + a.r0c2() * d2r12c01;

		final var det = a.r0c0() * d3r123c123 - a.r0c1() * d3r123c023 + a.r0c2() * d3r123c013 - a.r0c3() * d3r123c012;
		if (Math.abs(det) < 1e-8)
			return false;

		// @formatter:off
		final var n = 1.0 / det;
		r.r0c0 = n *  d3r123c123; r.r0c1 = n * -d3r023c123; r.r0c2 = n *  d3r013c123; r.r0c3 = n * -d3r012c123;
		r.r1c0 = n * -d3r123c023; r.r1c1 = n *  d3r023c023; r.r1c2 = n * -d3r013c023; r.r1c3 = n *  d3r012c023;
		r.r2c0 = n *  d3r123c013; r.r2c1 = n * -d3r023c013; r.r2c2 = n *  d3r013c013; r.r2c3 = n * -d3r012c013;
		r.r3c0 = n * -d3r123c012; r.r3c1 = n *  d3r023c012; r.r3c2 = n * -d3r013c012; r.r3c3 = n *  d3r012c012;
		// @formatter:on
		return true;
	}

	public static Mat4.Mutable set(Mat4.Mutable r, Mat4Access a) {
		// @formatter:off
		r.r0c0 = a.r0c0(); r.r0c1 = a.r0c1(); r.r0c2 = a.r0c2(); r.r0c3 = a.r0c3();
		r.r1c0 = a.r1c0(); r.r1c1 = a.r1c1(); r.r1c2 = a.r1c2(); r.r1c3 = a.r1c3();
		r.r2c0 = a.r2c0(); r.r2c1 = a.r2c1(); r.r2c2 = a.r2c2(); r.r2c3 = a.r2c3();
		r.r3c0 = a.r3c0(); r.r3c1 = a.r3c1(); r.r3c2 = a.r3c2(); r.r3c3 = a.r3c3();
		// @formatter:on
		return r;
	}

	public static Mat4.Mutable setIdentity(Mat4.Mutable r) {
		return setScale(r, 1);
	}

	public static Mat4.Mutable setRotation(Mat4.Mutable r, Quat a) {
		setIdentity(r);

		final var ii = 2 * a.i * a.i;
		final var jj = 2 * a.j * a.j;
		final var kk = 2 * a.k * a.k;
		final var ij = 2 * a.i * a.j;
		final var jk = 2 * a.j * a.k;
		final var ki = 2 * a.k * a.i;
		final var iw = 2 * a.i * a.w;
		final var jw = 2 * a.j * a.w;
		final var kw = 2 * a.k * a.w;

		r.r0c0 = 1 - jj - kk;
		r.r0c1 = ij - kw;
		r.r0c2 = ki + jw;
		r.r1c0 = ij + kw;
		r.r1c1 = 1 - kk - ii;
		r.r1c2 = jk - iw;
		r.r2c0 = ki - jw;
		r.r2c1 = jk + iw;
		r.r2c2 = 1 - ii - jj;
		r.r3c3 = 1;

		return r;
	}

	public static Mat4.Mutable setScale(Mat4.Mutable r, Vec4Access n) {
		// @formatter:off
		r.r0c0 = n.x(); r.r0c1 =     0; r.r0c2 =     0; r.r0c3 =     0;
		r.r1c0 =     0; r.r1c1 = n.y(); r.r1c2 =     0; r.r1c3 =     0;
		r.r2c0 =     0; r.r2c1 =     0; r.r2c2 = n.z(); r.r2c3 =     0;
		r.r3c0 =     0; r.r3c1 =     0; r.r3c2 =     0; r.r3c3 = n.w();
		// @formatter:on
		return r;
	}

	public static Mat4.Mutable setScale(Mat4.Mutable r, Vec3Access n) {
		// @formatter:off
		r.r0c0 = n.x(); r.r0c1 =     0; r.r0c2 =     0; r.r0c3 =   0;
		r.r1c0 =     0; r.r1c1 = n.y(); r.r1c2 =     0; r.r1c3 =   0;
		r.r2c0 =     0; r.r2c1 =     0; r.r2c2 = n.z(); r.r2c3 =   0;
		r.r3c0 =     0; r.r3c1 =     0; r.r3c2 =     0; r.r3c3 = 1.0;
		// @formatter:on
		return r;
	}

	public static Mat4.Mutable setScale(Mat4.Mutable r, double n) {
		// @formatter:off
		r.r0c0 = n; r.r0c1 = 0; r.r0c2 = 0; r.r0c3 = 0;
		r.r1c0 = 0; r.r1c1 = n; r.r1c2 = 0; r.r1c3 = 0;
		r.r2c0 = 0; r.r2c1 = 0; r.r2c2 = n; r.r2c3 = 0;
		r.r3c0 = 0; r.r3c1 = 0; r.r3c2 = 0; r.r3c3 = 1;
		// @formatter:on
		return r;
	}

	public static Mat4.Mutable setPerspectiveProjection(Mat4.Mutable r, double fovRad, double aspectRatio, double near,
			double far) {
		setIdentity(r);
		final var f = 1.0 / Math.tan(fovRad / 2.0);
		r.r0c0 = f / aspectRatio;
		r.r1c1 = f;
		r.r2c2 = (far + near) / (near - far);
		r.r3c2 = -1.0;
		r.r2c3 = 2.0 * far * near / (near - far);
		return r;
	}

	public static Mat4.Mutable setOrthographicProjection(Mat4.Mutable r,
			double minX, double maxX,
			double minY, double maxY,
			double minZ, double maxZ) {
		setIdentity(r);
		final var x = maxX - minX;
		final var y = minY - maxY;
		final var z = maxZ - minZ;
		r.r0c0 = 2.0 / x;
		r.r1c1 = 2.0 / y;
		r.r2c2 = -2.0 / z;
		r.r0c3 = -(maxX + minX) / x;
		r.r1c3 = -(minY + maxY) / y;
		r.r2c3 = -(maxZ + minZ) / z;
		r.r3c3 = 1.0;
		return r;
	}

	public static Mat4.Mutable setBases(Mat4.Mutable r, Vec3Access x, Vec3Access y, Vec3Access z, Vec3Access pos) {
		// @formatter:off
		r.r0c0 = x.x(); r.r0c1 = y.x(); r.r0c2 = z.x(); r.r0c3 = pos.x();
		r.r1c0 = x.y(); r.r1c1 = y.y(); r.r1c2 = z.y(); r.r1c3 = pos.y();
		r.r2c0 = x.z(); r.r2c1 = y.z(); r.r2c2 = z.z(); r.r2c3 = pos.z();
		r.r3c0 =     0; r.r3c1 =     0; r.r3c2 =     0; r.r3c3 =       1;
		// @formatter:on
		return r;
	}

	public static Mat4.Mutable setRotationTranslation(Mat4.Mutable r, Quat rotation, Vec3Access pos) {
		setIdentity(r);
		mulRotation(r, r, rotation);
		mulTranslation(r, r, pos);
		return r;
	}

	public static Vec3.Mutable basisX(Vec3.Mutable r, Mat4Access a) {
		r.x = a.r0c0();
		r.y = a.r1c0();
		r.z = a.r2c0();
		return r;
	}

	public static Vec3.Mutable basisY(Vec3.Mutable r, Mat4Access a) {
		r.x = a.r0c1();
		r.y = a.r1c1();
		r.z = a.r2c1();
		return r;
	}

	public static Vec3.Mutable basisZ(Vec3.Mutable r, Mat4Access a) {
		r.x = a.r0c2();
		r.y = a.r1c2();
		r.z = a.r2c2();
		return r;
	}

	public static Vec3.Mutable storeTranslation(Vec3.Mutable r, Mat4Access a) {
		r.x = a.r0c3();
		r.y = a.r1c3();
		r.z = a.r2c3();
		return r;
	}

	public Mat4 mul(double scalar) {
		return Mat4.mul(scalar, this);
	}

	public Mat4 mul(Mat4 rhs) {
		return Mat4.mul(this, rhs);
	}

	public Vec4 mul(Vec4Access rhs) {
		return Mat4.mul(this, rhs);
	}

	public Vec3 mul(Vec3Access rhs, double w) {
		return Mat4.mul(this, rhs, w);
	}

	public Mat4 prependTransform(Mat4 other) {
		return mul(this, other);
	}

	public Mat4 appendTransform(Mat4 other) {
		return mul(other, this);
	}

	public double trace() {
		return this.r0c0 + this.r1c1 + this.r2c2 + this.r3c3;
	}

	public Maybe<Mat4> inverse() {
		// unique combinations of 2x2 determinants (d2)
		final var d2r12c23 = r1c2 * r2c3 - r1c3 * r2c2;
		final var d2r12c12 = r1c1 * r2c2 - r1c2 * r2c1;
		final var d2r12c01 = r1c0 * r2c1 - r1c1 * r2c0;
		final var d2r12c13 = r1c1 * r2c3 - r1c3 * r2c1;
		final var d2r12c02 = r1c0 * r2c2 - r1c2 * r2c0;
		final var d2r12c03 = r1c0 * r2c3 - r1c3 * r2c0;
		final var d2r23c23 = r2c2 * r3c3 - r2c3 * r3c2;
		final var d2r23c12 = r2c1 * r3c2 - r2c2 * r3c1;
		final var d2r23c01 = r2c0 * r3c1 - r2c1 * r3c0;
		final var d2r23c13 = r2c1 * r3c3 - r2c3 * r3c1;
		final var d2r23c02 = r2c0 * r3c2 - r2c2 * r3c0;
		final var d2r23c03 = r2c0 * r3c3 - r2c3 * r3c0;
		final var d2r13c23 = r1c2 * r3c3 - r1c3 * r3c2;
		final var d2r13c12 = r1c1 * r3c2 - r1c2 * r3c1;
		final var d2r13c01 = r1c0 * r3c1 - r1c1 * r3c0;
		final var d2r13c13 = r1c1 * r3c3 - r1c3 * r3c1;
		final var d2r13c02 = r1c0 * r3c2 - r1c2 * r3c0;
		final var d2r13c03 = r1c0 * r3c3 - r1c3 * r3c0;

		// unique combinations of 3x3 determinants (d3)
		// This is also essentially the matrix of minors for this matrix.
		final var d3r123c123 = r1c1 * d2r23c23 - r1c2 * d2r23c13 + r1c3 * d2r23c12;
		final var d3r123c023 = r1c0 * d2r23c23 - r1c2 * d2r23c03 + r1c3 * d2r23c02;
		final var d3r123c013 = r1c0 * d2r23c13 - r1c1 * d2r23c03 + r1c3 * d2r23c01;
		final var d3r123c012 = r1c0 * d2r23c12 - r1c1 * d2r23c02 + r1c2 * d2r23c01;
		final var d3r023c123 = r0c1 * d2r23c23 - r0c2 * d2r23c13 + r0c3 * d2r23c12;
		final var d3r023c023 = r0c0 * d2r23c23 - r0c2 * d2r23c03 + r0c3 * d2r23c02;
		final var d3r023c013 = r0c0 * d2r23c13 - r0c1 * d2r23c03 + r0c3 * d2r23c01;
		final var d3r023c012 = r0c0 * d2r23c12 - r0c1 * d2r23c02 + r0c2 * d2r23c01;
		final var d3r013c123 = r0c1 * d2r13c23 - r0c2 * d2r13c13 + r0c3 * d2r13c12;
		final var d3r013c023 = r0c0 * d2r13c23 - r0c2 * d2r13c03 + r0c3 * d2r13c02;
		final var d3r013c013 = r0c0 * d2r13c13 - r0c1 * d2r13c03 + r0c3 * d2r13c01;
		final var d3r013c012 = r0c0 * d2r13c12 - r0c1 * d2r13c02 + r0c2 * d2r13c01;
		final var d3r012c123 = r0c1 * d2r12c23 - r0c2 * d2r12c13 + r0c3 * d2r12c12;
		final var d3r012c023 = r0c0 * d2r12c23 - r0c2 * d2r12c03 + r0c3 * d2r12c02;
		final var d3r012c013 = r0c0 * d2r12c13 - r0c1 * d2r12c03 + r0c3 * d2r12c01;
		final var d3r012c012 = r0c0 * d2r12c12 - r0c1 * d2r12c02 + r0c2 * d2r12c01;

		// final var m = asMinecraft();
		// return m.invert() ? Option.some(fromMinecraft(m)) : Option.none();

		final var det = r0c0 * d3r123c123 - r0c1 * d3r123c023 + r0c2 * d3r123c013 - r0c3 * d3r123c012;
		if (Math.abs(det) < 1e-8)
			return Maybe.none();

		final var invdet = 1.0 / det;

		// @formatter:off
		return Maybe.some(new Mat4(
			invdet *  d3r123c123, invdet * -d3r023c123, invdet *  d3r013c123, invdet * -d3r012c123,
			invdet * -d3r123c023, invdet *  d3r023c023, invdet * -d3r013c023, invdet *  d3r012c023,
			invdet *  d3r123c013, invdet * -d3r023c013, invdet *  d3r013c013, invdet * -d3r012c013,
			invdet * -d3r123c012, invdet *  d3r023c012, invdet * -d3r013c012, invdet *  d3r012c012
		));
		// @formatter:on
	}

	public static Mat4 from(Matrix4f m) {
		return new Mat4(
				m.m00, m.m01, m.m02, m.m03,
				m.m10, m.m11, m.m12, m.m13,
				m.m20, m.m21, m.m22, m.m23,
				m.m30, m.m31, m.m32, m.m33);
	}

	public static Mat4 from(Mutable m) {
		return new Mat4(
				m.r0c0, m.r0c1, m.r0c2, m.r0c3,
				m.r1c0, m.r1c1, m.r1c2, m.r1c3,
				m.r2c0, m.r2c1, m.r2c2, m.r2c3,
				m.r3c0, m.r3c1, m.r3c2, m.r3c3);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Mat4Access other) {
			boolean eq = true;
			eq &= this.r0c0 == other.r0c0();
			eq &= this.r0c1 == other.r0c1();
			eq &= this.r0c2 == other.r0c2();
			eq &= this.r0c3 == other.r0c3();
			eq &= this.r1c0 == other.r1c0();
			eq &= this.r1c1 == other.r1c1();
			eq &= this.r1c2 == other.r1c2();
			eq &= this.r1c3 == other.r1c3();
			eq &= this.r2c0 == other.r2c0();
			eq &= this.r2c1 == other.r2c1();
			eq &= this.r2c2 == other.r2c2();
			eq &= this.r2c3 == other.r2c3();
			eq &= this.r3c0 == other.r3c0();
			eq &= this.r3c1 == other.r3c1();
			eq &= this.r3c2 == other.r3c2();
			eq &= this.r3c3 == other.r3c3();
			return eq;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendDouble(this.r0c0);
		hasher.appendDouble(this.r0c1);
		hasher.appendDouble(this.r0c2);
		hasher.appendDouble(this.r0c3);
		hasher.appendDouble(this.r1c0);
		hasher.appendDouble(this.r1c1);
		hasher.appendDouble(this.r1c2);
		hasher.appendDouble(this.r1c3);
		hasher.appendDouble(this.r2c0);
		hasher.appendDouble(this.r2c1);
		hasher.appendDouble(this.r2c2);
		hasher.appendDouble(this.r2c3);
		hasher.appendDouble(this.r3c0);
		hasher.appendDouble(this.r3c1);
		hasher.appendDouble(this.r3c2);
		hasher.appendDouble(this.r3c3);
	}

	public static final class Mutable implements Hashable, Mat4Access {
		public double r0c0, r0c1, r0c2, r0c3;
		public double r1c0, r1c1, r1c2, r1c3;
		public double r2c0, r2c1, r2c2, r2c3;
		public double r3c0, r3c1, r3c2, r3c3;

		// @formatter:off
		@Override public final double r0c0() {return r0c0;}
		@Override public final double r0c1() {return r0c1;}
		@Override public final double r0c2() {return r0c2;}
		@Override public final double r0c3() {return r0c3;}
		@Override public final double r1c0() {return r1c0;}
		@Override public final double r1c1() {return r1c1;}
		@Override public final double r1c2() {return r1c2;}
		@Override public final double r1c3() {return r1c3;}
		@Override public final double r2c0() {return r2c0;}
		@Override public final double r2c1() {return r2c1;}
		@Override public final double r2c2() {return r2c2;}
		@Override public final double r2c3() {return r2c3;}
		@Override public final double r3c0() {return r3c0;}
		@Override public final double r3c1() {return r3c1;}
		@Override public final double r3c2() {return r3c2;}
		@Override public final double r3c3() {return r3c3;}
		// @formatter:on

		public Mutable mulAssign(Mat4Access rhs) {
			return Mat4.mul(this, this, rhs);
		}

		public Mutable prependTransform(Mat4Access other) {
			return Mat4.mul(this, this, other);
		}

		public Mutable appendTransform(Mat4Access other) {
			return Mat4.mul(this, other, this);
		}

		public Mutable prependRotation(Quat rotation) {
			return Mat4.mulRotation(this, this, rotation);
		}

		public Mutable appendRotation(Quat rotation) {
			return Mat4.mulRotation(this, rotation, this);
		}

		public Mutable prependTranslation(Vec3Access vec) {
			return Mat4.mulTranslation(this, this, vec);
		}

		public Mutable appendTranslation(Vec3Access vec) {
			return Mat4.mulTranslation(this, vec, this);
		}

		public Mutable prependScale(double scale) {
			return Mat4.mulScale(this, this, scale);
		}

		public Mutable appendScale(double scale) {
			return Mat4.mulScale(this, scale, this);
		}

		public boolean invert() {
			return Mat4.invert(this, this);
		}

		public Mutable loadIdentity() {
			return loadScale(1);
		}

		public Mutable copy() {
			return new Mutable().loadFrom(this);
		}

		public Mutable loadFrom(Mat4Access m) {
			return set(this, m);
		}

		public Mutable loadRotation(Quat a) {
			loadIdentity();

			final var ii = 2 * a.i * a.i;
			final var jj = 2 * a.j * a.j;
			final var kk = 2 * a.k * a.k;
			final var ij = 2 * a.i * a.j;
			final var jk = 2 * a.j * a.k;
			final var ki = 2 * a.k * a.i;
			final var iw = 2 * a.i * a.w;
			final var jw = 2 * a.j * a.w;
			final var kw = 2 * a.k * a.w;

			this.r0c0 = 1 - jj - kk;
			this.r0c1 = ij - kw;
			this.r0c2 = ki + jw;
			this.r1c0 = ij + kw;
			this.r1c1 = 1 - kk - ii;
			this.r1c2 = jk - iw;
			this.r2c0 = ki - jw;
			this.r2c1 = jk + iw;
			this.r2c2 = 1 - ii - jj;
			this.r3c3 = 1;

			return this;
		}

		public Mutable loadScale(Vec4Access n) {
			return setScale(this, n);
		}

		public Mutable loadScale(Vec3Access n) {
			return setScale(this, n);
		}

		public Mutable loadScale(double n) {
			return setScale(this, n);
		}

		public Mutable loadPerspectiveProjection(double fovRad, double aspectRatio, double near, double far) {
			return setPerspectiveProjection(this, fovRad, aspectRatio, near, far);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Mat4Access other) {
				boolean eq = true;
				eq &= this.r0c0 == other.r0c0();
				eq &= this.r0c1 == other.r0c1();
				eq &= this.r0c2 == other.r0c2();
				eq &= this.r0c3 == other.r0c3();
				eq &= this.r1c0 == other.r1c0();
				eq &= this.r1c1 == other.r1c1();
				eq &= this.r1c2 == other.r1c2();
				eq &= this.r1c3 == other.r1c3();
				eq &= this.r2c0 == other.r2c0();
				eq &= this.r2c1 == other.r2c1();
				eq &= this.r2c2 == other.r2c2();
				eq &= this.r2c3 == other.r2c3();
				eq &= this.r3c0 == other.r3c0();
				eq &= this.r3c1 == other.r3c1();
				eq &= this.r3c2 == other.r3c2();
				eq &= this.r3c3 == other.r3c3();
				return eq;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return FastHasher.hashToInt(this);
		}

		@Override
		public void appendHash(Hasher hasher) {
			hasher.appendDouble(this.r0c0);
			hasher.appendDouble(this.r0c1);
			hasher.appendDouble(this.r0c2);
			hasher.appendDouble(this.r0c3);
			hasher.appendDouble(this.r1c0);
			hasher.appendDouble(this.r1c1);
			hasher.appendDouble(this.r1c2);
			hasher.appendDouble(this.r1c3);
			hasher.appendDouble(this.r2c0);
			hasher.appendDouble(this.r2c1);
			hasher.appendDouble(this.r2c2);
			hasher.appendDouble(this.r2c3);
			hasher.appendDouble(this.r3c0);
			hasher.appendDouble(this.r3c1);
			hasher.appendDouble(this.r3c2);
			hasher.appendDouble(this.r3c3);
		}
	}

	// #region Formatting
	public String toString() {
		final var sb = new StringBuilder();
		final var r0c0 = formatNumber(this.r0c0);
		final var r0c1 = formatNumber(this.r0c1);
		final var r0c2 = formatNumber(this.r0c2);
		final var r0c3 = formatNumber(this.r0c3);
		final var r1c0 = formatNumber(this.r1c0);
		final var r1c1 = formatNumber(this.r1c1);
		final var r1c2 = formatNumber(this.r1c2);
		final var r1c3 = formatNumber(this.r1c3);
		final var r2c0 = formatNumber(this.r2c0);
		final var r2c1 = formatNumber(this.r2c1);
		final var r2c2 = formatNumber(this.r2c2);
		final var r2c3 = formatNumber(this.r2c3);
		final var r3c0 = formatNumber(this.r3c0);
		final var r3c1 = formatNumber(this.r3c1);
		final var r3c2 = formatNumber(this.r3c2);
		final var r3c3 = formatNumber(this.r3c3);
		final var c0 = FormattedColumnInfo.from(r0c0, r1c0, r2c0, r3c0);
		final var c1 = FormattedColumnInfo.from(r0c1, r1c1, r2c1, r3c1);
		final var c2 = FormattedColumnInfo.from(r0c2, r1c2, r2c2, r3c2);
		final var c3 = FormattedColumnInfo.from(r0c3, r1c3, r2c3, r3c3);

		sb.append("Mat4:\n⎡");
		alignDecimal(sb, r0c0, c0);
		sb.append(" ");
		alignDecimal(sb, r0c1, c1);
		sb.append(" ");
		alignDecimal(sb, r0c2, c2);
		sb.append(" ");
		alignDecimal(sb, r0c3, c3);
		sb.append("⎤\n⎢");
		alignDecimal(sb, r1c0, c0);
		sb.append(" ");
		alignDecimal(sb, r1c1, c1);
		sb.append(" ");
		alignDecimal(sb, r1c2, c2);
		sb.append(" ");
		alignDecimal(sb, r1c3, c3);
		sb.append("⎥\n⎢");
		alignDecimal(sb, r2c0, c0);
		sb.append(" ");
		alignDecimal(sb, r2c1, c1);
		sb.append(" ");
		alignDecimal(sb, r2c2, c2);
		sb.append(" ");
		alignDecimal(sb, r2c3, c3);
		sb.append("⎥\n⎣");
		alignDecimal(sb, r3c0, c0);
		sb.append(" ");
		alignDecimal(sb, r3c1, c1);
		sb.append(" ");
		alignDecimal(sb, r3c2, c2);
		sb.append(" ");
		alignDecimal(sb, r3c3, c3);
		sb.append("⎦\n");

		return sb.toString();
	}

	private static void alignDecimal(StringBuilder sb, FormattedNumberPair n, FormattedColumnInfo col) {
		final var lpad = col.lpad(n);
		for (int i = 0; i < lpad; ++i)
			sb.append(' ');
		sb.append(n.whole);
		if (n.fractional != null) {
			sb.append('.');
			sb.append(n.fractional);
		}
		final var rpad = col.rpad(n);
		for (int i = 0; i < rpad; ++i)
			sb.append(' ');
	}

	private static FormattedNumberPair formatNumber(double n) {
		var str = String.format("%.4f", n);
		// broken for locales that use "," as a separator.
		if (!str.contains("."))
			return new FormattedNumberPair(str, null);
		final var decimalIndex = str.indexOf(".");

		final var whole = str.substring(0, decimalIndex);
		var frac = str.substring(decimalIndex + 1, str.length());
		if (frac.isEmpty())
			return new FormattedNumberPair(whole, null);

		int lastNonzero = -1;
		for (int i = 0; i < frac.length(); ++i) {
			if (frac.charAt(i) != '0')
				lastNonzero = i;
		}
		if (lastNonzero == -1)
			return new FormattedNumberPair(whole, null);
		frac = frac.substring(0, lastNonzero + 1);
		return new FormattedNumberPair(whole, frac);
	}

	private record FormattedColumnInfo(int wholeMax, int fractionalMax, boolean anyDecimal) {
		public static FormattedColumnInfo from(FormattedNumberPair... rows) {
			int whole = 0, frac = 0;
			boolean hasDecimal = false;
			for (final var row : rows) {
				whole = Math.max(whole, row.whole.length());
				frac = Math.max(frac, row.fractional != null ? row.fractional.length() : 0);
				hasDecimal |= row.hasDecimal();
			}
			return new FormattedColumnInfo(whole, frac, hasDecimal);
		}

		public int lpad(FormattedNumberPair n) {
			return this.wholeMax - n.whole.length();
		}

		public int rpad(FormattedNumberPair n) {
			return n.hasDecimal()
					? this.fractionalMax - n.fractional.length()
					: this.fractionalMax + (this.anyDecimal ? 1 : 0);
		}
	}

	private record FormattedNumberPair(String whole, String fractional) {
		public boolean hasDecimal() {
			return this.fractional != null;
		}
	}
	// #endregion

}
