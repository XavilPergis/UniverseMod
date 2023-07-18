package net.xavil.ultraviolet.networking.c2s;

import javax.annotation.Nullable;

import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ServerboundDebugValueSetPacket extends ModPacket<ServerGamePacketListener> {

	public String name;
	@Nullable
	public Tag nbt;
	public int syncId;

	public ServerboundDebugValueSetPacket() {
	}

	public ServerboundDebugValueSetPacket(String name, Tag nbt, int syncId) {
		this.name = name;
		this.nbt = nbt;
		this.syncId = syncId;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.name = read(buf, NetworkSerializers.UTF);
		this.nbt = read(buf, NetworkSerializers.NBT);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		write(buf, this.name, NetworkSerializers.UTF);
		write(buf, this.nbt, NetworkSerializers.NBT);
	}

}
