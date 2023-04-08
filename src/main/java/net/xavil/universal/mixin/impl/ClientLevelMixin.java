package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.mixin.accessor.EntityAccessor;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	private SystemTicket systemTicket;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void setSystemNodeId(CallbackInfo info) {
		final var self = (ClientLevel) (Object) this;
		final var universe = MinecraftClientAccessor.getUniverse(Minecraft.getInstance());
		((LevelAccessor) self).universal_setUniverse(universe);

		// NOTE: all worlds posses a ticket that keeps themselves loaded.
		// final var galaxyTicket = universe.sectorManager
		// 		.createGalaxyTicket(savedData.systemNodeId.system().galaxySector());
		// final var galaxy = universe.sectorManager.forceLoad(this.minecraft.getProfiler(), galaxyTicket);
		// this.systemTicket = galaxy.sectorManager.createSystemTicket(savedData.systemNodeId.system().systemSector());
		// galaxy.sectorManager.forceLoad(this.server.getProfiler(), this.systemTicket);
	}

	@Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("TAIL"))
	private void onEntityAdded(Entity entity, CallbackInfo info) {
		final var self = (ClientLevel) (Object) this;
		EntityAccessor.setSystemNodeId(entity, LevelAccessor.getUniverseId(self));
		EntityAccessor.setUniverse(entity, MinecraftClientAccessor.getUniverse(Minecraft.getInstance()));
	}

}
