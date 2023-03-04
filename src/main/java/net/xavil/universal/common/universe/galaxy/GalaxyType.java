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
			final var radius = random.nextDouble(Units.ly(3000), Units.ly(300000));
			final var discHeightFactor = random.nextDouble(0.003, 0.03);
			final var ellipticalFactor = random.nextDouble(0.2, 0.8);

			var noise = DensityField3.simplexNoise(random.nextDouble()).scale(Units.LY_PER_TM / 5f);

			// FIXME: densities here are waaaay too big
			final var centralBulge = DensityField3.mul(
					DensityField3.uniform(10), DensityField3.sphereCloud(radius * ellipticalFactor));
			// final var thinDiscField = DensityField3.min(
			// 		DensityField3.cylinderMask(radius, radius * discHeightFactor),
			// 		DensityField3.uniform(0.4f));

			// var combined = DensityField3.min(noise, DensityField3.add(centralBulge, thinDiscField));
			// var combined = DensityField3.mul(noise, centralBulge);
			// var combined = DensityField3.mul(noise, DensityField3.uniform(10));
			// var combined = DensityField3.mul(DensityField3.max(DensityField3.uniform(0.03), noise), DensityField3.uniform(50));
			// var combined = DensityField3.mul(DensityField3.max(DensityField3.uniform(0.03), noise), DensityField3.uniform(20));
			var combined = DensityField3.uniform(10);
			// var combined = centralBulge;
			// var combined = DensityField3.uniform(10);

			return DensityField3.mul(DensityField3.uniform(Units.LY_PER_TM * Units.LY_PER_TM * Units.LY_PER_TM),
					combined);
		}
	};
	// ELLIPTICAL("elliptical") {
	// @Override
	// public DensityField3 createDensityField(Random random) {
	// var radius = random.nextDouble(Units.TM_PER_LY * 3000, Units.TM_PER_LY *
	// 300000);
	// var heightFactor = random.nextDouble(0.05, 1.0);
	// var widthFactor = random.nextDouble(0.7, 1.0);
	// final var field = DensityField3.sphereCloud(radius).scale(widthFactor,
	// heightFactor, 1).curvePoly(0.4f);
	// return DensityField3.mul(DensityField3.uniform(Units.LY_PER_TM *
	// Units.LY_PER_TM * Units.LY_PER_TM), field);

	// }
	// };
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