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
				+ escapeMinus(this.system.galaxySector().sectorPos().x) + "_"
				+ escapeMinus(this.system.galaxySector().sectorPos().y) + "_"
				+ escapeMinus(this.system.galaxySector().sectorPos().z) + "_"
				+ this.system.galaxySector().id() + "_"
				+ escapeMinus(this.system.systemSector().pos().x) + "_"
				+ escapeMinus(this.system.systemSector().pos().y) + "_"
				+ escapeMinus(this.system.systemSector().pos().z) + "_"
				+ this.system.systemSector().level() + "_"
				+ this.system.systemSector().layerIndex() + "_"
				+ this.system.systemSector().elementIndex() + "_"
				+ this.nodeId;
	}

}
