package net.xavil.universal.common.universe.universe;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.SectorPos;
import net.xavil.universal.common.universe.galaxy.SectorTicketInfo;
import net.xavil.universal.common.universe.galaxy.SectorTicket;
import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.universal.common.universe.id.UniverseSectorId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.util.Assert;
import net.xavil.util.Disposable;
import net.xavil.util.KeyedTaskQueue;
import net.xavil.util.Option;
import net.xavil.util.ThreadSignal;
import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.ImmutableList;
import net.xavil.util.collections.interfaces.MutableList;
import net.xavil.util.collections.interfaces.MutableMap;
import net.xavil.util.collections.interfaces.MutableMultiMap;
import net.xavil.util.collections.interfaces.MutableSet;
import net.xavil.util.iterator.Iterator;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec3i;

public final class UniverseSectorManager {

	public final class SectorTicketTracker {
		public final UniverseSectorTicket loanedTicket;
		private Option<UniverseSectorTicketInfo> prevInfo = Option.none();

		public SectorTicketTracker(UniverseSectorTicket loanedTicket) {
			this.loanedTicket = loanedTicket;
		}

		public void update(MutableSet<Vec3i> toLoad, MutableSet<Vec3i> toUnload) {
			final var prev = this.prevInfo;
			final var cur = this.loanedTicket.info;
			if (prev.innerEquals(cur))
				return;
			if (this.prevInfo.isNone()) {
				toLoad.extend(this.loanedTicket.info.affectedSectors());
			} else {
				final var diff = this.loanedTicket.info.diff(this.prevInfo.unwrap());
				toLoad.extend(diff.added());
				toUnload.extend(diff.removed());
			}
			this.prevInfo = Option.some(cur.copy());
		}
	}

	public final class GalaxyTicketTracker {
		public final GalaxyTicket loanedTicket;
		private Option<UniverseSectorId> prevId = Option.none();

		public GalaxyTicketTracker(GalaxyTicket loanedTicket) {
			this.loanedTicket = loanedTicket;
		}
	}

	// private final class SectorSlot {
	// public final UniverseSector sector;
	// public int referenceCount = 0;
	// public boolean isGenerationQueued = false;

	// // these fields need synchronization
	// public boolean isGenerated = false;
	// public final MutableList<ThreadSignal> listeners = new Vector<>();
	// public final MutableSet<GalaxySlot> waitingGalaxies = MutableSet.hashSet();

	// public SectorSlot(Vec3i coords) {
	// this.sector = new UniverseSector(coords);
	// }

	// public synchronized void addGenerationListener(ThreadSignal listener) {
	// if (this.isGenerated) {
	// listener.signal();
	// } else {
	// this.listeners.push(listener);
	// }
	// }

	// public synchronized void addWaitingGalaxy(GalaxySlot slot) {
	// if (!this.isGenerated)
	// this.waitingGalaxies.insert(slot);
	// }

	// public synchronized void notifyGenerationFinished() {
	// this.isGenerated = true;
	// this.listeners.forEach(listener -> listener.signal());
	// this.listeners.clear();
	// this.waitingGalaxies.forEach(slot -> slot.sectorGenerationListener.signal());
	// }

	// public synchronized Iterator<GalaxySlot> drainWaiting(Consumer<GalaxySlot>
	// consumer) {
	// final var set = MutableSet.hashSet(this.waitingGalaxies);
	// this.waitingGalaxies.clear();
	// return set.iter();
	// }
	// }

	private final class SectorSlot {
		public int referenceCount = 0;

		public final UniverseSector sector;
		public CompletableFuture<UniverseSector> waitingFuture = null;

		public SectorSlot(Vec3i coords) {
			this.sector = new UniverseSector(coords);
		}

		public CompletableFuture<UniverseSector> getSectorFuture() {
			return this.waitingFuture == null ? CompletableFuture.completedFuture(this.sector) : this.waitingFuture;
		}

		public void load() {
			this.referenceCount += 1;
			if (this.sector.isComplete() || this.waitingFuture != null)
				return;
			this.waitingFuture = CompletableFuture.supplyAsync(() -> {
				final var elements = universe.generateSectorElements(this.sector.pos);
				synchronized (this.sector) {
					if (!Thread.interrupted())
						sector.initialElements = elements;
				}
				return sector;
			});
		}

		public void unload() {
			this.referenceCount -= 1;
			if (this.referenceCount == 0) {
				if (this.waitingFuture != null)
					synchronized (this.sector) {
						this.waitingFuture.cancel(true);
					}
				sectorMap.remove(this.sector.pos);
			}
		}
	}

