package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.xavil.universal.mixin.accessor.EntityAccessor;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
	private void onEntityAdded(Entity entity, CallbackInfo info) {
		final var self = (ClientLevel) (Object) this;
		EntityAccessor.setSystemNodeId(entity, LevelAccessor.getUniverseId(self));
		EntityAccessor.setUniverse(entity, MinecraftClientAccessor.getUniverse(Minecraft.getInstance()));
	}
}
