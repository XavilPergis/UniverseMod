package net.xavil.util.math;

import com.mojang.math.Matrix4f;

import net.xavil.util.FastHasher;
import net.xavil.util.Hashable;
import net.xavil.util.Hasher;
import net.xavil.util.Option;

public final class Mat4 implements Hashable {

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

	public static Mat4 fromRows(Vec4 r0, Vec4 r1, Vec4 r2, Vec4 r3) {
		return new Mat4(
				r0.x, r0.y, r0.z, r0.w,
				r1.x, r1.y, r1.z, r1.w,
				r2.x, r2.y, r2.z, r2.w,
				r3.x, r3.y, r3.z, r3.w);
	}

	public static Mat4 fromColumns(Vec4 c0, Vec4 c1, Vec4 c2, Vec4 c3) {
		return new Mat4(
				c0.x, c1.x, c2.x, c3.x,
				c0.y, c1.y, c2.y, c3.y,
				c0.z, c1.z, c2.z, c3.z,
				c0.w, c1.w, c2.w, c3.w);
	}

	public static Mat4 fromBases(Vec3 x, Vec3 y, Vec3 z, Vec3 pos) {
		return fromColumns(x.xyz0(), y.xyz0(), z.xyz0(), pos.xyz1());
	}

	public static Mat4 diagonal(Vec4 diag) {
		return new Mat4(
				diag.x, 0.0, 0.0, 0.0,
				0.0, diag.y, 0.0, 0.0,
				0.0, 0.0, diag.z, 0.0,
				0.0, 0.0, 0.0, diag.w);
	}

