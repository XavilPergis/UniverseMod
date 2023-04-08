package net.xavil.util.math;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.util.FastHasher;
import net.xavil.util.Hashable;
import net.xavil.util.Hasher;

public final class Vec3i implements Hashable {

	public static final Vec3i ZERO = new Vec3i(0, 0, 0);
	public static final Vec3i XN = new Vec3i(-1, 0, 0);
	public static final Vec3i XP = new Vec3i(1, 0, 0);
	public static final Vec3i YN = new Vec3i(0, -1, 0);
	public static final Vec3i YP = new Vec3i(0, 1, 0);
	public static final Vec3i ZN = new Vec3i(0, 0, -1);
	public static final Vec3i ZP = new Vec3i(0, 0, 1);

	public static final Codec<Vec3i> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.INT.fieldOf("x").forGetter(v -> v.x),
			Codec.INT.fieldOf("y").forGetter(v -> v.y),
			Codec.INT.fieldOf("z").forGetter(v -> v.z))
			.apply(inst, Vec3i::new));

	public final int x, y, z;

	private Vec3i(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static Vec3i from(int x, int y, int z) {
		return new Vec3i(x, y, z);
	}

	public Vec3 lowerCorner() {
		return Vec3.from(x, y, z);
	}

	public Vec3 upperCorner() {
		return Vec3.from(x + 1, y + 1, z + 1);
	}

	public Vec3 center() {
		return Vec3.from(x + 0.5, y + 0.5, z + 0.5);
	}

	public Vec3i add(int x, int y, int z) {
		return new Vec3i(this.x + x, this.y + y, this.z + z);
	}

	public Vec3i add(Vec3i other) {
		return new Vec3i(this.x + other.x, this.y + other.y, this.z + other.z);
	}

	public Vec3i sub(int x, int y, int z) {
		return new Vec3i(this.x - x, this.y - y, this.z - z);
	}

	public Vec3i sub(Vec3i other) {
		return new Vec3i(this.x - other.x, this.y - other.y, this.z - other.z);
	}

	public Vec3i mul(int x, int y, int z) {
		return new Vec3i(this.x * x, this.y * y, this.z * z);
	}

	public Vec3i mul(Vec3i other) {
		return new Vec3i(this.x * other.x, this.y * other.y, this.z * other.z);
	}

	public Vec3i mul(int scalar) {
		return new Vec3i(this.x * scalar, this.y * scalar, this.z * scalar);
	}

	public Vec3i div(int x, int y, int z) {
		return new Vec3i(this.x / x, this.y / y, this.z / z);
	}

	public Vec3i div(Vec3i other) {
		return new Vec3i(this.x / other.x, this.y / other.y, this.z / other.z);
	}

	public Vec3i div(int scalar) {
		return new Vec3i(this.x / scalar, this.y / scalar, this.z / scalar);
	}

	public Vec3i neg() {
		return new Vec3i(-x, -y, -z);
	}

	public static Vec3i fromMinecraft(net.minecraft.core.Vec3i vec) {
		return new Vec3i(vec.getX(), vec.getY(), vec.getZ());
	}

	public net.minecraft.core.Vec3i asMinecraft() {
		return new net.minecraft.core.Vec3i(x, y, z);
	}

	@Override
	public String toString() {
		return "Vec3i[" + x + ", " + y + ", " + z + "]";
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Vec3i other) {
			return this.x == other.x && this.y == other.y && this.z == other.z;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendInt(this.x).appendInt(this.y).appendInt(this.z);
	}

	public static void iterateInclusive(Vec3i min, Vec3i max, Consumer<Vec3i> consumer) {
		for (var x = min.x; x <= max.x; ++x)
			for (var y = min.y; y <= max.y; ++y)
				for (var z = min.z; z <= max.z; ++z)
					consumer.accept(new Vec3i(x, y, z));
	}

}
