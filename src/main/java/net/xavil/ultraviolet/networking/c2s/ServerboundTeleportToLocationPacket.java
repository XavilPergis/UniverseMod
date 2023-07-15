package net.xavil.ultraviolet.networking.c2s;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.xavil.ultraviolet.common.universe.Location;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ServerboundTeleportToLocationPacket extends ModPacket<ServerGamePacketListener> {

	public Location location;

	public ServerboundTeleportToLocationPacket() {}

	public ServerboundTeleportToLocationPacket(Location location) {
		this.location = location;
	}

	@Override
	public void read(FriendlyByteBuf buf) {
		this.location = read(buf, NetworkSerializers.LOCATION);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		write(buf, this.location, NetworkSerializers.LOCATION);
	}
	
}
