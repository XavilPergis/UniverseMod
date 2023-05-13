package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.Random;

import net.xavil.ultraviolet.common.universe.DensityFields;
import net.xavil.ultraviolet.common.universe.DoubleField3;
import net.xavil.util.Units;
import net.xavil.util.math.matrices.Vec3;

public enum GalaxyType {

	SPIRAL("spiral") {

		private DoubleField3 globularClusters(Random random) {
			final var noiseSeed = 100.0 * random.nextDouble();
			final var scale = random.nextDouble(0.08, 0.12);

			final var a = DoubleField3.simplexNoise(noiseSeed).mulPos(scale);
			return a.sub(0.9999999).clamp().mul(100.0);
			// float sum = noise(s * pos);
			// return 100.0 * clamp(sum - 0.9999999, 0.0, 1.0);
		}

		@Override
		public DensityFields createDensityFields(double galaxyAge, Random random) {
			final var radius = Units.fromLy(random.nextDouble(30000, 60000));
			final var ellipticalFactor = random.nextDouble(0.85, 1.0);
			final var galacticCoreSizeFactor = random.nextDouble(0.3, 0.4);
			final var galacticCoreSquishFactor = random.nextDouble(2, 5);
			final var discHeightFactor = random.nextDouble(0.05, 0.2);
			final var spokeCount = random.nextInt(1, 2);
			final var spokeCurve = random.nextDouble(10, 30);
			var spiralFactor = random.nextDouble(1.5, 5);
			if (random.nextBoolean())
				spiralFactor *= -1.0;

			final var ns = 100.0 * random.nextDouble();

			// var noise1 = DoubleField3.simplexNoise(0.0).mulPos(1.0).mul(0.5).add(0.5);
			// var noise2 = DoubleField3.simplexNoise(1.0).mulPos(0.02).mul(0.5).add(0.5);
			// var noise1 = DoubleField3.max(DoubleField3.uniform(0.0),
			// DoubleField3.simplexNoise(ns + 0.0).mulPos(1.0));
			var noise2 = DoubleField3.simplexNoise(ns + 1.0).mulPos(1.0 / Units.fromLy(50.0));
			noise2 = noise2.lerp(0.25, 1.0);
			noise2 = noise2.max(0.0);

			// var noise =
			// DoubleField3.simplexNoise(random.nextDouble()).mulPos(Units.LY_PER_TM / 5f);

			final var galaxySquish = Vec3.from(ellipticalFactor, galacticCoreSquishFactor, 1);
			final var discSquish = Vec3.from(ellipticalFactor, 1, 1);

			// (i think) central bulge is full of mostly quite old stars orbiting somewhat
			// chaotically around the central black hole.
			var galacticCoreDensity = DoubleField3.sphereCloud(galacticCoreSizeFactor * radius)
					.mulPos(galaxySquish).withExponent(4).mul(120);
			// var galacticCoreAge = DoubleField3.random().lerp(1, galaxyAge);

			// galactic halo is a very large region of very low stellar density that extends
			// quite far, in a sphere around the central black hole
			var galacticHalo = DoubleField3.sphereMask(1.5 * radius);
			galacticHalo = galacticHalo.mulPos(galaxySquish).mul(0.01);
			// var galacticHaloAge = DoubleField3.random().withExponent(1e-5).lerp(1,
			// galaxyAge);

			// relatively thin disc of uniform star density and stellar ages
			// var uniformDisc = DoubleField3.verticalDisc(radius, radius *
			// discHeightFactor, 1).mul(15);
			var uniformDisc = DoubleField3.verticalDisc(radius, radius * discHeightFactor, 2)
					.mulPos(discSquish).withExponent(2.0).mul(20.0);
			// var uniformDiscAge = DoubleField3.random().lerp(0.3 * galaxyAge, galaxyAge);
			// uniformDisc = uniformDisc.withExponent(0.5);

			// "spokes" of higher star densities that are often (i think) home to active
			// star formation, meaning you have bands of new star systems all throughout the
			// spokes.
			var spokesDisc = DoubleField3.verticalDisc(radius, 1.5 * radius * discHeightFactor, 1)
					.mulPos(discSquish).withExponent(2.0);
			var spokes = DoubleField3.spokes(spokeCount, spokeCurve).mul(80.0)
					.spiralAboutY(spiralFactor * 1e-9 / radius)
					.mul(spokesDisc);
			// var spokesAge = DoubleField3.random().withExponent(1e5).lerp(1, galaxyAge);

			// var densityCombined = spokes;
			var densityCombined = galacticCoreDensity.add(galacticHalo).add(uniformDisc).add(spokes).mul(0.2);
			var ageCombined = DoubleField3.uniform(0.0);

			var finalStellarDensity = densityCombined.mul(ly3_PER_Tm3);
			var finalMinAge = ageCombined;

			return new DensityFields(radius, galaxyAge, finalStellarDensity, finalMinAge);
		}
	},
	// a disc galaxy with a nebulous, almost elliptical-like core and no noticable
	// spirals.
	// LENTICULAR("lenticular") {
	// @Override
	// public DensityFields createDensityFields(double galaxyAge, Random random) {
	// final var radius = Units.fromLy(random.nextDouble(2000, 9000));
	// var field = DoubleField3.sphereCloud(radius).withExponent(4).mul(5);
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
			final var radius = Units.fromLy(random.nextDouble(10000, 60000));
			var field = DoubleField3.sphereCloud(radius).withExponent(4).mul(5);

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