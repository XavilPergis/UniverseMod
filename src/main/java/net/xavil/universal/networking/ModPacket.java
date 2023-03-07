package net.xavil.universal.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.id.SectorId;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.util.math.Vec3i;

public abstract class ModPacket<T extends PacketListener> implements Packet<T> {

	public abstract void read(FriendlyByteBuf buf);

	public abstract void write(FriendlyByteBuf buf);

	@Override
	public final void handle(T listener) {
		ModNetworking.dispatch(this, listener);
	}

	public static Vec3i readVector(FriendlyByteBuf buf) {
		var x = buf.readInt();
		var y = buf.readInt();
		var z = buf.readInt();
		return Vec3i.from(x, y, z);
	}

	public static void writeVector(FriendlyByteBuf buf, Vec3i vec) {
		buf.writeInt(vec.x);
		buf.writeInt(vec.y);
		buf.writeInt(vec.z);
	}

	public static SectorId readSectorId(FriendlyByteBuf buf) {
		var x = buf.readInt();
		var y = buf.readInt();
		var z = buf.readInt();
		var layer = buf.readInt();
		var id = buf.readInt();
		return new SectorId(Vec3i.from(x, y, z), new Octree.Id(layer, id));
	}

	public static void writeSectorId(FriendlyByteBuf buf, SectorId id) {
		buf.writeInt(id.sectorPos().x);
		buf.writeInt(id.sectorPos().y);
		buf.writeInt(id.sectorPos().z);
		buf.writeInt(id.sectorId().layerIndex());
		buf.writeInt(id.sectorId().elementIndex());
	}

	public static SystemId readSystemId(FriendlyByteBuf buf) {
		var galaxySector = readSectorId(buf);
		var systemSector = readSectorId(buf);
		return new SystemId(galaxySector, systemSector);
	}

	public static void writeSystemId(FriendlyByteBuf buf, SystemId id) {
		writeSectorId(buf, id.galaxySector());
		writeSectorId(buf, id.systemSector());
	}

	public static SystemNodeId readSystemNodeId(FriendlyByteBuf buf) {
		if (buf.readBoolean())
			return null;

		var systemId = readSystemId(buf);
		var node = buf.readInt();
		return new SystemNodeId(systemId, node);
	}

	public static void writeSystemNodeId(FriendlyByteBuf buf, SystemNodeId id) {
		buf.writeBoolean(id == null);
		if (id != null) {
			writeSystemId(buf, id.system());
			buf.writeInt(id.nodeId());
		}
	}

}
