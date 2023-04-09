package net.xavil.universal.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.universal.common.universe.id.UniverseSectorId;
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

	public static UniverseSectorId readSectorId(FriendlyByteBuf buf) {
		var x = buf.readInt();
		var y = buf.readInt();
		var z = buf.readInt();
		var id = buf.readInt();
		return new UniverseSectorId(Vec3i.from(x, y, z), id);
	}

	public static void writeSectorId(FriendlyByteBuf buf, UniverseSectorId id) {
		buf.writeInt(id.sectorPos().x);
		buf.writeInt(id.sectorPos().y);
		buf.writeInt(id.sectorPos().z);
		buf.writeInt(id.id());
	}

	public static SystemId readSystemId(FriendlyByteBuf buf) {
		var galaxySector = readSectorId(buf);
		var packedInfo = buf.readInt();
		var pos = readVector(buf);
		return new SystemId(galaxySector, new GalaxySectorId(pos, packedInfo));
	}

	public static void writeSystemId(FriendlyByteBuf buf, SystemId id) {
		writeSectorId(buf, id.galaxySector());
		buf.writeInt(id.systemSector().packedInfo());
		writeVector(buf, id.systemSector().levelCoords());
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
