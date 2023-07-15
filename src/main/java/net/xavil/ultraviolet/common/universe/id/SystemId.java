package net.xavil.ultraviolet.common.universe.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.hawklib.Util;

public record SystemId(UniverseSectorId universeSector, GalaxySectorId galaxySector) {
	public static final Codec<SystemId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			UniverseSectorId.CODEC.fieldOf("universe").forGetter(SystemId::universeSector),
			GalaxySectorId.CODEC.fieldOf("galaxy").forGetter(SystemId::galaxySector))
			.apply(inst, SystemId::new));

	public String uniqueName() {
		return String.format("%s_%s", this.universeSector.uniqueName(), this.galaxySector.uniqueName());
	}

	@Override
	public String toString() {
		return this.universeSector.toString() + "/" + this.galaxySector.toString();
	}
}
