package net.xavil.universal.common.universe.galaxy;

import java.util.Random;

import net.xavil.universal.common.universe.DensityFields;
import net.xavil.universal.common.universe.DoubleField3;
import net.xavil.util.Units;

public enum GalaxyType {

	SPIRAL("spiral") {
		@Override
		public DensityFields createDensityFields(double galaxyAge, Random random) {
			final var radius = Units.fromLy(random.nextDouble(2000, 9000));
			final var galacticCoreSizeFactor = random.nextDouble(0.3, 0.5);
			final var galacticCoreSquishFactor = random.nextDouble(2, 5);
			final var discHeightFactor = random.nextDouble(0.05, 0.2);
			final var spokeCount = random.nextInt(1, 2);
			final var spokeCurve = random.nextDouble(2, 4);
			var spiralFactor = random.nextDouble(1, 5);
			if (random.nextBoolean())
				spiralFactor *= -1.0;

			// var noise =
			// DoubleField3.simplexNoise(random.nextDouble()).scale(Units.LY_PER_TM / 5f);

			// (i think) central bulge is full of mostly quite old stars orbiting somewhat
			// chaotically around the central black hole.
			var galacticCoreDensity = DoubleField3.sphereCloud(galacticCoreSizeFactor * radius)
					.scale(1, galacticCoreSquishFactor, 1).curvePoly(2).mul(3);
			// var galacticCoreAge = DoubleField3.random().lerp(1, galaxyAge);

			// galactic halo is a very large region of very low stellar density that extends
			// quite far, in a sphere around the central black hole
			var galacticHalo = DoubleField3.sphereMask(1.5 * radius).scale(1, galacticCoreSquishFactor, 1).mul(0.001);
			// var galacticHaloAge = DoubleField3.random().curvePoly(1e-5).lerp(1,
			// galaxyAge);

			// relatively thin disc of uniform star density and stellar ages
			// var uniformDisc = DoubleField3.verticalDisc(radius, radius *
			// discHeightFactor, 1).mul(15);
			var uniformDisc = DoubleField3.verticalDisc(radius, radius * discHeightFactor, 3).mul(1.5);
			// var uniformDiscAge = DoubleField3.random().lerp(0.3 * galaxyAge, galaxyAge);
			// uniformDisc = uniformDisc.curvePoly(0.5);

			// "spokes" of higher star densities that are often (i think) home to active
			// star formation, meaning you have bands of new star systems all throughout the
			// spokes.
			var spokesDisc = DoubleField3.verticalDisc(radius, 1.5 * radius * discHeightFactor, 1.8);
			var spokes = DoubleField3.spokes(spokeCount, spokeCurve).mul(2.0).spiralAboutY(spiralFactor * 1e-8 / radius)
					.mul(spokesDisc);
			// var spokesAge = DoubleField3.random().curvePoly(1e5).lerp(1, galaxyAge);

			var densityCombined = galacticCoreDensity.add(galacticHalo).add(uniformDisc).add(spokes);
			var ageCombined = DoubleField3.uniform(0.5);

			var finalStellarDensity = densityCombined.mul(ly3_PER_Tm3);
			var finalMinAge = ageCombined;

			return new DensityFields(galaxyAge, radius, finalStellarDensity, finalMinAge);
		}
	},
	// a disc galaxy with a nebulous, almost elliptical-like core and no noticable
	// spirals.
	// LENTICULAR("lenticular") {
	// @Override
	// public DensityFields createDensityFields(double galaxyAge, Random random) {
	// final var radius = Units.fromLy(random.nextDouble(2000, 9000));
	// var field = DoubleField3.sphereCloud(radius).curvePoly(4).mul(5);
	// var densityCombined = field;
	// var ageCombined = DoubleField3.uniform(0.5);
	// var finalStellarDensity = densityCombined.mul(ly3_PER_Tm3);
	// var finalMinAge = ageCombined;
	// return new DensityFields(galaxyAge, radius, finalStellarDensity,
	// finalMinAge);
	// }
	// },
	ELLIPTICAL("elliptical") {
		@Override
		public DensityFields createDensityFields(double galaxyAge, Random random) {
			final var radius = Units.fromLy(random.nextDouble(2000, 9000));
			var field = DoubleField3.sphereCloud(radius).curvePoly(4).mul(5);

			var densityCombined = field;
			var ageCombined = DoubleField3.uniform(0.5);

			var finalStellarDensity = densityCombined.mul(ly3_PER_Tm3);
			var finalMinAge = ageCombined;

			return new DensityFields(galaxyAge, radius, finalStellarDensity, finalMinAge);
		}
	};

	private static final double ly3_PER_Tm3 = Math.pow(Units.ly_PER_Tm, 3.0);

	public final String name;

	private GalaxyType(String name) {
		this.name = name;
	}

	public abstract DensityFields createDensityFields(double galaxyAge, Random random);
}