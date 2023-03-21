package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.Entity;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universal.mixin.accessor.EntityAccessor;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityAccessor {

	private Universe universe;
	private SystemNodeId currentSystemNode;

	@Override
	public SystemNodeId universal_getSystemNodeId() {
		return this.currentSystemNode;
	}

	@Override
	public void universal_setSystemNodeId(SystemNodeId id) {
		this.currentSystemNode = id;
	}
	@Override
	public Universe universal_getUniverse() {
		return this.universe;
	}

	@Override
	public void universal_setUniverse(Universe universe) {
		this.universe = universe;
	}
}
