package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.function.Consumer;
import java.util.function.Predicate;

import net.xavil.hawklib.collections.interfaces.ImmutableSet;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.math.Intersections;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;

public abstract sealed class SectorTicketInfo {

	public record Diff(ImmutableSet<SectorPos> added, ImmutableSet<SectorPos> removed) {
		public static final Diff EMPTY = new Diff(ImmutableSet.of(), ImmutableSet.of());
	}

	public static Multi visual(Vec3 centerPos) {
		return new Multi(centerPos, GalaxySector.BASE_SIZE_Tm, true);
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

	public void enumerateAllAffectedSectors(Consumer<SectorPos> consumer) {
		enumerateAffectedSectors(pos -> {
			consumer.accept(pos);
			return true;
		});
	}

	public abstract void enumerateAffectedSectors(Predicate<SectorPos> consumer);

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
		public void enumerateAffectedSectors(Predicate<SectorPos> consumer) {
			consumer.test(this.sector);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Single other ? this.sector.equals(other.sector) : false;
		}
	}

	public static final class Multi extends SectorTicketInfo {
		public Vec3 centerPos;
		public double baseRadius;
		/**
		 * If the field is set to {@code true}, then the radius around
		 * {@link #centerPos} that is loaded will double for each successive level.
		 */
		public boolean isMultiplicative;

		public Multi(Vec3 centerPos, double baseRadius, boolean isMultiplicative) {
			this.centerPos = centerPos;
			this.baseRadius = baseRadius;
			this.isMultiplicative = isMultiplicative;
		}

		@Override
		public Multi copy() {
			return new Multi(centerPos, baseRadius, isMultiplicative);
		}

		public double radiusForLevel(int level) {
			double res = this.baseRadius;
			if (this.isMultiplicative)
				res *= (1 << level);
			return res;
		}

		private boolean isInside(SectorPos pos) {
			final var radius = this.baseRadius * (1 << pos.level());
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
	}

}
