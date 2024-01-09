package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;

public final class SectorManager {

	public final class SectorTicketTracker {
		/**
		 * This specific ticket instance is shared with the outside world, which can
		 * make arbitrary changes.
		 */
		public final SectorTicket<?> loanedTicket;
		/**
		 * This ticket info represents the sectors that this ticket is currently keeping
		 * loaded.
		 */
		private SectorTicketInfo currentInfo = null;
		/**
		 * The current set of sectors covered by currentInfo
		 */
		private MutableSet<SectorPos> currentSectors = MutableSet.hashSet();
		// scratch space
		private MutableSet<SectorPos> newSectors = MutableSet.hashSet();

		public SectorTicketTracker(SectorTicket<?> loanedTicket) {
			this.loanedTicket = loanedTicket;
		}

		public void update(MutableSet<SectorPos> toLoad, MutableSet<SectorPos> toUnload) {
			final var cur = this.currentInfo;
			final var next = this.loanedTicket.info;
			if (Objects.equals(cur, next))
				return;
			if (cur != null && next != null && !next.shouldUpdate(cur))
				return;

			if (cur == null && next != null) {
				// empty -> full
				this.loanedTicket.info.enumerateAffectedSectors(toLoad::insert);
			} else if (cur != null && next == null) {
				// full -> empty
				this.loanedTicket.info.enumerateAffectedSectors(toUnload::insert);
			} else {
				// full -> full
				this.loanedTicket.info.enumerateAffectedSectors(this.newSectors::insert);

				// find differences between previously-loaded sectors and newly-loaded sectors
				this.currentSectors.forEach(sector -> {
					if (!this.newSectors.contains(sector))
						toUnload.insert(sector);
				});
				this.newSectors.forEach(sector -> {
					if (!this.currentSectors.contains(sector))
						toLoad.insert(sector);
				});

				final var tmp = this.newSectors;
				this.newSectors = this.currentSectors;
				this.currentSectors = tmp;

				this.newSectors.clear();
			}
			this.currentInfo = next != null ? next.clone() : null;
		}
	}

	public final class SystemTicketTracker {
		public final SystemTicket loanedTicket;
		private GalaxySectorId currentId = null;

		public SystemTicketTracker(SystemTicket loanedTicket) {
			this.loanedTicket = loanedTicket;
		}

		public void update(MutableSet<SectorPos> sectorsToLoad, MutableSet<SectorPos> sectorsToUnload,
				MutableSet<GalaxySectorId> toLoad, MutableSet<GalaxySectorId> toUnload) {
			final var cur = this.currentId;
			final var next = this.loanedTicket.id;
			if (Objects.equals(cur, next))
				return;

			// NOTE: system tickets force the sectors they reside within to be loaded
			if (cur != null && next != null) {
				toLoad.insert(next);
				toUnload.insert(cur);
				if (!Objects.equals(cur.sectorPos(), next.sectorPos())) {
					sectorsToLoad.insert(next.sectorPos());
					sectorsToUnload.insert(cur.sectorPos());
				}
			} else if (cur == null && next != null) {
				toLoad.insert(next);
				sectorsToLoad.insert(next.sectorPos());
			} else if (cur != null && next == null) {
				toUnload.insert(cur);
				sectorsToUnload.insert(cur.sectorPos());
			}

			this.currentId = next;
		}
	}

	private final class SectorSlot {
		// system slots have a 1:1 correlation with the systems they contain, but sector
		// slots are created only for each sector root, so this class needs to keep
		// track of multiple sectors at once.
		public final GalaxySector sector;

		public final class SectorFutures {
			public final CompletableFuture<GalaxySector.PackedElements> elementFuture;
			public final CompletableFuture<GalaxySector> sectorFuture;

			public SectorFutures(Galaxy galaxy, GalaxySector sector) {
				this.elementFuture = new CompletableFuture<>();
				this.elementFuture.completeAsync(() -> galaxy.generateSectorElements(sector.pos()));
				this.sectorFuture = new CompletableFuture<>();
			}
		}

		public final MutableMap<SectorPos, SectorFutures> sectorFutures = MutableMap
				.hashMap();

		public SectorSlot(Vec3i coords) {
			this.sector = new GalaxySector(coords);
		}

		private boolean isSectorLoaded(SectorPos pos) {
			final var sector = this.sector.lookupNode(pos);
			return sector != null && sector.isLoaded();
		}

		public void load(SectorPos pos) {
			final var isInitialLoad = !isSectorLoaded(pos);
			this.sector.load(pos);
			if (!isInitialLoad)
				return;
			final var sector = this.sector.lookupNode(pos);
			if (sector == null) {
				Mod.LOGGER.error("tried to generate sector that was not marked as loaded. ({})", pos);
				return;
			}

			final var prev = sectorFutures.insert(pos, new SectorFutures(galaxy, sector));
			Assert.isTrue(prev.isNone());
		}

