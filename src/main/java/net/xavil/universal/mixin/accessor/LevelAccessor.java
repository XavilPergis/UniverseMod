package net.xavil.universal.mixin.accessor;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.level.Level;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.universe.Universe;

public interface LevelAccessor {

	Universe universal_getUniverse();

	SystemNodeId universal_getUniverseId();

	@ApiStatus.Internal
	void universal_setUniverse(Universe universe);

	@ApiStatus.Internal
	void universal_setUniverseId(SystemNodeId id);

	@ApiStatus.Internal
	void universal_setUniverseIdRaw(SystemNodeId id);

	public static SystemNodeId getUniverseId(Level level) {
		return ((LevelAccessor) level).universal_getUniverseId();
	}

	public static void setUniverseId(Level level, SystemNodeId id) {
		((LevelAccessor) level).universal_setUniverseId(id);
	}

}
