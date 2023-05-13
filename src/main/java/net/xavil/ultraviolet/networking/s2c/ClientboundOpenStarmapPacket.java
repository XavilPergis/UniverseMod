package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.networking.ModPacket;

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
