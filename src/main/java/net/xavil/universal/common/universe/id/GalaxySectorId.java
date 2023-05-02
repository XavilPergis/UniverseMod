package net.xavil.universal.common.universe.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.universal.common.universe.galaxy.SectorPos;
import net.xavil.util.Util;
import net.xavil.util.math.matrices.Vec3i;

public record GalaxySectorId(Vec3i levelCoords, int packedInfo) {

	// public static final long MASK_X = Util.longMask(0, 21);
	// public static final long MASK_Y = Util.longMask(21, 42);
	// public static final long MASK_Z = Util.longMask(42, 63);
	public static final int MASK_ELEMENT_INDEX = Util.intMask(0, 16);
	// public static final int MASK_LAYER_INDEX = Util.intMask(16, 20);
	public static final int MASK_LEVEL = Util.intMask(20, 24);

	public static GalaxySectorId from(int level, Vec3i levelCoords, int elementIndex) {
		int info = 0;
		info |= (level << 20) & MASK_LEVEL;
		// info |= (layerIndex << 16) & MASK_LAYER_INDEX;
		info |= (elementIndex << 0) & MASK_ELEMENT_INDEX;
		return new GalaxySectorId(levelCoords, info);
	}
	public static GalaxySectorId from(SectorPos pos, int elementIndex) {
		return from(pos.level(), pos.levelCoords(), elementIndex);
	}

	public static final Codec<GalaxySectorId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Vec3i.CODEC.fieldOf("pos").forGetter(GalaxySectorId::levelCoords),
			Codec.INT.fieldOf("packed_info").forGetter(GalaxySectorId::packedInfo))
			.apply(inst, GalaxySectorId::new));

	// public int layerIndex() {
	// 	return (packedInfo & MASK_LAYER_INDEX) >>> 16;
	// }

	public int elementIndex() {
		return (packedInfo & MASK_ELEMENT_INDEX) >>> 0;
	}

	public int level() {
		return (packedInfo & MASK_LEVEL) >>> 20;
	}

	public SectorPos sectorPos() {
		return new SectorPos(level(), levelCoords());
	}

	public String uniqueName() {
		final var x = Util.escapeMinus(this.levelCoords.x);
		final var y = Util.escapeMinus(this.levelCoords.x);
		final var z = Util.escapeMinus(this.levelCoords.x);
		return String.format("%s_%s_%s_%d_%d", x, y, z, level(), elementIndex());
	}

	@Override
	public String toString() {
		final var p = this.levelCoords();
		return this.level() + ":[" + p.x + ", " + p.y + ", " + p.z + "]:" + this.elementIndex();
	}

}
