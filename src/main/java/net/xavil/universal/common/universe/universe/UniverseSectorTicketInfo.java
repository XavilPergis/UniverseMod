package net.xavil.universal.common.universe.universe;

import java.util.function.Consumer;

import net.xavil.util.collections.interfaces.ImmutableSet;
import net.xavil.util.collections.interfaces.MutableSet;
import net.xavil.util.math.matrices.Vec3;
import net.xavil.util.math.matrices.Vec3i;

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
			final var curMin = this.centerPos.sub(Vec3.broadcast(this.radius)).floor();
			final var curMax = this.centerPos.add(Vec3.broadcast(this.radius)).ceil();
			Vec3i.iterateInclusive(curMin, curMax, consumer);
		}

		@Override
		public Diff diff(UniverseSectorTicketInfo prev) {
			if (prev instanceof Multi multi) {
				final var added = MutableSet.<Vec3i>hashSet();
				final var removed = MutableSet.<Vec3i>hashSet();

				final var curMin = this.centerPos.sub(Vec3.broadcast(this.radius)).floor();
				final var curMax = this.centerPos.add(Vec3.broadcast(this.radius)).ceil();
				final var prevMin = multi.centerPos.sub(Vec3.broadcast(multi.radius)).floor();
				final var prevMax = multi.centerPos.add(Vec3.broadcast(multi.radius)).ceil();

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

}
