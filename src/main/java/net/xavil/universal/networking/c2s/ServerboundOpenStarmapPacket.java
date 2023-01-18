package net.xavil.universal.networking.c2s;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class ServerboundOpenStarmapPacket {
	public static void receive(MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler,
			FriendlyByteBuf buf, PacketSender responseSender) {
	}

}
