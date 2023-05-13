package net.xavil.ultraviolet.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.xavil.ultraviolet.common.universe.Location;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.util.math.matrices.Vec3i;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;

public abstract class ModPacket<T extends PacketListener> implements Packet<T> {

	// IMPORTANT: in local singleplayer worlds, neither `read` nor `write` are
	// called, and the packet it copied directly between server and client. This
	// means that all the fields of the packet MUST NOT BE SHARED.
	//
	// I ran into this issue by having a `CelestialNode` that was kept around
	// server-side embedded into the packet directly, which was then happily
	// accepted by the client, leading to some subtle race conditions when rendering
	// the sky.

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
		writeSectorId(buf, id.universeSector());
		buf.writeInt(id.galaxySector().packedInfo());
		writeVector(buf, id.galaxySector().levelCoords());
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

	public static Location readLocation(FriendlyByteBuf buf) {
		if (buf.readBoolean())
			return null;

		final var type = buf.readByte();
		if (type == 0) {
			return Location.UNKNOWN;
		} else if (type == 1) {
			final var systemNodeId = readSystemNodeId(buf);
			if (systemNodeId == null)
				return null;
			return new Location.World(systemNodeId);
		} else if (type == 2) {
			return new Location.Station(buf.readInt());
		}
		return null;
	}

	public static void writeLocation(FriendlyByteBuf buf, Location id) {
		buf.writeBoolean(id == null);
		if (id == null)
			return;
		if (id instanceof Location.Unknown) {
			buf.writeByte(0);
		} else if (id instanceof Location.World world) {
			buf.writeByte(1);
			writeSystemNodeId(buf, world.id);
		} else if (id instanceof Location.Station station) {
			buf.writeByte(2);
			buf.writeInt(station.id);
		}
	}

}
