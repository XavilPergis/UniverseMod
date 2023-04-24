package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.xavil.universal.Mod;
import net.xavil.universal.common.ModSavedData;
import net.xavil.universal.common.universe.Location;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.util.Disposable;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor {

	private Location location = null;
	private Universe universe = null;

	private Disposable.Multi disposer = new Disposable.Multi();
	private SystemTicket systemTicket;

	@Override
	public Location universal_getLocation() {
		return this.location;
	}

	@Override
	public void universal_setLocation(Location id) {
		final var self = (Level) (Object) this;
		if (id == null || id.equals(this.location))
			return;
		this.location = id;

		if (this.location instanceof Location.World world) {
			// NOTE: all worlds posses a ticket that keeps themselves loaded.
			Disposable.scope(disposer -> {
				final var sysId = world.id.system();
				final var galaxy = universe.loadGalaxy(disposer, sysId.universeSector()).unwrap();
				this.systemTicket = galaxy.sectorManager.createSystemTicket(this.disposer, sysId.galaxySector());
				galaxy.sectorManager.forceLoad(this.systemTicket);
				Mod.LOGGER.info("loaded system ticket for Level with id of {}", world.id);
			});
		}
		if (self instanceof ServerLevel serverLevel) {
			final var savedData = serverLevel.getDataStorage()
					.computeIfAbsent(ModSavedData::load, () -> new ModSavedData(location), "universe_id");
			if (!location.equals(savedData.location)) {
				savedData.location = location;
				savedData.setDirty(true);
			}
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

	@Inject(method = "close", at = @At("TAIL"))
	private void onClose(CallbackInfo info) {
		this.disposer.dispose();
	}

}
