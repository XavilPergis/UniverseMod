package net.xavil.universal;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.xavil.universal.client.screen.GalaxyMapScreen;
import net.xavil.universal.client.screen.SystemMapScreen;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universal.networking.ModClientNetworking;
import net.xavil.universal.networking.ModPacket;
import net.xavil.universal.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.universal.networking.s2c.ClientboundOpenStarmapPacket;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;

public class ClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ModClientNetworking.register();
	}

	public static void handlePacket(ModPacket packetUntyped) {
		final var client = Minecraft.getInstance();

		if (packetUntyped instanceof ClientboundOpenStarmapPacket packet) {
			var universe = MinecraftClientAccessor.getUniverse(client);
			var system = universe.getSystem(packet.toOpen.systemId());
			var galaxyMap = new GalaxyMapScreen(client.screen, packet.toOpen.systemId());
			var systemMap = new SystemMapScreen(galaxyMap, packet.toOpen, system);
			client.setScreen(systemMap);
		} else if (packetUntyped instanceof ClientboundUniverseInfoPacket packet) {
			var universe = MinecraftClientAccessor.getUniverse(client);
			universe.updateFromInfoPacket(packet);
		} else if (packetUntyped instanceof ClientboundChangeSystemPacket packet) {
			LevelAccessor.setUniverseId(client.level, packet.id);
		}
	}

}
