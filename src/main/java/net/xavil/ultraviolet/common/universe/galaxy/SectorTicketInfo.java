package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.function.Consumer;
import java.util.function.Predicate;

import net.xavil.hawklib.collections.interfaces.ImmutableSet;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.math.Intersections;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;

public abstract sealed class SectorTicketInfo {

	public static Multi visual(Vec3 centerPos) {
		return new Multi(centerPos, GalaxySector.BASE_SIZE_Tm, Multi.SCALES_EXP);
	}

	public static Single single(SectorPos pos) {
		return new Single(pos);
	}

	public ImmutableSet<SectorPos> allAffectedSectors() {
		final var sectors = MutableSet.<SectorPos>hashSet();
		enumerateAllAffectedSectors(sectors::insert);
		return sectors;
	}

	public void enumerateAllAffectedSectors(Consumer<SectorPos> consumer) {
		enumerateAffectedSectors(pos -> {
			consumer.accept(pos);
			return true;
		});
	}

	@Override
	public abstract SectorTicketInfo clone();

	public abstract void enumerateAffectedSectors(Predicate<SectorPos> consumer);

	/**
	 * Determines whether or not the change from {@code prev} should trigger loading
	 * and unloading of new sectors. It is always valid to return {@code true}.
	 * 
	 * @param currentInfo The info that the {@link SectorManager} is currently using
	 *                    to load sectors.
	 * @return {@code true} if the change from the current info should trigger
	 *         sector (un)loading.
	 */
	public boolean shouldUpdate(SectorTicketInfo currentInfo) {
		return true;
	}

	public static final class Single extends SectorTicketInfo {
		public SectorPos sector;

		public Single(SectorPos sector) {
			this.sector = sector;
		}

		@Override
		public Single clone() {
			return new Single(sector);
		}

		@Override
		public ImmutableSet<SectorPos> allAffectedSectors() {
			return ImmutableSet.of(this.sector);
		}

		@Override
		public void enumerateAffectedSectors(Predicate<SectorPos> consumer) {
			consumer.test(this.sector);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Single other ? this.sector.equals(other.sector) : false;
		}

		@Override
		public String toString() {
			return String.format("Single(%s)", this.sector.toString());
		}
	}

	public static final class Multi extends SectorTicketInfo {
		public Vec3 centerPos;
		public double baseRadius;
		/**
		 * {@code scales[level]} returns how many times larger the inclusion radius is
		 * than the {@link #baseRadius} for the given level.
		 */
		public double[] scales;

		public static final double[] SCALES_UNIFORM = { 1, 1, 1, 1, 1, 1, 1, 1 };
		public static final double[] SCALES_EXP = { 1, 2, 4, 8, 16, 32, 64, 128 };
		public static final double[] SCALES_EXP_ADJUSTED = { 1, 2, 4, 8, 16, 32, 64, 512 };

		public Multi(Vec3 centerPos, double baseRadius, double[] scales) {
			this.centerPos = centerPos;
			this.baseRadius = baseRadius;
			this.scales = scales;
		}

		@Override
		public Multi clone() {
			return new Multi(centerPos, baseRadius, scales.clone());
		}

		public double radiusForLevel(int level) {
			return this.baseRadius * this.scales[Math.min(7, level)];
		}

		private boolean isInside(SectorPos pos) {
			final var radius = radiusForLevel(pos.level());
			return Intersections.sphereAabb(this.centerPos, radius, pos.minBound(), pos.maxBound());
		}

		@Override
		public void enumerateAffectedSectors(Predicate<SectorPos> consumer) {
			for (int level = 0; level <= GalaxySector.ROOT_LEVEL; ++level) {
				final var radiusCur = radiusForLevel(level);
				final var curMin = GalaxySector.levelCoordsForPos(level, this.centerPos.sub(Vec3.broadcast(radiusCur)));
				final var curMax = GalaxySector.levelCoordsForPos(level, this.centerPos.add(Vec3.broadcast(radiusCur)));
				for (int x = curMin.x; x <= curMax.x; ++x) {
					for (int y = curMin.y; y <= curMax.y; ++y) {
						for (int z = curMin.z; z <= curMax.z; ++z) {
							final var spos = new SectorPos(level, new Vec3i(x, y, z));
							if (isInside(spos) && !consumer.test(spos))
								return;
						}
					}
				}
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

		@Override
		public boolean shouldUpdate(SectorTicketInfo currentInfo) {
			if (currentInfo instanceof Multi other) {
				final var d = this.centerPos.distanceTo(other.centerPos);
				// very small changes are not likely to be noticeable
				return d >= 0.01 * other.baseRadius;
			}
			return true;
		}

		@Override
		public String toString() {
			return String.format("Multi{center:%s, baseRadius:%f, ...}",
					this.centerPos.toString(), this.baseRadius);
		}
	}

}
