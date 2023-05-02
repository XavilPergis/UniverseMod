package net.xavil.util.math.matrices.interfaces;

import com.mojang.math.Vector4f;

import net.xavil.util.math.matrices.Vec2;
import net.xavil.util.math.matrices.Vec3;
import net.xavil.util.math.matrices.Vec4;

public interface Vec4Access {
	// @formatter:off
	double x();
	double y();
	double z();
	double w();
	// @formatter:on

	default Vector4f asMinecraft() {
		return new Vector4f((float) x(), (float) y(), (float) z(), (float) w());
	}

	default double dot(Vec4Access other) {
		return this.x() * other.x() + this.y() * other.y() + this.z() * other.z() + this.w() * other.w();
	}

	default double distanceToSquared(Vec4Access other) {
		final var dx = this.x() - other.x();
		final var dy = this.y() - other.y();
		final var dz = this.z() - other.z();
		final var dw = this.w() - other.w();
		return dx * dx + dy * dy + dz * dz + dw * dw;
	}

	default double distanceTo(Vec4Access other) {
		return Math.sqrt(distanceToSquared(other));
	}

	default double lengthSquared() {
		return this.x() * this.x() + this.y() * this.y() + this.z() * this.z() + this.w() * this.w();
	}

	default double length() {
		return Math.sqrt(lengthSquared());
	}

	// #region Swizzle Operations
	// @formatter:off
	default Vec2 xx() {return new Vec2(x(),x());}
	default Vec2 xy() {return new Vec2(x(),y());}
	default Vec2 xz() {return new Vec2(x(),z());}
	default Vec2 xw() {return new Vec2(x(),w());}
	default Vec2 yx() {return new Vec2(y(),x());}
	default Vec2 yy() {return new Vec2(y(),y());}
	default Vec2 yz() {return new Vec2(y(),z());}
	default Vec2 yw() {return new Vec2(y(),w());}
	default Vec2 zx() {return new Vec2(z(),x());}
	default Vec2 zy() {return new Vec2(z(),y());}
	default Vec2 zz() {return new Vec2(z(),z());}
	default Vec2 zw() {return new Vec2(z(),w());}
	default Vec2 wx() {return new Vec2(w(),x());}
	default Vec2 wy() {return new Vec2(w(),y());}
	default Vec2 wz() {return new Vec2(w(),z());}
	default Vec2 ww() {return new Vec2(w(),w());}

