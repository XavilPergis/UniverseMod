package net.xavil.hawklib.math;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public final class Sdf {

	public static double sphere(Vec3Access p, Vec3Access o, double r) {
		return o.distanceTo(p) - r;
	}

	public static double sphere(Vec3Access p, double r) {
		return p.length() - r;
	}

	public static double capsule(Vec3Access p, Vec3Access a, Vec3Access b, double r) {
		final var ba = b.sub(a);
		final var pa = p.sub(a);
		final var h = Mth.clamp(pa.dot(ba) / ba.lengthSquared(), 0.0, 1.0);
		return pa.sub(ba.mul(h)).length() - r;
	}

	public static double plane(Vec3Access p, Vec3Access o, Vec3Access N) {
		return p.sub(o).dot(N);
	}

	public static double plane(Vec3Access p, Vec3Access N) {
		return p.dot(N);
	}

}
