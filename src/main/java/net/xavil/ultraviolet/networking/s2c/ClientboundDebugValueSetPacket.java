package net.xavil.ultraviolet.networking.s2c;

import javax.annotation.Nullable;

import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ClientboundDebugValueSetPacket extends ModPacket<ClientGamePacketListener> {

	public String keyId;
	@Nullable
	public Tag nbt;
	public int syncId;

	public ClientboundDebugValueSetPacket() {
	}

	public ClientboundDebugValueSetPacket(String keyId, Tag nbt, int syncId) {
		this.keyId = keyId;
		this.nbt = nbt;
		this.syncId = syncId;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.keyId = read(buf, NetworkSerializers.UTF);
		this.nbt = read(buf, NetworkSerializers.NBT);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		write(buf, this.keyId, NetworkSerializers.UTF);
		write(buf, this.nbt, NetworkSerializers.NBT);
	}

}
