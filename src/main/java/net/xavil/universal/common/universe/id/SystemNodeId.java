package net.xavil.universal.common.universe.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SystemNodeId(SystemId system, int nodeId) {
	public static final Codec<SystemNodeId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			SystemId.CODEC.fieldOf("system").forGetter(SystemNodeId::system),
			Codec.INT.fieldOf("id").forGetter(SystemNodeId::nodeId))
			.apply(inst, SystemNodeId::new));

	private static String escapeMinus(int n) {
		return n >= 0 ? "" + n : "m" + -n;
	}

	public String uniqueName() {
		return "id"
				+ escapeMinus(this.system.galaxySector().sectorPos().getX()) + "_"
				+ escapeMinus(this.system.galaxySector().sectorPos().getY()) + "_"
				+ escapeMinus(this.system.galaxySector().sectorPos().getZ()) + "_"
				+ this.system.galaxySector().sectorId().layerIndex() + "_"
				+ this.system.galaxySector().sectorId().elementIndex() + "_"
				+ escapeMinus(this.system.systemSector().sectorPos().getX()) + "_"
				+ escapeMinus(this.system.systemSector().sectorPos().getY()) + "_"
				+ escapeMinus(this.system.systemSector().sectorPos().getZ()) + "_"
				+ this.system.systemSector().sectorId().layerIndex() + "_"
				+ this.system.systemSector().sectorId().elementIndex() + "_"
				+ this.nodeId;
	}

}
