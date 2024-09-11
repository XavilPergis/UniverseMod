package net.xavil.ultraviolet.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.ultraviolet.common.universe.station.SpaceStation;

public final class GlobalData extends SavedData {

	public final MutableList<ResourceLocation> dynamicLevels = new Vector<>();
	public final MutableList<SpaceStation> spaceStations = new Vector<>();

	private GlobalData() {
	}

	public static GlobalData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(
				GlobalData::load,
				GlobalData::new,
				"ultraviolet_global_data");
	}

	private static GlobalData load(CompoundTag nbt) {
		final var data = new GlobalData();
		final var dynamicLevels = nbt.getList("dynamic_levels", Tag.TAG_STRING);
		for (final var stringTag : dynamicLevels) {
			final var levelId = stringTag.getAsString();
			data.dynamicLevels.push(new ResourceLocation(levelId));
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag nbt) {
		final var dynamicLevels = new ListTag();
		for (final var loc : this.dynamicLevels.iterable()) {
			dynamicLevels.add(StringTag.valueOf(loc.toString()));
		}
		nbt.put("dynamic_levels", dynamicLevels);

		final var spaceStations = new ListTag();
		// for (final var station : this.spaceStations.iterable()) {
		// 	spaceStations.add(SpaceStation.toNbt(station));
		// }
		nbt.put("space_stations", spaceStations);

		return nbt;
	}
	
}
