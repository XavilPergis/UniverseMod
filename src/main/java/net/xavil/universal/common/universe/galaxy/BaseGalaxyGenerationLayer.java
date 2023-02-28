package net.xavil.universal.common.universe.galaxy;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.common.NameTemplate;
import net.xavil.universal.common.universe.DensityField3;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Units;
import net.xavil.universal.common.universe.Vec3;
import net.xavil.universal.common.universe.Vec3i;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystemGenerator;

public class BaseGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public static final int MAXIMUM_STARS_PER_SECTOR = 30000;
	public static final int MAXIMUM_STAR_PLACEMENT_ATTEMPTS = 16;
	public static final int DENSITY_SAMPLE_COUNT = 2500;

	public final Galaxy parentGalaxy;
	public final DensityField3 densityField;

	public BaseGalaxyGenerationLayer(Galaxy parentGalaxy, DensityField3 densityField) {
		super(1);
		this.parentGalaxy = parentGalaxy;
		this.densityField = densityField;
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
			sectorDensitySum += this.densityField.sampleDensity(ctx.volumeMin.add(volumeOffsetTm));
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
				var density = this.densityField.sampleDensity(systemPos);

				if (density >= random.nextDouble(0, maxDensity)) {
					var initial = generateStarSystemInfo(ctx.volumeCoords, volumeOffsetTm, infoSeed);
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

	public final double MINIMUM_STAR_MASS_YG = Units.YG_PER_MSOL * 0.1;
	public final double MAXIMUM_STAR_MASS_YG = Units.YG_PER_MSOL * 30.0;

	private double generateStarMass(Random random, double upperBoundYg) {
		upperBoundYg = Math.min(MAXIMUM_STAR_MASS_YG, upperBoundYg);
		var massFactor = Math.pow(random.nextDouble(), 60);
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
		info.systemAgeMya = Mth.lerp(systemAgeFactor, 1, this.parentGalaxy.info.ageMya);
		// noise field for driving this too; once again because stars form in clusters
		var remainingHydrogenYg = random.nextDouble(Units.YG_PER_MSOL * 0.1, Units.YG_PER_MSOL * 100);

		// there's always at least one star per system
		var initialStarMass = generateStarMass(random, remainingHydrogenYg);
		info.stars.add(generateStarNode(random, info.systemAgeMya, initialStarMass));

		// NOTE: generating the stars upfront in this simple way does not seem to be too
		// costly to do, even directly on the render thread. It still might make sense
		// to do everything a background thread, still.
		for (var attempts = 0; attempts < 256; ++attempts) {
			if (random.nextDouble() < 0.5)
				break;

			var mass = generateStarMass(random, remainingHydrogenYg);
			if (remainingHydrogenYg >= mass && remainingHydrogenYg >= MINIMUM_STAR_MASS_YG) {
				remainingHydrogenYg -= mass;
				info.stars.add(generateStarNode(random, info.systemAgeMya, mass));
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

	public static final double NEUTRON_STAR_MIN_INITIAL_MASS_YG = Units.msol(10);
	public static final double BLACK_HOLE_MIN_INITIAL_MASS_YG = Units.msol(25);

	private static @Nullable StarNode generateStarNode(Random random, double systemAgeMya, double massYg) {
		final var starLifetime = StarNode.mainSequenceLifetimeFromMass(massYg);

		// angular_momentum/angular_velocity = mass * radius^2

		// TODO: conservation of angular momentum when star changes mass or radius
		var targetType = StarNode.Type.MAIN_SEQUENCE;
		if (systemAgeMya > starLifetime) {
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

		var node = StarNode.fromMass(random, targetType, massYg);
		return node;
	}

}
