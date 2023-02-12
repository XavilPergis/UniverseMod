package net.xavil.universal.networking;

import net.minecraft.core.Vec3i;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.xavil.universal.common.universe.UniverseId;

public abstract class ModPacket {

	public abstract ResourceLocation getChannelName();

	public abstract void read(FriendlyByteBuf buf);

	public abstract void write(FriendlyByteBuf buf);

	public static Vec3i readVector(FriendlyByteBuf buf) {
		var x = buf.readInt();
		var y = buf.readInt();
		var z = buf.readInt();
		return new Vec3i(x, y, z);
	}

	public static void writeVector(FriendlyByteBuf buf, Vec3i vec) {
		buf.writeInt(vec.getX());
		buf.writeInt(vec.getY());
		buf.writeInt(vec.getZ());
	}

	public static UniverseId.SectorId readSectorId(FriendlyByteBuf buf) {
		var x = buf.readInt();
		var y = buf.readInt();
		var z = buf.readInt();
		var id = buf.readInt();
		return new UniverseId.SectorId(new Vec3i(x, y, z), id);
	}

	public static void writeSectorId(FriendlyByteBuf buf, UniverseId.SectorId id) {
		buf.writeInt(id.sectorPos().getX());
		buf.writeInt(id.sectorPos().getY());
		buf.writeInt(id.sectorPos().getZ());
		buf.writeInt(id.sectorId());
	}

	public static UniverseId readUniverseId(FriendlyByteBuf buf) {
		if (buf.readBoolean())
			return null;
		var galaxySectorId = readSectorId(buf);
		var systemSectorId = readSectorId(buf);
		var node = buf.readInt();
		return new UniverseId(galaxySectorId, systemSectorId, node);
	}

	public static void writeUniverseId(FriendlyByteBuf buf, UniverseId id) {
		buf.writeBoolean(id == null);
		if (id != null) {
			writeSectorId(buf, id.galaxySector());
			writeSectorId(buf, id.systemSector());
			buf.writeInt(id.systemNodeId());
		}
	}

}
