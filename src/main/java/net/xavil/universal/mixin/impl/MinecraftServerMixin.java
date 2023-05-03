package net.xavil.universal.mixin.impl;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.xavil.universal.common.dimension.DynamicDimensionManager;
import net.xavil.universal.common.universe.Location;
import net.xavil.universal.common.universe.universe.ServerUniverse;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements MinecraftServerAccessor {

	private DynamicDimensionManager dynamicDimensionManager = null;
	private ServerUniverse universe = null;

	@Shadow
	private ProfilerFiller profiler;

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

	@Inject(method = "tickChildren", at = @At("TAIL"))
	private void onTick(BooleanSupplier hasTimeLeft, CallbackInfo info) {
		if (this.universe != null)
			this.universe.tick(this.profiler, false);
	}

	@Inject(method = "createLevels", at = @At("HEAD"))
	private void onCreateLevels(ChunkProgressListener listener, CallbackInfo info) {
		this.dynamicDimensionManager = new DynamicDimensionManager((MinecraftServer) (Object) this);
		this.universe = new ServerUniverse((MinecraftServer) (Object) this);
		this.universe.prepare();
	}

	@Override
	public DynamicDimensionManager universal_getDimensionManager() {
		return this.dynamicDimensionManager;
	}

	@Override
	public ServerUniverse universal_getUniverse() {
		return this.universe;
	}

	@Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getDataStorage()Lnet/minecraft/world/level/storage/DimensionDataStorage;"))
	private void prepareStartingVolume(CallbackInfo info) {
		final var overworld = ((MinecraftServer) (Object) this).overworld();
		final var startingId = this.universe.getStartingSystemGenerator().getStartingSystemId();
		((LevelAccessor) overworld).universal_setUniverse(this.universe);
		LevelAccessor.setLocation(overworld, new Location.World(startingId));
	}
}
