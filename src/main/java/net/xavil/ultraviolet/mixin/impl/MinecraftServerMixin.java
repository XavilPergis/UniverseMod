package net.xavil.ultraviolet.mixin.impl;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.config.CommonConfig;
import net.xavil.ultraviolet.common.dimension.DynamicDimensionManager;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.universe.ServerUniverse;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftServerAccessor;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements MinecraftServerAccessor {

	private DynamicDimensionManager dynamicDimensionManager = null;
	private ServerUniverse universe = null;
	private CommonConfig commonDebug = new CommonConfig((MinecraftServer) (Object) this);

	@Shadow
	private ProfilerFiller profiler;

	@Override
	@Accessor("worldData")
	public abstract WorldData ultraviolet_getWorldData();

	@Override
	@Accessor("executor")
	public abstract Executor ultraviolet_getExecutor();

	@Override
	@Accessor("levels")
	public abstract Map<ResourceKey<Level>, ServerLevel> ultraviolet_getLevels();

	@Override
	@Accessor("storageSource")
	public abstract LevelStorageSource.LevelStorageAccess ultraviolet_getStorageSource();

	@Override
	@Accessor("progressListenerFactory")
	public abstract ChunkProgressListenerFactory ultraviolet_getProgressListenerFactory();

	@Inject(method = "tickChildren", at = @At("TAIL"))
	private void onTick(BooleanSupplier hasTimeLeft, CallbackInfo info) {
		if (this.universe != null)
			this.universe.tick(this.profiler, false);
		this.commonDebug.flush();
		if (this.dynamicDimensionManager != null)
			this.dynamicDimensionManager.tick();
	}

	@Inject(method = "createLevels", at = @At("HEAD"))
	private void onCreateLevels(ChunkProgressListener listener, CallbackInfo info) {
		this.dynamicDimensionManager = new DynamicDimensionManager((MinecraftServer) (Object) this);
		this.universe = new ServerUniverse((MinecraftServer) (Object) this);
		this.universe.prepare();
	}

	@Override
	public DynamicDimensionManager ultraviolet_getDimensionManager() {
		return this.dynamicDimensionManager;
	}

	@Override
	public ServerUniverse ultraviolet_getUniverse() {
		return this.universe;
	}

	@Override
	public CommonConfig ultraviolet_getCommonDebug() {
		return this.commonDebug;
	}

	@Inject(method = "createLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getDataStorage()Lnet/minecraft/world/level/storage/DimensionDataStorage;"))
	private void prepareStartingVolume(CallbackInfo info) {
		final var overworld = ((MinecraftServer) (Object) this).overworld();
		final var startingId = this.universe.getStartingSystemGenerator().getStartingSystemId();
		LevelAccessor.setUniverse(overworld, this.universe);
		LevelAccessor.setWorldType(overworld, new WorldType.SystemNode(startingId));
	}

	@Inject(method = "getLevel", at = @At("HEAD"), cancellable = true)
	private void loadDynamicLevelIfNeeded(ResourceKey<Level> dimension, CallbackInfoReturnable<ServerLevel> info) {
		if (!ultraviolet_getLevels().containsKey(dimension)) {
			// NOTE: this calls into DynamicDimensionManager, which will insder the new
			// level into the server's level list, but to be ase, we will also do it here.
			final var level = Mod.loadDynamicLevel((MinecraftServer) (Object) this, dimension);
			ultraviolet_getLevels().put(dimension, level);
			info.setReturnValue(level);
		}
	}
}
