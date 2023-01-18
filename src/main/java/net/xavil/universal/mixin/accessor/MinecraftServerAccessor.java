package net.xavil.universal.mixin.accessor;

import java.util.Map;
import java.util.concurrent.Executor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.xavil.universal.common.dimension.DynamicDimensionManager;
import net.xavil.universal.common.universe.universe.ServerUniverse;

public interface MinecraftServerAccessor {

	DynamicDimensionManager universal_getDimensionManager();

	ServerUniverse universal_getUniverse();

	WorldData universal_getWorldData();

	Executor universal_getExecutor();

	Map<ResourceKey<Level>, ServerLevel> universal_getLevels();

	LevelStorageSource.LevelStorageAccess universal_getStorageSource();

	ChunkProgressListenerFactory universal_getProgressListenerFactory();

	static DynamicDimensionManager getDimensionManager(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).universal_getDimensionManager();
	}

	static ServerUniverse getUniverse(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).universal_getUniverse();
	}

	static WorldData getWorldData(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).universal_getWorldData();
	}

	static Executor getExecutor(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).universal_getExecutor();
	}

	static Map<ResourceKey<Level>, ServerLevel> getLevels(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).universal_getLevels();
	}

	static LevelStorageSource.LevelStorageAccess getStorageSource(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).universal_getStorageSource();
	}

	static ChunkProgressListenerFactory getProgressListenerFactory(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).universal_getProgressListenerFactory();
	}

}
