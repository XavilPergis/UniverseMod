package net.xavil.util.math.interfaces;

import net.xavil.util.math.Vec2;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec4;

public interface Vec3Access {
	// @formatter:off
	double x();
	double y();
	double z();
	// @formatter:on

	default net.minecraft.world.phys.Vec3 asMinecraft() {
		return new net.minecraft.world.phys.Vec3(x(), y(), z());
	}

	default double dot(Vec3Access other) {
		return this.x() * other.x() + this.y() * other.y() + this.z() * other.z();
	}

	default double distanceToSquared(Vec3Access other) {
		final var dx = this.x() - other.x();
		final var dy = this.y() - other.y();
		final var dz = this.z() - other.z();
		return dx * dx + dy * dy + dz * dz;
	}

	default double distanceTo(Vec3Access other) {
		return Math.sqrt(distanceToSquared(other));
	}

	default double lengthSquared() {
		return this.x() * this.x() + this.y() * this.y() + this.z() * this.z();
	}

	default double length() {
		return Math.sqrt(lengthSquared());
	}

	// #region Swizzle Operations
	// @formatter:off
	default Vec2 xx() {return new Vec2(x(),x());}
	default Vec2 xy() {return new Vec2(x(),y());}
	default Vec2 xz() {return new Vec2(x(),z());}
	default Vec2 yx() {return new Vec2(y(),x());}
	default Vec2 yy() {return new Vec2(y(),y());}
	default Vec2 yz() {return new Vec2(y(),z());}
	default Vec2 zx() {return new Vec2(z(),x());}
	default Vec2 zy() {return new Vec2(z(),y());}
	default Vec2 zz() {return new Vec2(z(),z());}

	default Vec3 xxx() {return new Vec3(x(),x(),x());}
	default Vec3 xxy() {return new Vec3(x(),x(),y());}
	default Vec3 xxz() {return new Vec3(x(),x(),z());}
	default Vec3 xyx() {return new Vec3(x(),y(),x());}
	default Vec3 xyy() {return new Vec3(x(),y(),y());}
	default Vec3 xyz() {return new Vec3(x(),y(),z());}
	default Vec3 xzx() {return new Vec3(x(),z(),x());}
	default Vec3 xzy() {return new Vec3(x(),z(),y());}
	default Vec3 xzz() {return new Vec3(x(),z(),z());}
	default Vec3 yxx() {return new Vec3(y(),x(),x());}
	default Vec3 yxy() {return new Vec3(y(),x(),y());}
	default Vec3 yxz() {return new Vec3(y(),x(),z());}
	default Vec3 yyx() {return new Vec3(y(),y(),x());}
	default Vec3 yyy() {return new Vec3(y(),y(),y());}
	default Vec3 yyz() {return new Vec3(y(),y(),z());}
	default Vec3 yzx() {return new Vec3(y(),z(),x());}
	default Vec3 yzy() {return new Vec3(y(),z(),y());}
	default Vec3 yzz() {return new Vec3(y(),z(),z());}
	default Vec3 zxx() {return new Vec3(z(),x(),x());}
	default Vec3 zxy() {return new Vec3(z(),x(),y());}
	default Vec3 zxz() {return new Vec3(z(),x(),z());}
	default Vec3 zyx() {return new Vec3(z(),y(),x());}
	default Vec3 zyy() {return new Vec3(z(),y(),y());}
	default Vec3 zyz() {return new Vec3(z(),y(),z());}
	default Vec3 zzx() {return new Vec3(z(),z(),x());}
	default Vec3 zzy() {return new Vec3(z(),z(),y());}
	default Vec3 zzz() {return new Vec3(z(),z(),z());}

