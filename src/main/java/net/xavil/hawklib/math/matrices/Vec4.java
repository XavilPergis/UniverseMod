package net.xavil.hawklib.math.matrices;

import com.mojang.math.Vector4f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec4Access;

public final class Vec4 implements Hashable, Vec4Access {

	public static final Codec<Vec4> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.DOUBLE.fieldOf("x").forGetter(v -> v.x),
			Codec.DOUBLE.fieldOf("y").forGetter(v -> v.y),
			Codec.DOUBLE.fieldOf("z").forGetter(v -> v.z),
			Codec.DOUBLE.fieldOf("w").forGetter(v -> v.w))
			.apply(inst, Vec4::new));

	// @formatter:off
	public static final Vec4 NNNN = new Vec4(-1, -1, -1, -1);
	public static final Vec4 NNNC = new Vec4(-1, -1, -1,  0);
	public static final Vec4 NNNP = new Vec4(-1, -1, -1,  1);
	public static final Vec4 NNCN = new Vec4(-1, -1,  0, -1);
	public static final Vec4 NNCC = new Vec4(-1, -1,  0,  0);
	public static final Vec4 NNCP = new Vec4(-1, -1,  0,  1);
	public static final Vec4 NNPN = new Vec4(-1, -1,  1, -1);
	public static final Vec4 NNPC = new Vec4(-1, -1,  1,  0);
	public static final Vec4 NNPP = new Vec4(-1, -1,  1,  1);
	public static final Vec4 NCNN = new Vec4(-1,  0, -1, -1);
	public static final Vec4 NCNC = new Vec4(-1,  0, -1,  0);
	public static final Vec4 NCNP = new Vec4(-1,  0, -1,  1);
	public static final Vec4 NCCN = new Vec4(-1,  0,  0, -1);
	public static final Vec4 NCCC = new Vec4(-1,  0,  0,  0);
	public static final Vec4 NCCP = new Vec4(-1,  0,  0,  1);
	public static final Vec4 NCPN = new Vec4(-1,  0,  1, -1);
	public static final Vec4 NCPC = new Vec4(-1,  0,  1,  0);
	public static final Vec4 NCPP = new Vec4(-1,  0,  1,  1);
	public static final Vec4 NPNN = new Vec4(-1,  1, -1, -1);
	public static final Vec4 NPNC = new Vec4(-1,  1, -1,  0);
	public static final Vec4 NPNP = new Vec4(-1,  1, -1,  1);
	public static final Vec4 NPCN = new Vec4(-1,  1,  0, -1);
	public static final Vec4 NPCC = new Vec4(-1,  1,  0,  0);
	public static final Vec4 NPCP = new Vec4(-1,  1,  0,  1);
	public static final Vec4 NPPN = new Vec4(-1,  1,  1, -1);
	public static final Vec4 NPPC = new Vec4(-1,  1,  1,  0);
	public static final Vec4 NPPP = new Vec4(-1,  1,  1,  1);
	public static final Vec4 CNNN = new Vec4( 0, -1, -1, -1);
	public static final Vec4 CNNC = new Vec4( 0, -1, -1,  0);
	public static final Vec4 CNNP = new Vec4( 0, -1, -1,  1);
	public static final Vec4 CNCN = new Vec4( 0, -1,  0, -1);
	public static final Vec4 CNCC = new Vec4( 0, -1,  0,  0);
	public static final Vec4 CNCP = new Vec4( 0, -1,  0,  1);
	public static final Vec4 CNPN = new Vec4( 0, -1,  1, -1);
	public static final Vec4 CNPC = new Vec4( 0, -1,  1,  0);
	public static final Vec4 CNPP = new Vec4( 0, -1,  1,  1);
	public static final Vec4 CCNN = new Vec4( 0,  0, -1, -1);
	public static final Vec4 CCNC = new Vec4( 0,  0, -1,  0);
	public static final Vec4 CCNP = new Vec4( 0,  0, -1,  1);
	public static final Vec4 CCCN = new Vec4( 0,  0,  0, -1);
	public static final Vec4 CCCC = new Vec4( 0,  0,  0,  0);
	public static final Vec4 CCCP = new Vec4( 0,  0,  0,  1);
	public static final Vec4 CCPN = new Vec4( 0,  0,  1, -1);
	public static final Vec4 CCPC = new Vec4( 0,  0,  1,  0);
	public static final Vec4 CCPP = new Vec4( 0,  0,  1,  1);
	public static final Vec4 CPNN = new Vec4( 0,  1, -1, -1);
	public static final Vec4 CPNC = new Vec4( 0,  1, -1,  0);
	public static final Vec4 CPNP = new Vec4( 0,  1, -1,  1);
	public static final Vec4 CPCN = new Vec4( 0,  1,  0, -1);
	public static final Vec4 CPCC = new Vec4( 0,  1,  0,  0);
	public static final Vec4 CPCP = new Vec4( 0,  1,  0,  1);
	public static final Vec4 CPPN = new Vec4( 0,  1,  1, -1);
	public static final Vec4 CPPC = new Vec4( 0,  1,  1,  0);
	public static final Vec4 CPPP = new Vec4( 0,  1,  1,  1);
	public static final Vec4 PNNN = new Vec4( 1, -1, -1, -1);
	public static final Vec4 PNNC = new Vec4( 1, -1, -1,  0);
	public static final Vec4 PNNP = new Vec4( 1, -1, -1,  1);
	public static final Vec4 PNCN = new Vec4( 1, -1,  0, -1);
	public static final Vec4 PNCC = new Vec4( 1, -1,  0,  0);
	public static final Vec4 PNCP = new Vec4( 1, -1,  0,  1);
	public static final Vec4 PNPN = new Vec4( 1, -1,  1, -1);
	public static final Vec4 PNPC = new Vec4( 1, -1,  1,  0);
	public static final Vec4 PNPP = new Vec4( 1, -1,  1,  1);
	public static final Vec4 PCNN = new Vec4( 1,  0, -1, -1);
	public static final Vec4 PCNC = new Vec4( 1,  0, -1,  0);
	public static final Vec4 PCNP = new Vec4( 1,  0, -1,  1);
	public static final Vec4 PCCN = new Vec4( 1,  0,  0, -1);
	public static final Vec4 PCCC = new Vec4( 1,  0,  0,  0);
	public static final Vec4 PCCP = new Vec4( 1,  0,  0,  1);
	public static final Vec4 PCPN = new Vec4( 1,  0,  1, -1);
	public static final Vec4 PCPC = new Vec4( 1,  0,  1,  0);
	public static final Vec4 PCPP = new Vec4( 1,  0,  1,  1);
	public static final Vec4 PPNN = new Vec4( 1,  1, -1, -1);
	public static final Vec4 PPNC = new Vec4( 1,  1, -1,  0);
	public static final Vec4 PPNP = new Vec4( 1,  1, -1,  1);
	public static final Vec4 PPCN = new Vec4( 1,  1,  0, -1);
	public static final Vec4 PPCC = new Vec4( 1,  1,  0,  0);
	public static final Vec4 PPCP = new Vec4( 1,  1,  0,  1);
	public static final Vec4 PPPN = new Vec4( 1,  1,  1, -1);
	public static final Vec4 PPPC = new Vec4( 1,  1,  1,  0);
	public static final Vec4 PPPP = new Vec4( 1,  1,  1,  1);

	public static final Vec4 ZERO = CCCC;
	public static final Vec4 XN = NCCC, XP = PCCC;
	public static final Vec4 YN = CNCC, YP = CPCC;
	public static final Vec4 ZN = CCNC, ZP = CCPC;
	public static final Vec4 WN = CCCN, WP = CCCP;

	public static final ImmutableList<Vec4> VERTICES = ImmutableList.of(NNNN, NNNP, NNPN, NNPP, NPNN, NPNP, NPPN, NPPP, PNNN, PNNP, PNPN, PNPP, PPNN, PPNP, PPPN, PPPP);
	public static final ImmutableList<Vec4> EDGES = ImmutableList.of(NNNC, NNCN, NNCP, NNPC, NCNN, NCNP, NCPN, NCPP, NPNC, NPCN, NPCP, NPPC, CNNN, CNNP, CNPN, CNPP, CPNN, CPNP, CPPN, CPPP, PNNC, PNCN, PNCP, PNPC, PCNN, PCNP, PCPN, PCPP, PPNC, PPCN, PPCP, PPPC);
	public static final ImmutableList<Vec4> SURFACE = ImmutableList.of(NNNN, NNNC, NNNP, NNCN, NNCC, NNCP, NNPN, NNPC, NNPP, NCNN, NCNC, NCNP, NCCN, NCCC, NCCP, NCPN, NCPC, NCPP, NPNN, NPNC, NPNP, NPCN, NPCC, NPCP, NPPN, NPPC, NPPP, CNNN, CNNC, CNNP, CNCN, CNCC, CNCP, CNPN, CNPC, CNPP, CCNN, CCNC, CCNP, CCCN, CCCP, CCPN, CCPC, CCPP, CPNN, CPNC, CPNP, CPCN, CPCC, CPCP, CPPN, CPPC, CPPP, PNNN, PNNC, PNNP, PNCN, PNCC, PNCP, PNPN, PNPC, PNPP, PCNN, PCNC, PCNP, PCCN, PCCC, PCCP, PCPN, PCPC, PCPP, PPNN, PPNC, PPNP, PPCN, PPCC, PPCP, PPPN, PPPC, PPPP);
	public static final ImmutableList<Vec4> ALL = ImmutableList.of(NNNN, NNNC, NNNP, NNCN, NNCC, NNCP, NNPN, NNPC, NNPP, NCNN, NCNC, NCNP, NCCN, NCCC, NCCP, NCPN, NCPC, NCPP, NPNN, NPNC, NPNP, NPCN, NPCC, NPCP, NPPN, NPPC, NPPP, CNNN, CNNC, CNNP, CNCN, CNCC, CNCP, CNPN, CNPC, CNPP, CCNN, CCNC, CCNP, CCCN, CCCC, CCCP, CCPN, CCPC, CCPP, CPNN, CPNC, CPNP, CPCN, CPCC, CPCP, CPPN, CPPC, CPPP, PNNN, PNNC, PNNP, PNCN, PNCC, PNCP, PNPN, PNPC, PNPP, PCNN, PCNC, PCNP, PCCN, PCCC, PCCP, PCPN, PCPC, PCPP, PPNN, PPNC, PPNP, PPCN, PPCC, PPCP, PPPN, PPPC, PPPP);
	// @formatter:on

	public final double x, y, z, w;

	public Vec4(double x, double y, double z, double w) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.w = w;
	}

	public Vec4(Vec3Access vec, double w) {
		this.x = vec.x();
		this.y = vec.y();
		this.z = vec.z();
		this.w = w;
	}

	public Vec4(Vector4f vec) {
		this.x = vec.x();
		this.y = vec.y();
		this.z = vec.z();
		this.w = vec.w();
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

	public static Vec4 broadcast(double n) {return new Vec4(n, n, n, n);}

	// vector-vector ops
	public Vec4 add  (Vec4Access rhs) {return new Vec4(this.x + rhs.x(), this.y + rhs.y(), this.z + rhs.z(), this.w + rhs.w());}
	public Vec4 sub  (Vec4Access rhs) {return new Vec4(this.x - rhs.x(), this.y - rhs.y(), this.z - rhs.z(), this.w - rhs.w());}
	public Vec4 mul  (Vec4Access rhs) {return new Vec4(this.x * rhs.x(), this.y * rhs.y(), this.z * rhs.z(), this.w * rhs.w());}
	public Vec4 div  (Vec4Access rhs) {return new Vec4(this.x / rhs.x(), this.y / rhs.y(), this.z / rhs.z(), this.w / rhs.w());}
	// scalar-vector ops
	public Vec4 mul  (double n)       {return new Vec4(this.x * n,       this.y * n,       this.z * n,       this.w * n      );}
	public Vec4 div  (double n)       {return new Vec4(this.x / n,       this.y / n,       this.z / n,       this.w / n      );}
	public Vec4 recip(double n)       {return new Vec4(n / x,            n / y,            n / z,            n / w           );}
	// unary ops
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

	public static Vec4 lerp(double delta, Vec4 a, Vec4 b) {
		return new Vec4(
				Mth.lerp(delta, a.x, b.x),
				Mth.lerp(delta, a.y, b.y),
				Mth.lerp(delta, a.z, b.z),
				Mth.lerp(delta, a.w, b.w));
	}

	@Override
	public String toString() {
		return String.format("(%f, %f, %f, %f)", x, y, z, w);
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
		return hashToInt();
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
		public Mutable setX(double x)     {this.x = x; return this;}
		public Mutable setY(double y)     {this.y = y; return this;}
		public Mutable setZ(double z)     {this.z = z; return this;}
		public Mutable setW(double w)     {this.w = w; return this;}

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
			return String.format("(%f, %f, %f, %f)", x, y, z, w);
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
			return hashToInt();
		}

		@Override
		public void appendHash(Hasher hasher) {
			hasher.appendDouble(this.x).appendDouble(this.y).appendDouble(this.z).appendDouble(this.w);
		}

	}

}
