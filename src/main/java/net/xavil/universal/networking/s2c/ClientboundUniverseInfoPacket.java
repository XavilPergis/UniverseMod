package net.xavil.universal.networking.s2c;

import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.networking.ModPacket;

public class ClientboundUniverseInfoPacket extends ModPacket {

	public long commonSeed;
	public long uniqueSeed;
	public UniverseId.SectorId startingGalaxyId;
	public UniverseId.SectorId startingSystemId;

	@Override
	public void read(FriendlyByteBuf buf) {
		var gx = buf.readInt();
		var gy = buf.readInt();
		var gz = buf.readInt();
		var gid = buf.readInt();
		this.startingGalaxyId = new UniverseId.SectorId(new Vec3i(gx, gy, gz), gid);
		var sx = buf.readInt();
		var sy = buf.readInt();
		var sz = buf.readInt();
		var sid = buf.readInt();
		this.startingSystemId = new UniverseId.SectorId(new Vec3i(sx, sy, sz), sid);
		this.commonSeed = buf.readLong();
		this.uniqueSeed = buf.readLong();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeInt(this.startingGalaxyId.sectorPos().getX());
		buf.writeInt(this.startingGalaxyId.sectorPos().getY());
		buf.writeInt(this.startingGalaxyId.sectorPos().getZ());
		buf.writeInt(this.startingGalaxyId.sectorId());
		buf.writeInt(this.startingSystemId.sectorPos().getX());
		buf.writeInt(this.startingSystemId.sectorPos().getY());
		buf.writeInt(this.startingSystemId.sectorPos().getZ());
		buf.writeInt(this.startingSystemId.sectorId());
		buf.writeLong(this.commonSeed);
		buf.writeLong(this.uniqueSeed);
	}

}
