package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.function.Consumer;

import net.minecraft.util.Mth;
import net.xavil.hawklib.collections.interfaces.ImmutableSet;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;

public abstract sealed class SectorTicketInfo {

	public record Diff(ImmutableSet<SectorPos> added, ImmutableSet<SectorPos> removed) {
		public static final Diff EMPTY = new Diff(ImmutableSet.of(), ImmutableSet.of());
	}

	public static Multi visual(Vec3 centerPos) {
		return new Multi(centerPos, GalaxySector.BASE_SIZE_Tm);
	}

	public static Single single(SectorPos pos) {
		return new Single(pos);
	}

	public abstract SectorTicketInfo copy();

	public ImmutableSet<SectorPos> affectedSectors() {
		final var sectors = MutableSet.<SectorPos>hashSet();
		enumerateAffectedSectors(sectors::insert);
		return sectors;
	}

	public abstract void enumerateAffectedSectors(Consumer<SectorPos> consumer);

	public static final class Single extends SectorTicketInfo {
		public SectorPos sector;

		public Single(SectorPos sector) {
			this.sector = sector;
		}

		@Override
		public Single copy() {
			return new Single(sector);
		}

		@Override
		public ImmutableSet<SectorPos> affectedSectors() {
			return ImmutableSet.of(this.sector);
		}

		@Override
		public void enumerateAffectedSectors(Consumer<SectorPos> consumer) {
			consumer.accept(this.sector);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Single other ? this.sector.equals(other.sector) : false;
		}
	}

	public static final class Multi extends SectorTicketInfo {
		public Vec3 centerPos;
		public double baseRadius;

		public Multi(Vec3 centerPos, double baseRadius) {
			this.centerPos = centerPos;
			this.baseRadius = baseRadius;
		}

		@Override
		public Multi copy() {
			return new Multi(centerPos, baseRadius);
		}

		// https://stackoverflow.com/questions/28343716/sphere-intersection-test-of-aabb
		boolean sphereAabb(Vec3 sphereCenter, double sphereRadius, Vec3 aabbMin, Vec3 aabbMax) {
			double d = 0.0;
			if (sphereCenter.x < aabbMin.x)
				d += Mth.square(sphereCenter.x - aabbMin.x);
			else if (sphereCenter.x > aabbMax.x)
				d += Mth.square(sphereCenter.x - aabbMax.x);
			if (sphereCenter.y < aabbMin.y)
				d += Mth.square(sphereCenter.y - aabbMin.y);
			else if (sphereCenter.y > aabbMax.y)
				d += Mth.square(sphereCenter.y - aabbMax.y);
			if (sphereCenter.z < aabbMin.z)
				d += Mth.square(sphereCenter.z - aabbMin.z);
			else if (sphereCenter.z > aabbMax.z)
				d += Mth.square(sphereCenter.z - aabbMax.z);
			return d <= sphereRadius * sphereRadius;
		}

		private boolean isInside(SectorPos pos) {
			final var radius = this.baseRadius * (1 << pos.level());
			return sphereAabb(this.centerPos, radius, pos.minBound(), pos.maxBound());
		}

		@Override
		public void enumerateAffectedSectors(Consumer<SectorPos> consumer) {
			double radiusCur = this.baseRadius;
			for (int level = 0; level <= GalaxySector.ROOT_LEVEL; ++level) {
				final var level2 = level;
				final var curMin = GalaxySector.levelCoordsForPos(level, this.centerPos.sub(Vec3.broadcast(radiusCur)));
				final var curMax = GalaxySector.levelCoordsForPos(level, this.centerPos.add(Vec3.broadcast(radiusCur)));
				Vec3i.iterateInclusive(curMin, curMax, pos -> {
					final var spos = new SectorPos(level2, pos);
					if (isInside(spos))
						consumer.accept(spos);
				});
				radiusCur *= 2.0;
			}
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Multi other) {
				return this.centerPos.equals(other.centerPos)
						&& this.baseRadius == other.baseRadius;
			}
			return false;
		}
	}

}
