package net.xavil.universal.mixin.impl;

import java.util.Map;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.xavil.universal.common.dimension.DynamicDimensionManager;
import net.xavil.universal.common.universe.universe.ServerUniverse;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements MinecraftServerAccessor {

	private DynamicDimensionManager dynamicDimensionManager = null;
	private ServerUniverse universe = null;

	@Override
	@Accessor("worldData")
	public abstract WorldData universal_getWorldData();

	@Override
	@Accessor("executor")
	public abstract Executor universal_getExecutor();

	@Override
	@Accessor("levels")
	public abstract Map<ResourceKey<Level>, ServerLevel> universal_getLevels();

	@Override
	@Accessor("storageSource")
	public abstract LevelStorageSource.LevelStorageAccess universal_getStorageSource();

	@Override
	@Accessor("progressListenerFactory")
	public abstract ChunkProgressListenerFactory universal_getProgressListenerFactory();

	@Inject(method = "createLevels", at = @At("HEAD"))
	private void onCreateLevels(ChunkProgressListener listener, CallbackInfo info) {
		this.dynamicDimensionManager = new DynamicDimensionManager((MinecraftServer) (Object) this);
		this.universe = new ServerUniverse((MinecraftServer) (Object) this);
	}

	@Override
	public DynamicDimensionManager universal_getDimensionManager() {
		return this.dynamicDimensionManager;
	}

	@Override
	public ServerUniverse universal_getUniverse() {
		return this.universe;
	}

	@Inject(method = "loadLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;createLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V", shift = At.Shift.AFTER))
	private void prepareStartingVolume(CallbackInfo info) {
		this.universe.prepare();
		var overworld = ((MinecraftServer) (Object) this).overworld();
		var startingId = this.universe.getStartingSystemGenerator().getStartingSystemId();
		LevelAccessor.setUniverseId(overworld, startingId);
	}
}
