package net.xavil.util.math;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;
import net.xavil.util.FastHasher;
import net.xavil.util.Hashable;
import net.xavil.util.Hasher;

public final class Vec4 implements Hashable {

	public static final Codec<Vec4> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.DOUBLE.fieldOf("x").forGetter(v -> v.x),
			Codec.DOUBLE.fieldOf("y").forGetter(v -> v.y),
			Codec.DOUBLE.fieldOf("z").forGetter(v -> v.z),
			Codec.DOUBLE.fieldOf("w").forGetter(v -> v.w))
			.apply(inst, Vec4::new));

	public final double x, y, z, w;

	public Vec4(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public static Vec4 broadcast(double n) {
		return new Vec4(n, n, n, n);
	}

	public static Vec4 from(double x, double y, double z, double w) {
		return new Vec4(x, y, z, w);
	}

	public static Vec4 from(Vec3 vec, double w) {
		return new Vec4(vec.x, vec.y, vec.z, w);
	}

	public Vec4 add(Vec4 other) {
		return new Vec4(this.x + other.x, this.y + other.y, this.z + other.z, this.w + other.w);
	}

	public Vec4 sub(Vec4 other) {
		return new Vec4(this.x - other.x, this.y - other.y, this.z - other.z, this.w - other.w);
	}

	public Vec4 mul(Vec4 other) {
		return new Vec4(this.x * other.x, this.y * other.y, this.z * other.z, this.w * other.w);
	}

	public Vec4 mul(double scalar) {
		return new Vec4(this.x * scalar, this.y * scalar, this.z * scalar, this.w * scalar);
	}

	public Vec4 div(Vec4 other) {
		return new Vec4(this.x / other.x, this.y / other.y, this.z / other.z, this.w / other.w);
	}

	public Vec4 div(double scalar) {
		return new Vec4(this.x / scalar, this.y / scalar, this.z / scalar, this.w / scalar);
	}

	public Vec4 neg() {
		return new Vec4(-x, -y, -z, -w);
	}

	public Vec4 recip() {
		return new Vec4(1.0 / x, 1.0 / y, 1.0 / z, 1.0 / w);
	}

	public Vec4 recip(double n) {
		return new Vec4(n / x, n / y, n / z, n / w);
	}

	public double dot(Vec4 other) {
		return this.x * other.x + this.y * other.y + this.z * other.z + this.w * other.w;
	}

	public double distanceToSquared(Vec4 other) {
		final double dx = this.x - other.x, dy = this.y - other.y, dz = this.z - other.z, dw = this.w - other.w;
		return dx * dx + dy * dy + dz * dz + dw * dw;
	}

	public double distanceTo(Vec4 other) {
		return Math.sqrt(distanceToSquared(other));
	}

	public double lengthSquared() {
		return this.x * this.x + this.y * this.y + this.z * this.z + this.w * this.w;
	}

	public double length() {
		return Math.sqrt(lengthSquared());
	}

	public Vec4 normalize() {
		return mul(1 / length());
	}

	public Vec4 projectOnto(Vec4 other) {
		// return other.mul(this.dot(other) / other.lengthSquared());
		final var b = other.normalize();
		return b.mul(this.dot(b));
	}

	public Vec4 transformBy(PoseStack.Pose pose) {
		return transformBy(pose.pose());
	}

	public Vec4 transformBy(Matrix4f matrix) {
		final var vec = new Vector4f((float) this.x, (float) this.y, (float) this.z, (float) this.w);
		vec.transform(matrix);
		return new Vec4(vec.x(), vec.y(), vec.z(), vec.w());
	}

	public Vec4 transformBy(Mat4 matrix) {
		return matrix.mul(this);
	}

	public Vec3 perspectiveDivision() {
		return Vec3.from(x / w, y / w, z / w);
	}

	public Vec4 withX(double x) {
		return new Vec4(x, y, z, w);
	}
	public Vec4 withY(double y) {
		return new Vec4(x, y, z, w);
	}
	public Vec4 withZ(double z) {
		return new Vec4(x, y, z, w);
	}
	public Vec4 withW(double w) {
		return new Vec4(x, y, z, w);
	}

	public static Vec4 fromMinecraft(Vector4f pos) {
		return new Vec4(pos.x(), pos.y(), pos.z(), pos.w());
	}

	public Vector4f asMinecraft() {
		return new Vector4f((float) x, (float) y, (float) z, (float) w);
	}

	@Override
	public String toString() {
		return "Vec4[" + x + ", " + y + ", " + z + ", " + w + "]";
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Vec4 other) {
			return this.x == other.x && this.y == other.y && this.z == other.z && this.w == other.w;
		}
		return false;
	}

	public static Vec4 lerp(double delta, Vec4 a, Vec4 b) {
		return new Vec4(
				Mth.lerp(delta, a.x, b.x),
				Mth.lerp(delta, a.y, b.y),
				Mth.lerp(delta, a.z, b.z),
				Mth.lerp(delta, a.w, b.w));
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendDouble(this.x).appendDouble(this.y).appendDouble(this.z).appendDouble(this.w);
	}

	// #region Swizzle Operations
	// @formatter:off
	public Vec2 xx() {return new Vec2(x,x);}
	public Vec2 xy() {return new Vec2(x,y);}
	public Vec2 xz() {return new Vec2(x,z);}
	public Vec2 xw() {return new Vec2(x,w);}
	public Vec2 yx() {return new Vec2(y,x);}
	public Vec2 yy() {return new Vec2(y,y);}
	public Vec2 yz() {return new Vec2(y,z);}
	public Vec2 yw() {return new Vec2(y,w);}
	public Vec2 zx() {return new Vec2(z,x);}
	public Vec2 zy() {return new Vec2(z,y);}
	public Vec2 zz() {return new Vec2(z,z);}
	public Vec2 zw() {return new Vec2(z,w);}
	public Vec2 wx() {return new Vec2(w,x);}
	public Vec2 wy() {return new Vec2(w,y);}
	public Vec2 wz() {return new Vec2(w,z);}
	public Vec2 ww() {return new Vec2(w,w);}

	public Vec3 xxx() {return new Vec3(x,x,x);}
	public Vec3 xxy() {return new Vec3(x,x,y);}
	public Vec3 xxz() {return new Vec3(x,x,z);}
	public Vec3 xxw() {return new Vec3(x,x,w);}
	public Vec3 xyx() {return new Vec3(x,y,x);}
	public Vec3 xyy() {return new Vec3(x,y,y);}
	public Vec3 xyz() {return new Vec3(x,y,z);}
	public Vec3 xyw() {return new Vec3(x,y,w);}
	public Vec3 xzx() {return new Vec3(x,z,x);}
	public Vec3 xzy() {return new Vec3(x,z,y);}
	public Vec3 xzz() {return new Vec3(x,z,z);}
	public Vec3 xzw() {return new Vec3(x,z,w);}
	public Vec3 xwx() {return new Vec3(x,w,x);}
	public Vec3 xwy() {return new Vec3(x,w,y);}
	public Vec3 xwz() {return new Vec3(x,w,z);}
	public Vec3 xww() {return new Vec3(x,w,w);}
	public Vec3 yxx() {return new Vec3(y,x,x);}
	public Vec3 yxy() {return new Vec3(y,x,y);}
	public Vec3 yxz() {return new Vec3(y,x,z);}
	public Vec3 yxw() {return new Vec3(y,x,w);}
	public Vec3 yyx() {return new Vec3(y,y,x);}
	public Vec3 yyy() {return new Vec3(y,y,y);}
	public Vec3 yyz() {return new Vec3(y,y,z);}
	public Vec3 yyw() {return new Vec3(y,y,w);}
	public Vec3 yzx() {return new Vec3(y,z,x);}
	public Vec3 yzy() {return new Vec3(y,z,y);}
	public Vec3 yzz() {return new Vec3(y,z,z);}
	public Vec3 yzw() {return new Vec3(y,z,w);}
	public Vec3 ywx() {return new Vec3(y,w,x);}
	public Vec3 ywy() {return new Vec3(y,w,y);}
	public Vec3 ywz() {return new Vec3(y,w,z);}
	public Vec3 yww() {return new Vec3(y,w,w);}
	public Vec3 zxx() {return new Vec3(z,x,x);}
	public Vec3 zxy() {return new Vec3(z,x,y);}
	public Vec3 zxz() {return new Vec3(z,x,z);}
	public Vec3 zxw() {return new Vec3(z,x,w);}
	public Vec3 zyx() {return new Vec3(z,y,x);}
	public Vec3 zyy() {return new Vec3(z,y,y);}
	public Vec3 zyz() {return new Vec3(z,y,z);}
	public Vec3 zyw() {return new Vec3(z,y,w);}
	public Vec3 zzx() {return new Vec3(z,z,x);}
	public Vec3 zzy() {return new Vec3(z,z,y);}
	public Vec3 zzz() {return new Vec3(z,z,z);}
	public Vec3 zzw() {return new Vec3(z,z,w);}
	public Vec3 zwx() {return new Vec3(z,w,x);}
	public Vec3 zwy() {return new Vec3(z,w,y);}
	public Vec3 zwz() {return new Vec3(z,w,z);}
	public Vec3 zww() {return new Vec3(z,w,w);}
	public Vec3 wxx() {return new Vec3(w,x,x);}
	public Vec3 wxy() {return new Vec3(w,x,y);}
	public Vec3 wxz() {return new Vec3(w,x,z);}
	public Vec3 wxw() {return new Vec3(w,x,w);}
	public Vec3 wyx() {return new Vec3(w,y,x);}
	public Vec3 wyy() {return new Vec3(w,y,y);}
	public Vec3 wyz() {return new Vec3(w,y,z);}
	public Vec3 wyw() {return new Vec3(w,y,w);}
	public Vec3 wzx() {return new Vec3(w,z,x);}
	public Vec3 wzy() {return new Vec3(w,z,y);}
	public Vec3 wzz() {return new Vec3(w,z,z);}
	public Vec3 wzw() {return new Vec3(w,z,w);}
	public Vec3 wwx() {return new Vec3(w,w,x);}
	public Vec3 wwy() {return new Vec3(w,w,y);}
	public Vec3 wwz() {return new Vec3(w,w,z);}
	public Vec3 www() {return new Vec3(w,w,w);}

	public Vec4 xxxx() {return new Vec4(x,x,x,x);}
	public Vec4 xxxy() {return new Vec4(x,x,x,y);}
	public Vec4 xxxz() {return new Vec4(x,x,x,z);}
	public Vec4 xxxw() {return new Vec4(x,x,x,w);}
	public Vec4 xxyx() {return new Vec4(x,x,y,x);}
	public Vec4 xxyy() {return new Vec4(x,x,y,y);}
	public Vec4 xxyz() {return new Vec4(x,x,y,z);}
	public Vec4 xxyw() {return new Vec4(x,x,y,w);}
	public Vec4 xxzx() {return new Vec4(x,x,z,x);}
	public Vec4 xxzy() {return new Vec4(x,x,z,y);}
	public Vec4 xxzz() {return new Vec4(x,x,z,z);}
	public Vec4 xxzw() {return new Vec4(x,x,z,w);}
	public Vec4 xxwx() {return new Vec4(x,x,w,x);}
	public Vec4 xxwy() {return new Vec4(x,x,w,y);}
	public Vec4 xxwz() {return new Vec4(x,x,w,z);}
	public Vec4 xxww() {return new Vec4(x,x,w,w);}
	public Vec4 xyxx() {return new Vec4(x,y,x,x);}
	public Vec4 xyxy() {return new Vec4(x,y,x,y);}
	public Vec4 xyxz() {return new Vec4(x,y,x,z);}
	public Vec4 xyxw() {return new Vec4(x,y,x,w);}
	public Vec4 xyyx() {return new Vec4(x,y,y,x);}
	public Vec4 xyyy() {return new Vec4(x,y,y,y);}
	public Vec4 xyyz() {return new Vec4(x,y,y,z);}
	public Vec4 xyyw() {return new Vec4(x,y,y,w);}
	public Vec4 xyzx() {return new Vec4(x,y,z,x);}
	public Vec4 xyzy() {return new Vec4(x,y,z,y);}
	public Vec4 xyzz() {return new Vec4(x,y,z,z);}
	public Vec4 xyzw() {return this;}
	public Vec4 xywx() {return new Vec4(x,y,w,x);}
	public Vec4 xywy() {return new Vec4(x,y,w,y);}
	public Vec4 xywz() {return new Vec4(x,y,w,z);}
	public Vec4 xyww() {return new Vec4(x,y,w,w);}
	public Vec4 xzxx() {return new Vec4(x,z,x,x);}
	public Vec4 xzxy() {return new Vec4(x,z,x,y);}
	public Vec4 xzxz() {return new Vec4(x,z,x,z);}
	public Vec4 xzxw() {return new Vec4(x,z,x,w);}
	public Vec4 xzyx() {return new Vec4(x,z,y,x);}
	public Vec4 xzyy() {return new Vec4(x,z,y,y);}
	public Vec4 xzyz() {return new Vec4(x,z,y,z);}
	public Vec4 xzyw() {return new Vec4(x,z,y,w);}
	public Vec4 xzzx() {return new Vec4(x,z,z,x);}
	public Vec4 xzzy() {return new Vec4(x,z,z,y);}
	public Vec4 xzzz() {return new Vec4(x,z,z,z);}
	public Vec4 xzzw() {return new Vec4(x,z,z,w);}
	public Vec4 xzwx() {return new Vec4(x,z,w,x);}
	public Vec4 xzwy() {return new Vec4(x,z,w,y);}
	public Vec4 xzwz() {return new Vec4(x,z,w,z);}
	public Vec4 xzww() {return new Vec4(x,z,w,w);}
	public Vec4 xwxx() {return new Vec4(x,w,x,x);}
	public Vec4 xwxy() {return new Vec4(x,w,x,y);}
	public Vec4 xwxz() {return new Vec4(x,w,x,z);}
	public Vec4 xwxw() {return new Vec4(x,w,x,w);}
	public Vec4 xwyx() {return new Vec4(x,w,y,x);}
	public Vec4 xwyy() {return new Vec4(x,w,y,y);}
	public Vec4 xwyz() {return new Vec4(x,w,y,z);}
	public Vec4 xwyw() {return new Vec4(x,w,y,w);}
	public Vec4 xwzx() {return new Vec4(x,w,z,x);}
	public Vec4 xwzy() {return new Vec4(x,w,z,y);}
	public Vec4 xwzz() {return new Vec4(x,w,z,z);}
	public Vec4 xwzw() {return new Vec4(x,w,z,w);}
	public Vec4 xwwx() {return new Vec4(x,w,w,x);}
	public Vec4 xwwy() {return new Vec4(x,w,w,y);}
	public Vec4 xwwz() {return new Vec4(x,w,w,z);}
	public Vec4 xwww() {return new Vec4(x,w,w,w);}
	public Vec4 yxxx() {return new Vec4(y,x,x,x);}
	public Vec4 yxxy() {return new Vec4(y,x,x,y);}
	public Vec4 yxxz() {return new Vec4(y,x,x,z);}
	public Vec4 yxxw() {return new Vec4(y,x,x,w);}
	public Vec4 yxyx() {return new Vec4(y,x,y,x);}
	public Vec4 yxyy() {return new Vec4(y,x,y,y);}
	public Vec4 yxyz() {return new Vec4(y,x,y,z);}
	public Vec4 yxyw() {return new Vec4(y,x,y,w);}
	public Vec4 yxzx() {return new Vec4(y,x,z,x);}
	public Vec4 yxzy() {return new Vec4(y,x,z,y);}
	public Vec4 yxzz() {return new Vec4(y,x,z,z);}
	public Vec4 yxzw() {return new Vec4(y,x,z,w);}
	public Vec4 yxwx() {return new Vec4(y,x,w,x);}
	public Vec4 yxwy() {return new Vec4(y,x,w,y);}
	public Vec4 yxwz() {return new Vec4(y,x,w,z);}
	public Vec4 yxww() {return new Vec4(y,x,w,w);}
	public Vec4 yyxx() {return new Vec4(y,y,x,x);}
	public Vec4 yyxy() {return new Vec4(y,y,x,y);}
	public Vec4 yyxz() {return new Vec4(y,y,x,z);}
	public Vec4 yyxw() {return new Vec4(y,y,x,w);}
	public Vec4 yyyx() {return new Vec4(y,y,y,x);}
	public Vec4 yyyy() {return new Vec4(y,y,y,y);}
	public Vec4 yyyz() {return new Vec4(y,y,y,z);}
	public Vec4 yyyw() {return new Vec4(y,y,y,w);}
	public Vec4 yyzx() {return new Vec4(y,y,z,x);}
	public Vec4 yyzy() {return new Vec4(y,y,z,y);}
	public Vec4 yyzz() {return new Vec4(y,y,z,z);}
	public Vec4 yyzw() {return new Vec4(y,y,z,w);}
	public Vec4 yywx() {return new Vec4(y,y,w,x);}
	public Vec4 yywy() {return new Vec4(y,y,w,y);}
	public Vec4 yywz() {return new Vec4(y,y,w,z);}
	public Vec4 yyww() {return new Vec4(y,y,w,w);}
	public Vec4 yzxx() {return new Vec4(y,z,x,x);}
	public Vec4 yzxy() {return new Vec4(y,z,x,y);}
	public Vec4 yzxz() {return new Vec4(y,z,x,z);}
	public Vec4 yzxw() {return new Vec4(y,z,x,w);}
	public Vec4 yzyx() {return new Vec4(y,z,y,x);}
	public Vec4 yzyy() {return new Vec4(y,z,y,y);}
	public Vec4 yzyz() {return new Vec4(y,z,y,z);}
	public Vec4 yzyw() {return new Vec4(y,z,y,w);}
	public Vec4 yzzx() {return new Vec4(y,z,z,x);}
	public Vec4 yzzy() {return new Vec4(y,z,z,y);}
	public Vec4 yzzz() {return new Vec4(y,z,z,z);}
	public Vec4 yzzw() {return new Vec4(y,z,z,w);}
	public Vec4 yzwx() {return new Vec4(y,z,w,x);}
	public Vec4 yzwy() {return new Vec4(y,z,w,y);}
	public Vec4 yzwz() {return new Vec4(y,z,w,z);}
	public Vec4 yzww() {return new Vec4(y,z,w,w);}
	public Vec4 ywxx() {return new Vec4(y,w,x,x);}
	public Vec4 ywxy() {return new Vec4(y,w,x,y);}
	public Vec4 ywxz() {return new Vec4(y,w,x,z);}
	public Vec4 ywxw() {return new Vec4(y,w,x,w);}
	public Vec4 ywyx() {return new Vec4(y,w,y,x);}
	public Vec4 ywyy() {return new Vec4(y,w,y,y);}
	public Vec4 ywyz() {return new Vec4(y,w,y,z);}
	public Vec4 ywyw() {return new Vec4(y,w,y,w);}
	public Vec4 ywzx() {return new Vec4(y,w,z,x);}
	public Vec4 ywzy() {return new Vec4(y,w,z,y);}
	public Vec4 ywzz() {return new Vec4(y,w,z,z);}
	public Vec4 ywzw() {return new Vec4(y,w,z,w);}
	public Vec4 ywwx() {return new Vec4(y,w,w,x);}
	public Vec4 ywwy() {return new Vec4(y,w,w,y);}
	public Vec4 ywwz() {return new Vec4(y,w,w,z);}
	public Vec4 ywww() {return new Vec4(y,w,w,w);}
	public Vec4 zxxx() {return new Vec4(z,x,x,x);}
	public Vec4 zxxy() {return new Vec4(z,x,x,y);}
	public Vec4 zxxz() {return new Vec4(z,x,x,z);}
	public Vec4 zxxw() {return new Vec4(z,x,x,w);}
	public Vec4 zxyx() {return new Vec4(z,x,y,x);}
	public Vec4 zxyy() {return new Vec4(z,x,y,y);}
	public Vec4 zxyz() {return new Vec4(z,x,y,z);}
	public Vec4 zxyw() {return new Vec4(z,x,y,w);}
	public Vec4 zxzx() {return new Vec4(z,x,z,x);}
	public Vec4 zxzy() {return new Vec4(z,x,z,y);}
	public Vec4 zxzz() {return new Vec4(z,x,z,z);}
	public Vec4 zxzw() {return new Vec4(z,x,z,w);}
	public Vec4 zxwx() {return new Vec4(z,x,w,x);}
	public Vec4 zxwy() {return new Vec4(z,x,w,y);}
	public Vec4 zxwz() {return new Vec4(z,x,w,z);}
	public Vec4 zxww() {return new Vec4(z,x,w,w);}
	public Vec4 zyxx() {return new Vec4(z,y,x,x);}
	public Vec4 zyxy() {return new Vec4(z,y,x,y);}
	public Vec4 zyxz() {return new Vec4(z,y,x,z);}
	public Vec4 zyxw() {return new Vec4(z,y,x,w);}
	public Vec4 zyyx() {return new Vec4(z,y,y,x);}
	public Vec4 zyyy() {return new Vec4(z,y,y,y);}
	public Vec4 zyyz() {return new Vec4(z,y,y,z);}
	public Vec4 zyyw() {return new Vec4(z,y,y,w);}
	public Vec4 zyzx() {return new Vec4(z,y,z,x);}
	public Vec4 zyzy() {return new Vec4(z,y,z,y);}
	public Vec4 zyzz() {return new Vec4(z,y,z,z);}
	public Vec4 zyzw() {return new Vec4(z,y,z,w);}
	public Vec4 zywx() {return new Vec4(z,y,w,x);}
	public Vec4 zywy() {return new Vec4(z,y,w,y);}
	public Vec4 zywz() {return new Vec4(z,y,w,z);}
	public Vec4 zyww() {return new Vec4(z,y,w,w);}
	public Vec4 zzxx() {return new Vec4(z,z,x,x);}
	public Vec4 zzxy() {return new Vec4(z,z,x,y);}
	public Vec4 zzxz() {return new Vec4(z,z,x,z);}
	public Vec4 zzxw() {return new Vec4(z,z,x,w);}
	public Vec4 zzyx() {return new Vec4(z,z,y,x);}
	public Vec4 zzyy() {return new Vec4(z,z,y,y);}
	public Vec4 zzyz() {return new Vec4(z,z,y,z);}
	public Vec4 zzyw() {return new Vec4(z,z,y,w);}
	public Vec4 zzzx() {return new Vec4(z,z,z,x);}
	public Vec4 zzzy() {return new Vec4(z,z,z,y);}
	public Vec4 zzzz() {return new Vec4(z,z,z,z);}
	public Vec4 zzzw() {return new Vec4(z,z,z,w);}
	public Vec4 zzwx() {return new Vec4(z,z,w,x);}
	public Vec4 zzwy() {return new Vec4(z,z,w,y);}
	public Vec4 zzwz() {return new Vec4(z,z,w,z);}
	public Vec4 zzww() {return new Vec4(z,z,w,w);}
	public Vec4 zwxx() {return new Vec4(z,w,x,x);}
	public Vec4 zwxy() {return new Vec4(z,w,x,y);}
	public Vec4 zwxz() {return new Vec4(z,w,x,z);}
	public Vec4 zwxw() {return new Vec4(z,w,x,w);}
	public Vec4 zwyx() {return new Vec4(z,w,y,x);}
	public Vec4 zwyy() {return new Vec4(z,w,y,y);}
	public Vec4 zwyz() {return new Vec4(z,w,y,z);}
	public Vec4 zwyw() {return new Vec4(z,w,y,w);}
	public Vec4 zwzx() {return new Vec4(z,w,z,x);}
	public Vec4 zwzy() {return new Vec4(z,w,z,y);}
	public Vec4 zwzz() {return new Vec4(z,w,z,z);}
	public Vec4 zwzw() {return new Vec4(z,w,z,w);}
	public Vec4 zwwx() {return new Vec4(z,w,w,x);}
	public Vec4 zwwy() {return new Vec4(z,w,w,y);}
	public Vec4 zwwz() {return new Vec4(z,w,w,z);}
	public Vec4 zwww() {return new Vec4(z,w,w,w);}
	public Vec4 wxxx() {return new Vec4(w,x,x,x);}
	public Vec4 wxxy() {return new Vec4(w,x,x,y);}
	public Vec4 wxxz() {return new Vec4(w,x,x,z);}
	public Vec4 wxxw() {return new Vec4(w,x,x,w);}
	public Vec4 wxyx() {return new Vec4(w,x,y,x);}
	public Vec4 wxyy() {return new Vec4(w,x,y,y);}
	public Vec4 wxyz() {return new Vec4(w,x,y,z);}
	public Vec4 wxyw() {return new Vec4(w,x,y,w);}
	public Vec4 wxzx() {return new Vec4(w,x,z,x);}
	public Vec4 wxzy() {return new Vec4(w,x,z,y);}
	public Vec4 wxzz() {return new Vec4(w,x,z,z);}
	public Vec4 wxzw() {return new Vec4(w,x,z,w);}
	public Vec4 wxwx() {return new Vec4(w,x,w,x);}
	public Vec4 wxwy() {return new Vec4(w,x,w,y);}
	public Vec4 wxwz() {return new Vec4(w,x,w,z);}
	public Vec4 wxww() {return new Vec4(w,x,w,w);}
	public Vec4 wyxx() {return new Vec4(w,y,x,x);}
	public Vec4 wyxy() {return new Vec4(w,y,x,y);}
	public Vec4 wyxz() {return new Vec4(w,y,x,z);}
	public Vec4 wyxw() {return new Vec4(w,y,x,w);}
	public Vec4 wyyx() {return new Vec4(w,y,y,x);}
	public Vec4 wyyy() {return new Vec4(w,y,y,y);}
	public Vec4 wyyz() {return new Vec4(w,y,y,z);}
	public Vec4 wyyw() {return new Vec4(w,y,y,w);}
	public Vec4 wyzx() {return new Vec4(w,y,z,x);}
	public Vec4 wyzy() {return new Vec4(w,y,z,y);}
	public Vec4 wyzz() {return new Vec4(w,y,z,z);}
	public Vec4 wyzw() {return new Vec4(w,y,z,w);}
	public Vec4 wywx() {return new Vec4(w,y,w,x);}
	public Vec4 wywy() {return new Vec4(w,y,w,y);}
	public Vec4 wywz() {return new Vec4(w,y,w,z);}
	public Vec4 wyww() {return new Vec4(w,y,w,w);}
	public Vec4 wzxx() {return new Vec4(w,z,x,x);}
	public Vec4 wzxy() {return new Vec4(w,z,x,y);}
	public Vec4 wzxz() {return new Vec4(w,z,x,z);}
	public Vec4 wzxw() {return new Vec4(w,z,x,w);}
	public Vec4 wzyx() {return new Vec4(w,z,y,x);}
	public Vec4 wzyy() {return new Vec4(w,z,y,y);}
	public Vec4 wzyz() {return new Vec4(w,z,y,z);}
	public Vec4 wzyw() {return new Vec4(w,z,y,w);}
	public Vec4 wzzx() {return new Vec4(w,z,z,x);}
	public Vec4 wzzy() {return new Vec4(w,z,z,y);}
	public Vec4 wzzz() {return new Vec4(w,z,z,z);}
	public Vec4 wzzw() {return new Vec4(w,z,z,w);}
	public Vec4 wzwx() {return new Vec4(w,z,w,x);}
	public Vec4 wzwy() {return new Vec4(w,z,w,y);}
	public Vec4 wzwz() {return new Vec4(w,z,w,z);}
	public Vec4 wzww() {return new Vec4(w,z,w,w);}
	public Vec4 wwxx() {return new Vec4(w,w,x,x);}
	public Vec4 wwxy() {return new Vec4(w,w,x,y);}
	public Vec4 wwxz() {return new Vec4(w,w,x,z);}
	public Vec4 wwxw() {return new Vec4(w,w,x,w);}
	public Vec4 wwyx() {return new Vec4(w,w,y,x);}
	public Vec4 wwyy() {return new Vec4(w,w,y,y);}
	public Vec4 wwyz() {return new Vec4(w,w,y,z);}
	public Vec4 wwyw() {return new Vec4(w,w,y,w);}
	public Vec4 wwzx() {return new Vec4(w,w,z,x);}
	public Vec4 wwzy() {return new Vec4(w,w,z,y);}
	public Vec4 wwzz() {return new Vec4(w,w,z,z);}
	public Vec4 wwzw() {return new Vec4(w,w,z,w);}
	public Vec4 wwwx() {return new Vec4(w,w,w,x);}
	public Vec4 wwwy() {return new Vec4(w,w,w,y);}
	public Vec4 wwwz() {return new Vec4(w,w,w,z);}
	public Vec4 wwww() {return new Vec4(w,w,w,w);}
	// @formatter:on
	// #endregion

}
