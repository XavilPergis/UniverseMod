package net.xavil.universal.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.xavil.universal.common.universe.Location;

public class ModSavedData extends SavedData {

	public Location location;

	public ModSavedData(Location location) {
		this.location = location;
	}

	public static ModSavedData load(CompoundTag nbt) {
		final var location = Location.fromNbt(nbt.getCompound("location"));
		return new ModSavedData(location);
	}

	@Override
	public CompoundTag save(CompoundTag nbt) {
		nbt.put("location", Location.toNbt(this.location));
		return nbt;
	}

}
