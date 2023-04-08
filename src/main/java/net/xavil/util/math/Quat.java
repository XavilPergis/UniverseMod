package net.xavil.util.math;

import com.mojang.math.Quaternion;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;
import net.xavil.util.FastHasher;
import net.xavil.util.Hashable;
import net.xavil.util.Hasher;

public final class Quat implements Hashable {

	public static final Quat ZERO = new Quat(0, 0, 0, 0);
	public static final Quat IDENTITY = new Quat(1, 0, 0, 0);

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
		return Vec3.from(i, j, k).mul(h).mul(2 * halfAngle);
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
		return Vec3.from(i, j, k);
	}

	public Vec3 transform(Vec3 vec) {
		var norm = normalize();
		return norm.hamiltonProduct(Quat.fromIjk(vec))
				.hamiltonProduct(norm.conjugate())
				.ijk();
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
