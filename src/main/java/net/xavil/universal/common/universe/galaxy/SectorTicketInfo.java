package net.xavil.universal.common.universe.galaxy;

import java.util.HashSet;
import java.util.function.Consumer;

import com.google.common.collect.Sets;

import net.xavil.util.collections.interfaces.ImmutableSet;
import net.xavil.util.collections.interfaces.MutableSet;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec3i;

public abstract sealed class SectorTicketInfo {

	public record Diff(ImmutableSet<SectorPos> added, ImmutableSet<SectorPos> removed) {
		public static final Diff EMPTY = new Diff(ImmutableSet.of(), ImmutableSet.of());
	}

	public static Multi visual(Vec3 centerPos) {
		return new Multi(centerPos, GalaxySector.BASE_SIZE_Tm, 0.0, 2.0);
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

	public abstract Diff diff(SectorTicketInfo prev);

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
		public Diff diff(SectorTicketInfo prev) {
			if (prev instanceof Single single) {
				return new Diff(ImmutableSet.of(this.sector), ImmutableSet.of(single.sector));
			}
			return Diff.EMPTY;
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof Single other ? this.sector.equals(other.sector) : false;
		}
	}

	public static final class Multi extends SectorTicketInfo {
		public Vec3 centerPos;
		public double baseRadius;
		public double additiveFactor;
		public double multiplicitaveFactor;

		public Multi(Vec3 centerPos, double baseRadius, double additiveFactor, double multiplicitaveFactor) {
			this.centerPos = centerPos;
			this.baseRadius = baseRadius;
			this.additiveFactor = additiveFactor;
			this.multiplicitaveFactor = multiplicitaveFactor;
		}

		@Override
		public Multi copy() {
			return new Multi(centerPos, baseRadius, additiveFactor, multiplicitaveFactor);
		}

		@Override
		public void enumerateAffectedSectors(Consumer<SectorPos> consumer) {
			double radiusCur = this.baseRadius;
			for (int level = 0; level <= GalaxySector.ROOT_LEVEL; ++level) {
				final var level2 = level;
				final var curMin = GalaxySector.levelCoordsForPos(level, this.centerPos.sub(Vec3.broadcast(radiusCur)));
				final var curMax = GalaxySector.levelCoordsForPos(level, this.centerPos.add(Vec3.broadcast(radiusCur)));
				Vec3i.iterateInclusive(curMin, curMax, pos -> consumer.accept(new SectorPos(level2, pos)));
				radiusCur *= this.multiplicitaveFactor;
				radiusCur += this.additiveFactor;
			}
		}

		@Override
		public Diff diff(SectorTicketInfo prev) {
			if (prev instanceof Multi multi) {
				double radiusCur = this.baseRadius;
				double radiusPrev = multi.baseRadius;
				final var added = MutableSet.<SectorPos>hashSet();
				final var removed = MutableSet.<SectorPos>hashSet();
				for (int level = 0; level <= GalaxySector.ROOT_LEVEL; ++level) {
					final var level2 = level;

					final var curMin = GalaxySector.levelCoordsForPos(level,
							this.centerPos.sub(Vec3.broadcast(radiusCur)));
					final var curMax = GalaxySector.levelCoordsForPos(level,
							this.centerPos.add(Vec3.broadcast(radiusCur)));
					final var prevMin = GalaxySector.levelCoordsForPos(level,
							multi.centerPos.sub(Vec3.broadcast(radiusPrev)));
					final var prevMax = GalaxySector.levelCoordsForPos(level,
							multi.centerPos.add(Vec3.broadcast(radiusPrev)));

					if (!curMin.equals(prevMin) || !curMax.equals(prevMax)) {
						final var levelCur = MutableSet.<SectorPos>hashSet();
						final var levelPrev = MutableSet.<SectorPos>hashSet();
						Vec3i.iterateInclusive(curMin, curMax, pos -> levelCur.insert(new SectorPos(level2, pos)));
						Vec3i.iterateInclusive(prevMin, prevMax, pos -> levelPrev.insert(new SectorPos(level2, pos)));
						added.extend(levelCur.difference(levelPrev));
						removed.extend(levelPrev.difference(levelCur));
					}

					radiusCur *= this.multiplicitaveFactor;
					radiusCur += this.additiveFactor;
					radiusPrev *= multi.multiplicitaveFactor;
					radiusPrev += multi.additiveFactor;
				}
				return new Diff(added, removed);
			}
			return Diff.EMPTY;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Multi other) {
				return this.centerPos.equals(other.centerPos)
						&& this.baseRadius == other.baseRadius
						&& this.additiveFactor == other.additiveFactor
						&& this.multiplicitaveFactor == other.multiplicitaveFactor;
			}
			return false;
		}
	}

}