		public boolean unload(SectorPos pos) {
			final var futures = this.sectorFutures.remove(pos).unwrapOrNull();
			if (futures != null)
				futures.elementFuture.cancel(false);

			final var sector = this.sector.lookupNode(pos);
			if (sector != null) {
				try {
					return this.sector.unload(pos);
				} catch (GalaxySector.InvalidUnloadException ex) {
				}
			}

			// prevent unloads of non-existent sectors from completely fucking up the
			// reference counts of its would-be parent sectors.
			Mod.LOGGER.error("tried to unload sector that was not marked as loaded. ({})", pos);
			return !this.sector.isLoadedTransitively();
		}
	}

	private final class SystemSlot {
		public final GalaxySectorId id;
		public int referenceCount = 0;

		public boolean generationFailed = false;
		public boolean isLoaded = false;
		public StarSystem system;
		public CompletableFuture<Maybe<StarSystem>> waitingFuture = null;

		public SystemSlot(GalaxySectorId id) {
			this.id = id;
		}

		public void load() {
			this.referenceCount += 1;
			if (this.system != null || this.waitingFuture != null || this.generationFailed)
				return;

			final var sectorPos = this.id.sectorPos();
			final var sectorSlot = sectorMap.get(sectorPos.rootCoords()).unwrap();

			final var futures = sectorSlot.sectorFutures.getOrNull(sectorPos);
			if (futures != null) {
				this.waitingFuture = futures.sectorFuture.thenApplyAsync(sector -> generateSystem(sector, this.id));
			} else {
				final var sector = sectorSlot.sector.lookupNode(sectorPos);
				if (sector != null && sector.isComplete()) {
					this.waitingFuture = CompletableFuture.supplyAsync(() -> generateSystem(sector, this.id));
				} else {
					Mod.LOGGER.warn("sector generation failure caused system {} generation to fail", this.id);
				}
			}
		}

		public boolean unload() {
			this.referenceCount -= 1;
			if (this.referenceCount == 0) {
				if (this.waitingFuture != null)
					this.waitingFuture.cancel(false);
				return true;
			}
			return false;
		}
	}

	private final Galaxy galaxy;

	private final MutableMap<Vec3i, SectorSlot> sectorMap = MutableMap.hashMap();
	private final MutableList<SectorTicketTracker> trackedTickets = new Vector<>();
	private final MutableList<SectorTicketInfo> removedTickets = new Vector<>();

	private final MutableMap<GalaxySectorId, SystemSlot> systemMap = MutableMap.hashMap();
	private final MutableList<SystemTicketTracker> trackedSystemTickets = new Vector<>();
	private final MutableList<SystemTicket> removedSystemTickets = new Vector<>();

	private final MutableSet<SectorTicket<?>> allSectorTickets = MutableSet.identityHashSet();
	private final MutableSet<SystemTicket> allSystemTickets = MutableSet.identityHashSet();

	public SectorManager(Galaxy galaxy) {
		this.galaxy = galaxy;
	}

	public int getReferenceCount() {
		return this.trackedTickets.size() + this.trackedSystemTickets.size();
	}

	public void forceLoad(SectorTicket<?> sectorTicket) {
		forceLoad(InactiveProfiler.INSTANCE, sectorTicket);
	}

	public double percentComplete(SectorTicket<?> sectorTicket) {
		final var res = new Object() {
			int total = 0;
			int complete = 0;
		};
		sectorTicket.info.enumerateAffectedSectors(pos -> {
			res.total += 1;
			res.complete += isComplete(pos) ? 1 : 0;
			return true;
		});
		return res.complete / (double) res.total;
	}

	public void forceLoad(ProfilerFiller profiler, SectorTicket<?> sectorTicket) {
		sectorTicket.info.enumerateAllAffectedSectors(pos -> this.sectorMap.get(pos.rootCoords()).ifSome(slot -> {
			final var futures = slot.sectorFutures.getOrNull(pos);
			if (futures != null)
				futures.elementFuture.join();
		}));
		applyFinished();
	}

	public Maybe<StarSystem> forceLoad(SystemTicket systemTicket) {
		return forceLoad(InactiveProfiler.INSTANCE, systemTicket);
	}

	private void validateSystemTicket(SystemTicket ticket) {
		if (this != ticket.attachedManager) {
			throw new IllegalArgumentException(String.format(
					"ticket's attached manager is not this manager!"));
		}
		if (this.trackedSystemTickets.iter().all(t -> t.loanedTicket != ticket)) {
			throw new IllegalArgumentException(String.format(
					"ticket was removed from this manager!"));
		}
	}