	// special swizzles for converting to a vec4 with a w of 0 or 1
	default Vec4 xxx0() {return new Vec4(x(),x(),x(),0);}
	default Vec4 xxy0() {return new Vec4(x(),x(),y(),0);}
	default Vec4 xxz0() {return new Vec4(x(),x(),z(),0);}
	default Vec4 xyx0() {return new Vec4(x(),y(),x(),0);}
	default Vec4 xyy0() {return new Vec4(x(),y(),y(),0);}
	default Vec4 xyz0() {return new Vec4(x(),y(),z(),0);}
	default Vec4 xzx0() {return new Vec4(x(),z(),x(),0);}
	default Vec4 xzy0() {return new Vec4(x(),z(),y(),0);}
	default Vec4 xzz0() {return new Vec4(x(),z(),z(),0);}
	default Vec4 yxx0() {return new Vec4(y(),x(),x(),0);}
	default Vec4 yxy0() {return new Vec4(y(),x(),y(),0);}
	default Vec4 yxz0() {return new Vec4(y(),x(),z(),0);}
	default Vec4 yyx0() {return new Vec4(y(),y(),x(),0);}
	default Vec4 yyy0() {return new Vec4(y(),y(),y(),0);}
	default Vec4 yyz0() {return new Vec4(y(),y(),z(),0);}
	default Vec4 yzx0() {return new Vec4(y(),z(),x(),0);}
	default Vec4 yzy0() {return new Vec4(y(),z(),y(),0);}
	default Vec4 yzz0() {return new Vec4(y(),z(),z(),0);}
	default Vec4 zxx0() {return new Vec4(z(),x(),x(),0);}
	default Vec4 zxy0() {return new Vec4(z(),x(),y(),0);}
	default Vec4 zxz0() {return new Vec4(z(),x(),z(),0);}
	default Vec4 zyx0() {return new Vec4(z(),y(),x(),0);}
	default Vec4 zyy0() {return new Vec4(z(),y(),y(),0);}
	default Vec4 zyz0() {return new Vec4(z(),y(),z(),0);}
	default Vec4 zzx0() {return new Vec4(z(),z(),x(),0);}
	default Vec4 zzy0() {return new Vec4(z(),z(),y(),0);}
	default Vec4 zzz0() {return new Vec4(z(),z(),z(),0);}
	default Vec4 xxx1() {return new Vec4(x(),x(),x(),1);}
	default Vec4 xxy1() {return new Vec4(x(),x(),y(),1);}
	default Vec4 xxz1() {return new Vec4(x(),x(),z(),1);}
	default Vec4 xyx1() {return new Vec4(x(),y(),x(),1);}
	default Vec4 xyy1() {return new Vec4(x(),y(),y(),1);}
	default Vec4 xyz1() {return new Vec4(x(),y(),z(),1);}
	default Vec4 xzx1() {return new Vec4(x(),z(),x(),1);}
	default Vec4 xzy1() {return new Vec4(x(),z(),y(),1);}
	default Vec4 xzz1() {return new Vec4(x(),z(),z(),1);}
	default Vec4 yxx1() {return new Vec4(y(),x(),x(),1);}
	default Vec4 yxy1() {return new Vec4(y(),x(),y(),1);}
	default Vec4 yxz1() {return new Vec4(y(),x(),z(),1);}
	default Vec4 yyx1() {return new Vec4(y(),y(),x(),1);}
	default Vec4 yyy1() {return new Vec4(y(),y(),y(),1);}
	default Vec4 yyz1() {return new Vec4(y(),y(),z(),1);}
	default Vec4 yzx1() {return new Vec4(y(),z(),x(),1);}
	default Vec4 yzy1() {return new Vec4(y(),z(),y(),1);}
	default Vec4 yzz1() {return new Vec4(y(),z(),z(),1);}
	default Vec4 zxx1() {return new Vec4(z(),x(),x(),1);}
	default Vec4 zxy1() {return new Vec4(z(),x(),y(),1);}
	default Vec4 zxz1() {return new Vec4(z(),x(),z(),1);}
	default Vec4 zyx1() {return new Vec4(z(),y(),x(),1);}
	default Vec4 zyy1() {return new Vec4(z(),y(),y(),1);}
	default Vec4 zyz1() {return new Vec4(z(),y(),z(),1);}
	default Vec4 zzx1() {return new Vec4(z(),z(),x(),1);}
	default Vec4 zzy1() {return new Vec4(z(),z(),y(),1);}
	default Vec4 zzz1() {return new Vec4(z(),z(),z(),1);}

