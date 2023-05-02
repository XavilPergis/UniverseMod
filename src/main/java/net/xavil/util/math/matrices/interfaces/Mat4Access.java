package net.xavil.util.math.matrices.interfaces;

import net.xavil.util.math.matrices.Mat4;
import net.xavil.util.math.matrices.Vec3;
import net.xavil.util.math.matrices.Vec4;

public interface Mat4Access {
	double r0c0();
	double r0c1();
	double r0c2();
	double r0c3();
	double r1c0();
	double r1c1();
	double r1c2();
	double r1c3();
	double r2c0();
	double r2c1();
	double r2c2();
	double r2c3();
	double r3c0();
	double r3c1();
	double r3c2();
	double r3c3();

	default Mat4 asImmutable() {
		// @formatter:off
		return new Mat4(
			r0c0(), r0c1(), r0c2(), r0c3(),
			r1c0(), r1c1(), r1c2(), r1c3(),
			r2c0(), r2c1(), r2c2(), r2c3(),
			r3c0(), r3c1(), r3c2(), r3c3()
		);
		// @formatteron
	}

	default Mat4.Mutable asMutable() {
		return new Mat4.Mutable().loadFrom(this);
	}

	// @formatter:off
	default Vec4 c0() { return new Vec4(this.r0c0(), this.r1c0(), this.r2c0(), this.r3c0()); }
	default Vec4 c1() { return new Vec4(this.r0c1(), this.r1c1(), this.r2c1(), this.r3c1()); }
	default Vec4 c2() { return new Vec4(this.r0c2(), this.r1c2(), this.r2c2(), this.r3c2()); }
	default Vec4 c3() { return new Vec4(this.r0c3(), this.r1c3(), this.r2c3(), this.r3c3()); }
	default Vec4 r0() { return new Vec4(this.r0c0(), this.r0c1(), this.r0c2(), this.r0c3()); }
	default Vec4 r1() { return new Vec4(this.r1c0(), this.r1c1(), this.r1c2(), this.r1c3()); }
	default Vec4 r2() { return new Vec4(this.r2c0(), this.r2c1(), this.r2c2(), this.r2c3()); }
	default Vec4 r3() { return new Vec4(this.r3c0(), this.r3c1(), this.r3c2(), this.r3c3()); }

	// conveniences for `cN().xyz()`
	// mostly meant for affine matrices.
	default Vec3 basisX()      { return new Vec3(this.r0c0(), this.r1c0(), this.r2c0()); }
	default Vec3 basisY()      { return new Vec3(this.r0c1(), this.r1c1(), this.r2c1()); }
	default Vec3 basisZ()      { return new Vec3(this.r0c2(), this.r1c2(), this.r2c2()); }
	default Vec3 translation() { return new Vec3(this.r0c3(), this.r1c3(), this.r2c3()); }
	// @formatter:on
}
