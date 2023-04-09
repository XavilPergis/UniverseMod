package net.xavil.universal.common.universe.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.util.math.Vec3i;

public record UniverseSectorId(Vec3i sectorPos, int id) {
	public static final Codec<UniverseSectorId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Vec3i.CODEC.fieldOf("pos").forGetter(UniverseSectorId::sectorPos),
			Codec.INT.fieldOf("id").forGetter(UniverseSectorId::id))
			.apply(inst, UniverseSectorId::new));

	@Override
	public String toString() {
		return "[" + this.sectorPos.x + ", " + this.sectorPos.y + ", " + this.sectorPos.z + "]:" + this.id;
	}
}
