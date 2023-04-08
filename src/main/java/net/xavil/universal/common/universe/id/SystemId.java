package net.xavil.universal.common.universe.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SystemId(UniverseSectorId galaxySector, GalaxySectorId systemSector) {
	public static final Codec<SystemId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			UniverseSectorId.CODEC.fieldOf("galaxy").forGetter(SystemId::galaxySector),
			GalaxySectorId.CODEC.fieldOf("system").forGetter(SystemId::systemSector))
			.apply(inst, SystemId::new));

}
