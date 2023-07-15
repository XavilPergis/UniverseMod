package net.xavil.hawklib.math.matrices.interfaces;

import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec4;

public interface Vec2Access {
	// @formatter:off
	double x();
	double y();
	// @formatter:on

	default net.minecraft.world.phys.Vec2 asMinecraft() {
		return new net.minecraft.world.phys.Vec2((float) x(), (float) y());
	}

	default double dot(Vec2Access other) {
		return this.x() * other.x() + this.y() * other.y();
	}

	default double distanceToSquared(Vec2Access other) {
		final var dx = this.x() - other.x();
		final var dy = this.y() - other.y();
		return dx * dx + dy * dy;
	}

	default double distanceTo(Vec2Access other) {
		return Math.sqrt(distanceToSquared(other));
	}

	default double lengthSquared() {
		return this.x() * this.x() + this.y() * this.y();
	}

	default double length() {
		return Math.sqrt(lengthSquared());
	}

	// #region Swizzle Operations
	// @formatter:off
	default Vec2 xx() {return new Vec2(x(),x());}
	default Vec2 xy() {return new Vec2(x(),y());}
	default Vec2 yx() {return new Vec2(y(),x());}
	default Vec2 yy() {return new Vec2(y(),y());}

	default Vec3 x0x() {return new Vec3(x(),0,x());}
	default Vec3 x0y() {return new Vec3(x(),0,y());}
	default Vec3 y0x() {return new Vec3(y(),0,x());}
	default Vec3 y0y() {return new Vec3(y(),0,y());}
	default Vec3 x1x() {return new Vec3(x(),1,x());}
	default Vec3 x1y() {return new Vec3(x(),1,y());}
	default Vec3 y1x() {return new Vec3(y(),1,x());}
	default Vec3 y1y() {return new Vec3(y(),1,y());}

	default Vec3 xxx() {return new Vec3(x(),x(),x());}
	default Vec3 xxy() {return new Vec3(x(),x(),y());}
	default Vec3 xyx() {return new Vec3(x(),y(),x());}
	default Vec3 xyy() {return new Vec3(x(),y(),y());}
	default Vec3 yxx() {return new Vec3(y(),x(),x());}
	default Vec3 yxy() {return new Vec3(y(),x(),y());}
	default Vec3 yyx() {return new Vec3(y(),y(),x());}
	default Vec3 yyy() {return new Vec3(y(),y(),y());}

	default Vec4 xxxx() {return new Vec4(x(),x(),x(),x());}
	default Vec4 xxxy() {return new Vec4(x(),x(),x(),y());}
	default Vec4 xxyx() {return new Vec4(x(),x(),y(),x());}
	default Vec4 xxyy() {return new Vec4(x(),x(),y(),y());}
	default Vec4 xyxx() {return new Vec4(x(),y(),x(),x());}
	default Vec4 xyxy() {return new Vec4(x(),y(),x(),y());}
	default Vec4 xyyx() {return new Vec4(x(),y(),y(),x());}
	default Vec4 xyyy() {return new Vec4(x(),y(),y(),y());}
	default Vec4 yxxx() {return new Vec4(y(),x(),x(),x());}
	default Vec4 yxxy() {return new Vec4(y(),x(),x(),y());}
	default Vec4 yxyx() {return new Vec4(y(),x(),y(),x());}
	default Vec4 yxyy() {return new Vec4(y(),x(),y(),y());}
	default Vec4 yyxx() {return new Vec4(y(),y(),x(),x());}
	default Vec4 yyxy() {return new Vec4(y(),y(),x(),y());}
	default Vec4 yyyx() {return new Vec4(y(),y(),y(),x());}
	default Vec4 yyyy() {return new Vec4(y(),y(),y(),y());}
	// @formatter:on
	// #endregion

}