	private final class GalaxySlot {
		public final UniverseSectorId id;
		public int referenceCount = 0;

		public Galaxy galaxy;
		public CompletableFuture<Galaxy> waitingFuture = null;

		public GalaxySlot(UniverseSectorId id) {
			this.id = id;
		}

		public void load() {
			this.referenceCount += 1;
			if (this.galaxy != null || this.waitingFuture != null)
				return;
			final var sectorSlot = sectorMap.get(this.id.sectorPos()).unwrap();
			this.waitingFuture = sectorSlot.getSectorFuture().thenApplyAsync(sector -> generateGalaxy(this.id));
		}

		public void unload() {
			if (this.referenceCount > 0)
				this.referenceCount -= 1;
			if (this.referenceCount == 0) {
				if (this.waitingFuture != null)
					this.waitingFuture.cancel(false);
			}
			if (!isLive())
				galaxyMap.remove(this.id);
		}

		public boolean isLive() {
			return this.referenceCount > 0
					|| (this.galaxy != null && this.galaxy.sectorManager.getReferenceCount() > 0);
		}
	}

	private final Universe universe;

	private final MutableMap<Vec3i, SectorSlot> sectorMap = MutableMap.hashMap();
	private final MutableList<SectorTicketTracker> trackedTickets = new Vector<>();
	private final MutableList<UniverseSectorTicketInfo> removedTickets = new Vector<>();

	private final MutableMap<UniverseSectorId, GalaxySlot> galaxyMap = MutableMap.hashMap();
	private final MutableList<GalaxyTicketTracker> trackedGalaxyTickets = new Vector<>();
	private final MutableList<GalaxyTicket> removedGalaxyTickets = new Vector<>();

	public UniverseSectorManager(Universe universe) {
		this.universe = universe;
	}

	public void forceLoad(UniverseSectorTicket sectorTicket) {
		forceLoad(InactiveProfiler.INSTANCE, sectorTicket);
	}

	public void forceLoad(ProfilerFiller profiler, UniverseSectorTicket sectorTicket) {
		sectorTicket.info.affectedSectors().forEach(pos -> this.sectorMap.get(pos).ifSome(slot -> {
			if (slot.waitingFuture != null)
				slot.waitingFuture.join();
		}));
		applyFinished();
	}

	public Option<Galaxy> forceLoad(GalaxyTicket galaxyTicket) {
		return forceLoad(InactiveProfiler.INSTANCE, galaxyTicket);
	}

	public Option<Galaxy> forceLoad(ProfilerFiller profiler, GalaxyTicket galaxyTicket) {
		final var galaxySlot = this.galaxyMap.get(galaxyTicket.id).unwrap();
		if (galaxySlot.galaxy != null)
			return Option.some(galaxySlot.galaxy);
		final var sectorPos = galaxyTicket.id.sectorPos();
		final var sectorSlot = this.sectorMap.get(sectorPos).unwrap();
		final var sector = sectorSlot.sector;
		if (!sector.isComplete()) {
			sectorSlot.waitingFuture.join();
			Assert.isTrue(sector.isComplete());
		}
		if (sector.initialElements.size() <= galaxyTicket.id.id()) {
			return Option.none();
		}
		final var galaxy = galaxySlot.waitingFuture.join();
		applyFinished();
		return Option.some(galaxy);
	}

	public void tick(ProfilerFiller profiler) {
		profiler.push("tick_universe_generation");
		tickGeneration(profiler);
		profiler.popPush("tick_galaxies");

		final var deadGalaxies = MutableSet.<GalaxySlot>hashSet();
		for (final var slot : this.galaxyMap.values().iterable()) {
			if (slot.galaxy != null)
				slot.galaxy.tick(profiler);
			if (!slot.isLive())
				deadGalaxies.insert(slot);
		}
		deadGalaxies.forEach(slot -> slot.unload());
		profiler.pop();
	}

