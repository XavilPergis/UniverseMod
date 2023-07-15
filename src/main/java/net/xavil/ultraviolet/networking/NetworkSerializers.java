package net.xavil.ultraviolet.networking;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.xavil.hawklib.math.matrices.Vec3i;
import net.xavil.ultraviolet.common.universe.Location;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;

public class NetworkSerializers {

	protected NetworkSerializers() {
	}

	public static final <T extends Enum<T>> NetworkSerializer<T> forEnum(Class<T> clazz) {
		return new NetworkSerializer<T>() {
			@Override
			public void write(FriendlyByteBuf buf, T value) {
				buf.writeInt(value.ordinal());
			}

			@Override
			@Nullable
			public T read(FriendlyByteBuf buf) {
				final var index = buf.readInt();
				final var members = clazz.getEnumConstants();
				return index >= members.length ? null : members[index];
			}
		};
	}

	public static final NetworkSerializer<Vec3i> VEC3I = new NetworkSerializer<Vec3i>() {
		@Override
		@Nullable
		public Vec3i read(FriendlyByteBuf buf) {
			final var x = buf.readInt();
			final var y = buf.readInt();
			final var z = buf.readInt();
			return new Vec3i(x, y, z);
		}

		public void write(FriendlyByteBuf buf, Vec3i value) {
			buf.writeInt(value.x);
			buf.writeInt(value.y);
			buf.writeInt(value.z);
		}
	};

	public static final NetworkSerializer<CompoundTag> NBT = new NetworkSerializer<CompoundTag>() {
		@Override
		@Nullable
		public CompoundTag read(FriendlyByteBuf buf) {
			return buf.readNbt();
		}

		public void write(FriendlyByteBuf buf, CompoundTag value) {
			buf.writeNbt(value);
		}
	};

	public static final NetworkSerializer<UniverseSectorId> UNIVERSE_SECTOR_ID = new NetworkSerializer<UniverseSectorId>() {
		@Override
		public void write(FriendlyByteBuf buf, UniverseSectorId value) {
			VEC3I.write(buf, value.sectorPos());
			buf.writeInt(value.id());
		}

		@Override
		@Nullable
		public UniverseSectorId read(FriendlyByteBuf buf) {
			final var sectorPos = VEC3I.read(buf);
			final var id = buf.readInt();
			return new UniverseSectorId(sectorPos, id);
		}
	};

	public static final NetworkSerializer<GalaxySectorId> GALAXY_SECTOR_ID = new NetworkSerializer<GalaxySectorId>() {
		@Override
		public void write(FriendlyByteBuf buf, GalaxySectorId value) {
			VEC3I.write(buf, value.levelCoords());
			buf.writeInt(value.packedInfo());
		}

		@Override
		@Nullable
		public GalaxySectorId read(FriendlyByteBuf buf) {
			final var levelCoords = VEC3I.read(buf);
			final var packedInfo = buf.readInt();
			return new GalaxySectorId(levelCoords, packedInfo);
		}
	};

	public static final NetworkSerializer<SystemId> SYSTEM_ID = new NetworkSerializer<SystemId>() {
		@Override
		public void write(FriendlyByteBuf buf, SystemId value) {
			UNIVERSE_SECTOR_ID.write(buf, value.universeSector());
			GALAXY_SECTOR_ID.write(buf, value.galaxySector());
		}

		@Override
		@Nullable
		public SystemId read(FriendlyByteBuf buf) {
			final var universeSector = UNIVERSE_SECTOR_ID.read(buf);
			final var galaxySector = GALAXY_SECTOR_ID.read(buf);
			return new SystemId(universeSector, galaxySector);
		}
	};

	public static final NetworkSerializer<SystemNodeId> SYSTEM_NODE_ID = new NetworkSerializer<SystemNodeId>() {
		@Override
		public void write(FriendlyByteBuf buf, SystemNodeId value) {
			SYSTEM_ID.write(buf, value.system());
			buf.writeInt(value.nodeId());
		}

		@Override
		@Nullable
		public SystemNodeId read(FriendlyByteBuf buf) {
			final var system = SYSTEM_ID.read(buf);
			final var nodeId = buf.readInt();
			return new SystemNodeId(system, nodeId);
		}
	};

	public static final NetworkSerializer<Location> LOCATION = new NetworkSerializer<Location>() {

		enum LocationKind {
			UNKNOWN, WORLD, STATION,
		}

		private static final NetworkSerializer<LocationKind> KIND = forEnum(LocationKind.class);

		@Override
		public void write(FriendlyByteBuf buf, Location value) {
			buf.writeBoolean(value == null);
			if (value == null)
				return;
			if (value instanceof Location.Unknown) {
				KIND.write(buf, LocationKind.UNKNOWN);
			} else if (value instanceof Location.World world) {
				KIND.write(buf, LocationKind.WORLD);
				SYSTEM_NODE_ID.write(buf, world.id);
			} else if (value instanceof Location.Station station) {
				KIND.write(buf, LocationKind.STATION);
				buf.writeInt(station.id);
			}
		}

		@Override
		@Nullable
		public Location read(FriendlyByteBuf buf) {
			if (buf.readBoolean())
				return null;

			final var type = KIND.read(buf);
			if (type == LocationKind.UNKNOWN) {
				return Location.UNKNOWN;
			} else if (type == LocationKind.WORLD) {
				final var systemNodeId = SYSTEM_NODE_ID.read(buf);
				if (systemNodeId == null)
					return null;
				return new Location.World(systemNodeId);
			} else if (type == LocationKind.WORLD) {
				return new Location.Station(buf.readInt());
			}
			return null;
		}
	};

}
