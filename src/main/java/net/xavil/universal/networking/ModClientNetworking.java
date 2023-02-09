package net.xavil.universal.networking;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.xavil.universal.ClientMod;
import net.xavil.universal.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.universal.networking.s2c.ClientboundOpenStarmapPacket;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;

public class ModClientNetworking {

	public static void send(ModPacket packet) {
		var buf = PacketByteBufs.create();
		packet.write(buf);
		ClientPlayNetworking.send(packet.getChannelName(), buf);
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ClientboundUniverseInfoPacket.CHANNEL,
				(client, handler, buf, responseSender) -> {
					var packet = new ClientboundUniverseInfoPacket();
					packet.read(buf);
					client.execute(() -> ClientMod.handlePacket(packet));
				});
		ClientPlayNetworking.registerGlobalReceiver(ClientboundOpenStarmapPacket.CHANNEL,
				(client, handler, buf, responseSender) -> {
					var packet = ClientboundOpenStarmapPacket.empty();
					packet.read(buf);
					client.execute(() -> ClientMod.handlePacket(packet));
				});
		ClientPlayNetworking.registerGlobalReceiver(ClientboundChangeSystemPacket.CHANNEL,
				(client, handler, buf, responseSender) -> {
					var packet = ClientboundChangeSystemPacket.empty();
					packet.read(buf);
					client.execute(() -> ClientMod.handlePacket(packet));
				});
	}

}
