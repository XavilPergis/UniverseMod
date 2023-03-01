package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.xavil.universal.Mod;
import net.xavil.universal.common.ModSavedData;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

	@Shadow
	@Final
	private MinecraftServer server;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void setSystemNodeId(CallbackInfo info) {
		final var self = (ServerLevel) (Object) this;
		var savedData = self.getDataStorage().get(ModSavedData::load, "universe_id");
		if (savedData != null) {
			((LevelAccessor) self).universal_setUniverseIdRaw(savedData.systemNodeId);
			if (!self.dimension().equals(Level.OVERWORLD)) {
				var universe = MinecraftServerAccessor.getUniverse(this.server);
				var systemNode = universe.getSystemNode(savedData.systemNodeId);
				if (systemNode == null) {
					Mod.LOGGER.error("loaded server level with unknown ID " + savedData.systemNodeId);
					((LevelAccessor) self).universal_setUniverseIdRaw(null);
				}
			}
		}
	}

}
