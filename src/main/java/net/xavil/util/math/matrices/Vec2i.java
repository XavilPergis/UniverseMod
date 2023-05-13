package net.xavil.util.math.matrices;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.util.hash.FastHasher;
import net.xavil.util.hash.Hashable;
import net.xavil.util.hash.Hasher;

public class Vec2i implements Hashable {

	public static final Vec2i ZERO = new Vec2i(0, 0);
	public static final Vec2i XN = new Vec2i(-1, 0);
	public static final Vec2i XP = new Vec2i(1, 0);
	public static final Vec2i YN = new Vec2i(0, -1);
	public static final Vec2i YP = new Vec2i(0, 1);

	public static final Codec<Vec2i> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.INT.fieldOf("x").forGetter(v -> v.x),
			Codec.INT.fieldOf("y").forGetter(v -> v.y))
			.apply(inst, Vec2i::new));

	public final int x, y;

	public Vec2i(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public static Vec2i from(int x, int y) {
		return new Vec2i(x, y);
	}

	// public Vec2 lowerCorner() {
	// return Vec2.from(x, y, z);
	// }

	// public Vec2 upperCorner() {
	// return Vec2.from(x + 1, y + 1, z + 1);
	// }

	// public Vec2 center() {
	// return Vec2.from(x + 0.5, y + 0.5, z + 0.5);
	// }

	public Vec2i add(int x, int y) {
		return new Vec2i(this.x + x, this.y + y);
	}

	public Vec2i add(Vec2i other) {
		return new Vec2i(this.x + other.x, this.y + other.y);
	}

	public Vec2i sub(int x, int y) {
		return new Vec2i(this.x - x, this.y - y);
	}

	public Vec2i sub(Vec2i other) {
		return new Vec2i(this.x - other.x, this.y - other.y);
	}

	public Vec2i mul(int x, int y) {
		return new Vec2i(this.x * x, this.y * y);
	}

	public Vec2i mul(Vec2i other) {
		return new Vec2i(this.x * other.x, this.y * other.y);
	}

	public Vec2i mul(int scalar) {
		return new Vec2i(this.x * scalar, this.y * scalar);
	}

	public Vec2i div(int x, int y) {
		return new Vec2i(this.x / x, this.y / y);
	}

	public Vec2i div(Vec2i other) {
		return new Vec2i(this.x / other.x, this.y / other.y);
	}

	public Vec2i div(int scalar) {
		return new Vec2i(this.x / scalar, this.y / scalar);
	}

	public Vec2i floorDiv(int scalar) {
		return new Vec2i(Math.floorDiv(this.x, scalar), Math.floorDiv(this.y, scalar));
	}

	public Vec2i neg() {
		return new Vec2i(-x, -y);
	}

	public static Vec2i min(Vec2i a, Vec2i b) {
		return new Vec2i(Math.min(a.x, b.x), Math.min(a.y, b.y));
	}
	public static Vec2i max(Vec2i a, Vec2i b) {
		return new Vec2i(Math.max(a.x, b.x), Math.max(a.y, b.y));
	}

	@Override
	public String toString() {
		return "Vec2i[" + x + ", " + y + "]";
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Vec2i other) {
			return this.x == other.x && this.y == other.y;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendInt(this.x).appendInt(this.y);
	}

	public static void iterateInclusive(Vec2i min, Vec2i max, Consumer<Vec2i> consumer) {
		for (var x = min.x; x <= max.x; ++x)
			for (var y = min.y; y <= max.y; ++y)
				consumer.accept(new Vec2i(x, y));
	}

	public static Vec2i broadcast(int n) {
		return new Vec2i(n, n);
	}

}
