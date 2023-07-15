package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ClientboundUniverseInfoPacket extends ModPacket<ClientGamePacketListener> {

	public long commonSeed;
	public long uniqueSeed;
	public SystemNodeId startingId;
	public CompoundTag startingSystemNbt;

	@Override
	public void read(FriendlyByteBuf buf) {
		this.commonSeed = readLong(buf);
		this.uniqueSeed = readLong(buf);
		this.startingId = read(buf, NetworkSerializers.SYSTEM_NODE_ID);
		this.startingSystemNbt = read(buf, NetworkSerializers.NBT);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeLong(buf, this.commonSeed);
		writeLong(buf, this.uniqueSeed);
		write(buf, this.startingId, NetworkSerializers.SYSTEM_NODE_ID);
		write(buf, this.startingSystemNbt, NetworkSerializers.NBT);
	}

}
