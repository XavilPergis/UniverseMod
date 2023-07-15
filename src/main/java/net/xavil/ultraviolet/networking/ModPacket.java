package net.xavil.ultraviolet.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

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

	public static boolean readBoolean(FriendlyByteBuf buf) {
		return buf.readBoolean();
	}

	public static void writeBoolean(FriendlyByteBuf buf, boolean value) {
		buf.writeBoolean(value);
	}

	public static byte readByte(FriendlyByteBuf buf) {
		return buf.readByte();
	}

	public static void writeByte(FriendlyByteBuf buf, byte value) {
		buf.writeByte(value);
	}

	public static short readShort(FriendlyByteBuf buf) {
		return buf.readShort();
	}

	public static void writeShort(FriendlyByteBuf buf, short value) {
		buf.writeShort(value);
	}

	public static int readInt(FriendlyByteBuf buf) {
		return buf.readInt();
	}

	public static void writeInt(FriendlyByteBuf buf, int value) {
		buf.writeInt(value);
	}

	public static long readLong(FriendlyByteBuf buf) {
		return buf.readLong();
	}

	public static void writeLong(FriendlyByteBuf buf, long value) {
		buf.writeLong(value);
	}

	public static float readFloat(FriendlyByteBuf buf) {
		return buf.readFloat();
	}

	public static void writeFloat(FriendlyByteBuf buf, float value) {
		buf.writeFloat(value);
	}

	public static double readDouble(FriendlyByteBuf buf) {
		return buf.readDouble();
	}

	public static void writeDouble(FriendlyByteBuf buf, double value) {
		buf.writeDouble(value);
	}

	public static <T> T read(FriendlyByteBuf buf, NetworkSerializer<T> serializer) {
		return serializer.read(buf);
	}

	public static <T> void write(FriendlyByteBuf buf, T value, NetworkSerializer<T> serializer) {
		serializer.write(buf, value);
	}

	// public static Vec3i readVector(FriendlyByteBuf buf) {
	// 	var x = buf.readInt();
	// 	var y = buf.readInt();
	// 	var z = buf.readInt();
	// 	return Vec3i.from(x, y, z);
	// }

	// public static void writeVector(FriendlyByteBuf buf, Vec3i vec) {
	// 	buf.writeInt(vec.x);
	// 	buf.writeInt(vec.y);
	// 	buf.writeInt(vec.z);
	// }

	// public static UniverseSectorId readSectorId(FriendlyByteBuf buf) {
	// 	var x = buf.readInt();
	// 	var y = buf.readInt();
	// 	var z = buf.readInt();
	// 	var id = buf.readInt();
	// 	return new UniverseSectorId(Vec3i.from(x, y, z), id);
	// }

	// public static void writeSectorId(FriendlyByteBuf buf, UniverseSectorId id) {
	// 	buf.writeInt(id.sectorPos().x);
	// 	buf.writeInt(id.sectorPos().y);
	// 	buf.writeInt(id.sectorPos().z);
	// 	buf.writeInt(id.id());
	// }

	// public static SystemId readSystemId(FriendlyByteBuf buf) {
	// 	var galaxySector = readSectorId(buf);
	// 	var packedInfo = buf.readInt();
	// 	var pos = readVector(buf);
	// 	return new SystemId(galaxySector, new GalaxySectorId(pos, packedInfo));
	// }

	// public static void writeSystemId(FriendlyByteBuf buf, SystemId id) {
	// 	writeSectorId(buf, id.universeSector());
	// 	buf.writeInt(id.galaxySector().packedInfo());
	// 	writeVector(buf, id.galaxySector().levelCoords());
	// }

	// public static SystemNodeId readSystemNodeId(FriendlyByteBuf buf) {
	// 	if (buf.readBoolean())
	// 		return null;

	// 	var systemId = readSystemId(buf);
	// 	var node = buf.readInt();
	// 	return new SystemNodeId(systemId, node);
	// }

	// public static void writeSystemNodeId(FriendlyByteBuf buf, SystemNodeId id) {
	// 	buf.writeBoolean(id == null);
	// 	if (id != null) {
	// 		writeSystemId(buf, id.system());
	// 		buf.writeInt(id.nodeId());
	// 	}
	// }
}
