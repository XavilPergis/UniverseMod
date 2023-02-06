package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.Level;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.mixin.accessor.LevelAccessor;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAccessor {

	private UniverseId universeId;

	@Override
	public UniverseId universal_getUniverseId() {
		return this.universeId;
	}
	
	@Override
	public void universal_setUniverseId(UniverseId universeId) {
		this.universeId = universeId;
	}

}
