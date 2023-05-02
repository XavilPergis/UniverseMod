package net.xavil.universal.common.universe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.util.math.matrices.Vec3;

public abstract sealed class Location {

	public static final Unknown UNKNOWN = new Unknown();

	public static final class Unknown extends Location {
		private Unknown() {
		}
	}

	public static final class World extends Location {
		public final SystemNodeId id;

		public World(SystemNodeId id) {
			this.id = id;
		}
	}

	public static final class Station extends Location {
		public final int id;

		public Station(int id) {
			this.id = id;
		}
	}

	public static Location fromNbt(CompoundTag nbt) {
		final var type = nbt.getString("type");
		if (type.equals("unknown")) {
			return UNKNOWN;
		} else if (type.equals("world")) {
			final var id = SystemNodeId.CODEC.parse(NbtOps.INSTANCE, nbt.get("id"))
				.getOrThrow(true, Mod.LOGGER::error);
			return new World(id);
		} else if (type.equals("station")) {
			if (nbt.contains("id")) return null;
			return new Station(nbt.getInt("id"));
		}
		return null;
	}

	public static CompoundTag toNbt(Location location) {
		final var nbt = new CompoundTag();
		if (location instanceof Unknown) {
			nbt.putString("type", "unknown");
		} else if (location instanceof World world) {
			nbt.putString("type", "world");
			final var id = SystemNodeId.CODEC.encodeStart(NbtOps.INSTANCE, world.id)
					.getOrThrow(true, Mod.LOGGER::error);
			nbt.put("id", id);
		} else if (location instanceof Station station) {
			nbt.putString("type", "station");
			nbt.putInt("id", station.id);
		}
		return nbt;
	}

}
