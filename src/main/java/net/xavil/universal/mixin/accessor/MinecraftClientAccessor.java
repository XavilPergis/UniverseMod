package net.xavil.universal.mixin.accessor;

import net.minecraft.client.Minecraft;
import net.xavil.universal.common.universe.universe.ClientUniverse;

public interface MinecraftClientAccessor {

	ClientUniverse universal_getUniverse();

	static ClientUniverse getUniverse(Minecraft client) {
		return ((MinecraftClientAccessor) client).universal_getUniverse();
	}

}
