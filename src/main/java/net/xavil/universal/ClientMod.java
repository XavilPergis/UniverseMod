package net.xavil.universal;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.client.screen.GalaxyMapScreen;
import net.xavil.universal.client.screen.SystemMapScreen;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;
import net.xavil.universal.networking.ModNetworking;
import net.xavil.universal.networking.ModPacket;
import net.xavil.universal.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.universal.networking.s2c.ClientboundOpenStarmapPacket;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;

public class ClientMod implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		final var client = Minecraft.getInstance();

		ModNetworking.CLIENTBOUND_PLAY_HANDLER = packet -> client.execute(() -> handlePacket(packet));

		ModRendering.LOAD_SHADERS_EVENT.register(acceptor -> {
			acceptor.accept(ModRendering.PLANET_SHADER, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		});
	}

	public static void handlePacket(ModPacket<ClientGamePacketListener> packetUntyped) {
		final var client = Minecraft.getInstance();

		if (packetUntyped instanceof ClientboundOpenStarmapPacket packet) {
			var universe = MinecraftClientAccessor.getUniverse(client);
			var system = universe.getSystem(packet.toOpen.system());
			var galaxyMap = new GalaxyMapScreen(client.screen, packet.toOpen.system());
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
