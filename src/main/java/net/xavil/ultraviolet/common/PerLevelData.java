package net.xavil.ultraviolet.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.WorldType;

public final class PerLevelData extends SavedData {

	public static final String ID = "ultraviolet_per_level_data";

	// so we know where in the whole universe this level is located
	public WorldType worldType;
	// world properties + the generator settings, because registering to the
	// LevelStem registry is not working for some reason.
	public LevelStem levelStem;

	private PerLevelData() {
	}

	public static PerLevelData get(ServerLevel level) {
		return get(level.getDataStorage());
	}

	public static PerLevelData get(DimensionDataStorage storage) {
		return storage.computeIfAbsent(
				PerLevelData::load,
				PerLevelData::new,
				ID);
	}

	public static PerLevelData load(CompoundTag nbt) {
		final var data = new PerLevelData();
		data.worldType = WorldType.fromNbt(nbt.getCompound("location"));
		data.levelStem = LevelStem.CODEC.parse(NbtOps.INSTANCE, nbt.get("level_stem"))
				.get().left().orElse(null);
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag nbt) {
		nbt.put("location", WorldType.toNbt(this.worldType));
		if (this.levelStem != null) {
			nbt.put("level_stem", LevelStem.CODEC.encodeStart(NbtOps.INSTANCE, this.levelStem)
					.getOrThrow(false, Mod.LOGGER::error));
		}
		return nbt;
	}

}
