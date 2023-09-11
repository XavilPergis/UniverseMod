package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.Random;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.NameTemplate;
import net.xavil.ultraviolet.common.universe.DensityFields;
import net.xavil.ultraviolet.common.universe.DoubleField3;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.system.StarSystemGenerator;
import net.xavil.ultraviolet.common.universe.system.BasicStarSystemGenerator;
import net.xavil.ultraviolet.common.universe.system.RealisticStarSystemGenerator;
import net.xavil.universegen.LinearSpline;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

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
	public static final int SECTOR_ELEMENT_LIMIT = 3000;

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

	private static class InterpolatedField implements DoubleField3 {
		public final Vec3 min, max;
		public final double nnn, nnp, npn, npp, pnn, pnp, ppn, ppp;

		public InterpolatedField(DoubleField3 field, Vec3 min, Vec3 max) {
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
		}

		public double sample(double tx, double ty, double tz) {
			// trilinear interpolation
			final var xnn = Mth.lerp(tx, this.nnn, this.pnn);
			final var xnp = Mth.lerp(tx, this.nnp, this.pnp);
			final var xpn = Mth.lerp(tx, this.npn, this.ppn);
			final var xpp = Mth.lerp(tx, this.npp, this.ppp);
			final var yn = Mth.lerp(ty, xnn, xpn);
			final var yp = Mth.lerp(ty, xnp, xpp);
			return Mth.lerp(tz, yn, yp);
		}

		@Override
		public double sample(Vec3Access pos) {
			final var tx = Mth.inverseLerp(pos.x(), this.min.x, this.max.x);
			final var ty = Mth.inverseLerp(pos.y(), this.min.y, this.max.y);
			final var tz = Mth.inverseLerp(pos.z(), this.min.z, this.max.z);
			return sample(tx, ty, tz);
		}

	}

	private static class GenerationInfo {
		public final Context ctx;

		public final DoubleField3 stellarDensity;
		public final DoubleField3 stellarAge;
		public final double averageSectorDensity;
		public final int starAttemptCount;

		public GenerationInfo(Context ctx) {
			this.ctx = ctx;

			final var fields = ctx.galaxy.densityFields;
			// this.stellarDensity = new InterpolatedField(fields.stellarDensity, ctx.volumeMin, ctx.volumeMax);
			// this.stellarAge = new InterpolatedField(fields.minAgeFactor, ctx.volumeMin, ctx.volumeMax);
			this.stellarDensity = fields.stellarDensity;
			this.stellarAge = fields.minAgeFactor;

			// since we're using trilinear interpolation, there's likely an analytic
			// solution to the average density of the sector. but i forgor all of my high
			// school calculus and dont know how to find it.
			double sectorDensitySum = 0.0;
			for (var i = 0; i < DENSITY_SAMPLE_COUNT; ++i) {
				final double tx = ctx.rng.uniformDouble(), ty = ctx.rng.uniformDouble(), tz = ctx.rng.uniformDouble();
				sectorDensitySum += stellarDensity.sample(tx, ty, tz);
			}

			this.averageSectorDensity = Math.max(0, sectorDensitySum / DENSITY_SAMPLE_COUNT);
			final var sectorSideLengths = ctx.volumeMax.sub(ctx.volumeMin);
			final var sectorVolume = sectorSideLengths.x * sectorSideLengths.y * sectorSideLengths.z;
			final var starsPerSector = sectorVolume * this.averageSectorDensity * levelCoverage(ctx.level);

			int starAttemptCount = Mth.floor(starsPerSector);
			if (starAttemptCount > 2000) {
				starAttemptCount = 2000;
			}

			this.starAttemptCount = starAttemptCount;
		}
	}

	@Override
	public void generateInto(Context ctx, GalaxySector.PackedSectorElements elements) {
		final var volumeSeed = ctx.rng.uniformInt();
		final var info = new GenerationInfo(ctx);

		// TODO: find total sector mass and keep track of it as we generate star
		// systems.

		final var starProps = new StellarCelestialNode.Properties();
		final var elem = new GalaxySector.SectorElementHolder();
		elem.generationLayer = this.layerId;

		final int startIndex = elements.size();
		elements.beginWriting(info.starAttemptCount);
		
		int offset = 0;
		for (var i = 0; i < info.starAttemptCount; ++i) {
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
				Vec3.loadRandom(elem.systemPosTm, ctx.rng, ctx.volumeMin, ctx.volumeMax);
				final var density = info.stellarDensity.sample(elem.systemPosTm);
				if (density >= ctx.rng.uniformDouble(0.0, info.averageSectorDensity)) {
					generateStarSystemInfo(elem, starProps, info);
					elem.systemSeed = systemSeed(volumeSeed, i);
					elements.store(elem, startIndex + offset);
					offset += 1;
					break;
				}
			}
		}

		elements.endWriting(offset);

		Mod.LOGGER.trace("[galaxygen] average stellar density: {}", info.averageSectorDensity);
		Mod.LOGGER.trace("[galaxygen] star placement attempt count: {}", info.starAttemptCount);
		Mod.LOGGER.trace("[galaxygen] successful star placements: {}", offset);
	}

	public static final double MINIMUM_STAR_MASS_YG = Units.Yg_PER_Msol * 0.02;
	public static final double MAXIMUM_STAR_MASS_YG = Units.Yg_PER_Msol * 30.0;

	public static double generateStarMassForLevel(Rng rng, int level) {
		final var massT = rng.uniformDouble(LEVEL_MASS_INTERVALS[level]);
		return Units.Yg_PER_Msol * STAR_MASS_SPLINE.sample(massT);
	}

	public static double generateStarMass(Rng rng) {
		return Units.Yg_PER_Msol * STAR_MASS_SPLINE.sample(rng.uniformDouble());
	}

	// Basic system info
	// the position to sample everything at is contained in the element holder
	private void generateStarSystemInfo(GalaxySector.SectorElementHolder elem, StellarCelestialNode.Properties props, GenerationInfo info) {
		final var rng = Rng.wrap(new Random(info.ctx.rng.uniformLong()));

		final var minSystemAgeFactor = Math.min(1, info.stellarAge.sample(elem.systemPosTm));
		final var systemAgeFactor = Math.pow(rng.uniformDouble(), 2);
		final var systemAgeMyr = this.parentGalaxy.info.ageMya
				* Mth.lerp(systemAgeFactor, minSystemAgeFactor, 1);

		final var starMass = generateStarMassForLevel(rng, info.ctx.level);
		
		props.load(starMass, systemAgeMyr);
		elem.massYg = starMass;
		elem.systemAgeMyr = systemAgeMyr;
		elem.luminosityLsol = props.luminosityLsol;
		elem.temperatureK = props.temperatureK;
	}

	@Override
	public StarSystem generateFullSystem(GalaxySector.SectorElementHolder elem) {
		final var rng = Rng.wrap(new Random(elem.systemSeed));

		final var ctx = new StarSystemGenerator.Context(rng, this.parentGalaxy, elem);
		final var systemGenerator = new RealisticStarSystemGenerator();
		// final var systemGenerator = new BasicStarSystemGenerator();
		final var rootNode = systemGenerator.generate(ctx);
		rootNode.assignIds();

		final var name = NameTemplate.SECTOR_NAME.generate(rng);

		return new StarSystem(name, this.parentGalaxy, elem.systemPosTm.xyz(), rootNode);
	}

}
