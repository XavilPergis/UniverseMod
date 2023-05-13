package net.xavil.ultraviolet.common.level;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate.Sampler;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep.Carving;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

public class ModChunkGenerator extends ChunkGenerator {

	public ModChunkGenerator(Registry<StructureSet> registry, Optional<HolderSet<StructureSet>> optional,
			BiomeSource biomeSource, BiomeSource biomeSource2, long l) {
		super(registry, optional, biomeSource, biomeSource2, l);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected Codec<? extends ChunkGenerator> codec() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'codec'");
	}

	@Override
	public ChunkGenerator withSeed(long var1) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'withSeed'");
	}

	@Override
	public Sampler climateSampler() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'climateSampler'");
	}

	@Override
	public void applyCarvers(WorldGenRegion var1, long var2, BiomeManager var4, StructureFeatureManager var5,
			ChunkAccess var6, Carving var7) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'applyCarvers'");
	}

	@Override
	public void buildSurface(WorldGenRegion var1, StructureFeatureManager var2, ChunkAccess var3) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'buildSurface'");
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion var1) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'spawnOriginalMobs'");
	}

	@Override
	public int getGenDepth() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getGenDepth'");
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Executor var1, Blender var2, StructureFeatureManager var3,
			ChunkAccess var4) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'fillFromNoise'");
	}

	@Override
	public int getSeaLevel() {
		return 64;
	}

	@Override
	public int getMinY() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getMinY'");
	}

	@Override
	public int getBaseHeight(int var1, int var2, Types var3, LevelHeightAccessor var4) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBaseHeight'");
	}

	@Override
	public NoiseColumn getBaseColumn(int var1, int var2, LevelHeightAccessor var3) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'getBaseColumn'");
	}

	@Override
	public void addDebugScreenInfo(List<String> var1, BlockPos var2) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'addDebugScreenInfo'");
	}

}
