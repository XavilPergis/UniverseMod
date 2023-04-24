package net.xavil.universal.common.universe.galaxy;

import java.util.Random;

import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.common.NameTemplate;
import net.xavil.universal.common.universe.DensityFields;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystem.Info;
import net.xavil.universal.common.universe.system.StarSystemGeneratorImpl;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.FastHasher;
import net.xavil.util.Rng;
import net.xavil.util.Units;
import net.xavil.util.math.Vec3;

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
		final var starsPerSector = sectorVolume * averageSectorDensity * ctx.starCountFactor;

		int starAttemptCount = Mth.floor(starsPerSector);
		if (starAttemptCount > MAXIMUM_STARS_PER_SECTOR) {
			Mod.LOGGER.warn("star attempt count of {} exceeded limit of {}", starAttemptCount,
					MAXIMUM_STARS_PER_SECTOR);
			starAttemptCount = MAXIMUM_STARS_PER_SECTOR;
		}

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

				final var initial = generateStarSystemInfo(systemPos, infoSeed);
				sink.accept(systemPos, initial, systemSeed);
				successfulAttempts += 1;
				break;
			}
		}

		Mod.LOGGER.trace("[galaxygen] average stellar density: {}", averageSectorDensity);
		Mod.LOGGER.trace("[galaxygen] star placement attempt count: {}", starAttemptCount);
		Mod.LOGGER.trace("[galaxygen] successful star placements: {}", successfulAttempts);

	}

	public static final double MINIMUM_STAR_MASS_YG = Units.Yg_PER_Msol * 0.1;
	public static final double MAXIMUM_STAR_MASS_YG = Units.Yg_PER_Msol * 30.0;

	private double generateAvailableMass(Rng rng) {
		// maybe some sort of unbounded distribution would work best here. like maybe
		// find a way to use an IMF
		var t = Math.pow(rng.uniformDouble(), 8);
		var massYg = Mth.lerp(t, Units.Yg_PER_Msol * 0.1, Units.Yg_PER_Msol * 200.0);
		return massYg;
	}

	public static double generateStarMass(Rng rng, double availableMass) {
		availableMass = Math.min(MAXIMUM_STAR_MASS_YG, availableMass);
		var massFactor = Math.pow(rng.uniformDouble(), 8);
		var massYg = Mth.lerp(massFactor, MINIMUM_STAR_MASS_YG, availableMass);
		return massYg;
	}

	// Basic system info
	private StarSystem.Info generateStarSystemInfo(Vec3 systemPos, long seed) {
		var rng = Rng.wrap(new Random(seed));

		var minSystemAgeFactor = Math.min(1, this.densityFields.minAgeFactor.sample(systemPos));
		var systemAgeFactor = Math.pow(rng.uniformDouble(), 2);
		var systemAgeMyr = this.parentGalaxy.info.ageMya * Mth.lerp(systemAgeFactor, minSystemAgeFactor, 1);

		var availableMass = generateAvailableMass(rng);
		var starMass = generateStarMass(rng, availableMass);
		var primaryStar = StellarCelestialNode.fromMassAndAge(rng, starMass, systemAgeMyr);
		var remainingMass = availableMass - starMass;

		var name = NameTemplate.SECTOR_NAME.generate(rng);

		return new StarSystem.Info(systemAgeMyr, remainingMass, starMass - primaryStar.massYg, name, primaryStar);
	}

	@Override
	public StarSystem generateFullSystem(Info systemInfo, long systemSeed) {
		var rng = Rng.wrap(new Random(systemSeed));
	
		var systemGenerator = new StarSystemGeneratorImpl(rng, this.parentGalaxy, systemInfo);
		var rootNode = systemGenerator.generate();
		rootNode.assignIds();
	
		return new StarSystem(this.parentGalaxy, rootNode);	
	}

}