	default Vec4 xxxx() {return new Vec4(x(),x(),x(),x());}
	default Vec4 xxxy() {return new Vec4(x(),x(),x(),y());}
	default Vec4 xxxz() {return new Vec4(x(),x(),x(),z());}
	default Vec4 xxyx() {return new Vec4(x(),x(),y(),x());}
	default Vec4 xxyy() {return new Vec4(x(),x(),y(),y());}
	default Vec4 xxyz() {return new Vec4(x(),x(),y(),z());}
	default Vec4 xxzx() {return new Vec4(x(),x(),z(),x());}
	default Vec4 xxzy() {return new Vec4(x(),x(),z(),y());}
	default Vec4 xxzz() {return new Vec4(x(),x(),z(),z());}
	default Vec4 xyxx() {return new Vec4(x(),y(),x(),x());}
	default Vec4 xyxy() {return new Vec4(x(),y(),x(),y());}
	default Vec4 xyxz() {return new Vec4(x(),y(),x(),z());}
	default Vec4 xyyx() {return new Vec4(x(),y(),y(),x());}
	default Vec4 xyyy() {return new Vec4(x(),y(),y(),y());}
	default Vec4 xyyz() {return new Vec4(x(),y(),y(),z());}
	default Vec4 xyzx() {return new Vec4(x(),y(),z(),x());}
	default Vec4 xyzy() {return new Vec4(x(),y(),z(),y());}
	default Vec4 xyzz() {return new Vec4(x(),y(),z(),z());}
	default Vec4 xzxx() {return new Vec4(x(),z(),x(),x());}
	default Vec4 xzxy() {return new Vec4(x(),z(),x(),y());}
	default Vec4 xzxz() {return new Vec4(x(),z(),x(),z());}
	default Vec4 xzyx() {return new Vec4(x(),z(),y(),x());}
	default Vec4 xzyy() {return new Vec4(x(),z(),y(),y());}
	default Vec4 xzyz() {return new Vec4(x(),z(),y(),z());}
	default Vec4 xzzx() {return new Vec4(x(),z(),z(),x());}
	default Vec4 xzzy() {return new Vec4(x(),z(),z(),y());}
	default Vec4 xzzz() {return new Vec4(x(),z(),z(),z());}
	default Vec4 yxxx() {return new Vec4(y(),x(),x(),x());}
	default Vec4 yxxy() {return new Vec4(y(),x(),x(),y());}
	default Vec4 yxxz() {return new Vec4(y(),x(),x(),z());}
	default Vec4 yxyx() {return new Vec4(y(),x(),y(),x());}
	default Vec4 yxyy() {return new Vec4(y(),x(),y(),y());}
	default Vec4 yxyz() {return new Vec4(y(),x(),y(),z());}
	default Vec4 yxzx() {return new Vec4(y(),x(),z(),x());}
	default Vec4 yxzy() {return new Vec4(y(),x(),z(),y());}
	default Vec4 yxzz() {return new Vec4(y(),x(),z(),z());}
	default Vec4 yyxx() {return new Vec4(y(),y(),x(),x());}
	default Vec4 yyxy() {return new Vec4(y(),y(),x(),y());}
	default Vec4 yyxz() {return new Vec4(y(),y(),x(),z());}
	default Vec4 yyyx() {return new Vec4(y(),y(),y(),x());}
	default Vec4 yyyy() {return new Vec4(y(),y(),y(),y());}
	default Vec4 yyyz() {return new Vec4(y(),y(),y(),z());}
	default Vec4 yyzx() {return new Vec4(y(),y(),z(),x());}
	default Vec4 yyzy() {return new Vec4(y(),y(),z(),y());}
	default Vec4 yyzz() {return new Vec4(y(),y(),z(),z());}
	default Vec4 yzxx() {return new Vec4(y(),z(),x(),x());}
	default Vec4 yzxy() {return new Vec4(y(),z(),x(),y());}
	default Vec4 yzxz() {return new Vec4(y(),z(),x(),z());}
	default Vec4 yzyx() {return new Vec4(y(),z(),y(),x());}
	default Vec4 yzyy() {return new Vec4(y(),z(),y(),y());}
	default Vec4 yzyz() {return new Vec4(y(),z(),y(),z());}
	default Vec4 yzzx() {return new Vec4(y(),z(),z(),x());}
	default Vec4 yzzy() {return new Vec4(y(),z(),z(),y());}
	default Vec4 yzzz() {return new Vec4(y(),z(),z(),z());}
	default Vec4 zxxx() {return new Vec4(z(),x(),x(),x());}
	default Vec4 zxxy() {return new Vec4(z(),x(),x(),y());}
	default Vec4 zxxz() {return new Vec4(z(),x(),x(),z());}
	default Vec4 zxyx() {return new Vec4(z(),x(),y(),x());}
	default Vec4 zxyy() {return new Vec4(z(),x(),y(),y());}
	default Vec4 zxyz() {return new Vec4(z(),x(),y(),z());}
	default Vec4 zxzx() {return new Vec4(z(),x(),z(),x());}
	default Vec4 zxzy() {return new Vec4(z(),x(),z(),y());}
	default Vec4 zxzz() {return new Vec4(z(),x(),z(),z());}
	default Vec4 zyxx() {return new Vec4(z(),y(),x(),x());}
	default Vec4 zyxy() {return new Vec4(z(),y(),x(),y());}
	default Vec4 zyxz() {return new Vec4(z(),y(),x(),z());}
	default Vec4 zyyx() {return new Vec4(z(),y(),y(),x());}
	default Vec4 zyyy() {return new Vec4(z(),y(),y(),y());}
	default Vec4 zyyz() {return new Vec4(z(),y(),y(),z());}
	default Vec4 zyzx() {return new Vec4(z(),y(),z(),x());}
	default Vec4 zyzy() {return new Vec4(z(),y(),z(),y());}
	default Vec4 zyzz() {return new Vec4(z(),y(),z(),z());}
	default Vec4 zzxx() {return new Vec4(z(),z(),x(),x());}
	default Vec4 zzxy() {return new Vec4(z(),z(),x(),y());}
	default Vec4 zzxz() {return new Vec4(z(),z(),x(),z());}
	default Vec4 zzyx() {return new Vec4(z(),z(),y(),x());}
	default Vec4 zzyy() {return new Vec4(z(),z(),y(),y());}
	default Vec4 zzyz() {return new Vec4(z(),z(),y(),z());}
	default Vec4 zzzx() {return new Vec4(z(),z(),z(),x());}
	default Vec4 zzzy() {return new Vec4(z(),z(),z(),y());}
	default Vec4 zzzz() {return new Vec4(z(),z(),z(),z());}
	// @formatter:on
	// #endregion

}
