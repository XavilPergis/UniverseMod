package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.Level;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.mixin.accessor.LevelAccessor;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor {

	private SystemNodeId universeId;

	@Override
	public SystemNodeId universal_getUniverseId() {
		return this.universeId;
	}
	
	@Override
	public void universal_setUniverseId(SystemNodeId universeId) {
		this.universeId = universeId;
	}

}
