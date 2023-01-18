package net.xavil.universal;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.xavil.universal.client.screen.StarmapScreen;
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
			client.setScreen(new StarmapScreen());
		} else if (packet instanceof ClientboundUniverseInfoPacket infoPacket) {
			var universe = MinecraftClientAccessor.getUniverse(client);
			universe.updateFromInfoPacket(infoPacket);
		}
	}
	
}
