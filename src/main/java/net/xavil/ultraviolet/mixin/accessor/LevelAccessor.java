package net.xavil.ultraviolet.mixin.accessor;

import net.minecraft.world.level.Level;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.ultraviolet.debug.ConfigProvider;

public interface LevelAccessor {

	Universe ultraviolet_getUniverse();

	void ultraviolet_setUniverse(Universe universe);

	WorldType ultraviolet_getType();

	void ultraviolet_setType(WorldType id);

	ConfigProvider ultraviolet_getConfigProvider();

	void ultraviolet_setConfigProvider(ConfigProvider provider);

	public static Universe getUniverse(Level level) {
		return ((LevelAccessor) level).ultraviolet_getUniverse();
	}

	public static void setUniverse(Level level, Universe universe) {
		((LevelAccessor) level).ultraviolet_setUniverse(universe);
	}

	public static WorldType getWorldType(Level level) {
		return ((LevelAccessor) level).ultraviolet_getType();
	}

	public static void setWorldType(Level level, WorldType id) {
		((LevelAccessor) level).ultraviolet_setType(id);
	}

	public static ConfigProvider getConfigProvider(Level level) {
		return ((LevelAccessor) level).ultraviolet_getConfigProvider();
	}

	public static void setConfigProvider(Level level, ConfigProvider provider) {
		((LevelAccessor) level).ultraviolet_setConfigProvider(provider);
	}

}
