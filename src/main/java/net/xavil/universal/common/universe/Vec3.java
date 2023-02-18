package net.xavil.universal.common.universe;

import com.mojang.math.Vector3f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Position;
import net.minecraft.util.Mth;

public final class Vec3 {

	public static final Vec3 ZERO = new Vec3(0, 0, 0);
	public static final Vec3 XN = new Vec3(-1, 0, 0);
	public static final Vec3 XP = new Vec3(1, 0, 0);
	public static final Vec3 YN = new Vec3(0, -1, 0);
	public static final Vec3 YP = new Vec3(0, 1, 0);
	public static final Vec3 ZN = new Vec3(0, 0, -1);
	public static final Vec3 ZP = new Vec3(0, 0, 1);

	public static final Codec<Vec3> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.DOUBLE.fieldOf("x").forGetter(v -> v.x),
			Codec.DOUBLE.fieldOf("y").forGetter(v -> v.y),
			Codec.DOUBLE.fieldOf("z").forGetter(v -> v.z))
			.apply(inst, Vec3::new));

	public final double x, y, z;

	private Vec3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static Vec3 broadcast(double n) {
		return new Vec3(n, n, n);
	}

	public static Vec3 from(double x, double y, double z) {
		return new Vec3(x, y, z);
	}

	public static Vec3 fromCorner(Vec3i intVec) {
		return new Vec3(intVec.x, intVec.y, intVec.z);
	}

	public Vec3 add(double x, double y, double z) {
		return new Vec3(this.x + x, this.y + y, this.z + z);
	}

	public Vec3 add(Vec3 other) {
		return new Vec3(this.x + other.x, this.y + other.y, this.z + other.z);
	}

	public Vec3 sub(double x, double y, double z) {
		return new Vec3(this.x - x, this.y - y, this.z - z);
	}

	public Vec3 sub(Vec3 other) {
		return new Vec3(this.x - other.x, this.y - other.y, this.z - other.z);
	}

	public Vec3 mul(double x, double y, double z) {
		return new Vec3(this.x * x, this.y * y, this.z * z);
	}

	public Vec3 mul(Vec3 other) {
		return new Vec3(this.x * other.x, this.y * other.y, this.z * other.z);
	}

	public Vec3 mul(double scalar) {
		return new Vec3(this.x * scalar, this.y * scalar, this.z * scalar);
	}

	public Vec3 div(double x, double y, double z) {
		return new Vec3(this.x / x, this.y / y, this.z / z);
	}

	public Vec3 div(Vec3 other) {
		return new Vec3(this.x / other.x, this.y / other.y, this.z / other.z);
	}

	public Vec3 div(double scalar) {
		return new Vec3(this.x / scalar, this.y / scalar, this.z / scalar);
	}

	public Vec3 neg() {
		return new Vec3(-x, -y, -z);
	}

	public double dot(Vec3 other) {
		return this.x * other.x + this.y * other.y + this.z * other.z;
	}

	public Vec3 cross(Vec3 other) {
		return new Vec3(this.y * other.z - this.z * other.y,
				this.z * other.x - this.x * other.z,
				this.x * other.y - this.y * other.x);
	}

	public double distanceToSquared(Vec3 other) {
		final double dx = this.x - other.x, dy = this.y - other.y, dz = this.z - other.z;
		return dx * dx + dy * dy + dz * dz;
	}

	public double distanceTo(Vec3 other) {
		return Math.sqrt(distanceToSquared(other));
	}

	public double lengthSquared() {
		return this.x * this.x + this.y * this.y + this.z * this.z;
	}

	public double length() {
		return Math.sqrt(lengthSquared());
	}

	public Vec3 normalize() {
		return mul(1 / length());
	}

	public Vec3 rotateX(double angle) {
		final double c = Math.cos(angle), s = Math.sin(angle);
		final var nx = this.x;
		final var ny = this.y * c + this.z * s;
		final var nz = this.z * c - this.y * s;
		return new Vec3(nx, ny, nz);
	}

	public Vec3 rotateY(double angle) {
		final double c = Math.cos(angle), s = Math.sin(angle);
		final var nx = this.x * c + this.z * s;
		final var ny = this.y;
		final var nz = this.z * c - this.x * s;
		return new Vec3(nx, ny, nz);
	}

	public Vec3 rotateZ(double angle) {
		final double c = Math.cos(angle), s = Math.sin(angle);
		final var nx = this.x * c + this.y * s;
		final var ny = this.y * c - this.x * s;
		final var nz = this.z;
		return new Vec3(nx, ny, nz);
	}

	public static Vec3 fromMinecraft(Vector3f pos) {
		return new Vec3(pos.x(), pos.y(), pos.z());
	}

	public static Vec3 fromMinecraft(Position pos) {
		return new Vec3(pos.x(), pos.y(), pos.z());
	}

	public net.minecraft.world.phys.Vec3 asMinecraft() {
		return new net.minecraft.world.phys.Vec3(x, y, z);
	}

	@Override
	public String toString() {
		return "Vec3[" + x + ", " + y + ", " + z + "]";
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Vec3 other) {
			return this.x == other.x && this.y == other.y && this.z == other.z;
		}
		return false;
	}

	@Override
	public int hashCode() {
		long hash = 0L;
		hash ^= Mth.murmurHash3Mixer(Double.doubleToRawLongBits(this.x));
		hash ^= Mth.murmurHash3Mixer(Double.doubleToRawLongBits(this.y));
		hash ^= Mth.murmurHash3Mixer(Double.doubleToRawLongBits(this.z));
		return ((int) hash) ^ (int) (hash >> 32);
	}

}
