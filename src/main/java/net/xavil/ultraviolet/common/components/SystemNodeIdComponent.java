package net.xavil.ultraviolet.common.components;

import java.util.Objects;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;

public class SystemNodeIdComponent implements AutoSyncedComponent {

	public SystemNodeId id;

	@Override
	public void readFromNbt(CompoundTag tag) {
		var id = tag.get("system_node_id");
		if (id != null) {
			this.id = SystemNodeId.CODEC.parse(NbtOps.INSTANCE, id)
					.getOrThrow(true, Mod.LOGGER::error);
		}
	}

	@Override
	public void writeToNbt(CompoundTag tag) {
		if (this.id != null) {
			var id = SystemNodeId.CODEC.encodeStart(NbtOps.INSTANCE, this.id).getOrThrow(true, Mod.LOGGER::error);
			tag.put("system_node_id", id);
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof SystemNodeIdComponent other ? Objects.equals(other.id, this.id) : false;
	}

}
