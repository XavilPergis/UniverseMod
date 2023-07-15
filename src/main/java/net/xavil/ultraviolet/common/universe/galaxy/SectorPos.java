package net.xavil.ultraviolet.common.universe.galaxy;

import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;

public record SectorPos(int level, Vec3i levelCoords) implements Hashable {

	public static SectorPos fromPos(int level, Vec3 pos) {
		return new SectorPos(level, GalaxySector.levelCoordsForPos(level, pos));
	}


	public double width() {
		return GalaxySector.sizeForLevel(this.level);
	}

	public Vec3 minBound() {
		return this.levelCoords.lowerCorner().mul(width());
	}

	public Vec3 maxBound() {
		return this.minBound().add(Vec3.broadcast(this.width()));
	}

	public Vec3i rootCoords() {
		return this.levelCoords.floorDiv(1 << (GalaxySector.ROOT_LEVEL - this.level));
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendInt(level).append(levelCoords);
	}

}
