package net.xavil.ultraviolet.common.universe.galaxy;

import net.minecraft.util.Mth;
import net.xavil.hawklib.ProbabilityDistribution;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.ultraviolet.common.universe.GalaxyParameters;
import net.xavil.ultraviolet.common.universe.ScalarField;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.Sdf;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public enum GalaxyType {

	// TODO: sattelite galaxies n stuff

	SPIRAL("spiral") {

		final class Params {
			public final double radius;
			public final double eccentricity;
			public final double galacticCoreSizeFactor;
			public final double discHeightFactor;
			public final Vec3 galaxySquish;

			public Params(Galaxy.Info info, SplittableRng rng) {
				// this.radius = Units.Tm_PER_ly * rng.uniformDouble("radius", 40000, 80000);
				this.radius = info.radius;
				this.eccentricity = rng.uniformDouble("eccentricity", 0.0, 0.1);

				this.galacticCoreSizeFactor = rng.uniformDouble("core_size", 0.3, 0.4);
				this.discHeightFactor = rng.uniformDouble("disc_height", 0.05, 0.08);

				final var verticalSquish = rng.uniformDouble("height", 0.2, 0.333);
				this.galaxySquish = new Vec3(1 - this.eccentricity, 1 / verticalSquish, 1);
			}
		}

		static final class Spoke {
			public final double spokeSize, spokeDensity, spokeAngle;
			public final boolean isMajor;

			public Spoke(SplittableRng rng, Params params, boolean isMajor) {
				this.spokeSize = rng.uniformDouble("spoke_size");
				this.spokeDensity = rng.uniformDouble("spoke_density");
				this.spokeAngle = rng.uniformDouble("spoke_angle");
				this.isMajor = isMajor;
			}

			public double evaluate(Params params, Vec3Access pos) {
				final var size = Mth.lerp(this.spokeSize, 1.2, 1.5) * params.discHeightFactor * params.radius;
				final var density = Mth.lerp(this.spokeDensity, 1.0, 2.0);

				final var angleY = 2.0 * Math.PI * this.spokeAngle;
				final var p = Vec3.ZP.mul(params.radius).rotateY(angleY);

				var spoke = Sdf.capsule(pos, p.neg(), p, 0);
				spoke = Math.pow(spoke / size, 4);
				spoke = density * Math.pow(0.01, spoke);

				if (!this.isMajor)
					spoke *= 0.05;

				return spoke;
			}

		}

		static final class DensityField {

			private final Vector<Spoke> spokes = new Vector<>();
			private final double spiralFactor;

			public DensityField(SplittableRng rng, Params params) {
				// "spokes" of higher star densities that are often (i think) home to active
				// star formation, meaning you have bands of new star systems all throughout the
				// spokes.
				final var majorSpokeCount = Mth.floor(rng.uniformDouble("major_spoke_count", 2, 3));
				final var minorSpokeCount = Mth.floor(rng.uniformDouble("minor_spoke_count", 3, 10));
				var spiralFactor = rng.uniformDouble("spiral_factor", 1, 2);
				if (rng.chance("reverse_spiral", 0.5))
					spiralFactor *= -1.0;
				this.spiralFactor = spiralFactor;

				rng.push("major_spokes");
				for (int i = 0; i < majorSpokeCount; ++i) {
					rng.advance();
					this.spokes.push(new Spoke(rng, params, true));
				}
				rng.pop();
				rng.push("minor_spokes");
				for (int i = 0; i < minorSpokeCount; ++i) {
					rng.advance();
					this.spokes.push(new Spoke(rng, params, false));
				}
				rng.pop();
			}

			public void evaluate(Params params, Vec3Access pos, GalaxyRegionWeights weights) {
				weights.arms = weights.core = weights.disc = weights.halo = 0;

				final var centerDist = Sdf.sphere(pos, Vec3.ZERO, 0) / params.radius;
				final var galaxyMask = Math.pow(Math.max(0, 1 - pos.length() / params.radius), 0.33);

				// core
				final var corePos = pos.mul(params.galaxySquish.mul(1, 0.7, 1));
				final var coreDist = Sdf.sphere(corePos, 0) / params.radius;
				weights.core = 0;
				weights.core += 20000 * Math.pow(10, -20.0 * coreDist);
				weights.core += 0.5 * Math.pow(10, -5.0 * coreDist);

				// halo
				weights.halo = Math.max(0, 1 - centerDist / 2.5);

				// disc
				final var discPos = pos.mul(params.galaxySquish);
				final var discDist = Math.abs(Sdf.plane(discPos, Vec3.YP) / (params.radius));
				weights.disc = 0;
				weights.disc += 2 * Math.pow(10, -5.0 * discDist);
				weights.disc += 10 * Math.pow(10, -30.0 * discDist);
				weights.disc *= galaxyMask;

				// spiral arms
				Vec3 spokePos = pos.mul(params.galaxySquish);
				spokePos = ScalarField.spiralAboutY(spokePos, this.spiralFactor, 1.2 * params.radius);
				for (final var spoke : this.spokes.iterable()) {
					weights.arms += spoke.evaluate(params, spokePos);
				}

				// final var exclusionRadius = params.galacticCoreSizeFactor * params.radius;
				// masks.arms *= Math.pow(Math.min(1, pos.length() / exclusionRadius), 4.0);
				weights.arms *= Math.pow(10, -15.0 * discDist);
				// spoke contribution -> 0 at galaxy limit
				weights.arms *= galaxyMask;
				weights.arms *= 20;

				// weights.core *= 10;
				// weights.disc *= 0.005;
				weights.halo *= 0.00001;
			}
		}

		@Override
		public GalaxyParameters createGalaxyParameters(Galaxy.Info info, SplittableRng rng) {
			final var params = new Params(info, rng);
			final var df = new DensityField(rng, params);

			final GalaxyRegionWeights.Field maskField = (pos, masks) -> df.evaluate(params, pos, masks);

			final var ageDomain = new Interval(0, info.ageMyr);
			final var coreSfh = ProbabilityDistribution.interpolate(age -> {
				double t = age / info.ageMyr;
				t = Math.exp(-4.3 * t);
				return Mth.lerp(t, 1, 1000);
			}, ageDomain, 4096);

			final var armsSfh = ProbabilityDistribution.interpolate(age -> {
				double t = age / info.ageMyr;
				final var N = 21;
				t = Math.expm1(N * t) / Math.expm1(N);
				return Mth.lerp(t, 50, 1000);
			}, ageDomain, 4096);

			final var discSfh = ProbabilityDistribution.interpolate(age -> {
				double t = age / info.ageMyr;
				final double warp = -2.1, squish = 4.8, peak = 0.43;
				t = Math.expm1(warp * t) / Math.expm1(warp);
				t = Math.exp(-squish * Mth.square(t - peak));
				return t;
			}, ageDomain, 4096);

			final var haloSfh = ProbabilityDistribution.interpolate(age -> 1, ageDomain, 4096);

			return new GalaxyParameters(params.radius, info.ageMyr, maskField,
					coreSfh, armsSfh, discSfh, haloSfh);
		}
	},
	;

	public final String name;

	private GalaxyType(String name) {
		this.name = name;
	}

	public abstract GalaxyParameters createGalaxyParameters(Galaxy.Info info, SplittableRng rng);
}