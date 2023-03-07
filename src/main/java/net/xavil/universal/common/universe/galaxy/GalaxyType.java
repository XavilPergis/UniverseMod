package net.xavil.universal.common.universe.galaxy;

import java.util.Random;

import net.xavil.universal.common.universe.DensityFields;
import net.xavil.universal.common.universe.DoubleField3;
import net.xavil.util.Units;

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
		public DensityFields createDensityFields(Random random) {
			// final var radius = random.nextDouble(Units.ly(3000), Units.ly(300000));
			final var radius = Units.fromLy(3000);
			final var galacticCoreSizeFactor = random.nextDouble(0.5, 0.7);
			final var galacticCoreSquishFactor = random.nextDouble(3, 5);
			final var discHeightFactor = random.nextDouble(0.003, 0.03);
			final var spokeCount = random.nextInt(1, 4);
			final var spokeCurve = random.nextDouble(1.5, 6);
			final var spiralFactor = random.nextDouble(0.33, 2);
			// final var ellipticalFactor = random.nextDouble(0.2, 0.8);


			// var noise =
			// DoubleField3.simplexNoise(random.nextDouble()).scale(Units.LY_PER_TM / 5f);

			// (i think) central bulge is full of mostly quite old stars orbiting somewhat
			// chaotically around the central black hole.
			var galacticCore = DoubleField3.sphereCloud(galacticCoreSizeFactor * radius)
					.scale(1, galacticCoreSquishFactor, 1).curvePoly(2).mul(50);

			// galactic halo is a very large region of very low stellar density that extends
			// quite far, in a sphere around the central black hole
			var galacticHalo = DoubleField3.sphereMask(1.5 * radius).mul(0.01);

			// relatively thin disc of uniform star density and stellar ages
			var uniformDisc = DoubleField3.verticalDisc(radius, radius * discHeightFactor, 1, 1.5).mul(10);
			uniformDisc = uniformDisc.curvePoly(1.5);

			// "spokes" of higher star densities that are often (i think) home to active
			// star formation, meaning you have bands of new star systems all throughout the
			// spokes.
			var spokesDisc = DoubleField3.verticalDisc(radius, 1.5 * radius * discHeightFactor, 1.8, 2);
			var spokes = DoubleField3.spokes(spokeCount, spokeCurve)
					.mul(20)
					.spiralAboutY(spiralFactor * 1e-8 / radius)
					.mul(spokesDisc);

			// var densityCombined = galacticCore;
			// var densityCombined = DoubleField3.spokes(3, 10).mul(20).spiralAboutY(1e-8 /
			// radius);
			// var densityCombined = DoubleField3.verticalDisc(radius, radius, 1,
			// 1).mul(500);
			// var densityCombined = DoubleField3.cylinderMask(radius, radius).mul(500);
			// var densityCombined = DoubleField3.uniform(1);

			var densityCombined =
			galacticCore.add(galacticHalo).add(uniformDisc).add(spokes);
			var ageCombined = DoubleField3.uniform(0.5);

			var volumeFactor = Units.ly_PER_Tm * Units.ly_PER_Tm * Units.ly_PER_Tm;
			// var volumeFactor = 1;
			var finalStellarDensity = densityCombined.mul(volumeFactor);
			var finalMinAge = ageCombined;

			return new DensityFields(radius, finalStellarDensity, finalMinAge);
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

	public abstract DensityFields createDensityFields(Random random);
}