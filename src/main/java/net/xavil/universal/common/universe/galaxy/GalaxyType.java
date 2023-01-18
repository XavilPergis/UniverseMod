package net.xavil.universal.common.universe.galaxy;

import java.util.Random;

import net.xavil.universal.common.universe.DensityField3;
import net.xavil.universal.common.universe.Units;

public enum GalaxyType {
	// TODO: maybe split between barred spiral and regular spiral galaxies.
	// SPIRAL("spiral") {
	// @Override
	// DensityField3 createDensityField(Random random) {
	// final var isBarred = random.nextBoolean();
	// // TODO Auto-generated method stub
	// return null;
	// }
	// },
	// a disc galaxy with a nebulous, almost elliptical-like core and no noticable
	// spirals.
	LENTICULAR("lenticular") {
		@Override
		public DensityField3 createDensityField(Random random) {
			final var radius = random.nextDouble(Units.TM_PER_LY * 3000, Units.TM_PER_LY * 300000);
			final var discHeightFactor = random.nextDouble(0.003, 0.03);
			final var ellipticalFactor = random.nextDouble(0.2, 0.8);

			// FIXME: densities here are waaaay too big
			final var centralBulge = DensityField3.sphereCloud(radius * ellipticalFactor);
			final var thinDiscField = DensityField3.min(
					DensityField3.cylinderMask(radius, radius * discHeightFactor),
					DensityField3.uniform(Units.LY_PER_TM * 0.4f));

			return DensityField3.add(centralBulge, thinDiscField);
		}
	},
	ELLIPTICAL("elliptical") {
		@Override
		public DensityField3 createDensityField(Random random) {
			var radius = random.nextDouble(Units.TM_PER_LY * 3000, Units.TM_PER_LY * 300000);
			var heightFactor = random.nextDouble(0.05, 1.0);
			var widthFactor = random.nextDouble(0.7, 1.0);
			return DensityField3.sphereCloud(radius).scale(widthFactor, heightFactor, 1).curvePoly(0.4f);
		}
	};
	// a galaxy shape created by galactic collisions.
	// PECULIAR("peculiar"),
	// other strange galaxy shapes.
	// IRREGULAR("irregular");

	public final String name;

	private GalaxyType(String name) {
		this.name = name;
	}

	public abstract DensityField3 createDensityField(Random random);
}