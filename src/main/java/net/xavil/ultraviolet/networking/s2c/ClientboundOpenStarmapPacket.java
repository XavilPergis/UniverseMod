package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ClientboundOpenStarmapPacket extends ModPacket<ClientGamePacketListener> {

	public SystemId toOpen;
	// optional, anything less than 0 represents not present.
	public int focusedNode;

	public ClientboundOpenStarmapPacket() {
	}

	public ClientboundOpenStarmapPacket(SystemId toOpen) {
		this.toOpen = toOpen;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.toOpen = read(buf, NetworkSerializers.SYSTEM_ID);
		this.focusedNode = readInt(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		write(buf, this.toOpen, NetworkSerializers.SYSTEM_ID);
		writeInt(buf, this.focusedNode);
	}

}
