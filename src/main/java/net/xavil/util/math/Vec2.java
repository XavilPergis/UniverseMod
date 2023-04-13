package net.xavil.util.math;

import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;
import net.xavil.util.FastHasher;
import net.xavil.util.Hashable;
import net.xavil.util.Hasher;

public final class Vec2 implements Hashable {

	public static final Vec2 ZERO = new Vec2(0, 0);
	public static final Vec2 XN = new Vec2(-1, 0);
	public static final Vec2 XP = new Vec2(1, 0);
	public static final Vec2 YN = new Vec2(0, -1);
	public static final Vec2 YP = new Vec2(0, 1);

	public static final Codec<Vec2> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.DOUBLE.fieldOf("x").forGetter(v -> v.x),
			Codec.DOUBLE.fieldOf("y").forGetter(v -> v.y))
			.apply(inst, Vec2::new));

	public final double x, y;

	private Vec2(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public static Vec2 broadcast(double n) {
		return new Vec2(n, n);
	}

	public static Vec2 from(double x, double y) {
		return new Vec2(x, y);
	}

	public static Vec2 fromCorner(Vec2i intVec) {
		return new Vec2(intVec.x, intVec.y);
	}

	public static Vec2 min(Vec2 a, Vec2 b) {
		return new Vec2(
				Math.min(a.x, b.x),
				Math.min(a.y, b.y));
	}

	public static Vec2 max(Vec2 a, Vec2 b) {
		return new Vec2(
				Math.max(a.x, b.x),
				Math.max(a.y, b.y));
	}

	public static Vec2 random(Random random, Vec2 min, Vec2 max) {
		var x = random.nextDouble(min.x, max.x);
		var y = random.nextDouble(min.y, max.y);
		return new Vec2(x, y);
	}

	public Vec2 add(double x, double y) {
		return new Vec2(this.x + x, this.y + y);
	}

	public Vec2 add(Vec2 other) {
		return new Vec2(this.x + other.x, this.y + other.y);
	}

	public Vec2 sub(double x, double y) {
		return new Vec2(this.x - x, this.y - y);
	}

	public Vec2 sub(Vec2 other) {
		return new Vec2(this.x - other.x, this.y - other.y);
	}

	public Vec2 mul(double x, double y) {
		return new Vec2(this.x * x, this.y * y);
	}

	public Vec2 mul(Vec2 other) {
		return new Vec2(this.x * other.x, this.y * other.y);
	}

	public Vec2 mul(double scalar) {
		return new Vec2(this.x * scalar, this.y * scalar);
	}

	public Vec2 div(double x, double y) {
		return new Vec2(this.x / x, this.y / y);
	}

	public Vec2 div(Vec2 other) {
		return new Vec2(this.x / other.x, this.y / other.y);
	}

	public Vec2 div(double scalar) {
		return new Vec2(this.x / scalar, this.y / scalar);
	}

	public Vec2 neg() {
		return new Vec2(-x, -y);
	}

	public Vec2 recip() {
		return new Vec2(1.0 / x, 1.0 / y);
	}

	public Vec2 recip(double n) {
		return new Vec2(n / x, n / y);
	}

	public Vec2 abs() {
		return new Vec2(Math.abs(x), Math.abs(y));
	}

	public double dot(Vec2 other) {
		return this.x * other.x + this.y * other.y;
	}

	public double distanceToSquared(Vec2 other) {
		final double dx = this.x - other.x, dy = this.y - other.y;
		return dx * dx + dy * dy;
	}

	public double distanceTo(Vec2 other) {
		return Math.sqrt(distanceToSquared(other));
	}

	public double lengthSquared() {
		return this.x * this.x + this.y * this.y;
	}

	public double length() {
		return Math.sqrt(lengthSquared());
	}

	public Vec2 normalize() {
		return mul(1 / length());
	}

	public Vec2 projectOnto(Vec2 other) {
		// return other.mul(this.dot(other) / other.lengthSquared());
		final var b = other.normalize();
		return b.mul(this.dot(b));
	}

	public Vec2 rotate(double angle) {
		final double c = Math.cos(angle), s = Math.sin(angle);
		final var nx = this.x * c + this.y * s;
		final var ny = this.y * c - this.x * s;
		return new Vec2(nx, ny);
	}

	public static Vec2 fromMinecraft(net.minecraft.world.phys.Vec2 pos) {
		return new Vec2(pos.x, pos.y);
	}

	public net.minecraft.world.phys.Vec2 asMinecraft() {
		return new net.minecraft.world.phys.Vec2((float) x, (float) y);
	}

	@Override
	public String toString() {
		return "Vec2[" + x + ", " + y + "]";
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Vec2 other) {
			return this.x == other.x && this.y == other.y;
		}
		return false;
	}

	public static double area(Vec2 a, Vec2 b) {
		return Math.abs(a.x - b.x) * Math.abs(a.y - b.y);
	}

	public Vec2i floor() {
		return Vec2i.from(Mth.floor(x), Mth.floor(y));
	}

	public Vec2i ceil() {
		return Vec2i.from(Mth.ceil(x), Mth.ceil(y));
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendDouble(this.x).appendDouble(this.y);
	}

}
