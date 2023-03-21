package net.xavil.universal.mixin.accessor;

import net.minecraft.world.entity.Entity;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.universe.Universe;

public interface EntityAccessor {
	
	Universe universal_getUniverse();
	void universal_setUniverse(Universe universe);
	SystemNodeId universal_getSystemNodeId();
	void universal_setSystemNodeId(SystemNodeId id);

	static Universe getUniverse(Entity entity) {
		return ((EntityAccessor) entity).universal_getUniverse();
	}

	static void setUniverse(Entity entity, Universe universe) {
		((EntityAccessor) entity).universal_setUniverse(universe);
	}

	static SystemNodeId getSystemNodeId(Entity entity) {
		return ((EntityAccessor) entity).universal_getSystemNodeId();
	}

	static void setSystemNodeId(Entity entity, SystemNodeId id) {
		((EntityAccessor) entity).universal_setSystemNodeId(id);
	}

}
