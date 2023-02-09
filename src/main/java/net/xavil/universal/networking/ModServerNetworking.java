package net.xavil.universal.networking;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.xavil.universal.Mod;
import net.xavil.universal.networking.c2s.ServerboundTeleportToPlanetPacket;

public class ModServerNetworking {

	public static void send(ServerPlayer player, ModPacket packet) {
		var buf = PacketByteBufs.create();
		packet.write(buf);
		ServerPlayNetworking.send(player, packet.getChannelName(), buf);
	}

	public static void register() {
		ServerPlayNetworking.registerGlobalReceiver(ServerboundTeleportToPlanetPacket.CHANNEL,
				(server, player, handler, buf, responseSender) -> {
					var packet = new ServerboundTeleportToPlanetPacket();
					packet.read(buf);
					server.execute(() -> Mod.handlePacket(server, player, packet));
				});

	}

}
