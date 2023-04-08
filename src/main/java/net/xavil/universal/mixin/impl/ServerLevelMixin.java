package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.xavil.universal.Mod;
import net.xavil.universal.common.ModSavedData;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.mixin.accessor.EntityAccessor;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;
import net.xavil.util.Disposable;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

	@Shadow
	@Final
	private MinecraftServer server;

	private Disposable.Multi disposer = new Disposable.Multi();
	private SystemTicket systemTicket;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void setSystemNodeId(CallbackInfo info) {
		final var self = (ServerLevel) (Object) this;
		final var savedData = self.getDataStorage().get(ModSavedData::load, "universe_id");
		if (savedData == null)
			return;
		final var universe = MinecraftServerAccessor.getUniverse(this.server);
		((LevelAccessor) self).universal_setUniverse(universe);
		((LevelAccessor) self).universal_setUniverseIdRaw(savedData.systemNodeId);
		if (!self.dimension().equals(Level.OVERWORLD)) {
			// var systemNode = universe.getSystemNode(savedData.systemNodeId);
			// if (systemNode == null) {
			// Mod.LOGGER.error("loaded server level with unknown ID " +
			// savedData.systemNodeId);
			// ((LevelAccessor) self).universal_setUniverseIdRaw(null);
			// }
		}

		// NOTE: all worlds posses a ticket that keeps themselves loaded.
		Disposable.scope(disposer -> {
			final var galaxyTicket = universe.sectorManager.createGalaxyTicket(disposer, savedData.systemNodeId.system().galaxySector());
			final var galaxy = universe.sectorManager.forceLoad(galaxyTicket).unwrap();
			this.systemTicket = galaxy.sectorManager.createSystemTicket(this.disposer, savedData.systemNodeId.system().systemSector());
			galaxy.sectorManager.forceLoad(this.systemTicket);
		});
	}

	@Inject(method = "tickNonPassenger(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
	private void setSystemNodeId(Entity entity, CallbackInfo info) {
		final var self = (ServerLevel) (Object) this;
		EntityAccessor.setSystemNodeId(entity, LevelAccessor.getUniverseId(self));
		EntityAccessor.setUniverse(entity, MinecraftServerAccessor.getUniverse(this.server));
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void onClose(CallbackInfo info) {
		this.disposer.dispose();
	}

}
