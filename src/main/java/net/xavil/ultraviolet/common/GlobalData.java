package net.xavil.ultraviolet.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;

public final class GlobalData extends SavedData {

	private final MutableList<ResourceLocation> dynamicLevels = new Vector<>();

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
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag nbt) {
		final var dynamicLevels = new ListTag();
		for (final var loc : this.dynamicLevels.iterable()) {
			dynamicLevels.add(StringTag.valueOf(loc.toString()));
		}
		nbt.put("dynamic_levels", dynamicLevels);
		return nbt;
	}
	
}
