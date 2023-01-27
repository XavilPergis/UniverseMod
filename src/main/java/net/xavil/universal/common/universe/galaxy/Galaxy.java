package net.xavil.universal.common.universe.galaxy;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.DensityField3;
import net.xavil.universal.common.universe.LodVolume;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.universe.Universe;

public class Galaxy {

	public static class Info {
		public GalaxyType type;
		public double ageMya;
	}

	public final Universe parentUniverse;
	public final Info info;
	private final DensityField3 densityField;

	// private final Set<StarSystem> activeStarSystems = new HashSet<>();
	private final Map<Vec3i, LodVolume<StarSystem.Info, StarSystem>> loadedVolumes = new HashMap<>();

	public Galaxy(Universe parentUniverse, Info info, DensityField3 densityField) {
		this.parentUniverse = parentUniverse;
		this.info = info;
		this.densityField = densityField;
	}

	public static final double TM_PER_SECTOR = Units.TM_PER_LY * 10;
	public static final double TM_PER_SECTOR_3 = TM_PER_SECTOR * TM_PER_SECTOR * TM_PER_SECTOR;

	public static final int MAXIMUM_STARS_PER_SECTOR = 30000;
	public static final int MAXIMUM_STAR_PLACEMENT_ATTEMPTS = 16;
	public static final int DENSITY_SAMPLE_COUNT = 2500;

	public static final double SOL_LIFETIME_MYA = 10e4;

	private static Vec3 randomVec(Random random) {
		var x = random.nextDouble(0, TM_PER_SECTOR);
		var y = random.nextDouble(0, TM_PER_SECTOR);
		var z = random.nextDouble(0, TM_PER_SECTOR);
		return new Vec3(x, y, z);
	}

