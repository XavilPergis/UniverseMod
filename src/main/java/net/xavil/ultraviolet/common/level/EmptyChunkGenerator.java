package net.xavil.ultraviolet.common.level;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class EmptyChunkGenerator extends ChunkGenerator {

	public static final Codec<EmptyChunkGenerator> CODEC = RecordCodecBuilder
			.create(instance -> EmptyChunkGenerator.commonCodec(instance)
					.and(RegistryOps.retrieveRegistry(Registry.BIOME_REGISTRY).forGetter(gen -> gen.biomes))
					.apply(instance, instance.stable(EmptyChunkGenerator::new)));

	private final Registry<Biome> biomes;

	private static BiomeSource defaultBiomeSource(Registry<Biome> biomes) {
		// TODO: custom biome
		return new FixedBiomeSource(biomes.getOrCreateHolder(Biomes.END_BARRENS));
	}

	public EmptyChunkGenerator(Registry<StructureSet> registry, Registry<Biome> biomes) {
		this(registry, biomes, defaultBiomeSource(biomes));
	}

	private EmptyChunkGenerator(Registry<StructureSet> registry, Registry<Biome> biomes, BiomeSource src) {
		super(registry, Optional.empty(), src, src, 0L);
		this.biomes = biomes;
	}

	public static EmptyChunkGenerator create(RegistryAccess registries) {
		final var structureSet = registries.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
		final var biome = registries.registryOrThrow(Registry.BIOME_REGISTRY);
		return new EmptyChunkGenerator(structureSet, biome);
	}

	@Override
	protected Codec<? extends ChunkGenerator> codec() {
		return CODEC;
	}

	@Override
	public ChunkGenerator withSeed(long var1) {
		return this;
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

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Executor var1, Blender var2, StructureFeatureManager var3,
			ChunkAccess chunk) {
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public int getMinY() {
		return 0;
	}

	@Override
	public int getGenDepth() {
		return 384;
	}

	@Override
	public int getSeaLevel() {
		return -63;
	}

	@Override
	public int getBaseHeight(int var1, int var2, Types var3, LevelHeightAccessor heightAccessor) {
		return heightAccessor.getMinBuildHeight();
	}

	@Override
	public NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor heightAccessor) {
		return new NoiseColumn(heightAccessor.getMinBuildHeight(), new BlockState[0]);
	}

	@Override
	public void addDebugScreenInfo(List<String> var1, BlockPos var2) {
	}

}
