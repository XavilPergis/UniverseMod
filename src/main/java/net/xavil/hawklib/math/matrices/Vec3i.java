package net.xavil.hawklib.math.matrices;

import java.util.function.Consumer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;
import net.xavil.hawklib.math.matrices.interfaces.Vec3iAccess;

public final class Vec3i implements Hashable, Vec3iAccess {

	public static final Vec3i ZERO = new Vec3i(0, 0, 0);
	public static final Vec3i XN = new Vec3i(-1, 0, 0);
	public static final Vec3i XP = new Vec3i(1, 0, 0);
	public static final Vec3i YN = new Vec3i(0, -1, 0);
	public static final Vec3i YP = new Vec3i(0, 1, 0);
	public static final Vec3i ZN = new Vec3i(0, 0, -1);
	public static final Vec3i ZP = new Vec3i(0, 0, 1);

	@SuppressWarnings("null")
	public static final Codec<Vec3i> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.INT.fieldOf("x").forGetter(v -> v.x),
			Codec.INT.fieldOf("y").forGetter(v -> v.y),
			Codec.INT.fieldOf("z").forGetter(v -> v.z))
			.apply(inst, Vec3i::new));

	public final int x, y, z;

	public Vec3i(Vec3iAccess other) {
		this(other.x(), other.y(), other.z());
	}

	public Vec3i(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public static net.minecraft.core.Vec3i toMinecraft(Vec3iAccess v) {
		return new net.minecraft.core.Vec3i(v.x(), v.y(), v.z());
	}

	// @formatter:off
	@Override public int x() {return this.x;}
	@Override public int y() {return this.y;}
	@Override public int z() {return this.z;}
	public Vec3i withX(int x) {return new Vec3i(x, y, z);}
	public Vec3i withY(int y) {return new Vec3i(x, y, z);}
	public Vec3i withZ(int z) {return new Vec3i(x, y, z);}
	public Vec4 withW(int w) {return new Vec4(x, y, z, w);}
	public static Vec3i broadcast(int n) {return new Vec3i(n, n, n);}
	public static Vec3i from(net.minecraft.core.Vec3i vec) {return new Vec3i(vec.getX(), vec.getY(), vec.getZ());}
	// @formatter:on
	
	// @formatter:off
	public Vec3i add  (Vec3iAccess rhs)     {return new Vec3i(this.x + rhs.x(), this.y + rhs.y(), this.z + rhs.z());}
	public Vec3i sub  (Vec3iAccess rhs)     {return new Vec3i(this.x - rhs.x(), this.y - rhs.y(), this.z - rhs.z());}
	public Vec3i mul  (Vec3iAccess rhs)     {return new Vec3i(this.x * rhs.x(), this.y * rhs.y(), this.z * rhs.z());}
	public Vec3i div  (Vec3iAccess rhs)     {return new Vec3i(this.x / rhs.x(), this.y / rhs.y(), this.z / rhs.z());}
	public Vec3i add  (int x, int y, int z) {return new Vec3i(this.x + x,       this.y + y,       this.z + z      );}
	public Vec3i sub  (int x, int y, int z) {return new Vec3i(this.x - x,       this.y - y,       this.z - z      );}
	public Vec3i mul  (int x, int y, int z) {return new Vec3i(this.x * x,       this.y * y,       this.z * z      );}
	public Vec3i div  (int x, int y, int z) {return new Vec3i(this.x / x,       this.y / y,       this.z / z      );}
	public Vec3i mul  (int n)               {return new Vec3i(this.x * n,       this.y * n,       this.z * n      );}
	public Vec3i div  (int n)               {return new Vec3i(this.x / n,       this.y / n,       this.z / n      );}
	public Vec3i recip(int n)               {return new Vec3i(n / x,            n / y,            n / z           );}
	public Vec3i neg  ()                    {return new Vec3i(-x,               -y,               -z              );}
	public Vec3i abs  ()                    {return new Vec3i(Mth.abs(x),       Mth.abs(y),       Mth.abs(z)      );}
	// @formatter:on
	public Vec3i floorDiv(int scalar) {
		return new Vec3i(Math.floorDiv(this.x, scalar), Math.floorDiv(this.y, scalar), Math.floorDiv(this.z, scalar));
	}
	
	// @formatter:off
	public static Mutable set(Mutable out, int n)                            {out.x = n;                 out.y = n;                 out.z = n;                 return out;}
	public static Mutable set(Mutable out, Vec3iAccess in)                   {out.x = in.x();            out.y = in.y();            out.z = in.z();            return out;}
	public static Mutable set(Mutable out, int x, int y, int z)              {out.x = x;                 out.y = y;                 out.z = z;                 return out;}
	public static Mutable add(Mutable out, Vec3iAccess lhs, Vec3iAccess rhs) {out.x = lhs.x() + rhs.x(); out.y = lhs.y() + rhs.y(); out.z = lhs.z() + rhs.z(); return out;}
	public static Mutable sub(Mutable out, Vec3iAccess lhs, Vec3iAccess rhs) {out.x = lhs.x() - rhs.x(); out.y = lhs.y() - rhs.y(); out.z = lhs.z() - rhs.z(); return out;}
	public static Mutable mul(Mutable out, Vec3iAccess lhs, Vec3iAccess rhs) {out.x = lhs.x() * rhs.x(); out.y = lhs.y() * rhs.y(); out.z = lhs.z() * rhs.z(); return out;}
	public static Mutable mul(Mutable out, int lhs, Vec3iAccess rhs)         {out.x = lhs * rhs.x();     out.y = lhs * rhs.y();     out.z = lhs * rhs.z();     return out;}
	public static Mutable mul(Mutable out, Vec3iAccess lhs, int rhs)         {out.x = lhs.x() * rhs;     out.y = lhs.y() * rhs;     out.z = lhs.z() * rhs;     return out;}
	public static Mutable div(Mutable out, Vec3iAccess lhs, Vec3iAccess rhs) {out.x = lhs.x() / rhs.x(); out.y = lhs.y() / rhs.y(); out.z = lhs.z() / rhs.z(); return out;}
	public static Mutable div(Mutable out, int lhs, Vec3iAccess rhs)         {out.x = lhs / rhs.x();     out.y = lhs / rhs.y();     out.z = lhs / rhs.z();     return out;}
	public static Mutable div(Mutable out, Vec3iAccess lhs, int rhs)         {out.x = lhs.x() / rhs;     out.y = lhs.y() / rhs;     out.z = lhs.z() / rhs;     return out;}
	public static Mutable neg(Mutable out, Vec3iAccess in)                   {out.x = -in.x();           out.y = -in.y();           out.z = -in.z();           return out;}
	public static Mutable abs(Mutable out, Vec3iAccess in)                   {out.x = Math.abs(in.x());  out.y = Math.abs(in.y());  out.z = Math.abs(in.z());  return out;}
	// @formatter:on
	public static Mutable floorDiv(Mutable out, Mutable in, int scalar) {
		out.x = Math.floorDiv(in.x, scalar);
		out.y = Math.floorDiv(in.y, scalar);
		out.z = Math.floorDiv(in.z, scalar);
		return out;
	}
	public static Mutable floorDiv(Mutable out, int scalar, Mutable in) {
		out.x = Math.floorDiv(scalar, in.x);
		out.y = Math.floorDiv(scalar, in.y);
		out.z = Math.floorDiv(scalar, in.z);
		return out;
	}

	public static final class Mutable implements Hashable, Vec3iAccess {
		public int x, y, z;

		public Mutable() {
		}

		public Mutable(Vec3iAccess other) {
			this(other.x(), other.y(), other.z());
		}

		public Mutable(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		// @formatter:off
		@Override public final int x() {return x;}
		@Override public final int y() {return y;}
		@Override public final int z() {return z;}
		public Mutable withX(int x)    {this.x = x; return this;}
		public Mutable withY(int y)    {this.y = y; return this;}
		public Mutable withZ(int z)    {this.z = z; return this;}

		@Override
		public String toString() {
			return String.format("(%d, %d, %d)", x, y, z);
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

	@Override
	public String toString() {
		return String.format("(%d, %d, %d)", x, y, z);
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

	public static Iterator<Vec3i> iterateInclusive(Vec3i min, Vec3i max) {
		return new Iterator<Vec3i>() {
			private int x = min.x, y = min.y, z = min.z;

			@Override
			public boolean hasNext() {
				return this.y <= max.y;
			}

			@Override
			public Vec3i next() {
				final var res = new Vec3i(this.x, this.y, this.z);

				this.x += 1;
				if (this.x > max.x) {
					this.x = min.x;
					this.z += 1;
					if (this.z > max.z) {
						this.z = min.z;
						this.y += 1;
					}
				}

				return res;
			}
			
		};
	}

}
