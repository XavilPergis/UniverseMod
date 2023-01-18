package net.xavil.universal.common.dimension;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.dimension.LevelStem;

public record DimensionCreationProperties(LevelStem levelStem, List<CustomSpawner> customSpawners,
		boolean shouldTickTime, @Nullable ChunkProgressListener progressListener) {

	public static DimensionCreationProperties basic(LevelStem levelStem) {
		return new DimensionCreationProperties(levelStem, List.of(), false, null);
	}

}
