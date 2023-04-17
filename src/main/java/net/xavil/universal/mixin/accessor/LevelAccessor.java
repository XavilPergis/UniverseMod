package net.xavil.universal.mixin.accessor;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.level.Level;
import net.xavil.universal.common.universe.Location;
import net.xavil.universal.common.universe.universe.Universe;

public interface LevelAccessor {

	Universe universal_getUniverse();

	Location universal_getLocation();

	@ApiStatus.Internal
	void universal_setUniverse(Universe universe);

	@ApiStatus.Internal
	void universal_setLocation(Location id);

	public static Location getLocation(Level level) {
		return ((LevelAccessor) level).universal_getLocation();
	}

	public static Universe getUniverse(Level level) {
		return ((LevelAccessor) level).universal_getUniverse();
	}

	public static void setLocation(Level level, Location id) {
		((LevelAccessor) level).universal_setLocation(id);
	}

}
