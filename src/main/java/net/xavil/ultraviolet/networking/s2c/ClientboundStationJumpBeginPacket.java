package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.networking.ModPacket;

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
		this.target = readSystemNodeId(buf);
		this.isJumpInstant = buf.readBoolean();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeInt(this.stationId);
		writeSystemNodeId(buf, this.target);
		buf.writeBoolean(this.isJumpInstant);
	}

}
