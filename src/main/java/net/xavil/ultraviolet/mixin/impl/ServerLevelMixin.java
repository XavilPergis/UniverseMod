package net.xavil.ultraviolet.mixin.impl;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.xavil.ultraviolet.common.PerLevelData;
import net.xavil.ultraviolet.common.config.ConfigKey;
import net.xavil.ultraviolet.common.config.ConfigProvider;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftServerAccessor;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {

	protected ServerLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey,
			Holder<DimensionType> holder, Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l) {
		super(writableLevelData, resourceKey, holder, supplier, bl, bl2, l);
		throw new IllegalStateException("unreachable: mixin consturctor");
	}

	@Shadow
	@Final
	private MinecraftServer server;

	// NOTE: i'd like to do this right after super(), but targeting HEAD doesn't
	// automatically shift to after the call to super.
	@Inject(method = "<init>", at = @At("TAIL"))
	private void setConfigProvider(CallbackInfo info) {
		LevelAccessor.setConfigProvider(this, new ConfigProvider() {
			@Override
			public <T> T get(ConfigKey<T> key) {
				return ((MinecraftServerAccessor) server).ultraviolet_getCommonDebug().get(key);
			}
		});
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void loadLocationIfPresent(CallbackInfo info) {
		final var self = (ServerLevel) (Object) this;
		final var savedData = PerLevelData.get(self);
		final var universe = MinecraftServerAccessor.getUniverse(this.server);
		LevelAccessor.setUniverse(self, universe);
		LevelAccessor.setWorldType(self, savedData.worldType);
	}

}
