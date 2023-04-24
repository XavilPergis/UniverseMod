package net.xavil.util.math;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.xavil.util.FastHasher;
import net.xavil.util.Hashable;
import net.xavil.util.Hasher;

public final class Vec3 implements Hashable {

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

	public Vec3(double x, double y, double z) {
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

	public static Vec3 min(Vec3 a, Vec3 b) {
		return new Vec3(
				Math.min(a.x, b.x),
				Math.min(a.y, b.y),
				Math.min(a.z, b.z));
	}

	public static Vec3 max(Vec3 a, Vec3 b) {
		return new Vec3(
				Math.max(a.x, b.x),
				Math.max(a.y, b.y),
				Math.max(a.z, b.z));
	}

	public static Vec3 random(Random random, Vec3 min, Vec3 max) {
		var x = random.nextDouble(min.x, max.x);
		var y = random.nextDouble(min.y, max.y);
		var z = random.nextDouble(min.z, max.z);
		return new Vec3(x, y, z);
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

	public Vec3 recip() {
		return new Vec3(1.0 / x, 1.0 / y, 1.0 / z);
	}

	public Vec3 recip(double n) {
		return new Vec3(n / x, n / y, n / z);
	}

	public Vec3 abs() {
		return new Vec3(Math.abs(x), Math.abs(y), Math.abs(z));
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

	public Vec3 projectOnto(Vec3 other) {
		// return other.mul(this.dot(other) / other.lengthSquared());
		final var b = other.normalize();
		return b.mul(this.dot(b));
	}

	public Vec3 transformBy(PoseStack.Pose pose) {
		return transformBy(pose.pose());
	}

	public Vec3 transformBy(Matrix4f matrix) {
		return transformBy(matrix, 1);
	}

	public Vec3 transformBy(Matrix4f matrix, double w) {
		final var vec = new Vector4f((float) this.x, (float) this.y, (float) this.z, (float) w);
		vec.transform(matrix);
		return new Vec3(vec.x() / vec.w(), vec.y() / vec.w(), vec.z() / vec.w());
	}

	public Vec3 transformBy(Mat4 matrix) {
		return transformBy(matrix, 1);
	}

	public Vec3 transformBy(Mat4 matrix, double w) {
		return matrix.mul(withW(w)).perspectiveDivision();
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

	public Vec3 withX(double x) {
		return new Vec3(x, y, z);
	}
	public Vec3 withY(double y) {
		return new Vec3(x, y, z);
	}
	public Vec3 withZ(double z) {
		return new Vec3(x, y, z);
	}
	public Vec4 withW(double w) {
		return new Vec4(x, y, z, w);
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

	public static double volume(Vec3 a, Vec3 b) {
		return Math.abs(a.x - b.x) * Math.abs(a.y - b.y) * Math.abs(a.z - b.z);
	}

	public static Vec3 lerp(double delta, Vec3 a, Vec3 b) {
		return new Vec3(
				Mth.lerp(delta, a.x, b.x),
				Mth.lerp(delta, a.y, b.y),
				Mth.lerp(delta, a.z, b.z));
	}

	public Vec3i floor() {
		return Vec3i.from(Mth.floor(x), Mth.floor(y), Mth.floor(z));
	}

	public Vec3i ceil() {
		return Vec3i.from(Mth.ceil(x), Mth.ceil(y), Mth.ceil(z));
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendDouble(this.x).appendDouble(this.y).appendDouble(this.z);
	}

	// #region Swizzle Operations
	// @formatter:off
	public Vec2 xx() {return new Vec2(x,x);}
	public Vec2 xy() {return new Vec2(x,y);}
	public Vec2 xz() {return new Vec2(x,z);}
	public Vec2 yx() {return new Vec2(y,x);}
	public Vec2 yy() {return new Vec2(y,y);}
	public Vec2 yz() {return new Vec2(y,z);}
	public Vec2 zx() {return new Vec2(z,x);}
	public Vec2 zy() {return new Vec2(z,y);}
	public Vec2 zz() {return new Vec2(z,z);}

	public Vec3 xxx() {return new Vec3(x,x,x);}
	public Vec3 xxy() {return new Vec3(x,x,y);}
	public Vec3 xxz() {return new Vec3(x,x,z);}
	public Vec3 xyx() {return new Vec3(x,y,x);}
	public Vec3 xyy() {return new Vec3(x,y,y);}
	public Vec3 xyz() {return this;}
	public Vec3 xzx() {return new Vec3(x,z,x);}
	public Vec3 xzy() {return new Vec3(x,z,y);}
	public Vec3 xzz() {return new Vec3(x,z,z);}
	public Vec3 yxx() {return new Vec3(y,x,x);}
	public Vec3 yxy() {return new Vec3(y,x,y);}
	public Vec3 yxz() {return new Vec3(y,x,z);}
	public Vec3 yyx() {return new Vec3(y,y,x);}
	public Vec3 yyy() {return new Vec3(y,y,y);}
	public Vec3 yyz() {return new Vec3(y,y,z);}
	public Vec3 yzx() {return new Vec3(y,z,x);}
	public Vec3 yzy() {return new Vec3(y,z,y);}
	public Vec3 yzz() {return new Vec3(y,z,z);}
	public Vec3 zxx() {return new Vec3(z,x,x);}
	public Vec3 zxy() {return new Vec3(z,x,y);}
	public Vec3 zxz() {return new Vec3(z,x,z);}
	public Vec3 zyx() {return new Vec3(z,y,x);}
	public Vec3 zyy() {return new Vec3(z,y,y);}
	public Vec3 zyz() {return new Vec3(z,y,z);}
	public Vec3 zzx() {return new Vec3(z,z,x);}
	public Vec3 zzy() {return new Vec3(z,z,y);}
	public Vec3 zzz() {return new Vec3(z,z,z);}

	// special swizzles for converting to a vec4 with a w of 0 or 1
	public Vec4 xxx0() {return new Vec4(x,x,x,0);}
	public Vec4 xxy0() {return new Vec4(x,x,y,0);}
	public Vec4 xxz0() {return new Vec4(x,x,z,0);}
	public Vec4 xyx0() {return new Vec4(x,y,x,0);}
	public Vec4 xyy0() {return new Vec4(x,y,y,0);}
	public Vec4 xyz0() {return new Vec4(x,y,z,0);}
	public Vec4 xzx0() {return new Vec4(x,z,x,0);}
	public Vec4 xzy0() {return new Vec4(x,z,y,0);}
	public Vec4 xzz0() {return new Vec4(x,z,z,0);}
	public Vec4 yxx0() {return new Vec4(y,x,x,0);}
	public Vec4 yxy0() {return new Vec4(y,x,y,0);}
	public Vec4 yxz0() {return new Vec4(y,x,z,0);}
	public Vec4 yyx0() {return new Vec4(y,y,x,0);}
	public Vec4 yyy0() {return new Vec4(y,y,y,0);}
	public Vec4 yyz0() {return new Vec4(y,y,z,0);}
	public Vec4 yzx0() {return new Vec4(y,z,x,0);}
	public Vec4 yzy0() {return new Vec4(y,z,y,0);}
	public Vec4 yzz0() {return new Vec4(y,z,z,0);}
	public Vec4 zxx0() {return new Vec4(z,x,x,0);}
	public Vec4 zxy0() {return new Vec4(z,x,y,0);}
	public Vec4 zxz0() {return new Vec4(z,x,z,0);}
	public Vec4 zyx0() {return new Vec4(z,y,x,0);}
	public Vec4 zyy0() {return new Vec4(z,y,y,0);}
	public Vec4 zyz0() {return new Vec4(z,y,z,0);}
	public Vec4 zzx0() {return new Vec4(z,z,x,0);}
	public Vec4 zzy0() {return new Vec4(z,z,y,0);}
	public Vec4 zzz0() {return new Vec4(z,z,z,0);}
	public Vec4 xxx1() {return new Vec4(x,x,x,1);}
	public Vec4 xxy1() {return new Vec4(x,x,y,1);}
	public Vec4 xxz1() {return new Vec4(x,x,z,1);}
	public Vec4 xyx1() {return new Vec4(x,y,x,1);}
	public Vec4 xyy1() {return new Vec4(x,y,y,1);}
	public Vec4 xyz1() {return new Vec4(x,y,z,1);}
	public Vec4 xzx1() {return new Vec4(x,z,x,1);}
	public Vec4 xzy1() {return new Vec4(x,z,y,1);}
	public Vec4 xzz1() {return new Vec4(x,z,z,1);}
	public Vec4 yxx1() {return new Vec4(y,x,x,1);}
	public Vec4 yxy1() {return new Vec4(y,x,y,1);}
	public Vec4 yxz1() {return new Vec4(y,x,z,1);}
	public Vec4 yyx1() {return new Vec4(y,y,x,1);}
	public Vec4 yyy1() {return new Vec4(y,y,y,1);}
	public Vec4 yyz1() {return new Vec4(y,y,z,1);}
	public Vec4 yzx1() {return new Vec4(y,z,x,1);}
	public Vec4 yzy1() {return new Vec4(y,z,y,1);}
	public Vec4 yzz1() {return new Vec4(y,z,z,1);}
	public Vec4 zxx1() {return new Vec4(z,x,x,1);}
	public Vec4 zxy1() {return new Vec4(z,x,y,1);}
	public Vec4 zxz1() {return new Vec4(z,x,z,1);}
	public Vec4 zyx1() {return new Vec4(z,y,x,1);}
	public Vec4 zyy1() {return new Vec4(z,y,y,1);}
	public Vec4 zyz1() {return new Vec4(z,y,z,1);}
	public Vec4 zzx1() {return new Vec4(z,z,x,1);}
	public Vec4 zzy1() {return new Vec4(z,z,y,1);}
	public Vec4 zzz1() {return new Vec4(z,z,z,1);}

	public Vec4 xxxx() {return new Vec4(x,x,x,x);}
	public Vec4 xxxy() {return new Vec4(x,x,x,y);}
	public Vec4 xxxz() {return new Vec4(x,x,x,z);}
	public Vec4 xxyx() {return new Vec4(x,x,y,x);}
	public Vec4 xxyy() {return new Vec4(x,x,y,y);}
	public Vec4 xxyz() {return new Vec4(x,x,y,z);}
	public Vec4 xxzx() {return new Vec4(x,x,z,x);}
	public Vec4 xxzy() {return new Vec4(x,x,z,y);}
	public Vec4 xxzz() {return new Vec4(x,x,z,z);}
	public Vec4 xyxx() {return new Vec4(x,y,x,x);}
	public Vec4 xyxy() {return new Vec4(x,y,x,y);}
	public Vec4 xyxz() {return new Vec4(x,y,x,z);}
	public Vec4 xyyx() {return new Vec4(x,y,y,x);}
	public Vec4 xyyy() {return new Vec4(x,y,y,y);}
	public Vec4 xyyz() {return new Vec4(x,y,y,z);}
	public Vec4 xyzx() {return new Vec4(x,y,z,x);}
	public Vec4 xyzy() {return new Vec4(x,y,z,y);}
	public Vec4 xyzz() {return new Vec4(x,y,z,z);}
	public Vec4 xzxx() {return new Vec4(x,z,x,x);}
	public Vec4 xzxy() {return new Vec4(x,z,x,y);}
	public Vec4 xzxz() {return new Vec4(x,z,x,z);}
	public Vec4 xzyx() {return new Vec4(x,z,y,x);}
	public Vec4 xzyy() {return new Vec4(x,z,y,y);}
	public Vec4 xzyz() {return new Vec4(x,z,y,z);}
	public Vec4 xzzx() {return new Vec4(x,z,z,x);}
	public Vec4 xzzy() {return new Vec4(x,z,z,y);}
	public Vec4 xzzz() {return new Vec4(x,z,z,z);}
	public Vec4 yxxx() {return new Vec4(y,x,x,x);}
	public Vec4 yxxy() {return new Vec4(y,x,x,y);}
	public Vec4 yxxz() {return new Vec4(y,x,x,z);}
	public Vec4 yxyx() {return new Vec4(y,x,y,x);}
	public Vec4 yxyy() {return new Vec4(y,x,y,y);}
	public Vec4 yxyz() {return new Vec4(y,x,y,z);}
	public Vec4 yxzx() {return new Vec4(y,x,z,x);}
	public Vec4 yxzy() {return new Vec4(y,x,z,y);}
	public Vec4 yxzz() {return new Vec4(y,x,z,z);}
	public Vec4 yyxx() {return new Vec4(y,y,x,x);}
	public Vec4 yyxy() {return new Vec4(y,y,x,y);}
	public Vec4 yyxz() {return new Vec4(y,y,x,z);}
	public Vec4 yyyx() {return new Vec4(y,y,y,x);}
	public Vec4 yyyy() {return new Vec4(y,y,y,y);}
	public Vec4 yyyz() {return new Vec4(y,y,y,z);}
	public Vec4 yyzx() {return new Vec4(y,y,z,x);}
	public Vec4 yyzy() {return new Vec4(y,y,z,y);}
	public Vec4 yyzz() {return new Vec4(y,y,z,z);}
	public Vec4 yzxx() {return new Vec4(y,z,x,x);}
	public Vec4 yzxy() {return new Vec4(y,z,x,y);}
	public Vec4 yzxz() {return new Vec4(y,z,x,z);}
	public Vec4 yzyx() {return new Vec4(y,z,y,x);}
	public Vec4 yzyy() {return new Vec4(y,z,y,y);}
	public Vec4 yzyz() {return new Vec4(y,z,y,z);}
	public Vec4 yzzx() {return new Vec4(y,z,z,x);}
	public Vec4 yzzy() {return new Vec4(y,z,z,y);}
	public Vec4 yzzz() {return new Vec4(y,z,z,z);}
	public Vec4 zxxx() {return new Vec4(z,x,x,x);}
	public Vec4 zxxy() {return new Vec4(z,x,x,y);}
	public Vec4 zxxz() {return new Vec4(z,x,x,z);}
	public Vec4 zxyx() {return new Vec4(z,x,y,x);}
	public Vec4 zxyy() {return new Vec4(z,x,y,y);}
	public Vec4 zxyz() {return new Vec4(z,x,y,z);}
	public Vec4 zxzx() {return new Vec4(z,x,z,x);}
	public Vec4 zxzy() {return new Vec4(z,x,z,y);}
	public Vec4 zxzz() {return new Vec4(z,x,z,z);}
	public Vec4 zyxx() {return new Vec4(z,y,x,x);}
	public Vec4 zyxy() {return new Vec4(z,y,x,y);}
	public Vec4 zyxz() {return new Vec4(z,y,x,z);}
	public Vec4 zyyx() {return new Vec4(z,y,y,x);}
	public Vec4 zyyy() {return new Vec4(z,y,y,y);}
	public Vec4 zyyz() {return new Vec4(z,y,y,z);}
	public Vec4 zyzx() {return new Vec4(z,y,z,x);}
	public Vec4 zyzy() {return new Vec4(z,y,z,y);}
	public Vec4 zyzz() {return new Vec4(z,y,z,z);}
	public Vec4 zzxx() {return new Vec4(z,z,x,x);}
	public Vec4 zzxy() {return new Vec4(z,z,x,y);}
	public Vec4 zzxz() {return new Vec4(z,z,x,z);}
	public Vec4 zzyx() {return new Vec4(z,z,y,x);}
	public Vec4 zzyy() {return new Vec4(z,z,y,y);}
	public Vec4 zzyz() {return new Vec4(z,z,y,z);}
	public Vec4 zzzx() {return new Vec4(z,z,z,x);}
	public Vec4 zzzy() {return new Vec4(z,z,z,y);}
	public Vec4 zzzz() {return new Vec4(z,z,z,z);}
	// @formatter:on
	// #endregion

}
