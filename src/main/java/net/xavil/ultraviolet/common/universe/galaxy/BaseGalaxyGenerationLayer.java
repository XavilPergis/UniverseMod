package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.NameTemplate;
import net.xavil.ultraviolet.common.universe.DensityFields;
import net.xavil.ultraviolet.common.universe.ScalarField;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.system.StarSystemGenerator;
import net.xavil.ultraviolet.common.universe.system.BasicStarSystemGenerator;
import net.xavil.universegen.LinearSpline;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public class BaseGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public static final Logger LOGGER = LoggerFactory.getLogger(Mod.MOD_ID + "/GalaxyGen");

	public static final int MAXIMUM_STARS_PER_SECTOR = 2000;
	public static final int MAXIMUM_STAR_PLACEMENT_ATTEMPTS = 16;
	public static final int DENSITY_SAMPLE_COUNT = 100;

	public final DensityFields densityFields;

	public BaseGalaxyGenerationLayer(Galaxy parentGalaxy, DensityFields densityFields) {
		super(parentGalaxy);
		this.densityFields = densityFields;
	}

	public static final int LEVEL_COUNT = GalaxySector.ROOT_LEVEL + 1;
	public static final int SECTOR_ELEMENT_LIMIT = 3000;

	// @formatter:off
	// TODO: this should be an initial mass function
	private static final double[] STAR_CLASS_PERCENTAGES = { 0.7654, 0.121, 0.076, 0.03, 0.006, 0.0013, 0.0003      };
	private static final double[] STAR_CLASS_MASSES      = {   0.08,  0.45,   0.8, 1.04,   1.4,   2.1,      16, 100 };
	// @formatter:on

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

	private static double sectorDensityFactor(int level) {
		return 1;
		// final var interval = LEVEL_MASS_T_INTERVALS[level];
		// return (interval.higher - interval.lower);
	}

	public static double generateStarMassForLevel(SplittableRng rng, int level) {
		final var i = LEVEL_COVERAGE_INTERVALS[level];
		final var massT = rng.uniformDouble("star_mass", i.lower, i.higher);
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

	private static class InterpolatedField implements ScalarField {
		public final Vec3 min, max;
		public final double nnn, nnp, npn, npp, pnn, pnp, ppn, ppp;
		public final double invLerpFactorX, invLerpFactorY, invLerpFactorZ;

		public InterpolatedField(ScalarField field, Vec3 min, Vec3 max) {
			this.min = min;
			this.max = max;
			this.nnn = field.sample(min.x, min.y, min.z);
			this.nnp = field.sample(min.x, min.y, max.z);
			this.npn = field.sample(min.x, max.y, min.z);
			this.npp = field.sample(min.x, max.y, max.z);
			this.pnn = field.sample(max.x, min.y, min.z);
			this.pnp = field.sample(max.x, min.y, max.z);
			this.ppn = field.sample(max.x, max.y, min.z);
			this.ppp = field.sample(max.x, max.y, max.z);
			this.invLerpFactorX = 1.0 / (this.max.x - this.min.x);
			this.invLerpFactorY = 1.0 / (this.max.y - this.min.y);
			this.invLerpFactorZ = 1.0 / (this.max.z - this.min.z);
		}

		public double sample(double tx, double ty, double tz) {
			// trilinear interpolation
			final var xnn = this.nnn + tx * (this.pnn - this.nnn);
			final var xpn = this.npn + tx * (this.ppn - this.npn);
			final var xnp = this.nnp + tx * (this.pnp - this.nnp);
			final var xpp = this.npp + tx * (this.ppp - this.npp);
			final var yn = xnn + ty * (xpn - xnn);
			final var yp = xnp + ty * (xpp - xnp);
			return yn + tz * (yp - yn);
		}

		@Override
		public double sample(Vec3Access pos) {
			final var tx = this.invLerpFactorX * (pos.x() - this.min.x);
			final var ty = this.invLerpFactorY * (pos.y() - this.min.y);
			final var tz = this.invLerpFactorZ * (pos.z() - this.min.z);
			return sample(tx, ty, tz);
		}

	}

	private static class GenerationInfo {
		public final Context ctx;

		public final ScalarField stellarDensity;
		public final ScalarField stellarAge;
		public final double averageSectorDensity;
		public final int starAttemptCount;

		public GenerationInfo(Context ctx, SplittableRng rng) {
			this.ctx = ctx;

			final var fields = ctx.galaxy.densityFields;
			this.stellarDensity = new InterpolatedField(fields.stellarDensity, ctx.volumeMin, ctx.volumeMax);
			this.stellarAge = new InterpolatedField(fields.minAgeFactor, ctx.volumeMin, ctx.volumeMax);
			// this.stellarDensity = fields.stellarDensity;
			// this.stellarAge = fields.minAgeFactor;

			// since we're using trilinear interpolation, there's likely an analytic
			// solution to the average density of the sector. but i forgor all of my high
			// school calculus and dont know how to find it.
			final var sampleRng = rng.rng("density_sample");
			double sectorDensitySum = 0.0;
			for (var i = 0; i < DENSITY_SAMPLE_COUNT; ++i) {
				final double tx = sampleRng.uniformDouble(),
						ty = sampleRng.uniformDouble(),
						tz = sampleRng.uniformDouble();
				sectorDensitySum += stellarDensity.sample(tx, ty, tz);
			}

			this.averageSectorDensity = Math.max(0, sectorDensitySum / DENSITY_SAMPLE_COUNT);
			final var sectorSideLengths = ctx.volumeMax.sub(ctx.volumeMin);
			final var sectorVolume = sectorSideLengths.x * sectorSideLengths.y * sectorSideLengths.z;

			// the amount of stars expected to be contained within the volume of this
			// sector, including all its subsectors.
			double starsPerSector = sectorVolume * this.averageSectorDensity;
			starsPerSector *= LEVEL_COVERAGE_INTERVALS[ctx.level].size();
			// starsPerSector *= LEVEL_MASS_T_INTERVALS[ctx.level].size();
			starsPerSector /= 8;

			int starAttemptCount = Mth.floor(starsPerSector);
			if (starAttemptCount > 2048) {
				starAttemptCount = 2048;
			}

			this.starAttemptCount = starAttemptCount;
		}
	}

	@Override
	public void generateInto(Context ctx, GalaxySector.PackedElements elements) {
		final var rng = new SplittableRng(ctx.galaxy.info.seed);
		rng.advanceWith(ctx.pos.hash());

		final var info = new GenerationInfo(ctx, rng);

		// TODO: find total sector mass and keep track of it as we generate star
		// systems.

		final var starProps = new StellarCelestialNode.Properties();
		final var elem = new GalaxySector.ElementHolder();
		elem.generationLayer = this.layerId;

		final int startIndex = elements.size();
		elements.reserve(info.starAttemptCount);

		int offset = 0;
		for (int i = 0; i < info.starAttemptCount; ++i) {
			rng.advance();
			elem.systemSeed = rng.uniformLong("system_seed");

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

				final var density = info.averageSectorDensity * rng.uniformDouble("density");
				final var sampledDesnity = info.stellarDensity.sample(elem.systemPosTm);
				if (density < sampledDesnity)
					break;
			}

			generateStarSystemInfo(elem, starProps, info, rng);

			elements.store(elem, startIndex + offset);
			offset += 1;
		}

		elements.markWritten(offset);

		LOGGER.trace("average stellar density: {}", info.averageSectorDensity);
		LOGGER.trace("star placement attempt count: {}", info.starAttemptCount);
		LOGGER.trace("successful star placements: {}", offset);
	}

	// Basic system info
	// the position to sample everything at is contained in the element holder
	private void generateStarSystemInfo(GalaxySector.ElementHolder elem, StellarCelestialNode.Properties props,
			GenerationInfo info, SplittableRng rng) {
		final var minSystemAgeFactor = Math.min(1, info.stellarAge.sample(elem.systemPosTm));
		final var systemAgeFactor = rng.weightedDouble("age", 0.8);
		final var systemAgeMyr = this.parentGalaxy.info.ageMya
				* Mth.lerp(systemAgeFactor, minSystemAgeFactor, 1);

		final var starMass = generateStarMassForLevel(rng, info.ctx.level);
		// final var starMass = generateStarMass(rng);

		props.load(rng, starMass, systemAgeMyr);
		elem.massYg = starMass;
		elem.systemAgeMyr = systemAgeMyr + rng.uniformDouble("age_offset");

		elem.luminosityLsol = props.luminosityLsol;
		elem.temperatureK = props.temperatureK;
	}

	@Override
	public StarSystem generateFullSystem(GalaxySector sector, GalaxySectorId id, GalaxySector.ElementHolder elem) {
		final var rng = new SplittableRng(elem.systemSeed);

		final var ctx = new StarSystemGenerator.Context(rng.uniformLong("seed"), this.parentGalaxy, sector, id, elem);
		// final var systemGenerator = new RealisticStarSystemGenerator();
		rng.push("star_properties");
		CelestialNode rootNode = StellarCelestialNode.fromMassAndAge(rng, elem.massYg, elem.systemAgeMyr);
		rng.pop();
		final var systemGenerator = new BasicStarSystemGenerator(rootNode);

		rootNode = systemGenerator.generate(ctx);
		rootNode.build();
		rootNode.assignSeeds(rng.uniformLong("root_node_seed"));

		final var name = NameTemplate.SECTOR_NAME.generate(rng.rng("name"));

		return new StarSystem(name, this.parentGalaxy, elem, rootNode);
	}

}
