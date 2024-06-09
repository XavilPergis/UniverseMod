package net.xavil.ultraviolet.common.level;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.math.matrices.Vec2;
import net.xavil.ultraviolet.common.block.ModBlocks;
import net.xavil.ultraviolet.common.universe.ScalarField;
import net.xavil.ultraviolet.common.universe.ScalarField2;
import net.xavil.ultraviolet.common.universe.galaxy.InterpolatedField2;
import net.xavil.ultraviolet.common.universe.system.PlanetaryCelestialNode;

public final class ModChunkGenerator extends ChunkGenerator {

	public static final Codec<ModChunkGenerator> CODEC = RecordCodecBuilder
			.create(instance -> ModChunkGenerator.commonCodec(instance)
					.and(instance.group(
							RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter(gen -> gen.biomes),
							Codec.LONG.fieldOf("seed").forGetter(gen -> gen.seed),
							Codec.DOUBLE.fieldOf("gravity").forGetter(gen -> gen.gravity)))
					.apply(instance, instance.stable(ModChunkGenerator::new)));

	private final Registry<Biome> biomes;
	public final long seed;
	public final double gravity;
	private final int baseHeight = 128;

	private static BiomeSource defaultBiomeSource(Registry<Biome> biomes, long seed) {
		// TODO: custom biome/biome source?
		return new FixedBiomeSource(biomes.getOrCreateHolder(Biomes.END_BARRENS));
	}