	public Maybe<StarSystem> forceLoad(ProfilerFiller profiler, SystemTicket systemTicket) {
		validateSystemTicket(systemTicket);

		// ticket doesn't actually load anything lol
		if (systemTicket.id == null)
			return Maybe.none();

		final var systemSlot = this.systemMap.get(systemTicket.id).unwrap();

		// ticket's system is already loaded!
		if (systemSlot.system != null)
			return Maybe.some(systemSlot.system);

		final var sectorPos = systemTicket.id.sectorPos();
		final var sectorSlot = this.sectorMap.get(sectorPos.rootCoords()).unwrap();
		final var sector = sectorSlot.sector.lookupNode(sectorPos);
		if (!sector.isComplete()) {
			final var futures = sectorSlot.sectorFutures.get(sectorPos).unwrap();
			futures.elementFuture.join();
			applyFinished();
		}
		if (sector.elements.size() <= systemTicket.id.elementIndex()) {
			return Maybe.none();
		}
		if (systemSlot.waitingFuture == null)
			return Maybe.none();
		final var system = systemSlot.waitingFuture.join();
		applyFinished();
		return system;
	}

	public void tick(ProfilerFiller profiler) {
		tickGeneration(profiler);
		final var time = this.galaxy.parentUniverse.getCelestialTime();
		this.systemMap.values().forEach(slot -> {
			if (slot.system != null) {
				slot.system.rootNode.visit(node -> {
					Vec3.set(node.lastPosition, node.position);
				});
				slot.system.rootNode.updatePositions(time);
			}
		});
	}

	public void applyTickets(ProfilerFiller profiler) {
		profiler.push("collect");
		final var systemsToLoad = MutableSet.<GalaxySectorId>hashSet();
		final var systemsToUnload = MutableSet.<GalaxySectorId>hashSet();
		final var sectorsToLoad = MutableSet.<SectorPos>hashSet();
		final var sectorsToUnload = MutableSet.<SectorPos>hashSet();

		for (final var ticket : this.trackedSystemTickets.iterable())
			ticket.update(sectorsToLoad, sectorsToUnload, systemsToLoad, systemsToUnload);
		for (final var ticket : this.trackedTickets.iterable())
			ticket.update(sectorsToLoad, sectorsToUnload);

		// unload removed systems
		systemsToUnload.extend(this.removedSystemTickets.iter().map(ticket -> ticket.id));
		this.removedSystemTickets.clear();

		// unload removed sectors
		sectorsToUnload.extend(this.removedTickets.iter().flatMap(SectorTicketInfo::allAffectedSectors));
		this.removedTickets.clear();

		profiler.popPush("load");
		sectorsToLoad.forEach(pos -> {
			final var slot = this.sectorMap.entry(pos.rootCoords()).orInsertWithKey(SectorSlot::new);
			slot.load(pos);
		});
		systemsToLoad.forEach(id -> {
			final var slot = this.systemMap.entry(id).orInsertWithKey(SystemSlot::new);
			slot.load();
		});

		profiler.popPush("unload");
		systemsToUnload.forEach(id -> {
			final var slot = this.systemMap.get(id).unwrapOrNull();
			if (slot != null) {
				if (slot.unload())
					this.systemMap.remove(id);
			} else {
				Mod.LOGGER.error("tried to unload system that did not have a system slot. ({})", id);
			}
		});
		sectorsToUnload.forEach(pos -> {
			final var slot = this.sectorMap.get(pos.rootCoords()).unwrapOrNull();
			if (slot != null) {
				if (slot.unload(pos))
					this.sectorMap.remove(pos.rootCoords());
			} else {
				Mod.LOGGER.error("tried to unload sector that did not have a sector slot. ({}, root {})",
						pos, pos.rootCoords());
			}
		});

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
			slot.sectorFutures.retain((pos, futures) -> {
				if (!futures.elementFuture.isDone())
					return true;
				final var sector = slot.sector.lookupNode(pos);
				if (sector != null)
					sector.elements = futures.elementFuture.getNow(null);
				futures.sectorFuture.complete(sector);
				return false;
			});
		}

