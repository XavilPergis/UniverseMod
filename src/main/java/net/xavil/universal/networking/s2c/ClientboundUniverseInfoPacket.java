package net.xavil.universal.networking.s2c;

import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.networking.ModPacket;

public class ClientboundUniverseInfoPacket extends ModPacket {

	public long commonSeed;
	public long uniqueSeed;
	public UniverseId.SectorId startingGalaxyId;

	@Override
	public void read(FriendlyByteBuf buf) {
		buf.writeInt(this.startingGalaxyId.sectorPos().getX());
		buf.writeInt(this.startingGalaxyId.sectorPos().getY());
		buf.writeInt(this.startingGalaxyId.sectorPos().getZ());
		buf.writeInt(this.startingGalaxyId.sectorId());
		buf.writeLong(this.commonSeed);
		buf.writeLong(this.uniqueSeed);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		var x = buf.readInt();
		var y = buf.readInt();
		var z = buf.readInt();
		var id = buf.readInt();
		this.startingGalaxyId = new UniverseId.SectorId(new Vec3i(x, y, z), id);
		this.commonSeed = buf.readLong();
		this.uniqueSeed = buf.readLong();
	}

}
