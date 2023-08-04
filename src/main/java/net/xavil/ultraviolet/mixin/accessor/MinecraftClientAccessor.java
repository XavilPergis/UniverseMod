package net.xavil.ultraviolet.mixin.accessor;

import net.minecraft.client.Minecraft;
import net.xavil.ultraviolet.common.universe.universe.ClientUniverse;

public interface MinecraftClientAccessor {

	static MinecraftClientAccessor CLIENT = (MinecraftClientAccessor) Minecraft.getInstance();

	ClientUniverse ultraviolet_getUniverse();

	void ultraviolet_setUniverse(ClientUniverse universe);

	static ClientUniverse getUniverse() {
		return CLIENT.ultraviolet_getUniverse();
	}

	static void setUniverse(ClientUniverse universe) {
		CLIENT.ultraviolet_setUniverse(universe);
	}

}