		for (final var slot : this.systemMap.values().iterable()) {
			if (slot.waitingFuture != null && slot.waitingFuture.isDone()) {
				final var systemOpt = slot.waitingFuture.join();
				if (systemOpt.isNone()) {
					Mod.LOGGER.error("failed to generate system {}", slot.id);
					slot.generationFailed = true;
				} else {
					slot.system = systemOpt.unwrap();
				}
				slot.waitingFuture = null;
				slot.isLoaded = true;
			}
		}
	}

	private Maybe<StarSystem> generateSystem(GalaxySector sector, GalaxySectorId id) {
		if (id.elementIndex() >= sector.elements.size())
			return Maybe.none();
		try {
			final var elem = new GalaxySector.ElementHolder();
			sector.elements.load(elem, id.elementIndex());
			return Maybe.some(this.galaxy.generateFullSystem(sector, id, elem));
		} catch (Throwable t) {
			Mod.LOGGER.error("failed to generate system because of an exception!");
			t.printStackTrace();
		}
		return Maybe.none();
	}

	public boolean isLoaded(SectorPos pos) {
		final var slot = this.sectorMap.get(pos.rootCoords());
		if (slot.isNone())
			return false;
		final var sector = slot.unwrap().sector.lookupNode(pos);
		return sector == null ? false : sector.isLoaded();
	}

	public boolean isComplete(SectorPos pos) {
		final var slot = this.sectorMap.get(pos.rootCoords());
		if (slot.isNone())
			return false;
		final var sector = slot.unwrap().sector.lookupNode(pos);
		return sector == null ? false : sector.isComplete();
	}

	public <T extends SectorTicketInfo> SectorTicket<T> createSectorTicket(Disposable.Multi disposer, T info) {
		return disposer.attach(createSectorTicketManual(info));
	}

	@SuppressWarnings("unchecked")
	public <T extends SectorTicketInfo> SectorTicket<T> createSectorTicketManual(T info) {
		Mod.LOGGER.info("creating sector ticket for '{}'", info);
		final var ticket = new SectorTicket<>(this, info == null ? null : info.clone());
		this.trackedTickets.push(new SectorTicketTracker(ticket));
		this.allSectorTickets.insert(ticket);
		applyTickets(InactiveProfiler.INSTANCE);
		return (SectorTicket<T>) ticket;
	}

	public void removeSectorTicket(SectorTicket<?> ticket) {
		if (!this.allSectorTickets.remove(ticket)) {
			Mod.LOGGER.warn("tried removing sector ticket for '{}', but it was already removed.", ticket.info);
			return;
		}
		this.trackedTickets.retain(tracker -> tracker.loanedTicket != ticket);
		if (ticket.info != null)
			this.removedTickets.push(ticket.info);
		applyTickets(InactiveProfiler.INSTANCE);
	}

	public SystemTicket createSystemTicket(Disposable.Multi disposer, GalaxySectorId id) {
		return disposer.attach(createSystemTicketManual(id));
	}

	public SystemTicket createSystemTicketManual(GalaxySectorId id) {
		Mod.LOGGER.info("creating system ticket for {}", id);
		final var ticket = new SystemTicket(this, id);
		this.trackedSystemTickets.push(new SystemTicketTracker(ticket));
		this.allSystemTickets.insert(ticket);
		applyTickets(InactiveProfiler.INSTANCE);
		return ticket;
	}

	public void removeSystemTicket(SystemTicket ticket) {
		if (!this.allSystemTickets.remove(ticket)) {
			Mod.LOGGER.warn("tried removing system ticket for '{}', but it was already removed.", ticket.id);
			return;
		}
		Mod.LOGGER.info("removing system ticket for '{}'", ticket.id);
		this.trackedSystemTickets.retain(tracked -> tracked.loanedTicket != ticket);
		this.removedSystemTickets.push(ticket);
		applyTickets(InactiveProfiler.INSTANCE);
	}

	public Maybe<GalaxySector> getSector(SectorPos pos) {
		final var slot = this.sectorMap.get(pos.rootCoords());
		return slot.map(s -> s.sector.lookupNode(pos)).filter(sector -> sector.isComplete());
	}

	public boolean loadElement(GalaxySector.ElementHolder out, GalaxySectorId id) {
		final var slot = this.sectorMap.getOrNull(id.sectorPos().rootCoords());
		if (slot == null)
			return false;
		final var sector = slot.sector.lookupNode(id.sectorPos());
		if (!sector.isComplete())
			return false;
		sector.loadElement(out, id.elementIndex());
		return true;
	}

	public Maybe<StarSystem> getSystem(GalaxySectorId id) {
		return this.systemMap.get(id).flatMap(sys -> Maybe.fromNullable(sys.system));
	}

	public boolean isSystemLoaded(GalaxySectorId id) {
		final var slot = this.systemMap.get(id).unwrapOrNull();
		if (slot == null)
			return false;
		return slot.isLoaded;
	}

	public void enumerate(SectorTicket<?> ticket, Consumer<GalaxySector> sectorConsumer) {
		if (ticket.info == null)
			return;
		ticket.info.enumerateAllAffectedSectors(pos -> this.sectorMap.get(pos.rootCoords()).ifSome(slot -> {
			final var sector = slot.sector.lookupNode(pos);
			if (sector != null && sector.isComplete())
				sectorConsumer.accept(sector);
		}));
	}

}
