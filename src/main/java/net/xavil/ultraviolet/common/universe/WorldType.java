package net.xavil.ultraviolet.common.universe;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;

public abstract sealed class WorldType {

	public static final Unknown UNKNOWN = new Unknown();

	public static final class Unknown extends WorldType {
		private Unknown() {
		}
	}

	public static final class SystemNode extends WorldType {
		public final SystemNodeId id;

		public SystemNode(SystemNodeId id) {
			this.id = id;
		}
	}

	public static final class Station extends WorldType {
		public final int id;

		public Station(int id) {
			this.id = id;
		}
	}

	public static WorldType fromNbt(CompoundTag nbt) {
		final var type = nbt.getString("type");
		if (type.equals("unknown")) {
			return UNKNOWN;
		} else if (type.equals("world")) {
			final var id = SystemNodeId.CODEC.parse(NbtOps.INSTANCE, nbt.get("id"))
				.getOrThrow(true, Mod.LOGGER::error);
			return new SystemNode(id);
		} else if (type.equals("station")) {
			if (nbt.contains("id")) return null;
			return new Station(nbt.getInt("id"));
		}
		return null;
	}

	public static CompoundTag toNbt(WorldType location) {
		final var nbt = new CompoundTag();
		if (location instanceof Unknown) {
			nbt.putString("type", "unknown");
		} else if (location instanceof SystemNode world) {
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
