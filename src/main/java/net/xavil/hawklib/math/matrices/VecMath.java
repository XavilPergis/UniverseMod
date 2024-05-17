package net.xavil.hawklib.math.matrices;

import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.hawklib.math.matrices.interfaces.Vec4Access;

public final class VecMath {

	private VecMath() {
	}

	public static double dot(Vec3Access a, Vec3Access b) {
		return a.dot(b);
	}

	public static Vec3 transformPerspective(Mat4Access mat, Vec3Access xyz, double w) {
		final var xp = (mat.r0c0() * xyz.x()) + (mat.r0c1() * xyz.y()) + (mat.r0c2() * xyz.z()) + (mat.r0c3() * w);
		final var yp = (mat.r1c0() * xyz.x()) + (mat.r1c1() * xyz.y()) + (mat.r1c2() * xyz.z()) + (mat.r1c3() * w);
		final var zp = (mat.r2c0() * xyz.x()) + (mat.r2c1() * xyz.y()) + (mat.r2c2() * xyz.z()) + (mat.r2c3() * w);
		final var wp = (mat.r3c0() * xyz.x()) + (mat.r3c1() * xyz.y()) + (mat.r3c2() * xyz.z()) + (mat.r3c3() * w);
		return new Vec3(xp / wp, yp / wp, zp / wp);
	}

	public static Vec3.Mutable transformPerspective(Vec3.Mutable out, Mat4Access mat, Vec3Access xyz, double w) {
		final var xp = (mat.r0c0() * xyz.x()) + (mat.r0c1() * xyz.y()) + (mat.r0c2() * xyz.z()) + (mat.r0c3() * w);
		final var yp = (mat.r1c0() * xyz.x()) + (mat.r1c1() * xyz.y()) + (mat.r1c2() * xyz.z()) + (mat.r1c3() * w);
		final var zp = (mat.r2c0() * xyz.x()) + (mat.r2c1() * xyz.y()) + (mat.r2c2() * xyz.z()) + (mat.r2c3() * w);
		final var wp = (mat.r3c0() * xyz.x()) + (mat.r3c1() * xyz.y()) + (mat.r3c2() * xyz.z()) + (mat.r3c3() * w);
		out.x = xp / wp;
		out.y = yp / wp;
		out.z = zp / wp;
		return out;
	}

	public static double transform(Vec3.Mutable out, Mat4Access mat, Vec3Access xyz, double w) {
		final var xp = (mat.r0c0() * xyz.x()) + (mat.r0c1() * xyz.y()) + (mat.r0c2() * xyz.z()) + (mat.r0c3() * w);
		final var yp = (mat.r1c0() * xyz.x()) + (mat.r1c1() * xyz.y()) + (mat.r1c2() * xyz.z()) + (mat.r1c3() * w);
		final var zp = (mat.r2c0() * xyz.x()) + (mat.r2c1() * xyz.y()) + (mat.r2c2() * xyz.z()) + (mat.r2c3() * w);
		final var wp = (mat.r3c0() * xyz.x()) + (mat.r3c1() * xyz.y()) + (mat.r3c2() * xyz.z()) + (mat.r3c3() * w);
		out.x = xp;
		out.y = yp;
		out.z = zp;
		return wp;
	}

	public static Vec4 transform(Mat4Access mat, Vec4Access xyzw) {
		// @formatter:off
		final var x = (mat.r0c0() * xyzw.x()) + (mat.r0c1() * xyzw.y()) + (mat.r0c2() * xyzw.z()) + (mat.r0c3() * xyzw.w());
		final var y = (mat.r1c0() * xyzw.x()) + (mat.r1c1() * xyzw.y()) + (mat.r1c2() * xyzw.z()) + (mat.r1c3() * xyzw.w());
		final var z = (mat.r2c0() * xyzw.x()) + (mat.r2c1() * xyzw.y()) + (mat.r2c2() * xyzw.z()) + (mat.r2c3() * xyzw.w());
		final var w = (mat.r3c0() * xyzw.x()) + (mat.r3c1() * xyzw.y()) + (mat.r3c2() * xyzw.z()) + (mat.r3c3() * xyzw.w());
		// @formatter:on
		return new Vec4(x, y, z, w);
	}

	public static Vec4.Mutable transform(Vec4.Mutable out, Mat4Access mat, Vec4Access xyzw) {
		// @formatter:off
		final var x = (mat.r0c0() * xyzw.x()) + (mat.r0c1() * xyzw.y()) + (mat.r0c2() * xyzw.z()) + (mat.r0c3() * xyzw.w());
		final var y = (mat.r1c0() * xyzw.x()) + (mat.r1c1() * xyzw.y()) + (mat.r1c2() * xyzw.z()) + (mat.r1c3() * xyzw.w());
		final var z = (mat.r2c0() * xyzw.x()) + (mat.r2c1() * xyzw.y()) + (mat.r2c2() * xyzw.z()) + (mat.r2c3() * xyzw.w());
		final var w = (mat.r3c0() * xyzw.x()) + (mat.r3c1() * xyzw.y()) + (mat.r3c2() * xyzw.z()) + (mat.r3c3() * xyzw.w());
		out.x = x; out.y = y; out.z = z; out.w = w;
		// @formatter:on
		return out;
	}

}
