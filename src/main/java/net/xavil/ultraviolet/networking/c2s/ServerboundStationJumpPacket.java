package net.xavil.ultraviolet.networking.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

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
		this.stationId = readInt(buf);
		this.isJumpInstant = readBoolean(buf);
		this.target = read(buf, NetworkSerializers.SYSTEM_NODE_ID);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeInt(buf, this.stationId);
		writeBoolean(buf, this.isJumpInstant);
		write(buf, this.target, NetworkSerializers.SYSTEM_NODE_ID);
	}
	
}