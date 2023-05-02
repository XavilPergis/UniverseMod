package net.xavil.universal.common.universe.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.util.Util;
import net.xavil.util.math.matrices.Vec3i;

public record UniverseSectorId(Vec3i sectorPos, int id) {
	public static final Codec<UniverseSectorId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Vec3i.CODEC.fieldOf("pos").forGetter(UniverseSectorId::sectorPos),
			Codec.INT.fieldOf("id").forGetter(UniverseSectorId::id))
			.apply(inst, UniverseSectorId::new));

	public String uniqueName() {
		final var x = Util.escapeMinus(this.sectorPos.x);
		final var y = Util.escapeMinus(this.sectorPos.x);
		final var z = Util.escapeMinus(this.sectorPos.x);
		return String.format("%s_%s_%s_%d", x, y, z, this.id);
	}

	@Override
	public String toString() {
		return "[" + this.sectorPos.x + ", " + this.sectorPos.y + ", " + this.sectorPos.z + "]:" + this.id;
	}
}
