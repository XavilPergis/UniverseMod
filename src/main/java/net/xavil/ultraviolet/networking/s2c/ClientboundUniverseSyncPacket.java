package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ClientboundUniverseSyncPacket extends ModPacket<ClientGamePacketListener> {

	public long commonSeed;
	public long uniqueSeed;
	public double startingSystemAge;
	public String startingSystemName;
	public SystemNodeId startingId;
	public CompoundTag startingSystemNbt;
	public WorldType worldType;

	@Override
	public void read(FriendlyByteBuf buf) {
		this.commonSeed = readLong(buf);
		this.uniqueSeed = readLong(buf);
		this.startingSystemAge = readDouble(buf);
		this.startingSystemName = read(buf, NetworkSerializers.UTF);
		this.startingId = read(buf, NetworkSerializers.SYSTEM_NODE_ID);
		this.startingSystemNbt = read(buf, NetworkSerializers.NBT_COMPOUND);
		this.worldType = read(buf, NetworkSerializers.WORLD_TYPE);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeLong(buf, this.commonSeed);
		writeLong(buf, this.uniqueSeed);
		writeDouble(buf, this.startingSystemAge);
		write(buf, this.startingSystemName, NetworkSerializers.UTF);
		write(buf, this.startingId, NetworkSerializers.SYSTEM_NODE_ID);
		write(buf, this.startingSystemNbt, NetworkSerializers.NBT_COMPOUND);
		write(buf, this.worldType, NetworkSerializers.WORLD_TYPE);
	}

}
