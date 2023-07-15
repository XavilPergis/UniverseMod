package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ClientboundOpenStarmapPacket extends ModPacket<ClientGamePacketListener> {

	public SystemNodeId toOpen;

	public ClientboundOpenStarmapPacket() {
	}

	public ClientboundOpenStarmapPacket(SystemNodeId toOpen) {
		this.toOpen = toOpen;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.toOpen = read(buf, NetworkSerializers.SYSTEM_NODE_ID);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		write(buf, this.toOpen, NetworkSerializers.SYSTEM_NODE_ID);
	}

}
