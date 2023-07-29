package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.Random;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.NameTemplate;
import net.xavil.ultraviolet.common.universe.DensityFields;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.system.StarSystemGeneratorImpl;
import net.xavil.universegen.LinearSpline;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.matrices.Vec3;

public class BaseGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public static final int MAXIMUM_STARS_PER_SECTOR = 2000;
	public static final int MAXIMUM_STAR_PLACEMENT_ATTEMPTS = 16;
	public static final int DENSITY_SAMPLE_COUNT = 100;

	public final DensityFields densityFields;

	public BaseGalaxyGenerationLayer(Galaxy parentGalaxy, DensityFields densityFields) {
		super(parentGalaxy, 0);
		this.densityFields = densityFields;
	}

	private static long systemSeed(int volumeSeed, int id) {
		return FastHasher.create().appendInt(volumeSeed).appendInt(id).currentHashInt();
	}

	public static final int LEVEL_COUNT = GalaxySector.ROOT_LEVEL + 1;
	public static final int SECTOR_ELEMENT_LIMIT = 10000;

	// @formatter:off
	// TODO: this should be an initial mass function
	private static final double[] STAR_CLASS_PERCENTAGES = { 0.7645, 0.121, 0.076, 0.03, 0.006, 0.0013, 0.0000003      };
	private static final double[] STAR_CLASS_MASSES      = {   0.08,  0.45,   0.8, 1.04,   1.4,   2.1,         16, 100 };
	// @formatter:on

	private static final LinearSpline STAR_MASS_SPLINE = new LinearSpline();
	private static final double SECTORS_PER_ROOT_SECTOR;
	private static final Interval[] LEVEL_MASS_INTERVALS;

	static {
		// the total amount of sectors contained within the space a single root sector
		// occupies. This is used to calculate the probability that a randomly selected
		// sector will be of a certain level.
		double sectorsPerRootSector = 0.0;
		for (var level = 0; level <= GalaxySector.ROOT_LEVEL; ++level)
			sectorsPerRootSector += Math.pow(8.0, GalaxySector.ROOT_LEVEL - level);
		
		Mod.LOGGER.info("[galaxygen] sectorsPerRootSector: {}", sectorsPerRootSector);

		final var intervals = new Vector<Interval>();
		double cumulativePercentage = 0.0;
		for (var level = 0; level <= GalaxySector.ROOT_LEVEL; ++level) {
			// the probability that a randomly selected sector has a level of `level`. Can
			// also be thought of as the ratio between the amount of sectors that have a
			// level `level` and the total amount of sectors contained within a root sector.
			final var percentage = Math.pow(8.0, GalaxySector.ROOT_LEVEL - level) / sectorsPerRootSector;
			final var interval = new Interval(cumulativePercentage, cumulativePercentage + percentage);
			cumulativePercentage += percentage;
			intervals.push(interval);
		}
		LEVEL_MASS_INTERVALS = intervals.toArray(Interval.class);
		SECTORS_PER_ROOT_SECTOR = sectorsPerRootSector;

		cumulativePercentage = 0.0;
		for (var i = 0; i < 7; ++i) {
			STAR_MASS_SPLINE.addControlPoint(cumulativePercentage, STAR_CLASS_MASSES[i]);
			cumulativePercentage += STAR_CLASS_PERCENTAGES[i];
		}
		STAR_MASS_SPLINE.addControlPoint(1, STAR_CLASS_MASSES[7]);

		Mod.LOGGER.info("[galaxygen] star mass interval weights: {}", Vector.fromElements(LEVEL_MASS_INTERVALS));
		Mod.LOGGER.info("[galaxygen] star mass spline: {}", STAR_MASS_SPLINE);
	}

	static double levelCoverage(int level) {
		return Math.pow(8.0, GalaxySector.ROOT_LEVEL - level) / SECTORS_PER_ROOT_SECTOR;
	}

	@Override
	public void generateInto(Context ctx, Sink sink) {
		final var volumeSeed = ctx.random.nextInt();

		var sectorDensitySum = 0.0;
		for (var i = 0; i < DENSITY_SAMPLE_COUNT; ++i) {
			final var samplePos = Vec3.random(ctx.random, ctx.volumeMin, ctx.volumeMax);
			sectorDensitySum += this.densityFields.stellarDensity.sample(samplePos) * 0.05;
		}

		final var averageSectorDensity = Math.max(0, sectorDensitySum / DENSITY_SAMPLE_COUNT);
		final var sectorSideLengths = ctx.volumeMin.sub(ctx.volumeMax).abs();
		final var sectorVolume = sectorSideLengths.x * sectorSideLengths.y * sectorSideLengths.z;
		// final var starsPerSector = sectorVolume * averageSectorDensity *
		// ctx.starCountFactor;
		final var starFactor = levelCoverage(ctx.level);
		final var starsPerSector = sectorVolume * averageSectorDensity * starFactor;
		// final var starsPerSector = 500;

		int starAttemptCount = Mth.floor(starsPerSector);
		if (starAttemptCount > 3000) {
			Mod.LOGGER.warn("star attempt count of {} exceeded limit of {}", starAttemptCount,
					3000);
			starAttemptCount = 3000;
		}

		// TODO: find total sector mass and keep track of it as we generate star
		// systems.

		int successfulAttempts = 0;
		for (var i = 0; i < starAttemptCount; ++i) {
			final var infoSeed = ctx.random.nextLong();
			final var systemSeed = systemSeed(volumeSeed, i);

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
				final var systemPos = Vec3.random(ctx.random, ctx.volumeMin, ctx.volumeMax);
				final var density = this.densityFields.stellarDensity.sample(systemPos) * 0.05;

				if (density < ctx.random.nextDouble(0.0, averageSectorDensity))
					continue;

				final var initial = generateStarSystemInfo(systemPos, infoSeed, ctx.level);
				sink.accept(systemPos, initial, systemSeed);
				successfulAttempts += 1;
				break;
			}
		}

		Mod.LOGGER.trace("[galaxygen] average stellar density: {}", averageSectorDensity);
		Mod.LOGGER.trace("[galaxygen] star placement attempt count: {}", starAttemptCount);
		Mod.LOGGER.trace("[galaxygen] successful star placements: {}", successfulAttempts);

	}

	public static final double MINIMUM_STAR_MASS_YG = Units.Yg_PER_Msol * 0.02;
	public static final double MAXIMUM_STAR_MASS_YG = Units.Yg_PER_Msol * 30.0;

	// private double generateAvailableMass(Rng rng) {
	// // maybe some sort of unbounded distribution would work best here. like maybe
	// // find a way to use an IMF
	// var t = Math.pow(rng.uniformDouble(), 3);
	// var massYg = Mth.lerp(t, Units.Yg_PER_Msol * 0.1, Units.Yg_PER_Msol * 200.0);
	// return massYg;
	// }

	public static double generateStarMass(Rng rng, double availableMass, int level) {
		final var massT = rng.uniformDouble(LEVEL_MASS_INTERVALS[level]);
		return Units.Yg_PER_Msol * STAR_MASS_SPLINE.sample(massT);
	}

	public static double generateStarMass(Rng rng, double availableMass) {
		return Units.Yg_PER_Msol * STAR_MASS_SPLINE.sample(rng.uniformDouble());
	}

	// Basic system info
	private StarSystem.Info generateStarSystemInfo(Vec3 systemPos, long seed, int level) {
		final var rng = Rng.wrap(new Random(seed));

		final var minSystemAgeFactor = Math.min(1, this.densityFields.minAgeFactor.sample(systemPos));
		final var systemAgeFactor = Math.pow(rng.uniformDouble(), 2);
		final var systemAgeMyr = this.parentGalaxy.info.ageMya * Mth.lerp(systemAgeFactor, minSystemAgeFactor, 1);

		final var starMass = generateStarMass(rng, 0, level);
		// final var starMass = generateStarMass(rng, 0);

		final var primaryStar = StellarCelestialNode.fromMassAndAge(rng, starMass, systemAgeMyr);
		final var remainingMass = rng.uniformDouble(0.001, 0.05) * starMass;

		return new StarSystem.Info(systemAgeMyr, remainingMass, starMass - primaryStar.massYg, primaryStar);
	}

	@Override
	public StarSystem generateFullSystem(GalaxySector.InitialElement elem) {
		final var rng = Rng.wrap(new Random(elem.seed()));

		final var systemGenerator = new StarSystemGeneratorImpl(rng, this.parentGalaxy, elem.info());
		final var rootNode = systemGenerator.generate();
		rootNode.assignIds();

		final var name = NameTemplate.SECTOR_NAME.generate(rng);

		return new StarSystem(name, this.parentGalaxy, elem.pos(), rootNode);
	}

}
