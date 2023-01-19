package net.xavil.universal.common.universe.galaxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.DensityField3;
import net.xavil.universal.common.universe.LodVolume;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.system.CelestialNode;
import net.xavil.universal.common.universe.system.CelestialNode.OrbitalPlane;
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
		var remainingHydrogenYg = random.nextDouble(Units.YG_PER_MSOL * 0.1, Units.YG_PER_MSOL * 100);

		// there's always at least one star per system
		var initialStarMass = generateStarMass(random, remainingHydrogenYg);
		info.stars.add(generateStarNode(info.systemAgeMya, initialStarMass));

		// NOTE: generating the stars upfront in this simple way does not seem to be too
		// costly to do, even directly on the render thread. It still might make sense
		// to do everything a background thread, still.
		for (var attempts = 0; attempts < 256; ++attempts) {
			if (random.nextDouble() < 0.5)
				break;

			var mass = generateStarMass(random, remainingHydrogenYg);
			if (remainingHydrogenYg >= mass && remainingHydrogenYg >= MINIMUM_STAR_MASS_YG) {
				remainingHydrogenYg -= mass;
				info.stars.add(generateStarNode(info.systemAgeMya, mass));
			}
		}

		info.remainingHydrogenYg = remainingHydrogenYg;

		return info;
	}

	// Full system info
	public StarSystem generateStarSystem(Vec3i volumeCoords, Vec3 volumeOffsetTm, StarSystem.Info info, long seed) {
		var random = new Random(seed);

		var systemPlane = new CelestialNode.OrbitalPlane(
				random.nextDouble(-Math.PI * 2, Math.PI * 2),
				random.nextDouble(-Math.PI * 2, Math.PI * 2),
				random.nextDouble(-Math.PI * 2, Math.PI * 2));

		var rootNode = pairStars(random, systemPlane, info.stars);

		rootNode.assignIds();

		return new StarSystem(this, "Test", rootNode);
	}

	private static CelestialNode pairStars(Random random, CelestialNode.OrbitalPlane systemPlane, List<CelestialNode.StellarBodyNode> stars) {
		if (stars.isEmpty()) return null;

		var pairingList = new ArrayList<CelestialNode>(stars);
		while (pairingList.size() > 1) {
			var a = pairingList.remove(random.nextInt(0, pairingList.size()));
			var b = pairingList.remove(random.nextInt(0, pairingList.size()));
			if (a == b)
				continue;

			var eccentricity = random.nextDouble(0, 0.05);
			var smaTm = random.nextDouble(0.01, 10);
			var node = new CelestialNode.BinaryNode(a, b, systemPlane, eccentricity, smaTm);
			pairingList.add(node);
		}

		return pairingList.get(0);
	}

	private static CelestialNode.StellarBodyNode generateStarNode(double systemAgeMya, double massYg) {
		var massMsol = massYg / Units.YG_PER_MSOL;

		var luminosityLsol = Math.pow(massMsol, 3.5);
		var radiusRsol = Math.pow(massMsol, 0.8);
		var starLifetime = SOL_LIFETIME_MYA * (massMsol / luminosityLsol);

		var type = CelestialNode.StellarBodyNode.Type.MAIN_SEQUENCE;

		// idk im just making this up
		if (systemAgeMya > starLifetime) {
			// TODO: figure out luminosity and mass and stuff
			if (massMsol < 1.4) {
				// luminosity depends on the age of the star, since its just stored thermal
				// energy its radiating away
				type = CelestialNode.StellarBodyNode.Type.WHITE_DWARF;
				radiusRsol *= 0.00001;
			} else if (massMsol < 2.1) {
				type = CelestialNode.StellarBodyNode.Type.NEUTRON_STAR;
				radiusRsol *= 0.000004;
			} else {
				type = CelestialNode.StellarBodyNode.Type.BLACK_HOLE;
				radiusRsol *= 0.0000035;
				luminosityLsol = 0;
			}
		} else if (systemAgeMya > starLifetime * 0.8) {
			type = CelestialNode.StellarBodyNode.Type.GIANT;
			radiusRsol *= 100;
		}

		return new CelestialNode.StellarBodyNode(type, massYg, luminosityLsol, radiusRsol);
	}

}
