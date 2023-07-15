package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ClientboundStationJumpBeginPacket extends ModPacket<ClientGamePacketListener> {

	public int stationId;
	// FIXME: information leak when other stations jump to a new system. we may or
	// may not want this!
	public SystemNodeId target;
	public boolean isJumpInstant;

	public ClientboundStationJumpBeginPacket() {
	}

	public ClientboundStationJumpBeginPacket(int stationId, SystemNodeId target, boolean isJumpInstant) {
		this.stationId = stationId;
		this.target = target;
		this.isJumpInstant = isJumpInstant;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.stationId = buf.readInt();
		this.isJumpInstant = buf.readBoolean();
		this.target = read(buf, NetworkSerializers.SYSTEM_NODE_ID);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeInt(this.stationId);
		buf.writeBoolean(this.isJumpInstant);
		write(buf, this.target, NetworkSerializers.SYSTEM_NODE_ID);
	}

}
