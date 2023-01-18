package net.xavil.universal.common.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Vec3i;
import net.xavil.universal.common.universe.system.CelestialNode;

public record UniverseId(
		SectorId universeSector,
		SectorId galaxySector,
		/**
		 * The ID used in {@link CelestialNode#lookup(int)}
		 */
		int systemNodeId) {

	public record SectorId(Vec3i sectorPos, int sectorId) {
		public static final Codec<SectorId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
				Vec3i.CODEC.fieldOf("pos").forGetter(SectorId::sectorPos),
				Codec.INT.fieldOf("id").forGetter(SectorId::sectorId))
				.apply(inst, SectorId::new));
	}

	public static final Codec<UniverseId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			SectorId.CODEC.fieldOf("galaxy").forGetter(UniverseId::universeSector),
			SectorId.CODEC.fieldOf("system").forGetter(UniverseId::galaxySector),
			Codec.INT.fieldOf("id").forGetter(UniverseId::systemNodeId))
			.apply(inst, UniverseId::new));

}
