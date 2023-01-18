package net.xavil.universal.common.universe.galaxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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
	public static final int INITIAL_SAMPLE_COUNT = 10000;

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

		// the maximum system density we can accurately reproduce
		var maxDensity = INITIAL_SAMPLE_COUNT
				/ (TM_PER_SECTOR * TM_PER_SECTOR * TM_PER_SECTOR);

		for (var i = 0; i < INITIAL_SAMPLE_COUNT; ++i) {
			var infoSeed = random.nextLong();
			var volumeOffsetTm = randomVec(random);
			var density = this.densityField
					.sampleDensity(Vec3.atLowerCornerOf(volumeCoords).scale(TM_PER_SECTOR).add(volumeOffsetTm));

			if (density >= random.nextDouble(0, maxDensity)) {
				volume.addInitial(volumeOffsetTm, generateStarSystemInfo(volumeCoords, volumeOffsetTm, infoSeed));
			}
		}

		this.loadedVolumes.put(volumeCoords, volume);
		return volume;
	}

	private StarSystem.Info generateStarSystemInfo(Vec3i volumeCoords, Vec3 volumeOffsetTm, long seed) {
		var random = new Random(seed);
		var info = new StarSystem.Info();

		// TODO: systems tend to form in clusters, so a better idea might be to have a
		// noise field and use that as the system age directly, so we get a nice mix of
		// very young systems and old systems.
		var systemAgeFactor = Math.pow(random.nextDouble(), 3);
		info.systemAgeMya = Mth.lerp(systemAgeFactor, 1, this.info.ageMya);
		info.availableHydrogenYg = random.nextDouble(Units.YG_PER_MSOL * 0.1, Units.YG_PER_MSOL * 100);

		return info;
	}

	public StarSystem generateStarSystem(Vec3i volumeCoords, Vec3 volumeOffsetTm, StarSystem.Info info, long seed) {
		var random = new Random(seed);

		var systemPlane = new CelestialNode.OrbitalPlane(
				random.nextDouble(-Math.PI * 2, Math.PI * 2),
				random.nextDouble(-Math.PI * 2, Math.PI * 2),
				random.nextDouble(-Math.PI * 2, Math.PI * 2));

		var ctx = new SystemGenerationContext(systemPlane, info.systemAgeMya, random, info.availableHydrogenYg);
		var rootNode = ctx.generate();
		rootNode.assignIds();

		return new StarSystem(this, "Test", rootNode);
	}

	private static class SystemGenerationContext {
		public final CelestialNode.OrbitalPlane orbitalPlane;
		public final double systemAgeMya;
		public final Random random;

		private final List<CelestialNode.StellarBodyNode> starNodes = new ArrayList<>();
		private final List<CelestialNode.BinaryNode> binaryStarNodes = new ArrayList<>();

		public double remainingHydrogenYg;

		public final double MINIMUM_STAR_MASS_YG = Units.YG_PER_MSOL * 0.1;
		public final double MAXIMUM_STAR_MASS_YG = Units.YG_PER_MSOL * 30.0;

		public SystemGenerationContext(OrbitalPlane orbitalPlane, double systemAgeMya,
				Random random, double remainingHydrogenYg) {
			this.orbitalPlane = orbitalPlane;
			this.systemAgeMya = systemAgeMya;
			this.random = random;
			this.remainingHydrogenYg = remainingHydrogenYg;
		}

		private double generateStarMass(double upperBoundYg) {
			upperBoundYg = Math.min(MAXIMUM_STAR_MASS_YG, upperBoundYg);
			var massFactor = Math.pow(this.random.nextDouble(), 15);
			var massYg = Mth.lerp(massFactor, MINIMUM_STAR_MASS_YG, upperBoundYg);
			if (this.remainingHydrogenYg < MINIMUM_STAR_MASS_YG) {
				return 0;
			}
			this.remainingHydrogenYg -= massYg;
			return massYg;
		}

		public CelestialNode generate() {

			// In non-circumbinary planets, if a planet's distance to its primary exceeds
			// about one fifth of the closest approach of the other star, orbital stability
			// is not guaranteed

			// For a circumbinary planet, orbital stability is guaranteed only if the
			// planet's distance from the stars is significantly greater than star-to-star
			// distance.
			// The minimum stable star-to-circumbinary-planet separation is about 2–4 times
			// the binary star separation

			// there's always at least one star per system
			this.starNodes.add(generateStarNode(systemAgeMya, generateStarMass(this.remainingHydrogenYg)));

			for (var attempts = 0; attempts < 256; ++attempts) {
				if (this.random.nextDouble() < 0.5)
					break;

				var mass = generateStarMass(this.remainingHydrogenYg);
				if (mass >= MINIMUM_STAR_MASS_YG) {
					this.starNodes.add(generateStarNode(systemAgeMya, mass));
				}
			}

			// Consolidate the list of stars into a single celestial node.
			var pairingList = new ArrayList<CelestialNode>(this.starNodes);
			while (pairingList.size() > 1) {
				var a = pairingList.remove(this.random.nextInt(0, pairingList.size()));
				var b = pairingList.remove(this.random.nextInt(0, pairingList.size()));
				if (a == b)
					continue;

				var eccentricity = this.random.nextDouble(0, 0.05);
				var smaTm = this.random.nextDouble(0.01, 10);
				var node = new CelestialNode.BinaryNode(a, b, this.orbitalPlane, eccentricity, smaTm);
				pairingList.add(node);
			}

			var rootNode = pairingList.get(0);

			// TODO: generate planets
			// TODO: generate comets and such

			return rootNode;
		}

		private void generatePlanetsAround(CelestialNode node) {
		}

	}

	// private static CelestialNode.StellarBodyNode generateSeedStarNode() {}

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
