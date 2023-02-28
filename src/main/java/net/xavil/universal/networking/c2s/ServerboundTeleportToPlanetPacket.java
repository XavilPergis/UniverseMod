package net.xavil.universal.networking.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.networking.ModPacket;

public class ServerboundTeleportToPlanetPacket extends ModPacket<ServerGamePacketListener> {

	public SystemNodeId planetId;

	public ServerboundTeleportToPlanetPacket() {}

	public ServerboundTeleportToPlanetPacket(SystemNodeId planetId) {
		this.planetId = planetId;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.planetId = readSystemNodeId(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeSystemNodeId(buf, this.planetId);
	}
	
}
