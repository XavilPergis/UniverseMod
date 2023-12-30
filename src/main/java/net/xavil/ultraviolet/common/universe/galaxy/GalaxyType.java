package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.Random;

import net.minecraft.util.Mth;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.ultraviolet.common.universe.DensityFields;
import net.xavil.ultraviolet.common.universe.ScalarField;
import net.xavil.hawklib.math.matrices.Vec3;

public enum GalaxyType {

	// TODO: sattelite galaxies n stuff

	SPIRAL("spiral") {

		final class Params {
			public final double radius;
			public final double eccentricity;
			public final double galacticCoreSizeFactor;
			public final double discHeightFactor;
			public final Vec3 galaxySquish;

			public Params(SplittableRng rng) {
				this.radius = Units.Tm_PER_ly * rng.uniformDouble("radius", 40000, 80000);
				this.eccentricity = rng.uniformDouble("eccentricity", 0.0, 0.1);

				this.galacticCoreSizeFactor = rng.uniformDouble("core_size", 0.3, 0.4);
				this.discHeightFactor = rng.uniformDouble("disc_height", 0.05, 0.08);

				final var verticalSquish = rng.uniformDouble("height", 0.2, 0.333);
				this.galaxySquish = new Vec3(1 - this.eccentricity, 1 / verticalSquish, 1);
			}
		}

		static ScalarField spoke(SplittableRng rng, Params params) {
			final var size = rng.uniformDouble("spoke_size", 1.2, 1.5) * params.discHeightFactor * params.radius;
			final var density = rng.weightedDouble("spoke_density", 2, 1.0, 2.0);

			final var angleY = 2.0 * Math.PI * rng.uniformDouble("spoke_angle");
			final var p = Vec3.ZP.mul(params.radius).rotateY(angleY);

			var spoke = ScalarField.sdfLineSegment(p.neg(), p);

			// exponential^2 falloff
			spoke = ScalarField.pow(spoke.mul(1 / size), 2);
			// sets where the spoke "ends". ie, spoke density will be `0.01` at `size`
			// distance from the spoke center
			spoke = ScalarField.pow(0.01 / density, spoke).mul(density);

			// prevent core and spoke overlapping
			final var exclusionRadius = params.galacticCoreSizeFactor * params.radius;
			var coreMask = ScalarField.sub(1, ScalarField.sphereCloud(exclusionRadius));
			coreMask = coreMask.withExponent(4.0);
			spoke = spoke.mul(coreMask);

			return spoke;
		}

		@Override
		public DensityFields createDensityFields(double galaxyAge, SplittableRng rng) {
			final var params = new Params(rng);

			// (i think) central bulge is full of mostly quite old stars orbiting somewhat
			// chaotically around the central black hole.
			var galacticCoreDensity = ScalarField.sphereCloud(params.galacticCoreSizeFactor * params.radius);
			galacticCoreDensity = galacticCoreDensity.mulPos(params.galaxySquish);
			galacticCoreDensity = galacticCoreDensity.withExponent(4);
			galacticCoreDensity = galacticCoreDensity.mul(1600);
			// var galacticCoreAge = ScalarField.random().lerp(1, galaxyAge);

			// galactic halo is a very large region of very low stellar density that extends
			// quite far, in a sphere around the central black hole
			var galacticHalo = ScalarField.sphereCloud(2.5 * params.radius);
			galacticHalo = galacticHalo.mulPos(params.galaxySquish);
			galacticHalo = galacticHalo.mul(0.0000004);
			// var galacticHaloAge = ScalarField.random().withExponent(1e-5).lerp(1,
			// galaxyAge);

			// relatively thin disc of uniform star density and stellar ages
			var uniformDisc = ScalarField.verticalDisc(params.radius,
					params.radius * params.discHeightFactor, 2);
			uniformDisc = uniformDisc.mul(0.004);
			// var uniformDiscAge = ScalarField.random().lerp(0.3 * galaxyAge, galaxyAge);

			// "spokes" of higher star densities that are often (i think) home to active
			// star formation, meaning you have bands of new star systems all throughout the
			// spokes.
			final var majorSpokeCount = Mth.floor(rng.uniformDouble("major_spoke_count", 2, 3));
			final var minorSpokeCount = Mth.floor(rng.uniformDouble("minor_spoke_count", 3, 10));
			var spiralFactor = rng.uniformDouble("spiral_factor", 1, 2);
			if (rng.chance("reverse_spiral", 0.5))
				spiralFactor *= -1.0;

			ScalarField spokes = ScalarField.uniform(0);
			rng.push("major_spokes");
			for (int i = 0; i < majorSpokeCount; ++i) {
				rng.advance();
				spokes = spokes.add(spoke(rng, params));
			}
			rng.pop();
			rng.push("minor_spokes");
			for (int i = 0; i < minorSpokeCount; ++i) {
				rng.advance();
				spokes = spokes.add(spoke(rng, params).mul(0.1));
			}
			rng.pop();

			// spoke contribution -> 0 at galaxy limit
			spokes = spokes.mul(ScalarField.sphereCloud(params.radius));
			spokes = spokes.spiralAboutY(spiralFactor, 1.2 * params.radius);
			spokes = spokes.mulPos(params.galaxySquish);
			spokes = spokes.mul(50);
			// var spokesAge = ScalarField.random().withExponent(1e5).lerp(1, galaxyAge);

			// var densityCombined = galacticCoreDensity.add(galacticHalo).add(uniformDisc).add(spokes);
			// var densityCombined = galacticHalo.add(uniformDisc).add(spokes);
			// var densityCombined = ScalarField.uniform(0.004);
			var densityCombined = ScalarField.uniform(0);
			densityCombined = densityCombined.add(galacticCoreDensity);
			densityCombined = densityCombined.add(uniformDisc);
			densityCombined = densityCombined.add(spokes);
			var ageCombined = ScalarField.uniform(0.0);

			// Tm^-3/ly^-3 -> ly^3/Tm^3
			var finalStellarDensity = densityCombined.mul(ly3_PER_Tm3).max(0);
			var finalMinAge = ageCombined.max(0);
			// var finalMetallicity = ScalarField.uniform(0.5).max(0);
			var finalMetallicity = ScalarField.simplexNoise(rng.uniformDouble("metallicity_seed", 0, 10000))
					.mul(0.5).add(0.5)
					.withExponent(1.6)
					.lerp(0.0001, 0.2)
					.mulPos(10.0 * Units.Tm_PER_ly);

			return new DensityFields(params.radius, galaxyAge, finalStellarDensity, finalMinAge, finalMetallicity);
		}
	},
	// a disc galaxy with a nebulous, almost elliptical-like core and no noticable
	// spirals.
	// LENTICULAR("lenticular") {
	// @Override
	// public DensityFields createDensityFields(double galaxyAge, Random random) {
	// final var radius = Units.Tm_PER_ly * (random.nextDouble(2000, 9000));
	// var field = ScalarField.sphereCloud(radius).withExponent(4).mul(5);
	// var densityCombined = field;
	// var ageCombined = ScalarField.uniform(0.5);
	// var finalStellarDensity = densityCombined.mul(ly3_PER_Tm3);
	// var finalMinAge = ageCombined;
	// return new DensityFields(galaxyAge, radius, finalStellarDensity,
	// finalMinAge);
	// }
	// },
	ELLIPTICAL("elliptical") {
		@Override
		public DensityFields createDensityFields(double galaxyAge, SplittableRng rng) {
			final var radius = Units.Tm_PER_ly * (rng.uniformDouble("radius", 10000, 60000));
			var field = ScalarField.sphereCloud(radius).withExponent(2).mul(20);

			var densityCombined = field;
			var ageCombined = ScalarField.uniform(0.0);

			var finalStellarDensity = densityCombined.mul(ly3_PER_Tm3).max(0);
			var finalMinAge = ageCombined.max(0);
			var finalMetallicity = ScalarField.uniform(0.5).max(0);

			return new DensityFields(radius, galaxyAge, finalStellarDensity, finalMinAge, finalMetallicity);
		}
	};

	private static final double ly3_PER_Tm3 = Math.pow(Units.ly_PER_Tm, 3.0);

	public final String name;

	private GalaxyType(String name) {
		this.name = name;
	}

	public abstract DensityFields createDensityFields(double galaxyAge, SplittableRng rng);
}