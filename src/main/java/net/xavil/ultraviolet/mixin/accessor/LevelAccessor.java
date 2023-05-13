package net.xavil.ultraviolet.mixin.accessor;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.world.level.Level;
import net.xavil.ultraviolet.common.universe.Location;
import net.xavil.ultraviolet.common.universe.universe.Universe;

public interface LevelAccessor {

	Universe ultraviolet_getUniverse();

	Location ultraviolet_getLocation();

	@ApiStatus.Internal
	void ultraviolet_setUniverse(Universe universe);

	@ApiStatus.Internal
	void ultraviolet_setLocation(Location id);

	public static Location getLocation(Level level) {
		return ((LevelAccessor) level).ultraviolet_getLocation();
	}

	public static Universe getUniverse(Level level) {
		return ((LevelAccessor) level).ultraviolet_getUniverse();
	}

	public static void setLocation(Level level, Location id) {
		((LevelAccessor) level).ultraviolet_setLocation(id);
	}

}
