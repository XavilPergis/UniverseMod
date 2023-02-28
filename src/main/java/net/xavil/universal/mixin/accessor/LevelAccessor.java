package net.xavil.universal.mixin.accessor;

import net.minecraft.world.level.Level;
import net.xavil.universal.common.universe.id.SystemNodeId;

public interface LevelAccessor {
	
	SystemNodeId universal_getUniverseId();
	void universal_setUniverseId(SystemNodeId id);
	void universal_setUniverseIdRaw(SystemNodeId id);

	public static SystemNodeId getUniverseId(Level level) {
		return ((LevelAccessor) level).universal_getUniverseId();
	}

	public static void setUniverseId(Level level, SystemNodeId id) {
		((LevelAccessor) level).universal_setUniverseId(id);
	}

}
