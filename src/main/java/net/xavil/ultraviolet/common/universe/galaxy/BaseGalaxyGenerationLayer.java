package net.xavil.ultraviolet.common.universe.galaxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.LinearSpline;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.NameTemplate;
import net.xavil.ultraviolet.common.universe.GalaxyParameters;
import net.xavil.ultraviolet.common.universe.ScalarField;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode;
import net.xavil.ultraviolet.common.universe.system.StellarProperties;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.matrices.Vec3;

public class BaseGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public static final Logger LOGGER = LoggerFactory.getLogger(Mod.MOD_ID + "/GalaxyGen");

	// the maximum amount of stars this layer is allowed to generate
	public static final int MAXIMUM_STARS_PER_SECTOR = 1024;
	// the maximum amount of times a star will be rerolled in a different location
	// before giving up
	public static final int MAXIMUM_STAR_PLACEMENT_ATTEMPTS = 16;
	// the amount of samples used to determine the average sector density
	public static final int DENSITY_SAMPLE_COUNT = 32;

	public BaseGalaxyGenerationLayer(Galaxy parentGalaxy, GalaxyParameters densityFields) {
		super(parentGalaxy);
	}

	// @formatter:off
	// TODO: this should be an initial mass function
	private static final double[] STAR_CLASS_PERCENTAGES = { 0.7654, 0.121, 0.076, 0.03, 0.006, 0.0013, 0.000003      };
	private static final double[] STAR_CLASS_MASSES      = {   0.08,  0.45,   0.8, 1.04,   1.4,   2.1,        16, 100 };
	// @formatter:on

	private static final Interval[] LEVEL_MASS_RANGES = {
			new Interval(0.1, 0.5),
			new Interval(0.5, 1),
			new Interval(1, 2),
			new Interval(2, 4),
			new Interval(4, 6),
			new Interval(6, 8),
			new Interval(8, 10),
			new Interval(10, 100),
	};

	private static final LinearSpline STAR_MASS_SPLINE = new LinearSpline();
	private static final LinearSpline STAR_PERCENTAGE_SPLINE = new LinearSpline();

	private static final Interval[] LEVEL_MASS_T_INTERVALS;
	private static final Interval[] LEVEL_COVERAGE_INTERVALS;

	static {
		final var coverageIntervals = new Vector<Interval>();
		double cumulativePercentage = 0.0;
		for (var level = 0; level <= GalaxySector.ROOT_LEVEL; ++level) {
			// the probability that a randomly selected sector has a level of `level`. Can
			// also be thought of as the ratio between the amount of sectors that have a
			// level `level` and the total amount of sectors contained within a root sector.
			final var percentage = GalaxySector.sectorsPerRootSector(level)
					/ (double) GalaxySector.SUBSECTORS_PER_ROOT_SECTOR;
			final var interval = new Interval(cumulativePercentage, cumulativePercentage + percentage);
			cumulativePercentage += percentage;
			coverageIntervals.push(interval);
		}
		LEVEL_COVERAGE_INTERVALS = coverageIntervals.toArray(Interval.class);

		double massTMax = 0;
		for (int level = 0; level <= GalaxySector.ROOT_LEVEL; ++level) {
			massTMax += Math.pow(3.0, GalaxySector.ROOT_LEVEL - level);
		}
		final var massTIntervals = new Vector<Interval>();
		cumulativePercentage = 0.0;
		for (var level = 0; level <= GalaxySector.ROOT_LEVEL; ++level) {
			// the probability that a randomly selected sector has a level of `level`. Can
			// also be thought of as the ratio between the amount of sectors that have a
			// level `level` and the total amount of sectors contained within a root sector.
			final var percentage = Math.pow(3.0, GalaxySector.ROOT_LEVEL - level) / massTMax;
			final var interval = new Interval(cumulativePercentage, cumulativePercentage + percentage);
			cumulativePercentage += percentage;
			massTIntervals.push(interval);
		}
		LEVEL_MASS_T_INTERVALS = massTIntervals.toArray(Interval.class);

		cumulativePercentage = 0.0;
		for (var i = 0; i < 7; ++i) {
			STAR_MASS_SPLINE.addControlPoint(cumulativePercentage, STAR_CLASS_MASSES[i]);
			cumulativePercentage += STAR_CLASS_PERCENTAGES[i];
		}
		STAR_MASS_SPLINE.addControlPoint(1, STAR_CLASS_MASSES[7]);

		cumulativePercentage = 0.0;
		for (var i = 0; i < 6; ++i) {
			STAR_PERCENTAGE_SPLINE.addControlPoint(cumulativePercentage, STAR_CLASS_PERCENTAGES[i]);
			cumulativePercentage += STAR_CLASS_PERCENTAGES[i];
		}
		STAR_PERCENTAGE_SPLINE.addControlPoint(cumulativePercentage, STAR_CLASS_PERCENTAGES[6]);

		LOGGER.debug("star mass interval weights: {}", Vector.fromElements(LEVEL_MASS_T_INTERVALS));
		LOGGER.debug("star mass spline: {}", STAR_MASS_SPLINE);
	}

	static double levelCoverage(int level) {
		return LEVEL_COVERAGE_INTERVALS[level].size();
	}

	static Interval getMassRangeForLevel(int level) {
		Assert.isTrue(level >= 0 && level < 8);
		double min = 0, max = 0;
		if (level == 0) {
			min = 0.1;
			max = 0.7;
		} else if (level == 1) {
			min = 0.7;
			max = 1;
		} else if (level == 2) {
			min = 1;
			max = 1.4;
		} else if (level == 3) {
			min = 1.4;
			max = 2.1;
		} else if (level == 4) {
			min = 2.1;
			max = 3.5;
		} else if (level == 5) {
			min = 3.5;
			max = 8;
		} else if (level == 6) {
			min = 8;
			max = 16;
		} else if (level == 7) {
			min = 16;
			max = 100;
		}

		// if (level == 0) {
		// min = 0.1;
		// max = 2.0;
		// } else if (level == 1) {
		// min = 2.0;
		// max = 4.0;
		// } else if (level == 2) {
		// min = 4.0;
		// max = 6.0;
		// } else if (level == 3) {
		// min = 6.0;
		// max = 8.0;
		// } else if (level == 4) {
		// min = 8.0;
		// max = 10.0;
		// } else if (level == 5) {
		// min = 10.0;
		// max = 20.0;
		// } else if (level == 6) {
		// min = 20.0;
		// max = 30.0;
		// } else if (level == 7) {
		// min = 30.0;
		// max = 120.0;
		// }
		return new Interval(min, max).mul(Units.Yg_PER_Msol);

		// final var i = LEVEL_COVERAGE_INTERVALS[level];
		// min = STAR_MASS_SPLINE.sample(i.min);
		// max = STAR_MASS_SPLINE.sample(i.max);
	}

	public static double generateStarMassForLevelShittyVersion(SplittableRng rng, int level) {
		final var interval = getMassRangeForLevel(level);
		// double mass = rng.uniformDouble("star_mass", LEVEL_MASS_RANGES[level]);
		double mass = rng.uniformDouble("star_mass", interval);
		double h = Mth.inverseLerp(mass, 0.08, 300);
		h *= rng.weightedDouble("mass_variation", 1.5, 0.95, 1.2);
		mass = Mth.lerp(h, 0.08, 300);
		mass = Math.max(mass, 0.1);
		return Units.Yg_PER_Msol * mass;
	}

	public static double generateStarMassForLevel(SplittableRng rng, int level) {
		final var i = LEVEL_COVERAGE_INTERVALS[level];
		final var massT = rng.uniformDouble("star_mass", i.min, i.max);
		double mass = STAR_MASS_SPLINE.sample(massT);
		double h = Mth.inverseLerp(mass, 0.08, 300);
		h *= rng.weightedDouble("mass_variation", 1.5, 0.95, 1.2);
		mass = Mth.lerp(h, 0.08, 300);
		mass = Math.max(mass, 0.1);
		return Units.Yg_PER_Msol * mass;
	}

	public static double generateStarMass(Rng rng) {
		return Units.Yg_PER_Msol * STAR_MASS_SPLINE.sample(rng.uniformDouble());
	}

	private static class GenerationInfo {
		public final Context ctx;

		public final GalaxyRegionWeights.Field maskField;
		public final ScalarField stellarDensity;
		public final double averageSectorDensity;
		public final int starAttemptCount;
		public final Imf imf;
		public final Interval massRange;

		private static int subdivisionsPerLevel(int level) {
			return switch (level) {
				case 5 -> 1;
				case 6 -> 2;
				case 7 -> 3;
				default -> 0;
			};
		}

		public GenerationInfo(Context ctx, SplittableRng rng) {
			this.ctx = ctx;

			this.imf = Imf.SALTPETER;
			this.massRange = getMassRangeForLevel(ctx.level);

			final var galaxyParams = ctx.galaxy.parameters;

			final var subdiv = subdivisionsPerLevel(ctx.level);
			this.maskField = InterpolatedMaskField.create(galaxyParams.masks, ctx.volumeMin, ctx.volumeMax, subdiv);

			final var tmpPos = new Vec3.Mutable();
			final var tmpMasks = new GalaxyRegionWeights();
			// this.stellarDensity = InterpolatedField.create((x, y, z) -> {
			// Vec3.set(tmpPos, x, y, z);
			// galaxyParams.masks.evaluate(tmpPos, tmpMasks);
			// return GalaxyRegionWeights.dot(tmpMasks, galaxyParams.stellarDensityWeights);
			// }, ctx.volumeMin, ctx.volumeMax, subdiv);
			this.stellarDensity = (x, y, z) -> {
				Vec3.set(tmpPos, x, y, z);
				galaxyParams.masks.evaluate(tmpPos, tmpMasks);
				return GalaxyRegionWeights.dot(tmpMasks, galaxyParams.stellarDensityWeights);
			};

			// since we're using trilinear interpolation, there's likely an analytic
			// solution to the average density of the sector. but i forgor all of my high
			// school calculus and dont know how to find it.
			final var sampleRng = rng.rng("density_sample");
			double sectorDensitySum = 0.0;
			for (var i = 0; i < DENSITY_SAMPLE_COUNT; ++i) {
				final var lenX = ctx.volumeMax.x - ctx.volumeMin.x;
				final var lenY = ctx.volumeMax.y - ctx.volumeMin.y;
				final var lenZ = ctx.volumeMax.z - ctx.volumeMin.z;

				// final double lx = ctx.volumeMin.x - lenX, hx = ctx.volumeMax.x + lenX;
				// final double ly = ctx.volumeMin.y - lenY, hy = ctx.volumeMax.y + lenY;
				// final double lz = ctx.volumeMin.z - lenZ, hz = ctx.volumeMax.z + lenZ;
				final double lx = ctx.volumeMin.x, hx = ctx.volumeMax.x;
				final double ly = ctx.volumeMin.y, hy = ctx.volumeMax.y;
				final double lz = ctx.volumeMin.z, hz = ctx.volumeMax.z;

				final double tx = sampleRng.uniformDouble(lx, hx),
						ty = sampleRng.uniformDouble(ly, hy),
						tz = sampleRng.uniformDouble(lz, hz);

				sectorDensitySum += stellarDensity.sample(tx, ty, tz);
			}

			// this.averageSectorDensity = Math.max(0, sectorDensitySum /
			// DENSITY_SAMPLE_COUNT);
			// final var sectorSideLengths = ctx.volumeMax.sub(ctx.volumeMin);
			// final var sectorVolume = sectorSideLengths.x * sectorSideLengths.y *
			// sectorSideLengths.z;

			// the amount of stars expected to be contained within the volume of this
			// sector, including all its subsectors.
			// double starsPerSector = sectorVolume * this.averageSectorDensity;
			// starsPerSector *= LEVEL_COVERAGE_INTERVALS[ctx.level].size();
			// starsPerSector *= LEVEL_MASS_T_INTERVALS[ctx.level].size();
			// starsPerSector /= 8;

			// int starAttemptCount = Mth.floor(starsPerSector);

			this.averageSectorDensity = Math.max(0, sectorDensitySum / DENSITY_SAMPLE_COUNT);
			final var sectorSideLengths = ctx.volumeMax.sub(ctx.volumeMin).mul(Units.pc_PER_Tm);
			final var sectorVolume = sectorSideLengths.x * sectorSideLengths.y * sectorSideLengths.z;
			final var sectorMass = this.averageSectorDensity * sectorVolume;

			final var starCount = this.imf.totalNumberOfStars(sectorMass, this.massRange.mul(Units.Msol_PER_Yg));
			this.starAttemptCount = Math.min(2048, Mth.floor(starCount));
			// this.starAttemptCount = starAttemptCount;
		}
	}

	public static abstract class Imf {
		public static final ImfPowerLaw SALTPETER = new ImfPowerLaw(-2.35);

		public abstract double totalNumberOfStars(double totalMassInRange, Interval massRange);

		public abstract void sampleMass(Interval massRange, Rng rng, double[] samplesOut);
	}

	public static final class ImfPowerLaw extends Imf {

		final double alpha;

		public ImfPowerLaw(double alpha) {
			this.alpha = alpha;
		}

		@Override
		public double totalNumberOfStars(double totalMassInRange, Interval massRange) {
			// totalMassInRange *= Units.Msol_PER_Yg;
			// massRange = massRange.mul(Units.Msol_PER_Yg);
			final var r1 = Math.pow(massRange.max, alpha + 1) - Math.pow(massRange.min, alpha + 1);
			final var r2 = Math.pow(massRange.max, alpha + 2) - Math.pow(massRange.min, alpha + 2);
			return totalMassInRange * (r1 / r2) * ((alpha + 2) / (alpha + 1));
		}

		@Override
		public void sampleMass(Interval massRange, Rng rng, double[] samplesOut) {
			massRange = massRange.mul(Units.Msol_PER_Yg);
			for (int i = 0; i < samplesOut.length; ++i) {
				final var alpha1 = this.alpha + 1;
				final var factor = Math.pow(massRange.max / massRange.min, alpha1) - 1.0;
				samplesOut[i] = massRange.min * Math.pow(1.0 + (factor * rng.uniformDouble()), 1.0 / alpha1);
				samplesOut[i] *= Units.Yg_PER_Msol;
				// final var alpha1 = this.alpha + 1;
				// final var factor = Math.pow(massRange.max / massRange.min, alpha1);
				// samplesOut[i] = massRange.min * Math.pow(rng.uniformDouble(1, factor), 1.0 /
				// alpha1);
			}
		}
	}

	@Override
	public void generateInto(Context ctx, GalaxySector.PackedElements elements) {
		final var rng = new SplittableRng(ctx.galaxy.info.seed);
		rng.advanceWith(ctx.pos.hash());

		final var info = new GenerationInfo(ctx, rng);

		// TODO: find total sector mass and keep track of it as we generate star
		// systems.

		// generate all the star masses for this sector
		final var starMasses = new double[info.starAttemptCount];
		info.imf.sampleMass(info.massRange, rng.rng("mass"), starMasses);

		// final var starProps = new StellarCelestialNode.Properties();
		final var starProps = new StellarProperties();
		final var elem = new GalaxySector.ElementHolder();
		elem.generationLayer = this.layerId;

		final int startIndex = elements.size();
		elements.reserve(info.starAttemptCount);

		final var masks = new GalaxyRegionWeights();
		final var galaxyParams = ctx.galaxy.parameters;
		final var densityWeights = galaxyParams.stellarDensityWeights;

		int offset = 0;
		for (int i = 0; i < info.starAttemptCount && offset < 2048; ++i) {
			rng.advance();
			elem.systemSeed = rng.uniformLong("system_seed");
			elem.massYg = starMasses[i];

			// I think this retry behavior is warranted. We do a coarse esimate of the
			// average density of the sector, and then multiply that with the sector volume
			// to get the approximate amount of stars we expect to see in the sector. Say we
			// had a density field where half of the field had a density of 0, and the rest
			// had a density of 1. In that case, we'd expect the average density to be 0.5,
			// and try to place `0.5 * volume` stars. This already accounts for the
			// extinction from the part of the density field that is 0, so applying it again
			// by not retrying when we fail causes this loop to emit a smaller amount of
			// stars than the expected count.
			//
			// I still do wonder if this is correct for more complicated cases than a simple
			// "on or off" density field, as it generally causes placement attempts to
			// "migrate" towards areas of higher densities.
			for (var j = 0; j < MAXIMUM_STAR_PLACEMENT_ATTEMPTS; ++j) {
				rng.advance();

				elem.systemPosTm.x = rng.uniformDouble("x", ctx.volumeMin.x, ctx.volumeMax.x);
				elem.systemPosTm.y = rng.uniformDouble("y", ctx.volumeMin.y, ctx.volumeMax.y);
				elem.systemPosTm.z = rng.uniformDouble("z", ctx.volumeMin.z, ctx.volumeMax.z);

				info.maskField.evaluate(elem.systemPosTm, masks);
				masks.core *= densityWeights.core;
				masks.arms *= densityWeights.arms;
				masks.disc *= densityWeights.disc;
				masks.halo *= densityWeights.halo;

				final var density = info.averageSectorDensity * rng.uniformDouble("density");
				if (density < masks.totalWeight())
					break;
			}

			final var sfh = galaxyParams.pickSfh(masks, rng.uniformDouble("age_weight"));
			final var age = galaxyParams.galaxyAge - sfh.pick(rng.uniformDouble("age"));
			elem.systemAgeMyr = age;

			// FIXME: hardcoded metallicity
			final var metallicity = getSystemMetallicity(elem.systemSeed);
			starProps.load(rng, elem.massYg, elem.systemAgeMyr, metallicity);
			elem.massYg = starProps.massYg;
			elem.luminosityLsol = starProps.luminosityLsol;
			elem.temperatureK = starProps.temperatureK;

			elements.store(elem, startIndex + offset);
			offset += 1;
		}

		elements.markWritten(offset);

		LOGGER.trace("average stellar density: {}", info.averageSectorDensity);
		LOGGER.trace("star placement attempt count: {}", info.starAttemptCount);
		LOGGER.trace("successful star placements: {}", offset);
	}

	public static double getSystemMetallicity(long systemSeed) {
		final var rng = new SplittableRng(systemSeed);
		return rng.uniformDouble("metallicity", Galaxy.METALLICITY_RANGE);
	}

	@Override
	public StarSystem generateFullSystem(GalaxySector sector, GalaxySectorId id, GalaxySector.ElementHolder elem) {
		final var rng = new SplittableRng(elem.systemSeed);

		final var metallicity = getSystemMetallicity(elem.systemSeed);
		final var rootNode = StellarCelestialNode.fromInitialParameters(
				elem.systemSeed, elem.massYg, elem.systemAgeMyr, metallicity);

		final var node = rootNode.generateSystem(rng.uniformLong("seed"), this.parentGalaxy, sector, id, elem);
		final var name = NameTemplate.SECTOR_NAME.generate(rng.rng("name"));

		return new StarSystem(name, this.parentGalaxy, elem, node, metallicity);
	}

}
