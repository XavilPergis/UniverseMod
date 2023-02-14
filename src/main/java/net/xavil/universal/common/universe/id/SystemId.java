package net.xavil.universal.common.universe.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SystemId(SectorId galaxySector, SectorId systemSector) {
	public static final Codec<SystemId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		SectorId.CODEC.fieldOf("galaxy").forGetter(SystemId::galaxySector),
		SectorId.CODEC.fieldOf("system").forGetter(SystemId::systemSector))
		.apply(inst, SystemId::new));

}