	// constructor used for initial generator creation
	public ModChunkGenerator(RegistryAccess access, PlanetaryCelestialNode node) {
		this(access.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY),
				access.registryOrThrow(Registry.BIOME_REGISTRY),
				node.seed, node.surfaceGravityEarthRelative());
	}

	// constructor used for deserialization
	private ModChunkGenerator(Registry<StructureSet> registry, Registry<Biome> biomes,
			long seed, double gravity) {
		this(registry, biomes, defaultBiomeSource(biomes, seed), seed, gravity);
	}

	// helper constructor
	private ModChunkGenerator(Registry<StructureSet> registry, Registry<Biome> biomes, BiomeSource src,
			long seed, double gravity) {
		super(registry, Optional.empty(), src, src, 0L);
		this.biomes = biomes;
		this.seed = seed;
		this.gravity = gravity;
	}

	@Override
	protected Codec<ModChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public ChunkGenerator withSeed(long seed) {
		return new ModChunkGenerator(this.structureSets, this.biomes, seed, this.gravity);
	}

	@Override
	public Sampler climateSampler() {
		return Climate.empty();
	}

	@Override
	public void applyCarvers(WorldGenRegion var1, long var2, BiomeManager var4, StructureFeatureManager var5,
			ChunkAccess var6, Carving var7) {
	}

	@Override
	public void buildSurface(WorldGenRegion var1, StructureFeatureManager var2, ChunkAccess var3) {
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion var1) {
	}

	// === CRATERS ===
	// - small impact craters - mostly just a smooth divot
	// - large impact craters - can melt the impact site and flood the impact basin
	// with molten rock, creating a flat bottom
	// - large enoug impact sites - walls slump and pinch basin, forming a central
	// peak
	// - sometimes a central peak ridge can form instead of a single peak
	// - ejecta patterns? (maybe do small ones)
	// - grazing angles? (gonna leave this out for now lol)
	// - some impact basins are younger and have less impact craters on top, how do
	// we model this?
	// === LINEAE ===
	// idk how to approach this rn
	// === LARGE YOUNG BASINS ===
	// === MOUNTAINS/VOLCANOS ===
	// === RIVERS/LAKES/OCEANS ===

	private final double cellMargin = 2.0;

	// https://iquilezles.org/articles/smin/
	private static double smin(double a, double b, double k) {
		final var h = Math.max(k - Math.abs(a - b), 0.0) / k;
		return Math.min(a, b) - h * h * h * k * (1.0 / 6.0);
	}

	private final class SurfaceColumnInfo {
		public final CraterLevelParameters[] craterLevelParameters;
		public final ScalarField2 heightField;
		// public double surfaceHeight;

		public SurfaceColumnInfo(CraterLevelParameters[] craterLevelParameters, ScalarField2 heightField) {
			this.craterLevelParameters = craterLevelParameters;
			this.heightField = heightField;
		}
	}

	private record CraterLevelParameters(
			int maxAttempts,
			double amplitude,
			double scale,
			double chance,
			double steepness,
			double basinDepth,
			double rimRatio) {
	}

	private CraterLevelParameters[] makeLevelParameters() {
		return new CraterLevelParameters[] {
			// @formatter:off
			// new CraterLevelParameters(5,   1.0,      6.0, 0.70,  2.0, 0.9, 0.9),
			// new CraterLevelParameters(3,   3.0,     20.0, 0.50,  2.0, 0.1, 0.6),
			new CraterLevelParameters(2,   1.0,     40.0, 0.50,  4.0, 0.2, 0.5),
			new CraterLevelParameters(1,   3.0,     80.0, 0.50,  5.0, 0.4, 0.5),
			new CraterLevelParameters(1,   5.0,    160.0, 0.50,  5.0, 0.5, 0.5),
			new CraterLevelParameters(1,   7.0,    320.0, 0.50, 10.0, 0.6, 0.5),
			new CraterLevelParameters(1,  15.0,    640.0, 0.50, 10.0, 0.8, 0.5),
			new CraterLevelParameters(1,  25.0,   1280.0, 0.70, 20.0, 1.0, 0.5),
			new CraterLevelParameters(1,  40.0,   2560.0, 0.70, 20.0, 1.0, 0.5),
			new CraterLevelParameters(1,  60.0,   5120.0, 0.70, 30.0, 1.0, 0.5),
			new CraterLevelParameters(1,  80.0,  10240.0, 0.90, 30.0, 1.0, 0.5),
			new CraterLevelParameters(1, 100.0,  40960.0, 0.90, 40.0, 1.0, 0.5),
			new CraterLevelParameters(1, 100.0, 163840.0, 0.90, 80.0, 1.0, 0.4),
			// @formatter:on
		};
	}

	private double calculateCraterHeight(SurfaceColumnInfo info, SplittableRng rng, int bx, int bz) {
		double height = 0.0;

		for (int level = 0; level < info.craterLevelParameters.length; ++level) {
			rng.push(level);
			final var params = info.craterLevelParameters[level];

			for (int attempt = 0; attempt < params.maxAttempts; ++attempt) {
				rng.push(attempt);

				final double scale = this.cellMargin * params.scale;

				double sx = (double) bx / scale, sz = (double) bz / scale;
				sx -= rng.uniformDouble("grid_offset_x");
				sz -= rng.uniformDouble("grid_offset_z");
				final double cx = Math.floor(sx), cz = Math.floor(sz);

				rng.advanceWith(Double.doubleToLongBits(cx));
				rng.advanceWith(Double.doubleToLongBits(cz));

				if (rng.chance("is_cratered", params.chance)) {
					// random offset in cell so it doesnt look like everything is on a fixed grid
					// even though it is!
					final var maxOffset = this.cellMargin - 1.0;

					// uv coords for this cell (scaled by margin so we can offset without
					// overflowing the cell bounds)
					double fx = this.cellMargin * (2.0 * (sx - cx) - 1.0);
					double fz = this.cellMargin * (2.0 * (sz - cz) - 1.0);
					fx -= rng.uniformDouble("offset_x", -maxOffset, maxOffset);
					fz -= rng.uniformDouble("offset_z", -maxOffset, maxOffset);

					final double d = Math.sqrt(fx * fx + fz * fz);
					final double rimRatio = params.rimRatio;

					if (d <= 1.0) {
						// TODO: craters should "reset" everything in their impact basis that are older
						// than them
						final var age = rng.uniformDouble("age");

						// final double amplitude = Math.min(200, 0.2 * Math.sqrt(5 * scale));
						// final var basinSteepness = 3.0;
						final double amplitude = params.amplitude;
						final var basinDepth = params.basinDepth;
						final var basinSteepness = params.steepness;

						// f_{1}\left(x\right)=\left(n+1\right)\frac{x}{kL}^{N}-n\left\{x\ge0\right\}
						final var basinShape = (basinDepth + 1) * Math.pow(d / rimRatio, basinSteepness) - basinDepth;

						// f_{2}\left(x\right)=\left(\frac{L-x}{L-Lk}\right)B^{\frac{Lk-x}{L}}\left\{x\ge0\right\}
						final double rimFalloff = 10.0;
						final var rimShape = ((1 - d) / (1 - rimRatio)) * Math.pow(rimFalloff, rimRatio - d);

						height += amplitude * smin(basinShape, rimShape, Mth.lerp(age, 0.5, 0.9));
					}
				}
				rng.pop();
			}
			rng.pop();
		}

		final var gravityScale = this.gravity < 1 ? 2.0 - Math.pow(this.gravity, 2.0) : Math.pow(this.gravity, -2.0);
		// out.surfaceHeight += height * 0.5 * gravityScale;
		return height * 0.5 * gravityScale;
	}

	private double calculateNormalTerrainHeight(SurfaceColumnInfo info, SplittableRng rng, int bx, int bz) {
		double height = 0.0;
		height += 50 * info.heightField.sample(bx, bz);
		// height = Math.max(0, height);
		return height;
	}

	private double calculateSurfaceHeight(SurfaceColumnInfo out, SplittableRng rng, int bx, int bz) {
		double height = 0.0;
		height += calculateNormalTerrainHeight(out, rng, bx, bz);
		// if (hasCraters)
		// height += calculateCraterHeight(out, rng, bx, bz);
		return height;
	}

	@Override
	public int getBaseHeight(int x, int z, Types heightmapTypes, LevelHeightAccessor heightAccessor) {
		final SurfaceColumnInfo info = makeInfo(x, z);
		final var rng = new SplittableRng(this.seed);
		final var fracHeight = calculateSurfaceHeight(info, rng, x, z);
		final var height = blockHeight(fracHeight, heightAccessor.getHeight());

		final var minY = heightAccessor.getMinBuildHeight();
		final var maxY = heightAccessor.getMaxBuildHeight();
		for (int y = maxY - 1; y >= minY; --y) {
			final var state = stateForDepth(y, height);
			if (heightmapTypes.isOpaque().test(state))
				return y + 1;
		}

		return heightAccessor.getMinBuildHeight();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor) {
		final SurfaceColumnInfo info = makeInfo(x, z);

		final var rng = new SplittableRng(this.seed);
		final var fracHeight = calculateSurfaceHeight(info, rng, x, z);
		final var height = blockHeight(fracHeight, heightAccessor.getHeight());

		final var minY = heightAccessor.getMinBuildHeight();
		final var column = new BlockState[height - minY + 1];
		for (int i = 0; i < column.length; ++i) {
			final var state = stateForDepth(i + minY, height);
			column[i] = state;
		}

		return new NoiseColumn(minY, column);
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender,
			StructureFeatureManager structureFeatureManager, ChunkAccess chunk) {
		final var heightAccess = chunk.getHeightAccessorForGeneration();
		final var lockedSections = MutableSet.<LevelChunkSection>hashSet();
		for (int i = heightAccess.getMaxSection() - 1; i >= heightAccess.getMinSection(); --i) {
			final var section = chunk.getSection(chunk.getSectionIndexFromSectionY(i));
			section.acquire();
			lockedSections.insert(section);
		}

		final Supplier<ChunkAccess> task = () -> {
			this.buildChunk(chunk, blender, structureFeatureManager);
			return chunk;
		};

		return CompletableFuture
				.supplyAsync(Util.wrapThreadWithTaskName("uv_wgen_fill_noise", task), Util.backgroundExecutor())
				// .supplyAsync(task, executor)
				.whenCompleteAsync((c, t) -> lockedSections.forEach(LevelChunkSection::release), executor);
	}

	private SurfaceColumnInfo makeInfo(double x, double z) {

		final double gs = 16;
		final int subdiv = 4; // 4x4 cell grid
		final double cxn = gs * Math.floor(x / gs), czn = gs * Math.floor(z / gs);
		final double cxp = cxn + gs, czp = czn + gs;

		// ScalarField2 noiseField = ScalarField2.fractalSimplexNoise(0, 1, 1.0 / 400, 3, 1, 0.5);
		ScalarField2 noiseField = ScalarField2.fractalSimplexNoise(0, 5, 1.0 / 400, 3, 1, 0.5);
		noiseField = InterpolatedField2.create(noiseField, new Vec2(cxn, czn), new Vec2(cxp, czp), subdiv);

		final var levelParams = makeLevelParameters();
		return new SurfaceColumnInfo(levelParams, noiseField);
	}

	private void buildChunk(ChunkAccess chunk, Blender blender, StructureFeatureManager structureFeatureManager) {
		final var hm0 = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
		final var hm1 = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

		// -32k to 32k seems like a good enough range :p
		final var heights = new short[16 * 16];
		final var chunkHeight = chunk.getHeight();

		final var minAbsX = chunk.getPos().getMinBlockX();
		final var minAbsZ = chunk.getPos().getMinBlockZ();

		final SurfaceColumnInfo info = makeInfo(chunk.getPos().getMinBlockX(), chunk.getPos().getMinBlockZ());
		final var rng = new SplittableRng(this.seed);
		for (int x = 0; x < 16; ++x) {
			for (int z = 0; z < 16; ++z) {
				final var bx = chunk.getPos().getBlockX(x);
				final var bz = chunk.getPos().getBlockZ(z);
				final var fracHeight = calculateSurfaceHeight(info, rng, bx, bz);

				final var height = blockHeight(fracHeight, chunkHeight);
				// clamp just in case~
				heights[(z << 4) | x] = (short) Mth.clamp(height, Short.MIN_VALUE, Short.MAX_VALUE);
			}
		}

		final var blockPos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < chunk.getMaxSection() - chunk.getMinSection(); ++i) {
			final var section = chunk.getSection(i);
			final var minY = section.bottomBlockY();
			for (int y = 0; y < 16; ++y) {
				blockPos.setY(minY + y);
				for (int z = 0; z < 16; ++z) {
					blockPos.setZ(minAbsZ + z);
					for (int x = 0; x < 16; ++x) {
						blockPos.setX(minAbsX + x);
						final var height = (int) heights[(z << 4) | x];
						final var state = stateForDepth(blockPos.getY(), height);
						section.setBlockState(x, y, z, state, false);
						hm0.update(x, height, z, state);
						hm1.update(x, height, z, state);
						if (state.getLightEmission() != 0 && chunk instanceof ProtoChunk protoChunk) {
							protoChunk.addLight(blockPos);
						}
					}
				}
			}
		}
	}

	private int blockHeight(double height, int buildHeight) {
		return Mth.clamp(this.baseHeight + Mth.floor(height), 10, buildHeight - 10);
	}

	private BlockState stateForDepth(int y, int surfaceY) {
		if (y >= surfaceY) {
			if (y >= this.baseHeight) {
				return Blocks.AIR.defaultBlockState();
			} else {
				return Blocks.LAVA.defaultBlockState();
			}
		}
		return ModBlocks.SILICATE_ROCK.defaultBlockState();
		// return Blocks.STONE.defaultBlockState();
	}

	@Override
	public int getGenDepth() {
		return 384;
	}

	@Override
	public int getSeaLevel() {
		return -128;
	}

	@Override
	public int getMinY() {
		return -64;
	}

	@Override
	public void addDebugScreenInfo(List<String> output, BlockPos pos) {
	}

}
