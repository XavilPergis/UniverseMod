package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.xavil.universal.Mod;
import net.xavil.universal.common.ModSavedData;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.util.Disposable;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor {

	private SystemNodeId systemNodeId = null;
	private Universe universe = null;

	private Disposable.Multi disposer = new Disposable.Multi();
	private SystemTicket systemTicket;

	@Override
	public SystemNodeId universal_getUniverseId() {
		return this.systemNodeId;
	}

	@Override
	public void universal_setUniverseId(SystemNodeId systemNodeId) {
		universal_setUniverseIdRaw(systemNodeId);
		if ((Level) (Object) this instanceof ServerLevel serverLevel) {
			var savedData = serverLevel.getDataStorage()
					.computeIfAbsent(ModSavedData::load, () -> new ModSavedData(systemNodeId), "universe_id");
			savedData.systemNodeId = systemNodeId;
			savedData.setDirty(true);
		}
	}

	@Override
	public Universe universal_getUniverse() {
		return this.universe;
	}

	@Override
	public void universal_setUniverse(Universe universe) {
		this.universe = universe;
	}

	@Override
	public void universal_setUniverseIdRaw(SystemNodeId id) {
		if (id.equals(this.systemNodeId))
			return;
		this.systemNodeId = id;

		// NOTE: all worlds posses a ticket that keeps themselves loaded.
		Disposable.scope(disposer -> {
			final var galaxyTicket = universe.sectorManager.createGalaxyTicket(disposer,
					this.systemNodeId.system().galaxySector());
			final var galaxy = universe.sectorManager.forceLoad(galaxyTicket).unwrap();
			this.systemTicket = galaxy.sectorManager.createSystemTicket(this.disposer,
					this.systemNodeId.system().systemSector());
			galaxy.sectorManager.forceLoad(this.systemTicket);
			Mod.LOGGER.info("loaded system ticket for wLevel with id of {}", id);
		});
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void onClose(CallbackInfo info) {
		this.disposer.dispose();
	}

}