	default Vec3 xxx() {return new Vec3(x(),x(),x());}
	default Vec3 xxy() {return new Vec3(x(),x(),y());}
	default Vec3 xxz() {return new Vec3(x(),x(),z());}
	default Vec3 xxw() {return new Vec3(x(),x(),w());}
	default Vec3 xyx() {return new Vec3(x(),y(),x());}
	default Vec3 xyy() {return new Vec3(x(),y(),y());}
	default Vec3 xyz() {return new Vec3(x(),y(),z());}
	default Vec3 xyw() {return new Vec3(x(),y(),w());}
	default Vec3 xzx() {return new Vec3(x(),z(),x());}
	default Vec3 xzy() {return new Vec3(x(),z(),y());}
	default Vec3 xzz() {return new Vec3(x(),z(),z());}
	default Vec3 xzw() {return new Vec3(x(),z(),w());}
	default Vec3 xwx() {return new Vec3(x(),w(),x());}
	default Vec3 xwy() {return new Vec3(x(),w(),y());}
	default Vec3 xwz() {return new Vec3(x(),w(),z());}
	default Vec3 xww() {return new Vec3(x(),w(),w());}
	default Vec3 yxx() {return new Vec3(y(),x(),x());}
	default Vec3 yxy() {return new Vec3(y(),x(),y());}
	default Vec3 yxz() {return new Vec3(y(),x(),z());}
	default Vec3 yxw() {return new Vec3(y(),x(),w());}
	default Vec3 yyx() {return new Vec3(y(),y(),x());}
	default Vec3 yyy() {return new Vec3(y(),y(),y());}
	default Vec3 yyz() {return new Vec3(y(),y(),z());}
	default Vec3 yyw() {return new Vec3(y(),y(),w());}
	default Vec3 yzx() {return new Vec3(y(),z(),x());}
	default Vec3 yzy() {return new Vec3(y(),z(),y());}
	default Vec3 yzz() {return new Vec3(y(),z(),z());}
	default Vec3 yzw() {return new Vec3(y(),z(),w());}
	default Vec3 ywx() {return new Vec3(y(),w(),x());}
	default Vec3 ywy() {return new Vec3(y(),w(),y());}
	default Vec3 ywz() {return new Vec3(y(),w(),z());}
	default Vec3 yww() {return new Vec3(y(),w(),w());}
	default Vec3 zxx() {return new Vec3(z(),x(),x());}
	default Vec3 zxy() {return new Vec3(z(),x(),y());}
	default Vec3 zxz() {return new Vec3(z(),x(),z());}
	default Vec3 zxw() {return new Vec3(z(),x(),w());}
	default Vec3 zyx() {return new Vec3(z(),y(),x());}
	default Vec3 zyy() {return new Vec3(z(),y(),y());}
	default Vec3 zyz() {return new Vec3(z(),y(),z());}
	default Vec3 zyw() {return new Vec3(z(),y(),w());}
	default Vec3 zzx() {return new Vec3(z(),z(),x());}
	default Vec3 zzy() {return new Vec3(z(),z(),y());}
	default Vec3 zzz() {return new Vec3(z(),z(),z());}
	default Vec3 zzw() {return new Vec3(z(),z(),w());}
	default Vec3 zwx() {return new Vec3(z(),w(),x());}
	default Vec3 zwy() {return new Vec3(z(),w(),y());}
	default Vec3 zwz() {return new Vec3(z(),w(),z());}
	default Vec3 zww() {return new Vec3(z(),w(),w());}
	default Vec3 wxx() {return new Vec3(w(),x(),x());}
	default Vec3 wxy() {return new Vec3(w(),x(),y());}
	default Vec3 wxz() {return new Vec3(w(),x(),z());}
	default Vec3 wxw() {return new Vec3(w(),x(),w());}
	default Vec3 wyx() {return new Vec3(w(),y(),x());}
	default Vec3 wyy() {return new Vec3(w(),y(),y());}
	default Vec3 wyz() {return new Vec3(w(),y(),z());}
	default Vec3 wyw() {return new Vec3(w(),y(),w());}
	default Vec3 wzx() {return new Vec3(w(),z(),x());}
	default Vec3 wzy() {return new Vec3(w(),z(),y());}
	default Vec3 wzz() {return new Vec3(w(),z(),z());}
	default Vec3 wzw() {return new Vec3(w(),z(),w());}
	default Vec3 wwx() {return new Vec3(w(),w(),x());}
	default Vec3 wwy() {return new Vec3(w(),w(),y());}
	default Vec3 wwz() {return new Vec3(w(),w(),z());}
	default Vec3 www() {return new Vec3(w(),w(),w());}

