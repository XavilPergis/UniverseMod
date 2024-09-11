package net.xavil.ultraviolet.networking.s2c;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.hawklib.math.Quat;
import net.xavil.ultraviolet.common.universe.id.UniversePosition;
import net.xavil.ultraviolet.networking.ModPacket;
import net.xavil.ultraviolet.networking.NetworkSerializers;

public class ClientboundSpaceStationInfoPacket extends ModPacket<ClientGamePacketListener> {

	public int id;
	public String name;
	public UniversePosition position;
	public Quat orientation;

	@Override
	public void read(FriendlyByteBuf buf) {
		this.id = readInt(buf);
		this.name = read(buf, NetworkSerializers.UTF);
		this.position = read(buf, NetworkSerializers.UNIVERSE_POSITION);
		this.orientation = read(buf, NetworkSerializers.QUAT);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		writeInt(buf, this.id);
		write(buf, this.name, NetworkSerializers.UTF);
		write(buf, this.position, NetworkSerializers.UNIVERSE_POSITION);
		write(buf, this.orientation, NetworkSerializers.QUAT);
	}

}