	public static Mat4 scale(double n) {
		return diagonal(Vec4.broadcast(n));
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

	// @formatter:off
	public Vec4 c0() { return new Vec4(this.r0c0, this.r1c0, this.r2c0, this.r3c0); }
	public Vec4 c1() { return new Vec4(this.r0c1, this.r1c1, this.r2c1, this.r3c1); }
	public Vec4 c2() { return new Vec4(this.r0c2, this.r1c2, this.r2c2, this.r3c2); }
	public Vec4 c3() { return new Vec4(this.r0c3, this.r1c3, this.r2c3, this.r3c3); }
	public Vec4 r0() { return new Vec4(this.r0c0, this.r0c1, this.r0c2, this.r0c3); }
	public Vec4 r1() { return new Vec4(this.r1c0, this.r1c1, this.r1c2, this.r1c3); }
	public Vec4 r2() { return new Vec4(this.r2c0, this.r2c1, this.r2c2, this.r2c3); }
	public Vec4 r3() { return new Vec4(this.r3c0, this.r3c1, this.r3c2, this.r3c3); }

	// conveniences for `cN().xyz()`
	// these are only meaningful if `r3()` is `[0 0 0 1]` (ie, acts like a 3x4 matrix)
	public Vec3 basisX()      { return new Vec3(this.r0c0, this.r1c0, this.r2c0); }
	public Vec3 basisY()      { return new Vec3(this.r0c1, this.r1c1, this.r2c1); }
	public Vec3 basisZ()      { return new Vec3(this.r0c2, this.r1c2, this.r2c2); }
	public Vec3 translation() { return new Vec3(this.r0c3, this.r1c3, this.r2c3); }
	// @formatter:on

	private static Mat4 mul(Mat4 a, Mat4 b) {
		final var r0c0 = (a.r0c0 * b.r0c0) + (a.r0c1 * b.r1c0) + (a.r0c2 * b.r2c0) + (a.r0c3 * b.r3c0);
		final var r0c1 = (a.r0c0 * b.r0c1) + (a.r0c1 * b.r1c1) + (a.r0c2 * b.r2c1) + (a.r0c3 * b.r3c1);
		final var r0c2 = (a.r0c0 * b.r0c2) + (a.r0c1 * b.r1c2) + (a.r0c2 * b.r2c2) + (a.r0c3 * b.r3c2);
		final var r0c3 = (a.r0c0 * b.r0c3) + (a.r0c1 * b.r1c3) + (a.r0c2 * b.r2c3) + (a.r0c3 * b.r3c3);
		final var r1c0 = (a.r1c0 * b.r0c0) + (a.r1c1 * b.r1c0) + (a.r1c2 * b.r2c0) + (a.r1c3 * b.r3c0);
		final var r1c1 = (a.r1c0 * b.r0c1) + (a.r1c1 * b.r1c1) + (a.r1c2 * b.r2c1) + (a.r1c3 * b.r3c1);
		final var r1c2 = (a.r1c0 * b.r0c2) + (a.r1c1 * b.r1c2) + (a.r1c2 * b.r2c2) + (a.r1c3 * b.r3c2);
		final var r1c3 = (a.r1c0 * b.r0c3) + (a.r1c1 * b.r1c3) + (a.r1c2 * b.r2c3) + (a.r1c3 * b.r3c3);
		final var r2c0 = (a.r2c0 * b.r0c0) + (a.r2c1 * b.r1c0) + (a.r2c2 * b.r2c0) + (a.r2c3 * b.r3c0);
		final var r2c1 = (a.r2c0 * b.r0c1) + (a.r2c1 * b.r1c1) + (a.r2c2 * b.r2c1) + (a.r2c3 * b.r3c1);
		final var r2c2 = (a.r2c0 * b.r0c2) + (a.r2c1 * b.r1c2) + (a.r2c2 * b.r2c2) + (a.r2c3 * b.r3c2);
		final var r2c3 = (a.r2c0 * b.r0c3) + (a.r2c1 * b.r1c3) + (a.r2c2 * b.r2c3) + (a.r2c3 * b.r3c3);
		final var r3c0 = (a.r3c0 * b.r0c0) + (a.r3c1 * b.r1c0) + (a.r3c2 * b.r2c0) + (a.r3c3 * b.r3c0);
		final var r3c1 = (a.r3c0 * b.r0c1) + (a.r3c1 * b.r1c1) + (a.r3c2 * b.r2c1) + (a.r3c3 * b.r3c1);
		final var r3c2 = (a.r3c0 * b.r0c2) + (a.r3c1 * b.r1c2) + (a.r3c2 * b.r2c2) + (a.r3c3 * b.r3c2);
		final var r3c3 = (a.r3c0 * b.r0c3) + (a.r3c1 * b.r1c3) + (a.r3c2 * b.r2c3) + (a.r3c3 * b.r3c3);
		return new Mat4(r0c0, r0c1, r0c2, r0c3, r1c0, r1c1, r1c2, r1c3, r2c0, r2c1, r2c2, r2c3, r3c0, r3c1, r3c2, r3c3);
	}

	private static Vec4 mul(Mat4 a, Vec4 b) {
		final var x = (a.r0c0 * b.x) + (a.r0c1 * b.y) + (a.r0c2 * b.z) + (a.r0c3 * b.w);
		final var y = (a.r1c0 * b.x) + (a.r1c1 * b.y) + (a.r1c2 * b.z) + (a.r1c3 * b.w);
		final var z = (a.r2c0 * b.x) + (a.r2c1 * b.y) + (a.r2c2 * b.z) + (a.r2c3 * b.w);
		final var w = (a.r3c0 * b.x) + (a.r3c1 * b.y) + (a.r3c2 * b.z) + (a.r3c3 * b.w);
		return Vec4.from(x, y, z, w);
	}

	private static Mat4 mul(double a, Mat4 b) {
		return new Mat4(
				a * b.r0c0, a * b.r0c1, a * b.r0c2, a * b.r0c3,
				a * b.r1c0, a * b.r1c1, a * b.r1c2, a * b.r1c3,
				a * b.r2c0, a * b.r2c1, a * b.r2c2, a * b.r2c3,
				a * b.r3c0, a * b.r3c1, a * b.r3c2, a * b.r3c3);
	}

	public Mat4 mul(double scalar) {
		return Mat4.mul(scalar, this);
	}

	public Mat4 mul(Mat4 rhs) {
		return Mat4.mul(this, rhs);
	}

	public Vec4 mul(Vec4 rhs) {
		return Mat4.mul(this, rhs);
	}

	public Mat4 applyAfter(Mat4 other) {
		return mul(this, other);
	}

	public Mat4 applyBefore(Mat4 other) {
		return mul(other, this);
	}

	public double trace() {
		return this.r0c0 + this.r1c1 + this.r2c2 + this.r3c3;
	}

	public Option<Mat4> inverse() {
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

		final var det = r0c0 * d3r123c123 - r0c1 * d3r123c023 + r0c2 * d3r123c013 - r0c3 * d3r123c012;
		if (Math.abs(det) < 1e-6)
			return Option.none();

		final var invdet = 1.0 / det;

		// @formatter:off
		return Option.some(new Mat4(
			invdet *  d3r123c123, invdet * -d3r023c123, invdet *  d3r013c123, invdet * -d3r012c123,
			invdet * -d3r123c023, invdet *  d3r023c023, invdet * -d3r013c023, invdet *  d3r012c023,
			invdet *  d3r123c013, invdet * -d3r023c013, invdet *  d3r013c013, invdet * -d3r012c013,
			invdet * -d3r123c012, invdet *  d3r023c012, invdet * -d3r013c012, invdet *  d3r012c012
		));
		// @formatter:on
	}

	public static Mat4 fromMinecraft(Matrix4f m) {
		return new Mat4(
				m.m00, m.m01, m.m02, m.m03,
				m.m10, m.m11, m.m12, m.m13,
				m.m20, m.m21, m.m22, m.m23,
				m.m30, m.m31, m.m32, m.m33);
	}

	public Matrix4f asMinecraft() {
		final var mat = new Matrix4f();
		storeMinecraft(mat);
		return mat;
	}

	public void storeMinecraft(Matrix4f mat) {
		mat.m00 = (float) this.r0c0;
		mat.m01 = (float) this.r0c1;
		mat.m02 = (float) this.r0c2;
		mat.m03 = (float) this.r0c3;
		mat.m10 = (float) this.r1c0;
		mat.m11 = (float) this.r1c1;
		mat.m12 = (float) this.r1c2;
		mat.m13 = (float) this.r1c3;
		mat.m20 = (float) this.r2c0;
		mat.m21 = (float) this.r2c1;
		mat.m22 = (float) this.r2c2;
		mat.m23 = (float) this.r2c3;
		mat.m30 = (float) this.r3c0;
		mat.m31 = (float) this.r3c1;
		mat.m32 = (float) this.r3c2;
		mat.m33 = (float) this.r3c3;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Mat4 other) {
			boolean eq = true;
			eq &= this.r0c0 == other.r0c0;
			eq &= this.r0c1 == other.r0c1;
			eq &= this.r0c2 == other.r0c2;
			eq &= this.r0c3 == other.r0c3;
			eq &= this.r1c0 == other.r1c0;
			eq &= this.r1c1 == other.r1c1;
			eq &= this.r1c2 == other.r1c2;
			eq &= this.r1c3 == other.r1c3;
			eq &= this.r2c0 == other.r2c0;
			eq &= this.r2c1 == other.r2c1;
			eq &= this.r2c2 == other.r2c2;
			eq &= this.r2c3 == other.r2c3;
			eq &= this.r3c0 == other.r3c0;
			eq &= this.r3c1 == other.r3c1;
			eq &= this.r3c2 == other.r3c2;
			eq &= this.r3c3 == other.r3c3;
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
