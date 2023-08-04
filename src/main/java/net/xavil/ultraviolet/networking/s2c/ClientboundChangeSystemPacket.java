package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ClientboundChangeSystemPacket extends ModPacket<ClientGamePacketListener> {

	public WorldType location;

	public ClientboundChangeSystemPacket() {
	}

	public ClientboundChangeSystemPacket(WorldType location) {
		this.location = location;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.location = read(buf, NetworkSerializers.WORLD_TYPE);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		write(buf, this.location, NetworkSerializers.WORLD_TYPE);
	}

}
