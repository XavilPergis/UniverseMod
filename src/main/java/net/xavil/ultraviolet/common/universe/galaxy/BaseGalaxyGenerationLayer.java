package net.xavil.ultraviolet.common.universe.galaxy;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Constants;
import net.xavil.hawklib.ProbabilityDistribution;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.WeightedList;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.collections.iterator.Iterator;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.client.screen.layer.AxisMapping;
import net.xavil.ultraviolet.common.NameTemplate;
import net.xavil.ultraviolet.common.universe.GalaxyParameters;
import net.xavil.ultraviolet.common.universe.ScalarField;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode;
import net.xavil.ultraviolet.common.universe.system.StellarProperties;

public class BaseGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public static final Logger LOGGER = LoggerFactory.getLogger(Mod.MOD_ID + "/GalaxyGen");

	// the maximum amount of stars this layer is allowed to generate
	public static final int MAXIMUM_STARS_PER_SECTOR = 1024;
	// the maximum amount of times a star will be rerolled in a different location
	// before giving up
	public static final int MAXIMUM_STAR_PLACEMENT_ATTEMPTS = 16;
	// the amount of samples used to determine the average sector density
	public static final int DENSITY_SAMPLE_COUNT = 32;

	private final LuminosityFunctionTable luminosityTableCore;
	private final LuminosityFunctionTable luminosityTableArms;
	private final LuminosityFunctionTable luminosityTableDisc;
	private final LuminosityFunctionTable luminosityTableHalo;
	private final double[] levelWeights;

	public BaseGalaxyGenerationLayer(Galaxy parentGalaxy, GalaxyParameters galaxyParams) {
		super(parentGalaxy);

		final var imf = ProbabilityDistribution.interpolate(mass -> {
			return Math.pow(mass, -2.35);
		}, LuminosityFunctionTable.MASS_INTERVAL.domain, 4096);

		this.luminosityTableCore = new LuminosityFunctionTable(imf, galaxyParams.coreSfh);
		this.luminosityTableArms = new LuminosityFunctionTable(imf, galaxyParams.armsSfh);
		this.luminosityTableDisc = new LuminosityFunctionTable(imf, galaxyParams.discSfh);
		this.luminosityTableHalo = new LuminosityFunctionTable(imf, galaxyParams.haloSfh);

		// level weights for selecting how many stars should be placed in each level.
		this.levelWeights = new double[GalaxySector.LEVEL_COUNT];
		for (int i = 0; i < GalaxySector.LEVEL_COUNT; ++i) {
			final var w0 = this.luminosityTableCore.levelWeights[i];
			final var w1 = this.luminosityTableArms.levelWeights[i];
			final var w2 = this.luminosityTableDisc.levelWeights[i];
			final var w3 = this.luminosityTableHalo.levelWeights[i];
			this.levelWeights[i] = (w0 + w1 + w2 + w3) / 4;
		}
	}

	private static final class BasicSystemInfo {
		public double mass, age, metallicity, luminosity;

		private static final Interval MASS_VARIANCE = Interval.ONE.expand(0.05);
		private static final Interval AGE_VARIANCE = Interval.ONE.expand(0.08);
		private static final Interval METALLICITY_VARIANCE = Interval.ONE.expand(0.02);
		private static final Interval LUMINOSITY_VARIANCE = Interval.ONE.expand(0.1);

		public void randomize(SplittableRng rng) {
			this.mass *= rng.uniformDouble("mass", MASS_VARIANCE);
			this.age *= rng.uniformDouble("age", AGE_VARIANCE);
			this.metallicity *= rng.uniformDouble("metallicity", METALLICITY_VARIANCE);
			this.luminosity *= rng.uniformDouble("luminosity", LUMINOSITY_VARIANCE);
		}
	}

	private static double maxLuminosityAtDistance(double distance) {
		final var magLimit = 6;
		distance *= Units.pc_PER_Tm;
		return Constants.ZERO_POINT_LUMINSOITY_W / Units.W_PER_Lsol *
				Math.pow(distance, 2) / Math.pow(10, magLimit / 2.5 + 2);
	}

	// private static final double[] LEVEL_LUMINOSITY_POINTS = { 0.75, 5, 40, 110,
	// 400, 5000, 80000, 500000 };
	private static final double[] LEVEL_LUMINOSITY_POINTS = {
			maxLuminosityAtDistance(GalaxySector.sizeForLevel(0)),
			maxLuminosityAtDistance(GalaxySector.sizeForLevel(1)),
			maxLuminosityAtDistance(GalaxySector.sizeForLevel(2)),
			maxLuminosityAtDistance(GalaxySector.sizeForLevel(3)),
			maxLuminosityAtDistance(GalaxySector.sizeForLevel(4)),
			maxLuminosityAtDistance(GalaxySector.sizeForLevel(5)),
			maxLuminosityAtDistance(GalaxySector.sizeForLevel(6)),
			maxLuminosityAtDistance(GalaxySector.sizeForLevel(7)),
	};

	public static final class DebugStopwatch {
		private Instant timerStart, timerEnd;

		public void start() {
			this.timerStart = Instant.now();
		}

		public void end() {
			this.timerEnd = Instant.now();
		}

		public Duration elapsedTime() {
			if (this.timerStart == null || this.timerEnd == null || this.timerStart.isAfter(this.timerEnd))
				return Duration.ZERO;
			return Duration.between(this.timerStart, this.timerEnd);
		}

		public double elapsedTimeSeconds() {
			final var dur = elapsedTime();
			return dur.getSeconds() + (dur.getNano() / 1e9);
		}
	}

	private static final class LuminosityFunctionTable {
		private static final AxisMapping MASS_INTERVAL = new AxisMapping.Log(Math.E,
				Units.Yg_PER_Msol * 0.1, Units.Yg_PER_Msol * 100);
		private static final AxisMapping AGE_INTERVAL = new AxisMapping.Linear(0, 13000);
		private static final AxisMapping METALLICITY_INTERVAL = new AxisMapping.Linear(Galaxy.METALLICITY_RANGE);

		public LuminosityFunctionTable(
				ProbabilityDistribution imf, ProbabilityDistribution sfh) {

			final DebugStopwatch buildTableTimer = new DebugStopwatch();

			final var massInputs = new double[512];
			final var ageInputs = new double[512];
			final var metallicityInputs = new double[8];

			for (int i = 0; i < massInputs.length; ++i)
				massInputs[i] = MASS_INTERVAL.unmap(i / (massInputs.length - 1d));
			for (int i = 0; i < ageInputs.length; ++i)
				ageInputs[i] = AGE_INTERVAL.unmap(i / (ageInputs.length - 1d));
			for (int i = 0; i < metallicityInputs.length; ++i)
				metallicityInputs[i] = METALLICITY_INTERVAL.unmap(i / (metallicityInputs.length - 1d));

			final var levelEntries = new Vector<>(
					Iterator.generate(i -> new WeightedList.Builder<BasicSystemInfo>(), GalaxySector.LEVEL_COUNT));

			double totalProb = 0;
			final var levelTotalProbs = new double[GalaxySector.LEVEL_COUNT];

			buildTableTimer.start();
			final var starProps = new StellarProperties();
			for (int iMass = 0; iMass < massInputs.length; ++iMass) {
				for (int iAge = 0; iAge < ageInputs.length; ++iAge) {
					for (int iMetallicity = 0; iMetallicity < metallicityInputs.length; ++iMetallicity) {
						final var info = new BasicSystemInfo();
						info.age = ageInputs[iAge];
						info.mass = massInputs[iMass];
						info.metallicity = metallicityInputs[iMetallicity];

						starProps.load(info.mass, info.age, info.metallicity);
						info.luminosity = starProps.luminosityLsol;

						// falling through the loop and finding nothing means that the luminosity was
						// higher than any level endpoint, so we just want to put it in the topmost
						// level.
						int level = levelEntries.size() - 1;
						for (int i = 0; i < GalaxySector.LEVEL_COUNT; ++i) {
							final var hi = LEVEL_LUMINOSITY_POINTS[i];
							if (starProps.luminosityLsol < hi) {
								level = i;
								break;
							}
						}

						final var probability = imf.evaluate(info.mass) * sfh.evaluate(info.age);
						levelEntries.get(level).push(probability, info);

						totalProb += probability;
						levelTotalProbs[level] += probability;
					}
				}
			}

			this.levelWeights = new double[GalaxySector.LEVEL_COUNT];
			for (int i = 0; i < GalaxySector.LEVEL_COUNT; ++i) {
				// this.levelWeights[i] = levelTotalProbs[i] / totalProb;
				this.levelWeights[i] = 1.0 / GalaxySector.LEVEL_COUNT;
			}

			buildTableTimer.end();
			Mod.LOGGER.info("Built luminosity function table in {} seconds", buildTableTimer.elapsedTimeSeconds());

			this.levelEntries = levelEntries.iter().map(builder -> builder.build()).collectTo(Vector::new);

			for (int i = 0; i < GalaxySector.LEVEL_COUNT; ++i) {
				final var list = this.levelEntries.get(i);
				Mod.LOGGER.info("Level {} has {} entries and weight of {}", i, list.size(), this.levelWeights[i]);
			}
		}

		private final ImmutableList<WeightedList<BasicSystemInfo>> levelEntries;
		private final double[] levelWeights;

		public boolean pick(BasicSystemInfo out, SplittableRng rng, int level) {
			final var entry = this.levelEntries.get(level).pick(rng.uniformDouble("entry"));
			if (entry == null)
				return false;
			out.age = entry.age;
			out.mass = entry.mass;
			out.metallicity = entry.metallicity;
			out.luminosity = entry.luminosity;
			return true;
		}
	}

	private LuminosityFunctionTable pickTable(GalaxyRegionWeights weights, double t) {
		double momentum = t * weights.totalWeight();

		if (weights.core > momentum)
			return this.luminosityTableCore;
		momentum -= weights.core;
		if (weights.arms > momentum)
			return this.luminosityTableArms;
		momentum -= weights.arms;
		if (weights.disc > momentum)
			return this.luminosityTableDisc;
		momentum -= weights.disc;
		if (weights.halo > momentum)
			return this.luminosityTableHalo;
		momentum -= weights.halo;

		// shouldnt get here, but im not sure what happens when t is exactly 1, so i
		// return something just in case~
		return this.luminosityTableDisc;
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

	private static class GenerationInfo {
		public final Context ctx;

		public final GalaxyRegionWeights.Field maskField;
		public final ScalarField stellarDensity;
		public final double averageSectorDensity;
		public final int starAttemptCount;

		private static int subdivisionsPerLevel(int level) {
			return switch (level) {
				case 5 -> 2;
				case 6 -> 2;
				case 7 -> 3;
				default -> 1;
			};
		}

		public GenerationInfo(BaseGalaxyGenerationLayer gen, Context ctx, SplittableRng rng) {
			this.ctx = ctx;

			final var galaxyParams = ctx.galaxy.parameters;

			final var subdiv = subdivisionsPerLevel(ctx.level);
			this.maskField = InterpolatedMaskField.create(galaxyParams.masks, ctx.volumeMin, ctx.volumeMax, subdiv);

			final var tmpPos = new Vec3.Mutable();
			final var tmpMasks = new GalaxyRegionWeights();
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
				final double lx = ctx.volumeMin.x, hx = ctx.volumeMax.x;
				final double ly = ctx.volumeMin.y, hy = ctx.volumeMax.y;
				final double lz = ctx.volumeMin.z, hz = ctx.volumeMax.z;

				final double tx = sampleRng.uniformDouble(lx, hx),
						ty = sampleRng.uniformDouble(ly, hy),
						tz = sampleRng.uniformDouble(lz, hz);

				sectorDensitySum += stellarDensity.sample(tx, ty, tz);
			}

			this.averageSectorDensity = Math.max(0, sectorDensitySum / DENSITY_SAMPLE_COUNT);
			final var sectorSideLengths = ctx.volumeMax.sub(ctx.volumeMin);
			final var sectorVolume = sectorSideLengths.x * sectorSideLengths.y * sectorSideLengths.z;

			// the amount of stars expected to be contained within the volume of this
			// sector, including all its subsectors.
			double starsPerSector = sectorVolume * this.averageSectorDensity;
			// starsPerSector *= LEVEL_COVERAGE_INTERVALS[ctx.level].size();
			starsPerSector *= GalaxySector.sectorsPerRootSector(ctx.level)
					/ (double) GalaxySector.SUBSECTORS_PER_ROOT_SECTOR;
			starsPerSector /= 1e10;
			starsPerSector *= gen.levelWeights[ctx.level];

			this.starAttemptCount = Mth.clamp(Mth.floor(starsPerSector), 0, 2048);

			// this.averageSectorDensity = Math.max(0, sectorDensitySum /
			// DENSITY_SAMPLE_COUNT);
			// final var sectorSideLengths =
			// ctx.volumeMax.sub(ctx.volumeMin).mul(Units.pc_PER_Tm);
			// final var sectorVolume = sectorSideLengths.x * sectorSideLengths.y *
			// sectorSideLengths.z;
			// final var sectorMass = this.averageSectorDensity * sectorVolume;

			// this.starAttemptCount = Math.min(2048, Mth.floor(10000 * sectorMass *
			// Units.Msol_PER_Yg));
			// this.starAttemptCount = 1000;

			// final var starCount = this.imf.totalNumberOfStars(sectorMass,
			// this.massRange.mul(Units.Msol_PER_Yg));
			// this.starAttemptCount = Math.min(2048, Mth.floor(starCount));
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

		final var info = new GenerationInfo(this, ctx, rng);

		// TODO: find total sector mass and keep track of it as we generate star
		// systems.

		final var sysInfo = new BasicSystemInfo();

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

			final var table = pickTable(masks, rng.uniformDouble("pick_table"));
			rng.push("table");
			final var foundEntry = table.pick(sysInfo, rng, ctx.level);
			rng.pop();
			if (!foundEntry)
				continue;

			// sysInfo.mass *= rng.uniformDouble("mass_var", Interval.ONE.expand(0.05));
			// sysInfo.age *= rng.uniformDouble("age_var", Interval.ONE.expand(0.08));
			// sysInfo.metallicity *= rng.uniformDouble("metallicity_var",
			// Interval.ONE.expand(0.02));
			// sysInfo.luminosity *= rng.uniformDouble("luminosity_var",
			// Interval.ONE.expand(0.05));
			sysInfo.mass *= rng.uniformDouble("mass_var", Interval.ONE.expand(0.15));
			sysInfo.age *= rng.uniformDouble("age_var", Interval.ONE.expand(0.18));
			sysInfo.metallicity *= rng.uniformDouble("metallicity_var", Interval.ONE.expand(0.05));
			sysInfo.luminosity *= rng.uniformDouble("luminosity_var", Interval.ONE.expand(0.1));

			sysInfo.mass = Mth.clamp(sysInfo.mass, Units.Yg_PER_Msol * 0.1, Units.Yg_PER_Msol * 100);

			// TODO: better metallicity handling. parts of the galaxy are much more
			// metal-rich than others (eg., the spiral arms)
			elem.massYg = sysInfo.mass;
			elem.systemAgeMyr = sysInfo.age;
			elem.metallicity = sysInfo.metallicity;
			elem.luminosityLsol = sysInfo.luminosity;

			starProps.load(elem.massYg, elem.systemAgeMyr, elem.metallicity);
			elem.massYg = starProps.massYg;
			elem.luminosityLsol = starProps.luminosityLsol;
			elem.temperatureK = starProps.temperatureK;

			elem.massYg *= rng.uniformDouble("mass_var_2", Interval.ONE.expand(0.01));
			elem.systemAgeMyr *= rng.uniformDouble("age_var_2", Interval.ONE.expand(0.01));
			elem.metallicity *= rng.uniformDouble("metallicity_var_2", Interval.ONE.expand(0.01));
			elem.luminosityLsol *= rng.uniformDouble("luminosity_var_2", Interval.ONE.expand(0.01));

			elements.store(elem, startIndex + offset);
			offset += 1;
		}

		elements.markWritten(offset);

		LOGGER.trace("average stellar density: {}", info.averageSectorDensity);
		LOGGER.trace("star placement attempt count: {}", info.starAttemptCount);
		LOGGER.trace("successful star placements: {}", offset);
	}

	@Override
	public StarSystem generateFullSystem(GalaxySector sector, GalaxySectorId id, GalaxySector.ElementHolder elem) {
		final var rng = new SplittableRng(elem.systemSeed);

		final var rootNode = StellarCelestialNode.fromInitialParameters(
				elem.systemSeed, elem.massYg, elem.systemAgeMyr, elem.metallicity);

		rootNode.luminosityLsol = elem.luminosityLsol;
		rootNode.temperature = elem.temperatureK;
		rootNode.massYg = elem.massYg;

		final var node = rootNode.generateSystem(rng.uniformLong("seed"), this.parentGalaxy, sector, id, elem);
		final var name = NameTemplate.SECTOR_NAME.generate(rng.rng("name"));

		return new StarSystem(name, this.parentGalaxy, elem, node, elem.metallicity);
	}

}