	public void applyTickets(ProfilerFiller profiler) {
		profiler.push("collect");
		final var galaxiesToLoad = MutableSet.<UniverseSectorId>hashSet();
		final var galaxiesToUnload = MutableSet.<UniverseSectorId>hashSet();
		for (final var ticket : this.trackedGalaxyTickets.iterable()) {
			final var prev = ticket.prevId;
			final var cur = Option.fromNullable(ticket.loanedTicket.id);
			if (cur.equals(prev))
				continue;
			prev.ifSome(galaxiesToUnload::insert);
			cur.ifSome(galaxiesToLoad::insert);
			ticket.prevId = cur;
		}

		final var sectorsToLoad = MutableSet.<Vec3i>hashSet();
		final var sectorsToUnload = MutableSet.<Vec3i>hashSet();
		for (final var ticket : this.trackedTickets.iterable()) {
			ticket.update(sectorsToLoad, sectorsToUnload);
		}

		galaxiesToUnload.extend(this.removedGalaxyTickets.iter().map(ticket -> ticket.id));
		sectorsToUnload.extend(this.removedTickets.iter().flatMap(UniverseSectorTicketInfo::affectedSectors));
		this.removedGalaxyTickets.clear();
		this.removedTickets.clear();

		sectorsToLoad.extend(galaxiesToLoad.iter().map(UniverseSectorId::sectorPos));
		sectorsToUnload.extend(galaxiesToUnload.iter().map(UniverseSectorId::sectorPos));

		profiler.popPush("load");
		sectorsToLoad.forEach(pos -> this.sectorMap.entry(pos).orInsertWith(SectorSlot::new).load());
		galaxiesToLoad.forEach(id -> this.galaxyMap.entry(id).orInsertWith(GalaxySlot::new).load());

		profiler.popPush("unload");
		galaxiesToUnload.forEach(id -> this.galaxyMap.get(id).ifSome(slot -> slot.unload()));
		sectorsToUnload.forEach(pos -> this.sectorMap.get(pos).ifSome(slot -> slot.unload()));

		profiler.pop();
	}

	public void tickGeneration(ProfilerFiller profiler) {
		profiler.push("apply");
		applyTickets(profiler);
		profiler.popPush("drain");
		applyFinished();
		profiler.pop();
	}

	private void applyFinished() {
		for (final var slot : this.sectorMap.values().iterable()) {
			if (slot.waitingFuture.isDone()) {
				// NOTE: this future sets the elements for this slot, so we dont have to do
				// anything else here.
				slot.waitingFuture = null;
			}
		}
		for (final var slot : this.galaxyMap.values().iterable()) {
			if (slot.waitingFuture.isDone()) {
				slot.galaxy = slot.waitingFuture.join();
				slot.waitingFuture = null;
			}
		}
	}

	private Galaxy generateGalaxy(UniverseSectorId id) {
		final var slot = this.sectorMap.get(id.sectorPos()).unwrap();
		return this.universe.generateGalaxy(id, slot.sector.lookupInitial(id.id()).info());
	}

	public boolean isLoaded(Vec3i pos) {
		return this.sectorMap.containsKey(pos);
	}

	public boolean isComplete(Vec3i pos) {
		final var slot = this.sectorMap.get(pos);
		return slot.isNone() ? false : slot.unwrap().sector.isComplete();
	}

	public UniverseSectorTicket createSectorTicket(Disposable.Multi disposer, UniverseSectorTicketInfo info) {
		final var ticket = new UniverseSectorTicket(this, info.copy());
		this.trackedTickets.push(new SectorTicketTracker(ticket));
		applyTickets(InactiveProfiler.INSTANCE);
		return disposer.attach(ticket);
	}

	public void removeSectorTicket(UniverseSectorTicket ticket) {
		this.trackedTickets.retain(tracked -> tracked.loanedTicket != ticket);
		this.removedTickets.push(ticket.info);
		applyTickets(InactiveProfiler.INSTANCE);

	}

	public GalaxyTicket createGalaxyTicket(Disposable.Multi disposer, UniverseSectorId id) {
		final var ticket = new GalaxyTicket(this, id);
		this.trackedGalaxyTickets.push(new GalaxyTicketTracker(ticket));
		applyTickets(InactiveProfiler.INSTANCE);
		return disposer.attach(ticket);
	}

	public void removeGalaxyTicket(GalaxyTicket ticket) {
		this.trackedGalaxyTickets.retain(tracked -> tracked.loanedTicket != ticket);
		this.removedGalaxyTickets.push(ticket);
		applyTickets(InactiveProfiler.INSTANCE);
	}

	public Option<UniverseSector> getSector(Vec3i pos) {
		return this.sectorMap.get(pos).map(slot -> slot.sector);
	}

	public Option<UniverseSector.InitialElement> getInitial(UniverseSectorId id) {
		return this.sectorMap.get(id.sectorPos()).map(slot -> slot.sector.lookupInitial(id.id()));
	}

	public Option<Galaxy> getGalaxy(UniverseSectorId id) {
		return this.galaxyMap.get(id).flatMap(slot -> Option.fromNullable(slot.galaxy));
	}

	public void enumerate(SectorTicket ticket, Consumer<UniverseSector> sectorConsumer) {
		ticket.info.enumerateAffectedSectors(pos -> this.sectorMap.get(pos.rootCoords())
				.ifSome(slot -> sectorConsumer.accept(slot.sector)));
	}

}