package net.xavil.universal;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.xavil.universal.client.screen.GalaxyMapScreen;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universal.networking.ModNetworking;
import net.xavil.universal.networking.ModPacket;
import net.xavil.universal.networking.s2c.ClientboundOpenStarmapPacket;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;

public class ClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ModNetworking.registerClientside();
	}

	public static void handlePacket(ModPacket packet) {
		final var client = Minecraft.getInstance();

		if (packet instanceof ClientboundOpenStarmapPacket starmapPacket) {
			var universe = MinecraftClientAccessor.getUniverse(client);
			var id = new UniverseId.SystemId(universe.getStartingGalaxyId(), universe.getStartingSystemId());
			client.setScreen(new GalaxyMapScreen(client.screen, id));
		} else if (packet instanceof ClientboundUniverseInfoPacket infoPacket) {
			var universe = MinecraftClientAccessor.getUniverse(client);
			universe.updateFromInfoPacket(infoPacket);
		}
	}

}
