package net.xavil.ultraviolet.mixin.accessor;

import net.minecraft.client.Minecraft;
import net.xavil.ultraviolet.common.universe.universe.ClientUniverse;

public interface MinecraftClientAccessor {

	ClientUniverse ultraviolet_getUniverse();

	static ClientUniverse getUniverse(Minecraft client) {
		return ((MinecraftClientAccessor) client).ultraviolet_getUniverse();
	}

}
