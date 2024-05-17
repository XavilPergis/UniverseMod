package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.function.Consumer;

import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.hash.Hashable;
import net.xavil.hawklib.hash.Hasher;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public record SectorPos(int level, Vec3i levelCoords) implements Hashable {

	public static SectorPos fromPos(int level, Vec3Access pos) {
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

	public void enumerateDirectChildren(Consumer<SectorPos> consumer) {
		if (this.level <= 0)
			return;
		final int cx = 2 * this.levelCoords.x, cy = 2 * this.levelCoords.y, cz = 2 * this.levelCoords.z;
		consumer.accept(new SectorPos(level - 1, new Vec3i(cx * 2 + 0, cy * 2 + 0, cz * 2 + 0)));
		consumer.accept(new SectorPos(level - 1, new Vec3i(cx * 2 + 0, cy * 2 + 0, cz * 2 + 1)));
		consumer.accept(new SectorPos(level - 1, new Vec3i(cx * 2 + 0, cy * 2 + 1, cz * 2 + 0)));
		consumer.accept(new SectorPos(level - 1, new Vec3i(cx * 2 + 0, cy * 2 + 1, cz * 2 + 1)));
		consumer.accept(new SectorPos(level - 1, new Vec3i(cx * 2 + 1, cy * 2 + 0, cz * 2 + 0)));
		consumer.accept(new SectorPos(level - 1, new Vec3i(cx * 2 + 1, cy * 2 + 0, cz * 2 + 1)));
		consumer.accept(new SectorPos(level - 1, new Vec3i(cx * 2 + 1, cy * 2 + 1, cz * 2 + 0)));
		consumer.accept(new SectorPos(level - 1, new Vec3i(cx * 2 + 1, cy * 2 + 1, cz * 2 + 1)));
	}

	@Override
	public int hashCode() {
		return FastHasher.hashToInt(this);
	}

	@Override
	public void appendHash(Hasher hasher) {
		hasher.appendInt(level).append(levelCoords);
	}

	@Override
	public String toString() {
		return String.format(
				"#L%d:(%d, %d, %d)",
				this.level, this.levelCoords.x, this.levelCoords.y, this.levelCoords.z);
	}

}
