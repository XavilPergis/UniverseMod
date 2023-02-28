package net.xavil.universal.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.networking.ModPacket;

public class ClientboundOpenStarmapPacket extends ModPacket<ClientGamePacketListener> {

	public SystemNodeId toOpen;

	public ClientboundOpenStarmapPacket() {
	}

	public ClientboundOpenStarmapPacket(SystemNodeId toOpen) {
		this.toOpen = toOpen;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.toOpen = readSystemNodeId(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeSystemNodeId(buf, this.toOpen);
	}

}
