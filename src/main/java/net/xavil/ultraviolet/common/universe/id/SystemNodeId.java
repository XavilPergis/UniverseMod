package net.xavil.ultraviolet.common.universe.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SystemNodeId(SystemId system, int nodeId) {
	public static final Codec<SystemNodeId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			SystemId.CODEC.fieldOf("system").forGetter(SystemNodeId::system),
			Codec.INT.fieldOf("id").forGetter(SystemNodeId::nodeId))
			.apply(inst, SystemNodeId::new));

	public UniverseSectorId universeSector() {
		return this.system.universeSector();
	}

	public GalaxySectorId galaxySector() {
		return this.system.galaxySector();
	}

	public String uniqueName() {
		return String.format("%s_%d", system.uniqueName(), this.nodeId);
	}

	@Override
	public String toString() {
		return this.system.toString() + "/" + this.nodeId;
	}

}
