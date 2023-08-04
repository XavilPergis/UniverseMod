package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.Random;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Units;
import net.xavil.ultraviolet.common.universe.DensityFields;
import net.xavil.ultraviolet.common.universe.DoubleField3;
import net.xavil.hawklib.math.matrices.Vec3;

public enum GalaxyType {

	// TODO: sattelite galaxies n stuff

	SPIRAL("spiral") {

		// private DoubleField3 globularClusters(Random random) {
		// final var noiseSeed = 100.0 * random.nextDouble();
		// final var scale = random.nextDouble(0.08, 0.12);

		// final var a = DoubleField3.simplexNoise(noiseSeed).mulPos(scale);
		// return a.sub(0.9999999).clamp().mul(100.0);
		// // float sum = noise(s * pos);
		// // return 100.0 * clamp(sum - 0.9999999, 0.0, 1.0);
		// }

		private static DoubleField3 spoke(Random random, double size, double exclusionRadius) {
			final var angleY = 2.0 * Math.PI * random.nextDouble();
			// final var angleX1 = 0.02 * 2.0 * Math.PI * random.nextDouble();
			// final var angleX2 = 0.02 * 2.0 * Math.PI * random.nextDouble();
			// final var p = Vec3.ZP.rotateX(angleX1).rotateY(angleY).rotateX(-angleX2);
			final var p = Vec3.ZP.mul(Units.fromLy(30000)).rotateY(angleY);
			var spoke = DoubleField3.sdfLineSegment(Vec3.ZERO, Vec3.ZP.mul(Units.fromLy(60000))).sdfCloud(size);
			// spoke = spoke.mul(DoubleField3.sphereCloud(exclusionRadius).neg().add(1));
			return spoke;
		}

		@Override
		public DensityFields createDensityFields(double galaxyAge, Random random) {
			final var radius = Units.fromLy(random.nextDouble(30000, 60000));
			final var ellipticalFactor = random.nextDouble(0.85, 1.0);
			final var galacticCoreSizeFactor = random.nextDouble(0.3, 0.4);
			final var galacticCoreSquishFactor = random.nextDouble(3, 5);
			final var discHeightFactor = random.nextDouble(0.05, 0.2);

			final var galaxySquish = new Vec3(ellipticalFactor, galacticCoreSquishFactor, 1);
			// final var discSquish = new Vec3(ellipticalFactor, 1, 1);

			// (i think) central bulge is full of mostly quite old stars orbiting somewhat
			// chaotically around the central black hole.
			var galacticCoreDensity = DoubleField3.sphereCloud(galacticCoreSizeFactor * radius)
					.mulPos(galaxySquish).withExponent(1).mul(120);
			// var galacticCoreAge = DoubleField3.random().lerp(1, galaxyAge);

			// galactic halo is a very large region of very low stellar density that extends
			// quite far, in a sphere around the central black hole
			var galacticHalo = DoubleField3.sphereCloud(1.5 * radius);
			galacticHalo = galacticHalo.mulPos(galaxySquish).mul(0.001);
			// var galacticHaloAge = DoubleField3.random().withExponent(1e-5).lerp(1,
			// galaxyAge);

			// relatively thin disc of uniform star density and stellar ages
			// var uniformDisc = DoubleField3.verticalDisc(radius, radius *
			// discHeightFactor, 1).mul(15);
			var uniformDisc = DoubleField3.verticalDisc(radius, radius * discHeightFactor, 1)
					/*.mulPos(discSquish)*/.withExponent(2.0).mul(5.0);
			// var uniformDiscAge = DoubleField3.random().lerp(0.3 * galaxyAge, galaxyAge);
			// uniformDisc = uniformDisc.withExponent(0.5);

			// "spokes" of higher star densities that are often (i think) home to active
			// star formation, meaning you have bands of new star systems all throughout the
			// spokes.
			final var majorSpokeCount = random.nextInt(2, 3);
			final var minorSpokeCount = random.nextInt(3, 10);
			var spiralFactor = random.nextDouble(1, 2);
			if (random.nextBoolean())
				spiralFactor *= -1.0;

			DoubleField3 spokes = DoubleField3.uniform(0);
			for (int i = 0; i < majorSpokeCount; ++i) {
				final var size = random.nextDouble(0.15, 0.19) * radius;
				final var exclusionRadius = galacticCoreSizeFactor * radius;
				// final var spoke = spoke(random, 0.5 * radius, 0);

				final var angleY = 2.0 * Math.PI * random.nextDouble();
				// final var angleX1 = 0.02 * 2.0 * Math.PI * random.nextDouble();
				// final var angleX2 = 0.02 * 2.0 * Math.PI * random.nextDouble();
				// final var p = Vec3.ZP.rotateX(angleX1).rotateY(angleY).rotateX(-angleX2);
				final var p = Vec3.ZP.mul(radius).rotateY(angleY);
				var spoke = DoubleField3.sdfLineSegment(p.neg(), p).sdfCloud(size);
				spoke = spoke.mul(DoubleField3.uniform(1).sub(DoubleField3.sphereCloud(exclusionRadius)).withExponent(8.0));
				spoke = spoke.mul(2.0);
				// spoke = spoke.mul(DoubleField3.sphereMask(exclusionRadius));
				spokes = spokes.add(spoke);
			}
			for (int i = 0; i < minorSpokeCount; ++i) {
				final var size = random.nextDouble(0.08, 0.1) * radius;
				final var exclusionRadius = Mth.lerp(random.nextDouble(), galacticCoreSizeFactor * radius, 0.6 * radius);
				final var angleY = 2.0 * Math.PI * random.nextDouble();
				final var p = Vec3.ZP.rotateY(angleY).mul(radius);
				final var angleY2 = 2.0 * Math.PI * random.nextDouble();
				final var p2 = Vec3.ZP.rotateY(angleY2).mul(0.6 * radius * random.nextDouble());
				var spoke = DoubleField3.sdfLineSegment(p2, p).sdfCloud(size);
				spoke = spoke.mul(DoubleField3.uniform(1).sub(DoubleField3.sphereCloud(exclusionRadius)).withExponent(8.0));
				spokes = spokes.add(spoke);
			}
			var spokesDisc = DoubleField3.verticalDisc(radius, radius * discHeightFactor, 1).withExponent(2.0);

			// spokes = DoubleField3.sdfLineSegment(Vec3.ZERO, Vec3.ZP.mul(radius)).sdfCloud(0.1 * radius);
			spokes = spokes.mul(DoubleField3.sphereCloud(radius));
			// spokes = spokes.mulPos(discSquish);
			spokes = spokes.spiralAboutY(spiralFactor, 1.2 * radius);
			spokes = spokes.mul(spokesDisc);
			spokes = spokes.mul(120.0);
			// var spokesAge = DoubleField3.random().withExponent(1e5).lerp(1, galaxyAge);


			var densityCombined = galacticCoreDensity.add(galacticHalo).add(uniformDisc).add(spokes);
			// var densityCombined = galacticCoreDensity.add(spokes);
			var ageCombined = DoubleField3.uniform(0.0);

			var finalStellarDensity = densityCombined.mul(ly3_PER_Tm3).max(0);
			var finalMinAge = ageCombined.max(0);
			var finalMetallicity = DoubleField3.uniform(0.5).max(0);

			return new DensityFields(radius, galaxyAge, finalStellarDensity, finalMinAge, finalMetallicity);
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
			var field = DoubleField3.sphereCloud(radius).withExponent(2).mul(20);

			var densityCombined = field;
			var ageCombined = DoubleField3.uniform(0.0);

			var finalStellarDensity = densityCombined.mul(ly3_PER_Tm3).max(0);
			var finalMinAge = ageCombined.max(0);
			var finalMetallicity = DoubleField3.uniform(0.5).max(0);

			return new DensityFields(radius, galaxyAge, finalStellarDensity, finalMinAge, finalMetallicity);
		}
	};

	private static final double ly3_PER_Tm3 = Math.pow(Units.ly_PER_Tm, 3.0);

	public final String name;

	private GalaxyType(String name) {
		this.name = name;
	}

	public abstract DensityFields createDensityFields(double galaxyAge, Random random);
}