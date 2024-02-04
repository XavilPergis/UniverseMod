package net.xavil.ultraviolet.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.xavil.hawklib.Disposable;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.PerLevelData;
import net.xavil.ultraviolet.common.config.ConfigProvider;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor {

	private WorldType type = null;
	private Universe universe = null;

	private Disposable.Multi disposer = new Disposable.Multi();
	private SystemTicket systemTicket;
	private ConfigProvider configProvider;

	@Override
	public WorldType ultraviolet_getType() {
		return this.type;
	}

	@Override
	public void ultraviolet_setType(WorldType id) {
		@SuppressWarnings("resource")
		final var self = (Level) (Object) this;
		if (id == null || id.equals(this.type))
			return;
		this.type = id;

		if (this.type instanceof WorldType.SystemNode world) {
			// NOTE: all worlds posses a ticket that keeps themselves loaded.
			try (final var disposer = Disposable.scope()) {
				final var sysId = world.id.system();
				final var galaxy = universe.loadGalaxy(disposer, sysId.universeSector()).unwrap();
				this.systemTicket = galaxy.sectorManager.createSystemTicket(this.disposer, sysId.galaxySector());
				galaxy.sectorManager.forceLoad(this.systemTicket);
				Mod.LOGGER.info("loaded system ticket for Level with id of {}", world.id);
			}
		}
		if (self instanceof ServerLevel serverLevel) {
			final var savedData = PerLevelData.get(serverLevel);
			if (!type.equals(savedData.worldType)) {
				savedData.worldType = type;
				savedData.setDirty();
			}
		}
	}

	@Override
	public Universe ultraviolet_getUniverse() {
		return this.universe;
	}

	@Override
	public void ultraviolet_setUniverse(Universe universe) {
		this.universe = universe;
	}

	@Override
	public ConfigProvider ultraviolet_getConfigProvider() {
		return this.configProvider;
	}

	@Override
	public void ultraviolet_setConfigProvider(ConfigProvider provider) {
		this.configProvider = provider;
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void onClose(CallbackInfo info) {
		this.disposer.close();
	}

}
