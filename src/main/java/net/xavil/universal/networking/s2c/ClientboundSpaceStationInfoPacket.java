package net.xavil.universal.networking.s2c;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.xavil.universal.common.universe.station.StationLocation;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universal.networking.ModPacket;
import net.xavil.util.math.Quat;

public class ClientboundSpaceStationInfoPacket extends ModPacket<ClientGamePacketListener> {

	public int id;
	public String name;
	public Quat orientation;
	public CompoundTag locationNbt;

	@Override
	public void read(FriendlyByteBuf buf) {
		this.id = buf.readInt();
		this.name = buf.readUtf();
		final var w = buf.readDouble();
		final var i = buf.readDouble();
		final var j = buf.readDouble();
		final var k = buf.readDouble();
		this.orientation = Quat.from(w, i, j, k);
		this.locationNbt = buf.readNbt();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeInt(this.id);
		buf.writeUtf(this.name);
		buf.writeDouble(this.orientation.w);
		buf.writeDouble(this.orientation.i);
		buf.writeDouble(this.orientation.j);
		buf.writeDouble(this.orientation.k);
		buf.writeNbt(this.locationNbt);
	}

	public StationLocation getLocation(Universe universe) {
		return StationLocation.fromNbt(universe, this.locationNbt);
	}
	
}
