package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.xavil.universal.common.ModSavedData;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universal.mixin.accessor.LevelAccessor;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor {

	private SystemNodeId systemNodeId = null;
	private Universe universe = null;

	@Override
	public SystemNodeId universal_getUniverseId() {
		return this.systemNodeId;
	}

	@Override
	public void universal_setUniverseId(SystemNodeId systemNodeId) {
		this.systemNodeId = systemNodeId;
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
		this.systemNodeId = id;
	}

}
