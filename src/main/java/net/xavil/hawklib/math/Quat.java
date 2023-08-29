package net.xavil.hawklib.math;

import com.mojang.math.Quaternion;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec3;

public final class Quat implements Hashable {

	public static final Quat ZERO = new Quat(0, 0, 0, 0);
	public static final Quat IDENTITY = new Quat(1, 0, 0, 0);

	@SuppressWarnings("null")
	public static final Codec<Quat> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.DOUBLE.fieldOf("w").forGetter(v -> v.w),
			Codec.DOUBLE.fieldOf("i").forGetter(v -> v.i),
			Codec.DOUBLE.fieldOf("j").forGetter(v -> v.j),
			Codec.DOUBLE.fieldOf("k").forGetter(v -> v.k))
			.apply(inst, Quat::new));

	public final double w, i, j, k;

	private Quat(double w, double i, double j, double k) {
		this.w = w;
		this.i = i;
		this.j = j;
		this.k = k;
	}

	public static Quat fromMinecraft(Quaternion quat) {
		return new Quat(quat.r(), quat.i(), quat.j(), quat.k());
	}

	public Quaternion toMinecraft() {
		return new Quaternion((float) i, (float) j, (float) k, (float) w);
	}

	public static Quat from(double w, double i, double j, double k) {
		return new Quat(w, i, j, k);
	}

	public static Quat fromIjk(Vec3 ijk) {
		return new Quat(0, ijk.x, ijk.y, ijk.z);
	}

	// https://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/
	public static Quat fromOrthonormalBasis(Vec3 a, Vec3 b, Vec3 c) {
		double tr = a.x + b.y + c.z;

		double qw = 0, qx = 0, qy = 0, qz = 0;
		if (tr > 0) {
			double S = Math.sqrt(tr + 1.0) * 2; // S=4*qw
			qw = 0.25 * S;
			qx = (c.y - b.z) / S;
			qy = (a.z - c.x) / S;
			qz = (b.x - a.y) / S;
		} else if ((a.x > b.y) & (a.x > c.z)) {
			double S = Math.sqrt(1.0 + a.x - b.y - c.z) * 2; // S=4*qx
			qw = (c.y - b.z) / S;
			qx = 0.25 * S;
			qy = (a.y + b.x) / S;
			qz = (a.z + c.x) / S;
		} else if (b.y > c.z) {
			double S = Math.sqrt(1.0 + b.y - a.x - c.z) * 2; // S=4*qy
			qw = (a.z - c.x) / S;
			qx = (a.y + b.x) / S;
			qy = 0.25 * S;
			qz = (b.z + c.y) / S;
		} else {
			double S = Math.sqrt(1.0 + c.z - a.x - b.y) * 2; // S=4*qz
			qw = (b.x - a.y) / S;
			qx = (a.z + c.x) / S;
			qy = (b.z + c.y) / S;
			qz = 0.25 * S;
		}

		return new Quat(qw, qx, qy, qz);
	}

	public static Quat fromAffineMatrix(Mat4 m) {
		return fromOrthonormalBasis(m.basisX(), m.basisY(), m.basisZ());
	}

	public static Quat axisAngle(Vec3 dir, double angle) {
		var qx = dir.x * Math.sin(angle / 2);
		var qy = dir.y * Math.sin(angle / 2);
		var qz = dir.z * Math.sin(angle / 2);
		var qw = Math.cos(angle / 2);
		return new Quat(qw, qx, qy, qz);
	}

	public Vec3 axisAngle() {
		final var halfAngle = Math.acos(this.w);
		final var h = 1 / Mth.fastInvSqrt(1 - this.w * this.w);
		final var n = h * 2 * halfAngle;
		return new Vec3(n * i, n * j, n * k);
	}

	// returns a quaternion that applies `rhs` first, then `this`
	public Quat hamiltonProduct(Quat rhs) {
		final double l0 = this.w, l1 = this.i, l2 = this.j, l3 = this.k;
		final double r0 = rhs.w, r1 = rhs.i, r2 = rhs.j, r3 = rhs.k;
		final double w = l0 * r0 - l1 * r1 - l2 * r2 - l3 * r3;
		final double i = l0 * r1 + l1 * r0 + l2 * r3 - l3 * r2;
		final double j = l0 * r2 - l1 * r3 + l2 * r0 + l3 * r1;
		final double k = l0 * r3 + l1 * r2 - l2 * r1 + l3 * r0;
		return new Quat(w, i, j, k);
	}

	public Quat conjugate() {
		return new Quat(w, -i, -j, -k);
	}

	public Quat inverse() {
		var len2 = w * w + i * i + j * j + k * k;
		return new Quat(w / len2, -i / len2, -j / len2, -k / len2);
	}

	public Quat normalize() {
		final var f = Mth.fastInvSqrt(w * w + i * i + j * j + k * k);
		return new Quat(f * w, f * i, f * j, f * k);
	}

	public Vec3 ijk() {
		return new Vec3(i, j, k);
	}

	public Vec3 transform(Vec3 vec) {
		final var f = Mth.fastInvSqrt(w * w + i * i + j * j + k * k);
		final double normw = f * w, normi = f * i, normj = f * j, normk = f * k;

		final double l00 = normw, l01 =  normi, l02 =  normj, l03 =  normk;
		final double r00 =     0, r01 =  vec.x, r02 =  vec.y, r03 =  vec.z;
		final double r10 = normw, r11 = -normi, r12 = -normj, r13 = -normk;

		final double w0 = l00 * r00 - l01 * r01 - l02 * r02 - l03 * r03;
		final double i0 = l00 * r01 + l01 * r00 + l02 * r03 - l03 * r02;
		final double j0 = l00 * r02 - l01 * r03 + l02 * r00 + l03 * r01;
		final double k0 = l00 * r03 + l01 * r02 - l02 * r01 + l03 * r00;
		final double i1 =  w0 * r11 +  i0 * r10 +  j0 * r13 -  k0 * r12;
		final double j1 =  w0 * r12 -  i0 * r13 +  j0 * r10 +  k0 * r11;
		final double k1 =  w0 * r13 +  i0 * r12 -  j0 * r11 +  k0 * r10;

		return new Vec3(i1, j1, k1);
	}

	public static Vec3.Mutable transform(Vec3.Mutable out, Vec3.Mutable in, Quat q) {
		final var f = Mth.fastInvSqrt(q.w * q.w + q.i * q.i + q.j * q.j + q.k * q.k);
		final double normw = f * q.w, normi = f * q.i, normj = f * q.j, normk = f * q.k;

		final double l00 = normw, l01 =  normi, l02 =  normj, l03 =  normk;
		final double r00 =     0, r01 =  in.x,  r02 =  in.y,  r03 =  in.z;
		final double r10 = normw, r11 = -normi, r12 = -normj, r13 = -normk;

		final double w0 = l00 * r00 - l01 * r01 - l02 * r02 - l03 * r03;
		final double i0 = l00 * r01 + l01 * r00 + l02 * r03 - l03 * r02;
		final double j0 = l00 * r02 - l01 * r03 + l02 * r00 + l03 * r01;
		final double k0 = l00 * r03 + l01 * r02 - l02 * r01 + l03 * r00;
		final double i1 =  w0 * r11 +  i0 * r10 +  j0 * r13 -  k0 * r12;
		final double j1 =  w0 * r12 -  i0 * r13 +  j0 * r10 +  k0 * r11;
		final double k1 =  w0 * r13 +  i0 * r12 -  j0 * r11 +  k0 * r10;

		return Vec3.set(out, i1, j1, k1);
	}

	@Override
	public String toString() {
		return "Quat[" + w + " + " + i + "i + " + j + "j + " + k + "k]";
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Quat other) {
			return this.w == other.w && this.i == other.i && this.j == other.j && this.k == other.k;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendDouble(this.w).appendDouble(this.i).appendDouble(this.j).appendDouble(this.k);
	}

}
