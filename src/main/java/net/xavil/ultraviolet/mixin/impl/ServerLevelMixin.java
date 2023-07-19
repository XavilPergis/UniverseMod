package net.xavil.ultraviolet.mixin.impl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.xavil.ultraviolet.common.ModSavedData;
import net.xavil.ultraviolet.debug.ConfigKey;
import net.xavil.ultraviolet.debug.ConfigProvider;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftServerAccessor;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

	@Shadow
	@Final
	private MinecraftServer server;

	// NOTE: i'd like to do this right after super(), but targeting HEAD doesn't
	// automatically shift to after the call to super.
	@Inject(method = "<init>", at = @At("TAIL"))
	private void setConfigProvider(CallbackInfo info) {
		((LevelAccessor) (Object) this).ultraviolet_setConfigProvider(new ConfigProvider() {
			@Override
			public <T> T get(ConfigKey<T> key) {
				return ((MinecraftServerAccessor) server).ultraviolet_getCommonDebug().get(key);
			}
		});
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void loadLocationIfPresent(CallbackInfo info) {
		@SuppressWarnings("resource")
		final var self = (ServerLevel) (Object) this;
		final var savedData = self.getDataStorage().get(ModSavedData::load, "universe_id");
		if (savedData == null)
			return;
		final var universe = MinecraftServerAccessor.getUniverse(this.server);
		((LevelAccessor) self).ultraviolet_setUniverse(universe);
		((LevelAccessor) self).ultraviolet_setLocation(savedData.location);
	}

}
