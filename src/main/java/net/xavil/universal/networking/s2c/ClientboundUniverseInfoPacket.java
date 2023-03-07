package net.xavil.universal.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.networking.ModPacket;
import net.xavil.universegen.system.CelestialNode;

public class ClientboundUniverseInfoPacket extends ModPacket<ClientGamePacketListener> {

	public long commonSeed;
	public long uniqueSeed;
	public SystemNodeId startingId;
	public CelestialNode startingSystem;

	@Override
	public void read(FriendlyByteBuf buf) {
		this.commonSeed = buf.readLong();
		this.uniqueSeed = buf.readLong();
		this.startingId = readSystemNodeId(buf);
		this.startingSystem = CelestialNode.readNbt(buf.readNbt());
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeLong(this.commonSeed);
		buf.writeLong(this.uniqueSeed);
		writeSystemNodeId(buf, this.startingId);
		buf.writeNbt(CelestialNode.writeNbt(this.startingSystem));
	}

}
