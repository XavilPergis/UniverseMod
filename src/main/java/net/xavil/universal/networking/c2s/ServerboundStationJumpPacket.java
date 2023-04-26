package net.xavil.universal.networking.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.networking.ModPacket;

public class ServerboundStationJumpPacket extends ModPacket<ServerGamePacketListener> {

	public int stationId;
	public SystemNodeId target;
	public boolean isJumpInstant;

	public ServerboundStationJumpPacket() {}

	public ServerboundStationJumpPacket(int stationId, SystemNodeId target, boolean isJumpInstant) {
		this.target = target;
		this.stationId = stationId;
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