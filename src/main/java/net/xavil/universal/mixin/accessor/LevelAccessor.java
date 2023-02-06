package net.xavil.universal.mixin.accessor;

import net.minecraft.world.level.Level;
import net.xavil.universal.common.universe.UniverseId;

public interface LevelAccessor {
	
	UniverseId universal_getUniverseId();
	void universal_setUniverseId(UniverseId id);

	public static UniverseId getUniverseId(Level level) {
		return ((LevelAccessor) level).universal_getUniverseId();
	}

	public static void setUniverseId(Level level, UniverseId id) {
		((LevelAccessor) level).universal_setUniverseId(id);
	}

}
