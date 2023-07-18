package net.xavil.ultraviolet.mixin.accessor;

import java.util.Map;
import java.util.concurrent.Executor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.xavil.ultraviolet.common.dimension.DynamicDimensionManager;
import net.xavil.ultraviolet.common.universe.universe.ServerUniverse;
import net.xavil.ultraviolet.debug.CommonDebug;

public interface MinecraftServerAccessor {

	DynamicDimensionManager ultraviolet_getDimensionManager();

	CommonDebug ultraviolet_getCommonDebug();

	ServerUniverse ultraviolet_getUniverse();

	WorldData ultraviolet_getWorldData();

	Executor ultraviolet_getExecutor();

	Map<ResourceKey<Level>, ServerLevel> ultraviolet_getLevels();

	LevelStorageSource.LevelStorageAccess ultraviolet_getStorageSource();

	ChunkProgressListenerFactory ultraviolet_getProgressListenerFactory();

	static DynamicDimensionManager getDimensionManager(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).ultraviolet_getDimensionManager();
	}

	static ServerUniverse getUniverse(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).ultraviolet_getUniverse();
	}

	static WorldData getWorldData(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).ultraviolet_getWorldData();
	}

	static Executor getExecutor(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).ultraviolet_getExecutor();
	}

	static Map<ResourceKey<Level>, ServerLevel> getLevels(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).ultraviolet_getLevels();
	}

	static LevelStorageSource.LevelStorageAccess getStorageSource(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).ultraviolet_getStorageSource();
	}

	static ChunkProgressListenerFactory getProgressListenerFactory(MinecraftServer server) {
		return ((MinecraftServerAccessor) server).ultraviolet_getProgressListenerFactory();
	}

}
