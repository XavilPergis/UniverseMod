package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.ultraviolet.common.universe.Location;
import net.xavil.ultraviolet.networking.ModPacket;

public class ClientboundChangeSystemPacket extends ModPacket<ClientGamePacketListener> {

	public Location location;

	public ClientboundChangeSystemPacket() {
	}

	public ClientboundChangeSystemPacket(Location location) {
		this.location = location;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.location = readLocation(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeLocation(buf, this.location);
	}

}
