package net.xavil.universal.common.universe.galaxy;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.common.NameTemplate;
import net.xavil.universal.common.universe.DensityFields;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystemGenerator;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.Units;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec3i;

public class BaseGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public static final int MAXIMUM_STARS_PER_SECTOR = 30000;
	public static final int MAXIMUM_STAR_PLACEMENT_ATTEMPTS = 16;
	public static final int DENSITY_SAMPLE_COUNT = 2500;

	public final Galaxy parentGalaxy;
	public final DensityFields densityFields;

	public BaseGalaxyGenerationLayer(Galaxy parentGalaxy, DensityFields densityFields) {
		super(1);
		this.parentGalaxy = parentGalaxy;
		this.densityFields = densityFields;
	}

	private static Vec3 randomVec(Random random) {
		var x = random.nextDouble(0, Galaxy.TM_PER_SECTOR);
		var y = random.nextDouble(0, Galaxy.TM_PER_SECTOR);
		var z = random.nextDouble(0, Galaxy.TM_PER_SECTOR);
		return Vec3.from(x, y, z);
	}

	private long volumeSeed(Vec3i volumeCoords) {
		var seed = Mth.murmurHash3Mixer(this.parentGalaxy.parentUniverse.getCommonUniverseSeed());
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.x);
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.y);
		seed ^= Mth.murmurHash3Mixer(seed ^ (long) volumeCoords.z);
		return seed;
	}

	private long systemSeed(Vec3i volumeCoords, int id) {
		var seed = volumeSeed(volumeCoords);
		seed ^= Mth.murmurHash3Mixer((long) id);
		return seed;
	}

	@Override
	public void generateInto(Context ctx, Sink sink) {
		var random = new Random(volumeSeed(ctx.volumeCoords));

		var sectorDensitySum = 0.0;
		for (var i = 0; i < DENSITY_SAMPLE_COUNT; ++i) {
			var volumeOffsetTm = randomVec(random);
			sectorDensitySum += this.densityFields.stellarDensity.sampleDensity(ctx.volumeMin.add(volumeOffsetTm));
		}
		final var averageSectorDensity = Math.max(0, sectorDensitySum / DENSITY_SAMPLE_COUNT);

		var starAttemptCount = (int) (averageSectorDensity * Galaxy.TM_PER_SECTOR_3);

		if (starAttemptCount > MAXIMUM_STARS_PER_SECTOR) {
			Mod.LOGGER.warn("high star attempt count: {}", starAttemptCount);
			starAttemptCount = MAXIMUM_STARS_PER_SECTOR;
		}

		int successfulAttempts = 0;
		var maxDensity = starAttemptCount / Galaxy.TM_PER_SECTOR_3;
		for (var i = 0; i < starAttemptCount; ++i) {
			var infoSeed = random.nextLong();

			for (var j = 0; j < MAXIMUM_STAR_PLACEMENT_ATTEMPTS; ++j) {
				var volumeOffsetTm = randomVec(random);
				var systemPos = ctx.volumeMin.add(volumeOffsetTm);
				var density = this.densityFields.stellarDensity.sampleDensity(systemPos);

				if (density >= random.nextDouble(0, maxDensity)) {
					var initial = generateStarSystemInfo(ctx.volumeCoords, systemPos, infoSeed);
					var systemSeed = systemSeed(ctx.volumeCoords, i);
					var i2 = i;
					var lazy = new Lazy<>(initial,
							info -> generateStarSystem(ctx.volumeCoords, systemPos, info, i2, systemSeed));
					sink.accept(systemPos, lazy);
					successfulAttempts += 1;
					break;
				}
			}
		}

		Mod.LOGGER.debug("[galaxygen] average stellar density: {}", averageSectorDensity);
		Mod.LOGGER.debug("[galaxygen] star placement attempt count: {}", starAttemptCount);
		Mod.LOGGER.debug("[galaxygen] successful star placements: {}", successfulAttempts);

	}

	public final double MINIMUM_STAR_MASS_YG = Units.Yg_PER_Msol * 0.1;
	public final double MAXIMUM_STAR_MASS_YG = Units.Yg_PER_Msol * 30.0;

	private double generateStarMass(Random random, double upperBoundYg) {
		upperBoundYg = Math.min(MAXIMUM_STAR_MASS_YG, upperBoundYg);
		var massFactor = Math.pow(random.nextDouble(), 60);
		var massYg = Mth.lerp(massFactor, MINIMUM_STAR_MASS_YG, upperBoundYg);
		return massYg;
	}

	// Basic system info
	private StarSystem.Info generateStarSystemInfo(Vec3i volumeCoords, Vec3 systemPos, long seed) {
		var random = new Random(seed);
		var info = new StarSystem.Info();

		var minSystemAgeFactor = Math.min(1, this.densityFields.minAgeFactor.sampleDensity(systemPos));
		var systemAgeFactor = Math.pow(random.nextDouble(), 2);

		info.systemAgeMya = this.parentGalaxy.info.ageMya * Mth.lerp(systemAgeFactor, minSystemAgeFactor, 1);
		// noise field for driving this too; once again because stars form in clusters
		var remainingHydrogenYg = random.nextDouble(Units.Yg_PER_Msol * 0.1, Units.Yg_PER_Msol * 100);

		// there's always at least one star per system
		var initialStarMass = generateStarMass(random, remainingHydrogenYg);
		info.addStar(generateStarNode(random, info.systemAgeMya, initialStarMass));

		// NOTE: generating the stars upfront in this simple way does not seem to be too
		// costly to do, even directly on the render thread. It still might make sense
		// to do everything a background thread, still.
		for (var attempts = 0; attempts < 256; ++attempts) {
			if (random.nextDouble() < 0.5)
				break;

			var mass = generateStarMass(random, remainingHydrogenYg);
			if (remainingHydrogenYg >= mass && remainingHydrogenYg >= MINIMUM_STAR_MASS_YG) {
				remainingHydrogenYg -= mass;
				info.addStar(generateStarNode(random, info.systemAgeMya, mass));
			}
		}

		info.remainingHydrogenYg = remainingHydrogenYg;
		info.name = NameTemplate.SECTOR_NAME.generate(random);

		return info;
	}

	// Full system info
	public StarSystem generateStarSystem(Vec3i volumeCoords, Vec3 volumeOffsetTm, StarSystem.Info info, int i,
			long seed) {
		var random = new Random(seed);

		var systemGenerator = new StarSystemGenerator(random, this.parentGalaxy, info);
		var rootNode = systemGenerator.generate();
		rootNode.assignIds();

		return new StarSystem(this.parentGalaxy, rootNode);
	}

	public static final double NEUTRON_STAR_MIN_INITIAL_MASS_YG = Units.fromMsol(10);
	public static final double BLACK_HOLE_MIN_INITIAL_MASS_YG = Units.fromMsol(25);

	private static @Nullable StellarCelestialNode generateStarNode(Random random, double systemAgeMya, double massYg) {
		final var starLifetime = StellarCelestialNode.mainSequenceLifetimeFromMass(massYg);

		// angular_momentum/angular_velocity = mass * radius^2

		// TODO: conservation of angular momentum when star changes mass or radius
		var targetType = StellarCelestialNode.Type.MAIN_SEQUENCE;
		if (systemAgeMya > starLifetime) {
			if (massYg < NEUTRON_STAR_MIN_INITIAL_MASS_YG) {
				targetType = StellarCelestialNode.Type.WHITE_DWARF;
			} else if (massYg < BLACK_HOLE_MIN_INITIAL_MASS_YG) {
				targetType = StellarCelestialNode.Type.NEUTRON_STAR;
			} else {
				targetType = StellarCelestialNode.Type.BLACK_HOLE;
			}
		} else if (systemAgeMya > starLifetime * 0.8) {
			targetType = StellarCelestialNode.Type.GIANT;
		}

		var node = StellarCelestialNode.fromMass(random, targetType, massYg);
		return node;
	}

}
