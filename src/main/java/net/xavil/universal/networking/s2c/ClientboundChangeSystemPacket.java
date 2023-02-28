package net.xavil.universal.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.networking.ModPacket;

public class ClientboundChangeSystemPacket extends ModPacket<ClientGamePacketListener> {

	public SystemNodeId id;

	public ClientboundChangeSystemPacket() {
	}

	public ClientboundChangeSystemPacket(SystemNodeId id) {
		this.id = id;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.id = readSystemNodeId(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeSystemNodeId(buf, this.id);
	}

}
