package net.xavil.universal.common.universe.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.Vec3i;

public record SectorId(Vec3i sectorPos, Octree.Id sectorId) {
	public static final Codec<SectorId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
		Vec3i.CODEC.fieldOf("pos").forGetter(SectorId::sectorPos),
		Octree.Id.CODEC.fieldOf("id").forGetter(SectorId::sectorId))
		.apply(inst, SectorId::new));

}