	private long volumeSeed(Vec3i volumeCoords) {
		var seed = Mth.murmurHash3Mixer(this.parentUniverse.getCommonUniverseSeed());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.getX());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.getY());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.getZ());
		return seed;
	}

	private long systemSeed(Vec3i volumeCoords, int id) {
		var seed = volumeSeed(volumeCoords);
		seed ^= Mth.murmurHash3Mixer((long) id);
		return seed;
	}

	public LodVolume<StarSystem.Info, StarSystem> getOrGenerateVolume(Vec3i volumeCoords) {
		if (this.loadedVolumes.containsKey(volumeCoords)) {
			return this.loadedVolumes.get(volumeCoords);
		}

		var random = new Random(volumeSeed(volumeCoords));
		var volume = new LodVolume<StarSystem.Info, StarSystem>(volumeCoords, TM_PER_SECTOR,
				(info, offset, id) -> generateStarSystem(volumeCoords, offset, info, systemSeed(volumeCoords, id)));

		final var sectorBase = volume.getBasePos();
		var sectorDensitySum = 0.0;
		for (var i = 0; i < DENSITY_SAMPLE_COUNT; ++i) {
			var volumeOffsetTm = randomVec(random);
			sectorDensitySum += this.densityField.sampleDensity(sectorBase.add(volumeOffsetTm));
		}
		final var averageSectorDensity = Math.max(0, sectorDensitySum / DENSITY_SAMPLE_COUNT);

		var starAttemptCount = (int) (averageSectorDensity * TM_PER_SECTOR_3);

		if (starAttemptCount > MAXIMUM_STARS_PER_SECTOR) {
			Mod.LOGGER.warn("high star attempt count: {}", starAttemptCount);
			starAttemptCount = MAXIMUM_STARS_PER_SECTOR;
		}

		int successfulAttempts = 0;
		var maxDensity = starAttemptCount / TM_PER_SECTOR_3;
		for (var i = 0; i < starAttemptCount; ++i) {
			var infoSeed = random.nextLong();

			for (var j = 0; j < MAXIMUM_STAR_PLACEMENT_ATTEMPTS; ++j) {
				var volumeOffsetTm = randomVec(random);
				var density = this.densityField.sampleDensity(sectorBase.add(volumeOffsetTm));

				if (density >= random.nextDouble(0, maxDensity)) {
					volume.addInitial(volumeOffsetTm, generateStarSystemInfo(volumeCoords, volumeOffsetTm, infoSeed));
					successfulAttempts += 1;
					break;
				}
			}
		}

		Mod.LOGGER.info("[galaxygen] average stellar density: {}", averageSectorDensity);
		Mod.LOGGER.info("[galaxygen] star placement attempt count: {}", starAttemptCount);
		Mod.LOGGER.info("[galaxygen] successful star placements: {}", successfulAttempts);

		this.loadedVolumes.put(volumeCoords, volume);
		return volume;
	}

	public final double MINIMUM_STAR_MASS_YG = Units.YG_PER_MSOL * 0.1;
	public final double MAXIMUM_STAR_MASS_YG = Units.YG_PER_MSOL * 30.0;

	private double generateStarMass(Random random, double upperBoundYg) {
		upperBoundYg = Math.min(MAXIMUM_STAR_MASS_YG, upperBoundYg);
		var massFactor = Math.pow(random.nextDouble(), 15);
		var massYg = Mth.lerp(massFactor, MINIMUM_STAR_MASS_YG, upperBoundYg);
		return massYg;
	}

	// Basic system info
	private StarSystem.Info generateStarSystemInfo(Vec3i volumeCoords, Vec3 volumeOffsetTm, long seed) {
		var random = new Random(seed);
		var info = new StarSystem.Info();

		// TODO: systems tend to form in clusters, so a better idea might be to have a
		// noise field and use that as the system age directly, so we get a nice mix of
		// very young systems and old systems.
		var systemAgeFactor = Math.pow(random.nextDouble(), 3);
		info.systemAgeMya = Mth.lerp(systemAgeFactor, 1, this.info.ageMya);
		// noise field for driving this too; once again because stars form in clusters
		var remainingHydrogenYg = random.nextDouble(Units.YG_PER_MSOL * 0.1, Units.YG_PER_MSOL * 100);

		// there's always at least one star per system
		var initialStarMass = generateStarMass(random, remainingHydrogenYg);
		info.stars.add(generateStarNode(random, info.systemAgeMya, initialStarMass));

		// NOTE: generating the stars upfront in this simple way does not seem to be too
		// costly to do, even directly on the render thread. It still might make sense
		// to do everything a background thread, still.
		for (var attempts = 0; attempts < 256; ++attempts) {
			if (random.nextDouble() < 0.3)
				break;

			var mass = generateStarMass(random, remainingHydrogenYg);
			if (remainingHydrogenYg >= mass && remainingHydrogenYg >= MINIMUM_STAR_MASS_YG) {
				remainingHydrogenYg -= mass;
				info.stars.add(generateStarNode(random, info.systemAgeMya, mass));
			}
		}

		info.remainingHydrogenYg = remainingHydrogenYg;

		return info;
	}

	// Full system info
	public StarSystem generateStarSystem(Vec3i volumeCoords, Vec3 volumeOffsetTm, StarSystem.Info info, long seed) {
		var random = new Random(seed);

		var systemGenerator = new StarSystemGenerator(random, this, info);
		var rootNode = systemGenerator.generate();
		rootNode.assignIds();

		return new StarSystem(this, "Test", rootNode);
	}

	public static final double NEUTRON_STAR_MIN_INITIAL_MASS_YG = Units.msol(10);
	public static final double BLACK_HOLE_MIN_INITIAL_MASS_YG = Units.msol(25);

	private static @Nullable StarNode generateStarNode(Random random, double systemAgeMya, double massYg) {
		final var massMsol = massYg / Units.YG_PER_MSOL;

		final var initialLuminosityLsol = Math.pow(massMsol + random.nextDouble(-0.1, 0.1), 3.5);
		final var initialRadiusRsol = Math.pow(massMsol + random.nextDouble(-0.1, 0.1), 0.8);

		final var starLifetime = SOL_LIFETIME_MYA * (massMsol / initialLuminosityLsol);

		var targetType = StarNode.Type.MAIN_SEQUENCE;
		if (systemAgeMya > starLifetime) {
			// TODO: figure out luminosity and mass and stuff
			if (massYg < NEUTRON_STAR_MIN_INITIAL_MASS_YG) {
				targetType = StarNode.Type.WHITE_DWARF;
			} else if (massYg < BLACK_HOLE_MIN_INITIAL_MASS_YG) {
				targetType = StarNode.Type.NEUTRON_STAR;
			} else {
				targetType = StarNode.Type.BLACK_HOLE;
			}
		} else if (systemAgeMya > starLifetime * 0.8) {
			targetType = StarNode.Type.GIANT;
		}

		final var finalMassYg = targetType.curveMass(random, massYg);
		final var luminosityLsol = targetType.curveLuminosity(random, initialLuminosityLsol);
		final var radiusRsol = targetType.curveRadius(random, initialRadiusRsol);
		final var temperatureK = Units.K_PER_TSOL * Math.pow(luminosityLsol, 0.25) * Math.sqrt(1 / radiusRsol);

		return new StarNode(targetType, finalMassYg, luminosityLsol, radiusRsol, temperatureK);
	}

}
