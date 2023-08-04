package net.xavil.hawklib.math.matrices;

import java.util.Random;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Position;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public final class Vec3 implements Hashable, Vec3Access {

	public static final Vec3 ZERO = new Vec3(0, 0, 0);
	public static final Vec3 XN = new Vec3(-1, 0, 0);
	public static final Vec3 XP = new Vec3(1, 0, 0);
	public static final Vec3 YN = new Vec3(0, -1, 0);
	public static final Vec3 YP = new Vec3(0, 1, 0);
	public static final Vec3 ZN = new Vec3(0, 0, -1);
	public static final Vec3 ZP = new Vec3(0, 0, 1);

	@SuppressWarnings("null")
	public static final Codec<Vec3> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.DOUBLE.fieldOf("x").forGetter(v -> v.x),
			Codec.DOUBLE.fieldOf("y").forGetter(v -> v.y),
			Codec.DOUBLE.fieldOf("z").forGetter(v -> v.z))
			.apply(inst, Vec3::new));

	public final double x, y, z;

	public Vec3(Vec3Access other) {
		this(other.x(), other.y(), other.z());
	}

	public Vec3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static net.minecraft.world.phys.Vec3 toMinecraft(Vec3Access v) {
		return new net.minecraft.world.phys.Vec3(v.x(), v.y(), v.z());
	}

	// @formatter:off
	@Override public final double x() {return x;}
	@Override public final double y() {return y;}
	@Override public final double z() {return z;}
	public Vec3 withX(double x) {return new Vec3(x, y, z);}
	public Vec3 withY(double y) {return new Vec3(x, y, z);}
	public Vec3 withZ(double z) {return new Vec3(x, y, z);}
	public Vec4 withW(double w) {return new Vec4(x, y, z, w);}
	public static Vec3 broadcast(double n) {return new Vec3(n, n, n);}
	public static Vec3 from(Vector3f vec)  {return new Vec3(vec.x(), vec.y(), vec.z());}
	public static Vec3 from(Position vec)  {return new Vec3(vec.x(), vec.y(), vec.z());}
	// @formatter:on
	
	// @formatter:off
	public Vec3 add  (Vec3Access rhs)               {return new Vec3(this.x + rhs.x(), this.y + rhs.y(), this.z + rhs.z());}
	public Vec3 sub  (Vec3Access rhs)               {return new Vec3(this.x - rhs.x(), this.y - rhs.y(), this.z - rhs.z());}
	public Vec3 mul  (Vec3Access rhs)               {return new Vec3(this.x * rhs.x(), this.y * rhs.y(), this.z * rhs.z());}
	public Vec3 div  (Vec3Access rhs)               {return new Vec3(this.x / rhs.x(), this.y / rhs.y(), this.z / rhs.z());}
	public Vec3 add  (double x, double y, double z) {return new Vec3(this.x + x,       this.y + y,       this.z + z      );}
	public Vec3 sub  (double x, double y, double z) {return new Vec3(this.x - x,       this.y - y,       this.z - z      );}
	public Vec3 mul  (double x, double y, double z) {return new Vec3(this.x * x,       this.y * y,       this.z * z      );}
	public Vec3 div  (double x, double y, double z) {return new Vec3(this.x / x,       this.y / y,       this.z / z      );}
	public Vec3 mul  (double n)                     {return new Vec3(this.x * n,       this.y * n,       this.z * n      );}
	public Vec3 div  (double n)                     {return new Vec3(this.x / n,       this.y / n,       this.z / n      );}
	public Vec3 recip(double n)                     {return new Vec3(n / x,            n / y,            n / z           );}
	public Vec3 recip()                             {return new Vec3(1.0 / x,          1.0 / y,          1.0 / z         );}
	public Vec3 neg  ()                             {return new Vec3(-x,               -y,               -z              );}
	public Vec3 abs  ()                             {return new Vec3(Math.abs(x),      Math.abs(y),      Math.abs(z)     );}
	// @formatter:on
	
	// @formatter:off
	public static Mutable set(Mutable out, double n)                       {out.x = n;                 out.y = n;                 out.z = n;                 return out;}
	public static Mutable set(Mutable out, Vec3Access in)                  {out.x = in.x();            out.y = in.y();            out.z = in.z();            return out;}
	public static Mutable set(Mutable out, double x, double y, double z)   {out.x = x;                 out.y = y;                 out.z = z;                 return out;}
	public static Mutable add(Mutable out, Vec3Access lhs, Vec3Access rhs) {out.x = lhs.x() + rhs.x(); out.y = lhs.y() + rhs.y(); out.z = lhs.z() + rhs.z(); return out;}
	public static Mutable sub(Mutable out, Vec3Access lhs, Vec3Access rhs) {out.x = lhs.x() - rhs.x(); out.y = lhs.y() - rhs.y(); out.z = lhs.z() - rhs.z(); return out;}
	public static Mutable mul(Mutable out, Vec3Access lhs, Vec3Access rhs) {out.x = lhs.x() * rhs.x(); out.y = lhs.y() * rhs.y(); out.z = lhs.z() * rhs.z(); return out;}
	public static Mutable mul(Mutable out, double lhs, Vec3Access rhs)     {out.x = lhs * rhs.x();     out.y = lhs * rhs.y();     out.z = lhs * rhs.z();     return out;}
	public static Mutable mul(Mutable out, Vec3Access lhs, double rhs)     {out.x = lhs.x() * rhs;     out.y = lhs.y() * rhs;     out.z = lhs.z() * rhs;     return out;}
	public static Mutable div(Mutable out, Vec3Access lhs, Vec3Access rhs) {out.x = lhs.x() / rhs.x(); out.y = lhs.y() / rhs.y(); out.z = lhs.z() / rhs.z(); return out;}
	public static Mutable div(Mutable out, double lhs, Vec3Access rhs)     {out.x = lhs / rhs.x();     out.y = lhs / rhs.y();     out.z = lhs / rhs.z();     return out;}
	public static Mutable div(Mutable out, Vec3Access lhs, double rhs)     {out.x = lhs.x() / rhs;     out.y = lhs.y() / rhs;     out.z = lhs.z() / rhs;     return out;}
	public static Mutable neg(Mutable out, Vec3Access in)                  {out.x = -in.x();           out.y = -in.y();           out.z = -in.z();           return out;}
	public static Mutable abs(Mutable out, Vec3Access in)                  {out.x = Math.abs(in.x());  out.y = Math.abs(in.y());  out.z = Math.abs(in.z());  return out;}
	// @formatter:on

	public static void cross(Mutable out, Vec3Access lhs, Vec3Access rhs) {
		final var x = lhs.y() * rhs.z() - lhs.z() * rhs.y();
		final var y = lhs.z() * rhs.x() - lhs.x() * rhs.z();
		final var z = lhs.x() * rhs.y() - lhs.y() * rhs.x();
		out.x = x;
		out.y = y;
		out.z = z;
	}

	public static void normalize(Mutable out, Vec3Access in) {
		div(out, in, in.length());
	}

	public static void projectOnto(Mutable out, Vec3Access a, Vec3Access b) {
		normalize(out, b);
		mul(out, out, a.dot(b));
	}

	public Vec3 cross(Vec3 other) {
		return new Vec3(this.y * other.z - this.z * other.y,
				this.z * other.x - this.x * other.z,
				this.x * other.y - this.y * other.x);
	}

	public Vec3 normalize() {
		return mul(1 / length());
	}

	public Vec3 projectOnto(Vec3 other) {
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
		return matrix.mul(this, w);
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

	public static double volume(Vec3 a, Vec3 b) {
		return Math.abs(a.x - b.x) * Math.abs(a.y - b.y) * Math.abs(a.z - b.z);
	}

	public static Vec3 lerp(double delta, Vec3Access a, Vec3Access b) {
		return new Vec3(
				Mth.lerp(delta, a.x(), b.x()),
				Mth.lerp(delta, a.y(), b.y()),
				Mth.lerp(delta, a.z(), b.z()));
	}

	public static Vec3 inverseLerp(double delta, Vec3Access a, Vec3Access b) {
		return new Vec3(
				Mth.inverseLerp(delta, a.x(), b.x()),
				Mth.inverseLerp(delta, a.y(), b.y()),
				Mth.inverseLerp(delta, a.z(), b.z()));
	}

	public Vec3i floor() {
		return new Vec3i(Mth.floor(x), Mth.floor(y), Mth.floor(z));
	}

	public Vec3i ceil() {
		return new Vec3i(Mth.ceil(x), Mth.ceil(y), Mth.ceil(z));
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

	public static Vec3 random(Rng random, Vec3 min, Vec3 max) {
		var x = random.uniformDouble(min.x, max.x);
		var y = random.uniformDouble(min.y, max.y);
		var z = random.uniformDouble(min.z, max.z);
		return new Vec3(x, y, z);
	}

	public static void loadRandom(Mutable out, Rng rng, Vec3Access min, Vec3Access max) {
		out.x = rng.uniformDouble(min.x(), max.x());
		out.y = rng.uniformDouble(min.y(), max.y());
		out.z = rng.uniformDouble(min.z(), max.z());
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
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendDouble(this.x).appendDouble(this.y).appendDouble(this.z);
	}

	public static final class Mutable implements Hashable, Vec3Access {
		public double x, y, z;

		public Mutable() {
		}

		public Mutable(Vec3Access other) {
			this(other.x(), other.y(), other.z());
		}

		public Mutable(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		// @formatter:off
		@Override public final double x() {return x;}
		@Override public final double y() {return y;}
		@Override public final double z() {return z;}
		public Mutable withX(double x)    {this.x = x; return this;}
		public Mutable withY(double y)    {this.y = y; return this;}
		public Mutable withZ(double z)    {this.z = z; return this;}

		// public Mutable loadUniform(double n)                   {this.x = n;           this.y = n;           this.z = n;           return this;}
		// public Mutable load(double x, double y, double z)      {this.x = x;           this.y = y;           this.z = z;           return this;}
		// public Mutable load(Vector3f vec)                      {this.x = vec.x();     this.y = vec.y();     this.z = vec.z();     return this;}
		// public Mutable load(Position vec)                      {this.x = vec.x();     this.y = vec.y();     this.z = vec.z();     return this;}
		// public Mutable load(Vec3Access vec)                    {this.x = vec.x();     this.y = vec.y();     this.z = vec.z();     return this;}
		// public Mutable addAssign(Vec3Access other)             {this.x += other.x();  this.y += other.y();  this.z += other.z();  return this;}
		// public Mutable subAssign(Vec3Access other)             {this.x -= other.x();  this.y -= other.y();  this.z -= other.z();  return this;}
		// public Mutable mulAssign(Vec3Access other)             {this.x *= other.x();  this.y *= other.y();  this.z *= other.z();  return this;}
		// public Mutable divAssign(Vec3Access other)             {this.x /= other.x();  this.y /= other.y();  this.z /= other.z();  return this;}
		// public Mutable addAssign(double x, double y, double z) {this.x += x;          this.y += y;          this.z += z;          return this;}
		// public Mutable subAssign(double x, double y, double z) {this.x -= x;          this.y -= y;          this.z -= z;          return this;}
		// public Mutable mulAssign(double x, double y, double z) {this.x *= x;          this.y *= y;          this.z *= z;          return this;}
		// public Mutable divAssign(double x, double y, double z) {this.x /= x;          this.y /= y;          this.z /= z;          return this;}
		// public Mutable mulAssign(double n)                     {this.x *= n;          this.y *= n;          this.z *= n;          return this;}
		// public Mutable divAssign(double n)                     {this.x /= n;          this.y /= n;          this.z /= n;          return this;}
		// public Mutable recipAssign(double n)                   {this.x = n / x;       this.y = n / y;       this.z = n / z;       return this;}
		// public Mutable recipAssign()                           {this.x = 1.0 / x;     this.y = 1.0 / y;     this.z = 1.0 / z;     return this;}
		// public Mutable negAssign()                             {this.x = -x;          this.y = -y;          this.z = -z;          return this;}
		// public Mutable absAssign()                             {this.x = Math.abs(x); this.y = Math.abs(y); this.z = Math.abs(z); return this;}
		// @formatter:on

		@Override
		public String toString() {
			return "Vec3.Mutable[" + x + ", " + y + ", " + z + "]";
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
			return FastHasher.hashToInt(this);
		}

		@Override
		public void appendHash(Hasher hasher) {
			hasher.appendDouble(this.x).appendDouble(this.y).appendDouble(this.z);
		}

	}

}
