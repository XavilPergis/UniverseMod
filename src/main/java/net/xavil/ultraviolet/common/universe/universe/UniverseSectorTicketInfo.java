package net.xavil.ultraviolet.common.universe.universe;

import java.util.function.Consumer;

import net.xavil.hawklib.collections.interfaces.ImmutableSet;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;

public abstract sealed class UniverseSectorTicketInfo {

	public record Diff(ImmutableSet<Vec3i> added, ImmutableSet<Vec3i> removed) {
		public static final Diff EMPTY = new Diff(ImmutableSet.of(), ImmutableSet.of());
	}

	public static Multi visual(Vec3 centerPos) {
		return new Multi(centerPos, Universe.VOLUME_LENGTH_ZM);
	}

	public static Single single(Vec3i pos) {
		return new Single(pos);
	}

	// must return the same type
	public abstract UniverseSectorTicketInfo copy();

	public ImmutableSet<Vec3i> affectedSectors() {
		final var sectors = MutableSet.<Vec3i>hashSet();
		enumerateAffectedSectors(sectors::insert);
		return sectors;
	}

	public abstract void enumerateAffectedSectors(Consumer<Vec3i> consumer);

	public abstract Diff diff(UniverseSectorTicketInfo prev);

	public static final class Single extends UniverseSectorTicketInfo {
		public Vec3i sector;

		public Single(Vec3i sector) {
			this.sector = sector;
		}

		@Override
		public Single copy() {
			return new Single(sector);
		}

		@Override
		public ImmutableSet<Vec3i> affectedSectors() {
			return ImmutableSet.of(this.sector);
		}

		@Override
		public void enumerateAffectedSectors(Consumer<Vec3i> consumer) {
			consumer.accept(this.sector);
		}

		@Override
		public Diff diff(UniverseSectorTicketInfo prev) {
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

	public static final class Multi extends UniverseSectorTicketInfo {
		public Vec3 centerPos;
		public double radius;

		public Multi(Vec3 centerPos, double radius) {
			this.centerPos = centerPos;
			this.radius = radius;
		}

		@Override
		public Multi copy() {
			return new Multi(centerPos, radius);
		}

		@Override
		public void enumerateAffectedSectors(Consumer<Vec3i> consumer) {
			final var curMin = this.centerPos.sub(Vec3.broadcast(this.radius))
					.div(Universe.VOLUME_LENGTH_ZM).floor();
			final var curMax = this.centerPos.add(Vec3.broadcast(this.radius))
					.div(Universe.VOLUME_LENGTH_ZM).ceil();
			Vec3i.iterateInclusive(curMin, curMax, consumer);
		}

		@Override
		public Diff diff(UniverseSectorTicketInfo prev) {
			if (prev instanceof Multi multi) {
				final var added = MutableSet.<Vec3i>hashSet();
				final var removed = MutableSet.<Vec3i>hashSet();

				final var curMin = this.centerPos.sub(Vec3.broadcast(this.radius))
						.div(Universe.VOLUME_LENGTH_ZM).floor();
				final var curMax = this.centerPos.add(Vec3.broadcast(this.radius))
						.div(Universe.VOLUME_LENGTH_ZM).ceil();
				final var prevMin = multi.centerPos.sub(Vec3.broadcast(multi.radius))
						.div(Universe.VOLUME_LENGTH_ZM).floor();
				final var prevMax = multi.centerPos.add(Vec3.broadcast(multi.radius))
						.div(Universe.VOLUME_LENGTH_ZM).ceil();

				if (!curMin.equals(prevMin) || !curMax.equals(prevMax)) {
					final var levelCur = MutableSet.<Vec3i>hashSet();
					final var levelPrev = MutableSet.<Vec3i>hashSet();
					Vec3i.iterateInclusive(curMin, curMax, levelCur::insert);
					Vec3i.iterateInclusive(prevMin, prevMax, levelPrev::insert);
					added.extend(levelCur.difference(levelPrev));
					removed.extend(levelPrev.difference(levelCur));
				}

				return new Diff(added, removed);
			}
			return Diff.EMPTY;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Multi other) {
				return this.centerPos.equals(other.centerPos) && this.radius == other.radius;
			}
			return false;
		}
	}

	public static final class Cube extends UniverseSectorTicketInfo {
		public Vec3i centerSector;
		public int radius; // radius of 0 means just the center.

		public Cube(Vec3i centerSector, int radius) {
			this.centerSector = centerSector;
			this.radius = radius;
		}

		@Override
		public Cube copy() {
			return new Cube(this.centerSector, this.radius);
		}

		@Override
		public void enumerateAffectedSectors(Consumer<Vec3i> consumer) {
			final var curMin = this.centerSector.sub(Vec3i.broadcast(this.radius));
			final var curMax = this.centerSector.add(Vec3i.broadcast(this.radius));
			Vec3i.iterateInclusive(curMin, curMax, consumer);
		}

		@Override
		public Diff diff(UniverseSectorTicketInfo prev) {
			if (prev instanceof Cube other) {
				final var added = MutableSet.<Vec3i>hashSet();
				final var removed = MutableSet.<Vec3i>hashSet();

				final var curMin = this.centerSector.sub(Vec3i.broadcast(this.radius));
				final var curMax = this.centerSector.add(Vec3i.broadcast(this.radius));
				final var prevMin = other.centerSector.sub(Vec3i.broadcast(other.radius));
				final var prevMax = other.centerSector.add(Vec3i.broadcast(other.radius));

				if (!curMin.equals(prevMin) || !curMax.equals(prevMax)) {
					final var levelCur = MutableSet.<Vec3i>hashSet();
					final var levelPrev = MutableSet.<Vec3i>hashSet();
					Vec3i.iterateInclusive(curMin, curMax, levelCur::insert);
					Vec3i.iterateInclusive(prevMin, prevMax, levelPrev::insert);
					added.extend(levelCur.difference(levelPrev));
					removed.extend(levelPrev.difference(levelCur));
				}

				return new Diff(added, removed);
			}
			return Diff.EMPTY;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Cube other) {
				return this.centerSector.equals(other.centerSector) && this.radius == other.radius;
			}
			return false;
		}
	}

}
