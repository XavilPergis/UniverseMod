package net.xavil.universal.common.universe.id;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.universal.common.universe.galaxy.SectorPos;
import net.xavil.util.Util;
import net.xavil.util.math.Vec3i;

public record GalaxySectorId(int packedInfo, long packedPos) {

	public static final long MASK_X = Util.longMask(0, 21);
	public static final long MASK_Y = Util.longMask(21, 42);
	public static final long MASK_Z = Util.longMask(42, 63);
	public static final int MASK_ELEMENT_INDEX = Util.intMask(0, 16);
	public static final int MASK_LAYER_INDEX = Util.intMask(16, 20);
	public static final int MASK_LEVEL = Util.intMask(20, 24);

	public static GalaxySectorId from(int level, Vec3i levelCoords, int layerIndex, int elementIndex) {
		int info = 0;
		info |= (level << 20) & MASK_LEVEL;
		info |= (layerIndex << 16) & MASK_LAYER_INDEX;
		info |= (elementIndex << 0) & MASK_ELEMENT_INDEX;
		long pos = 0L;
		pos |= (levelCoords.x << 0L) & MASK_X;
		pos |= (levelCoords.y << 21L) & MASK_Y;
		pos |= (levelCoords.z << 42L) & MASK_Z;
		return new GalaxySectorId(info, pos);
	}

	public static final Codec<GalaxySectorId> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.INT.fieldOf("packed_info").forGetter(GalaxySectorId::packedInfo),
			Codec.LONG.fieldOf("packed_pos").forGetter(GalaxySectorId::packedPos))
			.apply(inst, GalaxySectorId::new));

	public int layerIndex() {
		return (packedInfo & MASK_LAYER_INDEX) >>> 16;
	}

	public int elementIndex() {
		return (packedInfo & MASK_ELEMENT_INDEX) >>> 0;
	}

	public int level() {
		return (packedInfo & MASK_LEVEL) >>> 20;
	}

	public Vec3i pos() {
		final var x = (int) ((packedPos & MASK_X) >>> 0L);
		final var y = (int) ((packedPos & MASK_Y) >>> 21L);
		final var z = (int) ((packedPos & MASK_Z) >>> 42L);
		return Vec3i.from(x, y, z);
	}

	public SectorPos sectorPos() {
		return new SectorPos(level(), pos());
	}

}
