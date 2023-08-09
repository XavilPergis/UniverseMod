package net.xavil.hawklib.math.matrices.interfaces;

import net.xavil.hawklib.math.matrices.Vec3;

public interface Vec3iAccess {
	// @formatter:off
	int x();
	int y();
	int z();
	// @formatter:on


	default Vec3 lowerCorner() {
		return new Vec3(x(), y(), z());
	}

	default Vec3 upperCorner() {
		return new Vec3(x() + 1.0, y() + 1.0, z() + 1.0);
	}

	default Vec3 center() {
		return new Vec3(x() + 0.5, y() + 0.5, z() + 0.5);
	}

}
