package net.xavil.universal.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.saveddata.SavedData;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.id.SystemNodeId;

public class ModSavedData extends SavedData {

	public SystemNodeId systemNodeId;

	public ModSavedData(SystemNodeId systemNodeId) {
		this.systemNodeId = systemNodeId;
	}

	public static ModSavedData load(CompoundTag nbt) {
		var id = SystemNodeId.CODEC.parse(NbtOps.INSTANCE, nbt.get("system_node_id"))
				.getOrThrow(true, Mod.LOGGER::error);
		return new ModSavedData(id);
	}

	@Override
	public CompoundTag save(CompoundTag nbt) {
		var id = SystemNodeId.CODEC.encodeStart(NbtOps.INSTANCE, this.systemNodeId)
				.getOrThrow(true, Mod.LOGGER::error);
		nbt.put("system_node_id", id);
		return nbt;
	}

}
