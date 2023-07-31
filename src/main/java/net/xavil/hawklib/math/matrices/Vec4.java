package net.xavil.hawklib.math.matrices;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec4Access;

public final class Vec4 implements Hashable, Vec4Access {

	@SuppressWarnings("null")
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

	// @formatter:off
	@Override public final double x() {return x;}
	@Override public final double y() {return y;}
	@Override public final double z() {return z;}
	@Override public final double w() {return w;}
	public Vec4 withX(double x) {return new Vec4(x, y, z, w);}
	public Vec4 withY(double y) {return new Vec4(x, y, z, w);}
	public Vec4 withZ(double z) {return new Vec4(x, y, z, w);}
	public Vec4 withW(double w) {return new Vec4(x, y, z, w);}

	public static Vec4 broadcast(double n)                          {return new Vec4(n, n, n, n);}
	public static Vec4 from(double x, double y, double z, double w) {return new Vec4(x, y, z, w);}
	public static Vec4 from(Vec3Access vec, double w)               {return new Vec4(vec.x(), vec.y(), vec.z(), w);}
	public static Vec4 from(Vector4f vec)                           {return new Vec4(vec.x(), vec.y(), vec.z(), vec.w());}

	public Vec4 add  (Vec4Access rhs) {return new Vec4(this.x + rhs.x(), this.y + rhs.y(), this.z + rhs.z(), this.w + rhs.w());}
	public Vec4 sub  (Vec4Access rhs) {return new Vec4(this.x - rhs.x(), this.y - rhs.y(), this.z - rhs.z(), this.w - rhs.w());}
	public Vec4 mul  (Vec4Access rhs) {return new Vec4(this.x * rhs.x(), this.y * rhs.y(), this.z * rhs.z(), this.w * rhs.w());}
	public Vec4 div  (Vec4Access rhs) {return new Vec4(this.x / rhs.x(), this.y / rhs.y(), this.z / rhs.z(), this.w / rhs.w());}
	public Vec4 mul  (double n)       {return new Vec4(this.x * n,       this.y * n,       this.z * n,       this.w * n      );}
	public Vec4 div  (double n)       {return new Vec4(this.x / n,       this.y / n,       this.z / n,       this.w / n      );}
	public Vec4 recip(double n)       {return new Vec4(n / x,            n / y,            n / z,            n / w           );}
	public Vec4 recip()               {return new Vec4(1.0 / x,          1.0 / y,          1.0 / z,          1.0 / w         );}
	public Vec4 neg  ()               {return new Vec4(-x,               -y,               -z,               -w              );}
	// @formatter:on

	public Vec3 perspectiveDivision() {
		return new Vec3(x / w, y / w, z / w);
	}

	public Vec4 normalize() {
		return mul(1 / length());
	}

	public Vec4 projectOnto(Vec4 other) {
		final var b = other.normalize();
		return b.mul(this.dot(b));
	}

	public Vec4 transformBy(PoseStack.Pose pose) {
		return transformBy(pose.pose());
	}

	public Vec4 transformBy(Matrix4f matrix) {
		final var vec = asMinecraft();
		vec.transform(matrix);
		return from(vec);
	}

	public Vec4 transformBy(Mat4 matrix) {
		return matrix.mul(this);
	}

	public static Vec4 lerp(double delta, Vec4 a, Vec4 b) {
		return new Vec4(
				Mth.lerp(delta, a.x, b.x),
				Mth.lerp(delta, a.y, b.y),
				Mth.lerp(delta, a.z, b.z),
				Mth.lerp(delta, a.w, b.w));
	}

	@Override
	public String toString() {
		return "Vec4[" + x + ", " + y + ", " + z + ", " + w + "]";
	}

	@Override
	public boolean equals(Object arg0) {
		if (arg0 instanceof Vec4Access other) {
			return this.x == other.x() && this.y == other.y() && this.z == other.z() && this.w == other.w();
		}
		return false;
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendDouble(this.x).appendDouble(this.y).appendDouble(this.z).appendDouble(this.w);
	}

	public static final class Mutable implements Hashable, Vec4Access {

		public double x, y, z, w;

		public Mutable(double x, double y, double z, double w) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.w = w;
		}

		// @formatter:off
		@Override public final double x() {return x;}
		@Override public final double y() {return y;}
		@Override public final double z() {return z;}
		@Override public final double w() {return w;}
		public Mutable withX(double x)    {this.x = x; return this;}
		public Mutable withY(double y)    {this.y = y; return this;}
		public Mutable withZ(double z)    {this.z = z; return this;}
		public Mutable withW(double w)    {this.w = w; return this;}

		public Mutable loadUniform(double n)                        {this.x = n;               this.y = n;               this.z = n;               this.w = n;           return this;}
		public Mutable load(double x, double y, double z, double w) {this.x = x;               this.y = y;               this.z = z;               this.w = w;           return this;}
		public Mutable load(Vec4Access vec)                         {this.x = vec.x();         this.y = vec.y();         this.z = vec.z();         this.w = vec.w();     return this;}
		public Mutable load(Vec3Access vec, double w)               {this.x = vec.x();         this.y = vec.y();         this.z = vec.z();         this.w = w;           return this;}
		public Mutable addAssign(Vec4Access other)                  {this.x += other.x();      this.y += other.y();      this.z += other.z();      this.w += other.w();  return this;}
		public Mutable subAssign(Vec4Access other)                  {this.x -= other.x();      this.y -= other.y();      this.z -= other.z();      this.w -= other.w();  return this;}
		public Mutable mulAssign(Vec4Access other)                  {this.x *= other.x();      this.y *= other.y();      this.z *= other.z();      this.w *= other.w();  return this;}
		public Mutable divAssign(Vec4Access other)                  {this.x /= other.x();      this.y /= other.y();      this.z /= other.z();      this.w /= other.w();  return this;}
		public Mutable mulAssign(double n)                          {this.x *= n;              this.y *= n;              this.z *= n;              this.w *= n;          return this;}
		public Mutable divAssign(double n)                          {this.x /= n;              this.y /= n;              this.z /= n;              this.w /= n;          return this;}
		public Mutable recipAssign(double n)                        {this.x = n / x;           this.y = n / y;           this.z = n / z;           this.w = n / w;       return this;}
		public Mutable recipAssign()                                {this.x = 1.0 / x;         this.y = 1.0 / y;         this.z = 1.0 / z;         this.w = 1.0 / w;     return this;}
		public Mutable negAssign()                                  {this.x = -x;              this.y = -y;              this.z = -z;              this.w = -w;          return this;}
		public Mutable perspectiveDivision()                        {this.x = this.x / this.w; this.y = this.y / this.w; this.z = this.z / this.w; this.w = 1.0;         return this;}
		// @formatter:on

		public Mutable normalize() {
			final var l = length();
			this.x /= l;
			this.y /= l;
			this.z /= l;
			this.w /= l;
			return this;
		}

		@Override
		public String toString() {
			return "Vec4.Mutable[" + x + ", " + y + ", " + z + ", " + w + "]";
		}

		@Override
		public boolean equals(Object arg0) {
			if (arg0 instanceof Vec4Access other) {
				return this.x == other.x() && this.y == other.y() && this.z == other.z() && this.w == other.w();
			}
			return false;
		}

		@Override
		public int hashCode() {
			return FastHasher.hashToInt(this);
		}

		@Override
		public void appendHash(Hasher hasher) {
			hasher.appendDouble(this.x).appendDouble(this.y).appendDouble(this.z).appendDouble(this.w);
		}

	}

}
