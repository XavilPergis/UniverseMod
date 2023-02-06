package net.xavil.universal.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import net.xavil.universal.ClientMod;
import net.xavil.universal.Mod;
import net.xavil.universal.networking.c2s.ServerboundOpenStarmapPacket;
import net.xavil.universal.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.universal.networking.s2c.ClientboundOpenStarmapPacket;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;

public class ModNetworking {

	public static final ResourceLocation CLIENTBOUND_OPEN_STARMAP = Mod.namespaced("clientbound_open_starmap");
	public static final ResourceLocation CLIENTBOUND_UNIVERSE_INFO = Mod.namespaced("clientbound_universe_info");
	public static final ResourceLocation CLIENTBOUND_CHANGE_SYSTEM = Mod.namespaced("clientbound_change_system");

	public static void registerClientside() {
		ClientPlayNetworking.registerGlobalReceiver(CLIENTBOUND_UNIVERSE_INFO,
				(client, handler, buf, responseSender) -> {
					var packet = new ClientboundUniverseInfoPacket();
					packet.read(buf);
					client.execute(() -> ClientMod.handlePacket(packet));
				});
		ClientPlayNetworking.registerGlobalReceiver(CLIENTBOUND_OPEN_STARMAP,
				(client, handler, buf, responseSender) -> {
					var packet = ClientboundOpenStarmapPacket.empty();
					packet.read(buf);
					client.execute(() -> ClientMod.handlePacket(packet));
				});
		ClientPlayNetworking.registerGlobalReceiver(CLIENTBOUND_CHANGE_SYSTEM,
				(client, handler, buf, responseSender) -> {
					var packet = ClientboundChangeSystemPacket.empty();
					packet.read(buf);
					client.execute(() -> ClientMod.handlePacket(packet));
				});
	}

	public static void registerServerside() {
		// ServerPlayNetworking.registerGlobalReceiver(SERVERBOUND_OPEN_STARMAP,
		// ServerboundOpenStarmapPacket::receive);
	}

}
