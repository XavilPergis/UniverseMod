package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.xavil.universal.common.ModSavedData;
import net.xavil.universal.mixin.accessor.LevelAccessor;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
	
	@Inject(method = "<init>", at = @At("TAIL"))
	private void setSystemNodeId(CallbackInfo info) {
		final var self = (ServerLevel) (Object) this;
		var savedData = self.getDataStorage().get(ModSavedData::load, "universe_id");
		if (savedData != null) {
			((LevelAccessor) self).universal_setUniverseIdRaw(savedData.systemNodeId);
		}
	}

}