	default Vec4 xxxx() {return new Vec4(x(),x(),x(),x());}
	default Vec4 xxxy() {return new Vec4(x(),x(),x(),y());}
	default Vec4 xxxz() {return new Vec4(x(),x(),x(),z());}
	default Vec4 xxxw() {return new Vec4(x(),x(),x(),w());}
	default Vec4 xxyx() {return new Vec4(x(),x(),y(),x());}
	default Vec4 xxyy() {return new Vec4(x(),x(),y(),y());}
	default Vec4 xxyz() {return new Vec4(x(),x(),y(),z());}
	default Vec4 xxyw() {return new Vec4(x(),x(),y(),w());}
	default Vec4 xxzx() {return new Vec4(x(),x(),z(),x());}
	default Vec4 xxzy() {return new Vec4(x(),x(),z(),y());}
	default Vec4 xxzz() {return new Vec4(x(),x(),z(),z());}
	default Vec4 xxzw() {return new Vec4(x(),x(),z(),w());}
	default Vec4 xxwx() {return new Vec4(x(),x(),w(),x());}
	default Vec4 xxwy() {return new Vec4(x(),x(),w(),y());}
	default Vec4 xxwz() {return new Vec4(x(),x(),w(),z());}
	default Vec4 xxww() {return new Vec4(x(),x(),w(),w());}
	default Vec4 xyxx() {return new Vec4(x(),y(),x(),x());}
	default Vec4 xyxy() {return new Vec4(x(),y(),x(),y());}
	default Vec4 xyxz() {return new Vec4(x(),y(),x(),z());}
	default Vec4 xyxw() {return new Vec4(x(),y(),x(),w());}
	default Vec4 xyyx() {return new Vec4(x(),y(),y(),x());}
	default Vec4 xyyy() {return new Vec4(x(),y(),y(),y());}
	default Vec4 xyyz() {return new Vec4(x(),y(),y(),z());}
	default Vec4 xyyw() {return new Vec4(x(),y(),y(),w());}
	default Vec4 xyzx() {return new Vec4(x(),y(),z(),x());}
	default Vec4 xyzy() {return new Vec4(x(),y(),z(),y());}
	default Vec4 xyzz() {return new Vec4(x(),y(),z(),z());}
	default Vec4 xyzw() {return new Vec4(x(),y(),z(),w());}
	default Vec4 xywx() {return new Vec4(x(),y(),w(),x());}
	default Vec4 xywy() {return new Vec4(x(),y(),w(),y());}
	default Vec4 xywz() {return new Vec4(x(),y(),w(),z());}
	default Vec4 xyww() {return new Vec4(x(),y(),w(),w());}
	default Vec4 xzxx() {return new Vec4(x(),z(),x(),x());}
	default Vec4 xzxy() {return new Vec4(x(),z(),x(),y());}
	default Vec4 xzxz() {return new Vec4(x(),z(),x(),z());}
	default Vec4 xzxw() {return new Vec4(x(),z(),x(),w());}
	default Vec4 xzyx() {return new Vec4(x(),z(),y(),x());}
	default Vec4 xzyy() {return new Vec4(x(),z(),y(),y());}
	default Vec4 xzyz() {return new Vec4(x(),z(),y(),z());}
	default Vec4 xzyw() {return new Vec4(x(),z(),y(),w());}
	default Vec4 xzzx() {return new Vec4(x(),z(),z(),x());}
	default Vec4 xzzy() {return new Vec4(x(),z(),z(),y());}
	default Vec4 xzzz() {return new Vec4(x(),z(),z(),z());}
	default Vec4 xzzw() {return new Vec4(x(),z(),z(),w());}
	default Vec4 xzwx() {return new Vec4(x(),z(),w(),x());}
	default Vec4 xzwy() {return new Vec4(x(),z(),w(),y());}
	default Vec4 xzwz() {return new Vec4(x(),z(),w(),z());}
	default Vec4 xzww() {return new Vec4(x(),z(),w(),w());}
	default Vec4 xwxx() {return new Vec4(x(),w(),x(),x());}
	default Vec4 xwxy() {return new Vec4(x(),w(),x(),y());}
	default Vec4 xwxz() {return new Vec4(x(),w(),x(),z());}
	default Vec4 xwxw() {return new Vec4(x(),w(),x(),w());}
	default Vec4 xwyx() {return new Vec4(x(),w(),y(),x());}
	default Vec4 xwyy() {return new Vec4(x(),w(),y(),y());}
	default Vec4 xwyz() {return new Vec4(x(),w(),y(),z());}
	default Vec4 xwyw() {return new Vec4(x(),w(),y(),w());}
	default Vec4 xwzx() {return new Vec4(x(),w(),z(),x());}
	default Vec4 xwzy() {return new Vec4(x(),w(),z(),y());}
	default Vec4 xwzz() {return new Vec4(x(),w(),z(),z());}
	default Vec4 xwzw() {return new Vec4(x(),w(),z(),w());}
	default Vec4 xwwx() {return new Vec4(x(),w(),w(),x());}
	default Vec4 xwwy() {return new Vec4(x(),w(),w(),y());}
	default Vec4 xwwz() {return new Vec4(x(),w(),w(),z());}
	default Vec4 xwww() {return new Vec4(x(),w(),w(),w());}
	default Vec4 yxxx() {return new Vec4(y(),x(),x(),x());}
	default Vec4 yxxy() {return new Vec4(y(),x(),x(),y());}
	default Vec4 yxxz() {return new Vec4(y(),x(),x(),z());}
	default Vec4 yxxw() {return new Vec4(y(),x(),x(),w());}
	default Vec4 yxyx() {return new Vec4(y(),x(),y(),x());}
	default Vec4 yxyy() {return new Vec4(y(),x(),y(),y());}
	default Vec4 yxyz() {return new Vec4(y(),x(),y(),z());}
	default Vec4 yxyw() {return new Vec4(y(),x(),y(),w());}
	default Vec4 yxzx() {return new Vec4(y(),x(),z(),x());}
	default Vec4 yxzy() {return new Vec4(y(),x(),z(),y());}
	default Vec4 yxzz() {return new Vec4(y(),x(),z(),z());}
	default Vec4 yxzw() {return new Vec4(y(),x(),z(),w());}
	default Vec4 yxwx() {return new Vec4(y(),x(),w(),x());}
	default Vec4 yxwy() {return new Vec4(y(),x(),w(),y());}
	default Vec4 yxwz() {return new Vec4(y(),x(),w(),z());}
	default Vec4 yxww() {return new Vec4(y(),x(),w(),w());}
	default Vec4 yyxx() {return new Vec4(y(),y(),x(),x());}
	default Vec4 yyxy() {return new Vec4(y(),y(),x(),y());}
	default Vec4 yyxz() {return new Vec4(y(),y(),x(),z());}
	default Vec4 yyxw() {return new Vec4(y(),y(),x(),w());}
	default Vec4 yyyx() {return new Vec4(y(),y(),y(),x());}
	default Vec4 yyyy() {return new Vec4(y(),y(),y(),y());}
	default Vec4 yyyz() {return new Vec4(y(),y(),y(),z());}
	default Vec4 yyyw() {return new Vec4(y(),y(),y(),w());}
	default Vec4 yyzx() {return new Vec4(y(),y(),z(),x());}
	default Vec4 yyzy() {return new Vec4(y(),y(),z(),y());}
	default Vec4 yyzz() {return new Vec4(y(),y(),z(),z());}
	default Vec4 yyzw() {return new Vec4(y(),y(),z(),w());}
	default Vec4 yywx() {return new Vec4(y(),y(),w(),x());}
	default Vec4 yywy() {return new Vec4(y(),y(),w(),y());}
	default Vec4 yywz() {return new Vec4(y(),y(),w(),z());}
	default Vec4 yyww() {return new Vec4(y(),y(),w(),w());}
	default Vec4 yzxx() {return new Vec4(y(),z(),x(),x());}
	default Vec4 yzxy() {return new Vec4(y(),z(),x(),y());}
	default Vec4 yzxz() {return new Vec4(y(),z(),x(),z());}
	default Vec4 yzxw() {return new Vec4(y(),z(),x(),w());}
	default Vec4 yzyx() {return new Vec4(y(),z(),y(),x());}
	default Vec4 yzyy() {return new Vec4(y(),z(),y(),y());}
	default Vec4 yzyz() {return new Vec4(y(),z(),y(),z());}
	default Vec4 yzyw() {return new Vec4(y(),z(),y(),w());}
	default Vec4 yzzx() {return new Vec4(y(),z(),z(),x());}
	default Vec4 yzzy() {return new Vec4(y(),z(),z(),y());}
	default Vec4 yzzz() {return new Vec4(y(),z(),z(),z());}
	default Vec4 yzzw() {return new Vec4(y(),z(),z(),w());}
	default Vec4 yzwx() {return new Vec4(y(),z(),w(),x());}
	default Vec4 yzwy() {return new Vec4(y(),z(),w(),y());}
	default Vec4 yzwz() {return new Vec4(y(),z(),w(),z());}
	default Vec4 yzww() {return new Vec4(y(),z(),w(),w());}
	default Vec4 ywxx() {return new Vec4(y(),w(),x(),x());}
	default Vec4 ywxy() {return new Vec4(y(),w(),x(),y());}
	default Vec4 ywxz() {return new Vec4(y(),w(),x(),z());}
	default Vec4 ywxw() {return new Vec4(y(),w(),x(),w());}
	default Vec4 ywyx() {return new Vec4(y(),w(),y(),x());}
	default Vec4 ywyy() {return new Vec4(y(),w(),y(),y());}
	default Vec4 ywyz() {return new Vec4(y(),w(),y(),z());}
	default Vec4 ywyw() {return new Vec4(y(),w(),y(),w());}
	default Vec4 ywzx() {return new Vec4(y(),w(),z(),x());}
	default Vec4 ywzy() {return new Vec4(y(),w(),z(),y());}
	default Vec4 ywzz() {return new Vec4(y(),w(),z(),z());}
	default Vec4 ywzw() {return new Vec4(y(),w(),z(),w());}
	default Vec4 ywwx() {return new Vec4(y(),w(),w(),x());}
	default Vec4 ywwy() {return new Vec4(y(),w(),w(),y());}
	default Vec4 ywwz() {return new Vec4(y(),w(),w(),z());}
	default Vec4 ywww() {return new Vec4(y(),w(),w(),w());}
	default Vec4 zxxx() {return new Vec4(z(),x(),x(),x());}
	default Vec4 zxxy() {return new Vec4(z(),x(),x(),y());}
	default Vec4 zxxz() {return new Vec4(z(),x(),x(),z());}
	default Vec4 zxxw() {return new Vec4(z(),x(),x(),w());}
	default Vec4 zxyx() {return new Vec4(z(),x(),y(),x());}
	default Vec4 zxyy() {return new Vec4(z(),x(),y(),y());}
	default Vec4 zxyz() {return new Vec4(z(),x(),y(),z());}
	default Vec4 zxyw() {return new Vec4(z(),x(),y(),w());}
	default Vec4 zxzx() {return new Vec4(z(),x(),z(),x());}
	default Vec4 zxzy() {return new Vec4(z(),x(),z(),y());}
	default Vec4 zxzz() {return new Vec4(z(),x(),z(),z());}
	default Vec4 zxzw() {return new Vec4(z(),x(),z(),w());}
	default Vec4 zxwx() {return new Vec4(z(),x(),w(),x());}
	default Vec4 zxwy() {return new Vec4(z(),x(),w(),y());}
	default Vec4 zxwz() {return new Vec4(z(),x(),w(),z());}
	default Vec4 zxww() {return new Vec4(z(),x(),w(),w());}
	default Vec4 zyxx() {return new Vec4(z(),y(),x(),x());}
	default Vec4 zyxy() {return new Vec4(z(),y(),x(),y());}
	default Vec4 zyxz() {return new Vec4(z(),y(),x(),z());}
	default Vec4 zyxw() {return new Vec4(z(),y(),x(),w());}
	default Vec4 zyyx() {return new Vec4(z(),y(),y(),x());}
	default Vec4 zyyy() {return new Vec4(z(),y(),y(),y());}
	default Vec4 zyyz() {return new Vec4(z(),y(),y(),z());}
	default Vec4 zyyw() {return new Vec4(z(),y(),y(),w());}
	default Vec4 zyzx() {return new Vec4(z(),y(),z(),x());}
	default Vec4 zyzy() {return new Vec4(z(),y(),z(),y());}
	default Vec4 zyzz() {return new Vec4(z(),y(),z(),z());}
	default Vec4 zyzw() {return new Vec4(z(),y(),z(),w());}
	default Vec4 zywx() {return new Vec4(z(),y(),w(),x());}
	default Vec4 zywy() {return new Vec4(z(),y(),w(),y());}
	default Vec4 zywz() {return new Vec4(z(),y(),w(),z());}
	default Vec4 zyww() {return new Vec4(z(),y(),w(),w());}
	default Vec4 zzxx() {return new Vec4(z(),z(),x(),x());}
	default Vec4 zzxy() {return new Vec4(z(),z(),x(),y());}
	default Vec4 zzxz() {return new Vec4(z(),z(),x(),z());}
	default Vec4 zzxw() {return new Vec4(z(),z(),x(),w());}
	default Vec4 zzyx() {return new Vec4(z(),z(),y(),x());}
	default Vec4 zzyy() {return new Vec4(z(),z(),y(),y());}
	default Vec4 zzyz() {return new Vec4(z(),z(),y(),z());}
	default Vec4 zzyw() {return new Vec4(z(),z(),y(),w());}
	default Vec4 zzzx() {return new Vec4(z(),z(),z(),x());}
	default Vec4 zzzy() {return new Vec4(z(),z(),z(),y());}
	default Vec4 zzzz() {return new Vec4(z(),z(),z(),z());}
	default Vec4 zzzw() {return new Vec4(z(),z(),z(),w());}
	default Vec4 zzwx() {return new Vec4(z(),z(),w(),x());}
	default Vec4 zzwy() {return new Vec4(z(),z(),w(),y());}
	default Vec4 zzwz() {return new Vec4(z(),z(),w(),z());}
	default Vec4 zzww() {return new Vec4(z(),z(),w(),w());}
	default Vec4 zwxx() {return new Vec4(z(),w(),x(),x());}
	default Vec4 zwxy() {return new Vec4(z(),w(),x(),y());}
	default Vec4 zwxz() {return new Vec4(z(),w(),x(),z());}
	default Vec4 zwxw() {return new Vec4(z(),w(),x(),w());}
	default Vec4 zwyx() {return new Vec4(z(),w(),y(),x());}
	default Vec4 zwyy() {return new Vec4(z(),w(),y(),y());}
	default Vec4 zwyz() {return new Vec4(z(),w(),y(),z());}
	default Vec4 zwyw() {return new Vec4(z(),w(),y(),w());}
	default Vec4 zwzx() {return new Vec4(z(),w(),z(),x());}
	default Vec4 zwzy() {return new Vec4(z(),w(),z(),y());}
	default Vec4 zwzz() {return new Vec4(z(),w(),z(),z());}
	default Vec4 zwzw() {return new Vec4(z(),w(),z(),w());}
	default Vec4 zwwx() {return new Vec4(z(),w(),w(),x());}
	default Vec4 zwwy() {return new Vec4(z(),w(),w(),y());}
	default Vec4 zwwz() {return new Vec4(z(),w(),w(),z());}
	default Vec4 zwww() {return new Vec4(z(),w(),w(),w());}
	default Vec4 wxxx() {return new Vec4(w(),x(),x(),x());}
	default Vec4 wxxy() {return new Vec4(w(),x(),x(),y());}
	default Vec4 wxxz() {return new Vec4(w(),x(),x(),z());}
	default Vec4 wxxw() {return new Vec4(w(),x(),x(),w());}
	default Vec4 wxyx() {return new Vec4(w(),x(),y(),x());}
	default Vec4 wxyy() {return new Vec4(w(),x(),y(),y());}
	default Vec4 wxyz() {return new Vec4(w(),x(),y(),z());}
	default Vec4 wxyw() {return new Vec4(w(),x(),y(),w());}
	default Vec4 wxzx() {return new Vec4(w(),x(),z(),x());}
	default Vec4 wxzy() {return new Vec4(w(),x(),z(),y());}
	default Vec4 wxzz() {return new Vec4(w(),x(),z(),z());}
	default Vec4 wxzw() {return new Vec4(w(),x(),z(),w());}
	default Vec4 wxwx() {return new Vec4(w(),x(),w(),x());}
	default Vec4 wxwy() {return new Vec4(w(),x(),w(),y());}
	default Vec4 wxwz() {return new Vec4(w(),x(),w(),z());}
	default Vec4 wxww() {return new Vec4(w(),x(),w(),w());}
	default Vec4 wyxx() {return new Vec4(w(),y(),x(),x());}
	default Vec4 wyxy() {return new Vec4(w(),y(),x(),y());}
	default Vec4 wyxz() {return new Vec4(w(),y(),x(),z());}
	default Vec4 wyxw() {return new Vec4(w(),y(),x(),w());}
	default Vec4 wyyx() {return new Vec4(w(),y(),y(),x());}
	default Vec4 wyyy() {return new Vec4(w(),y(),y(),y());}
	default Vec4 wyyz() {return new Vec4(w(),y(),y(),z());}
	default Vec4 wyyw() {return new Vec4(w(),y(),y(),w());}
	default Vec4 wyzx() {return new Vec4(w(),y(),z(),x());}
	default Vec4 wyzy() {return new Vec4(w(),y(),z(),y());}
	default Vec4 wyzz() {return new Vec4(w(),y(),z(),z());}
	default Vec4 wyzw() {return new Vec4(w(),y(),z(),w());}
	default Vec4 wywx() {return new Vec4(w(),y(),w(),x());}
	default Vec4 wywy() {return new Vec4(w(),y(),w(),y());}
	default Vec4 wywz() {return new Vec4(w(),y(),w(),z());}
	default Vec4 wyww() {return new Vec4(w(),y(),w(),w());}
	default Vec4 wzxx() {return new Vec4(w(),z(),x(),x());}
	default Vec4 wzxy() {return new Vec4(w(),z(),x(),y());}
	default Vec4 wzxz() {return new Vec4(w(),z(),x(),z());}
	default Vec4 wzxw() {return new Vec4(w(),z(),x(),w());}
	default Vec4 wzyx() {return new Vec4(w(),z(),y(),x());}
	default Vec4 wzyy() {return new Vec4(w(),z(),y(),y());}
	default Vec4 wzyz() {return new Vec4(w(),z(),y(),z());}
	default Vec4 wzyw() {return new Vec4(w(),z(),y(),w());}
	default Vec4 wzzx() {return new Vec4(w(),z(),z(),x());}
	default Vec4 wzzy() {return new Vec4(w(),z(),z(),y());}
	default Vec4 wzzz() {return new Vec4(w(),z(),z(),z());}
	default Vec4 wzzw() {return new Vec4(w(),z(),z(),w());}
	default Vec4 wzwx() {return new Vec4(w(),z(),w(),x());}
	default Vec4 wzwy() {return new Vec4(w(),z(),w(),y());}
	default Vec4 wzwz() {return new Vec4(w(),z(),w(),z());}
	default Vec4 wzww() {return new Vec4(w(),z(),w(),w());}
	default Vec4 wwxx() {return new Vec4(w(),w(),x(),x());}
	default Vec4 wwxy() {return new Vec4(w(),w(),x(),y());}
	default Vec4 wwxz() {return new Vec4(w(),w(),x(),z());}
	default Vec4 wwxw() {return new Vec4(w(),w(),x(),w());}
	default Vec4 wwyx() {return new Vec4(w(),w(),y(),x());}
	default Vec4 wwyy() {return new Vec4(w(),w(),y(),y());}
	default Vec4 wwyz() {return new Vec4(w(),w(),y(),z());}
	default Vec4 wwyw() {return new Vec4(w(),w(),y(),w());}
	default Vec4 wwzx() {return new Vec4(w(),w(),z(),x());}
	default Vec4 wwzy() {return new Vec4(w(),w(),z(),y());}
	default Vec4 wwzz() {return new Vec4(w(),w(),z(),z());}
	default Vec4 wwzw() {return new Vec4(w(),w(),z(),w());}
	default Vec4 wwwx() {return new Vec4(w(),w(),w(),x());}
	default Vec4 wwwy() {return new Vec4(w(),w(),w(),y());}
	default Vec4 wwwz() {return new Vec4(w(),w(),w(),z());}
	default Vec4 wwww() {return new Vec4(w(),w(),w(),w());}
	// @formatter:on
	// #endregion
}
