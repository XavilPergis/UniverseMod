package net.xavil.universal.common.universe.galaxy;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.util.Assert;
import net.xavil.util.Disposable;
import net.xavil.util.Option;
import net.xavil.util.Util;
import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.MutableList;
import net.xavil.util.collections.interfaces.MutableMap;
import net.xavil.util.collections.interfaces.MutableSet;
import net.xavil.util.math.Vec3i;

public final class SectorManager {

	public final class SectorTicketTracker {
		public final SectorTicket<?> loanedTicket;
		private Option<? extends SectorTicketInfo> prevInfo = Option.none();

		public SectorTicketTracker(SectorTicket<?> loanedTicket) {
			this.loanedTicket = loanedTicket;
		}

		public void update(MutableSet<SectorPos> toLoad, MutableSet<SectorPos> toUnload) {
			final var prev = this.prevInfo;
			final var cur = Option.fromNullable(this.loanedTicket.info);
			if (prev.equals(cur))
				return;
			if (prev.isNone() && cur.isSome()) {
				toLoad.extend(this.loanedTicket.info.affectedSectors());
			} else if (prev.isSome() && cur.isNone()) {
				toUnload.extend(this.loanedTicket.info.affectedSectors());
			} else {
				final var diff = this.loanedTicket.info.diff(prev.unwrap());
				toLoad.extend(diff.added());
				toUnload.extend(diff.removed());
			}
			this.prevInfo = cur.isSome() ? Option.some(cur.unwrap().copy()) : Option.none();
		}
	}

	public final class SystemTicketTracker {
		public final SystemTicket loanedTicket;
		private Option<GalaxySectorId> prevId = Option.none();

		public SystemTicketTracker(SystemTicket loanedTicket) {
			this.loanedTicket = loanedTicket;
		}
	}

	private final class SectorSlot {
		// NOTE: lots of stuff that is tracked per-slot for systems is tracked inside of
		// GalaxySector for sector slots instead.
		public final GalaxySector sector;

		public final MutableMap<SectorPos, CompletableFuture<GalaxySector>> waitingFutures = MutableMap.hashMap();

		public SectorSlot(Vec3i coords) {
			this.sector = new GalaxySector(coords);
		}

		public CompletableFuture<GalaxySector> getSectorFuture(SectorPos pos) {
			return this.waitingFutures.get(pos)
					.unwrapOrElse(() -> CompletableFuture.completedFuture(this.sector.lookupSubtree(pos)));
		}

		private boolean isSectorLoaded(SectorPos pos) {
			final var sector = this.sector.lookupSubtree(pos);
			return sector != null && sector.isLoaded();
		}

		public void load(SectorPos pos) {
			final var isInitialLoad = !isSectorLoaded(pos);
			this.sector.load(pos);
			if (isInitialLoad) {
				final var sector = this.sector.lookupSubtree(pos);
				final var prev = this.waitingFutures.insert(pos,
						Util.makeSupplyFuture(Universe.IS_UNIVERSE_GEN_ASYNC, () -> {
							final var elements = galaxy.generateSectorElements(pos);
							synchronized (sector) {
								if (!Thread.interrupted())
									sector.initialElements = elements;
							}
							return sector;
						}));
				Assert.isTrue(prev.isNone());
			}
		}

		public void unload(SectorPos pos) {
			final var sector = this.sector.lookupSubtree(pos);
			if (sector == null) {
				Mod.LOGGER.error("tried to remove null sector {}", pos);
			} else {
				synchronized (sector) {
					this.waitingFutures.remove(pos).ifSome(future -> future.cancel(true));
				}
			}
			if (this.sector.unload(pos)) {
				sectorMap.remove(pos.rootCoords());
			}
		}
	}

	private final class SystemSlot {
		public final GalaxySectorId id;
		public int referenceCount = 0;

		public boolean generationFailed = false;
		public boolean isLoaded = false;
		public StarSystem system;
		public CompletableFuture<Option<StarSystem>> waitingFuture = null;

		public SystemSlot(GalaxySectorId id) {
			this.id = id;
		}

		public void load() {
			this.referenceCount += 1;
			if (this.system != null || this.waitingFuture != null || this.generationFailed)
				return;
			final var sectorSlot = sectorMap.get(this.id.sectorPos().rootCoords()).unwrap();
			final var sectorFuture = sectorSlot.getSectorFuture(this.id.sectorPos());
			this.waitingFuture = Util.makeApplyFuture(Universe.IS_UNIVERSE_GEN_ASYNC, sectorFuture,
					sector -> generateSystem(sector, this.id));
		}

		public void unload() {
			this.referenceCount -= 1;
			if (this.referenceCount == 0) {
				if (this.waitingFuture != null)
					this.waitingFuture.cancel(false);
				systemMap.remove(this.id);
			}
		}
	}

	private final Galaxy galaxy;

	private final MutableMap<Vec3i, SectorSlot> sectorMap = MutableMap.hashMap();
	private final MutableList<SectorTicketTracker> trackedTickets = new Vector<>();
	private final MutableList<SectorTicketInfo> removedTickets = new Vector<>();
	private final MutableMap<GalaxySectorId, SystemSlot> systemMap = MutableMap.hashMap();
	private final MutableList<SystemTicketTracker> trackedSystemTickets = new Vector<>();
	private final MutableList<SystemTicket> removedSystemTickets = new Vector<>();

	public SectorManager(Galaxy galaxy) {
		this.galaxy = galaxy;
	}

	public int getReferenceCount() {
		return this.trackedTickets.size() + this.trackedSystemTickets.size();
	}

	public void forceLoad(SectorTicket<?> sectorTicket) {
		forceLoad(InactiveProfiler.INSTANCE, sectorTicket);
	}

	public void forceLoad(ProfilerFiller profiler, SectorTicket<?> sectorTicket) {
		sectorTicket.info.affectedSectors().forEach(pos -> this.sectorMap.get(pos.rootCoords()).ifSome(slot -> {
			slot.waitingFutures.get(pos).ifSome(future -> future.join());
		}));
		applyFinished();
	}

	public Option<StarSystem> forceLoad(SystemTicket systemTicket) {
		return forceLoad(InactiveProfiler.INSTANCE, systemTicket);
	}

	public Option<StarSystem> forceLoad(ProfilerFiller profiler, SystemTicket systemTicket) {
		if (systemTicket.id == null)
			return Option.none();
		final var systemSlot = this.systemMap.get(systemTicket.id).unwrap();
		if (systemSlot.system != null)
			return Option.some(systemSlot.system);
		final var sectorPos = systemTicket.id.sectorPos();
		final var sectorSlot = this.sectorMap.get(sectorPos.rootCoords()).unwrap();
		final var sector = sectorSlot.sector.lookupSubtree(sectorPos);
		if (!sector.isComplete()) {
			sectorSlot.waitingFutures.get(sectorPos).unwrap().join();
			Assert.isTrue(sector.isComplete());
		}
		if (sector.initialElements.size() <= systemTicket.id.elementIndex()) {
			return Option.none();
		}
		final var system = systemSlot.waitingFuture.join();
		applyFinished();
		return system;
	}

	public void tick(ProfilerFiller profiler) {
		tickGeneration(profiler);
		final var time = this.galaxy.parentUniverse.getCelestialTime();
		this.systemMap.values().forEach(slot -> {
			if (slot.system != null) {
				slot.system.rootNode.visit(node -> node.lastPosition = node.position);
				slot.system.rootNode.updatePositions(time);
			}
		});
	}

	public void applyTickets(ProfilerFiller profiler) {
		profiler.push("collect");
		final var systemsToLoad = MutableSet.<GalaxySectorId>hashSet();
		final var systemsToUnload = MutableSet.<GalaxySectorId>hashSet();
		for (final var ticket : this.trackedSystemTickets.iterable()) {
			final var prev = ticket.prevId;
			final var cur = Option.fromNullable(ticket.loanedTicket.id);
			if (cur.equals(prev))
				continue;
			prev.ifSome(systemsToUnload::insert);
			cur.ifSome(systemsToLoad::insert);
			ticket.prevId = cur;
		}

		final var sectorsToLoad = MutableSet.<SectorPos>hashSet();
		final var sectorsToUnload = MutableSet.<SectorPos>hashSet();
		for (final var ticket : this.trackedTickets.iterable()) {
			ticket.update(sectorsToLoad, sectorsToUnload);
		}

		systemsToUnload.extend(this.removedSystemTickets.iter().map(ticket -> ticket.id));
		sectorsToUnload.extend(this.removedTickets.iter().flatMap(SectorTicketInfo::affectedSectors));
		this.removedSystemTickets.clear();
		this.removedTickets.clear();

		// system tickets force the sectors they reside within to be loaded
		sectorsToLoad.extend(systemsToLoad.iter().map(GalaxySectorId::sectorPos));
		sectorsToUnload.extend(systemsToUnload.iter().filterNull().map(GalaxySectorId::sectorPos));

		profiler.popPush("load");
		sectorsToLoad.forEach(pos -> this.sectorMap.entry(pos.rootCoords()).orInsertWith(SectorSlot::new).load(pos));
		systemsToLoad.forEach(id -> this.systemMap.entry(id).orInsertWith(SystemSlot::new).load());

		profiler.popPush("unload");
		systemsToUnload.forEach(id -> this.systemMap.get(id).ifSome(slot -> slot.unload()));
		sectorsToUnload.forEach(pos -> this.sectorMap.get(pos.rootCoords()).ifSome(slot -> slot.unload(pos)));

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
			slot.waitingFutures.retain((pos, future) -> !future.isDone());
		}
		for (final var slot : this.systemMap.values().iterable()) {
			if (slot.waitingFuture != null && slot.waitingFuture.isDone()) {
				final var systemOpt = slot.waitingFuture.join();
				if (systemOpt.isNone()) {
					Mod.LOGGER.warn("failed to generate system {}", slot.id);
					slot.generationFailed = true;
				} else {
					slot.system = systemOpt.unwrap();
				}
				slot.waitingFuture = null;
				slot.isLoaded = true;
			}
		}
	}

	private Option<StarSystem> generateSystem(GalaxySector sector, GalaxySectorId id) {
		if (id.elementIndex() >= sector.initialElements.size())
			return Option.none();
		try {
			return Option.some(this.galaxy.generateFullSystem(sector, sector.initialElements.get(id.elementIndex())));
		} catch (Throwable t) {
			Mod.LOGGER.error("failed to generate system because of an exception!");
			t.printStackTrace();
		}
		return Option.none();
	}

	public boolean isLoaded(SectorPos pos) {
		final var slot = this.sectorMap.get(pos.rootCoords());
		if (slot.isNone())
			return false;
		final var sector = slot.unwrap().sector.lookupSubtree(pos);
		return sector == null ? false : sector.isLoaded();
	}

	public boolean isComplete(SectorPos pos) {
		final var slot = this.sectorMap.get(pos.rootCoords());
		if (slot.isNone())
			return false;
		final var sector = slot.unwrap().sector.lookupSubtree(pos);
		return sector == null ? false : sector.isComplete();
	}

	public <T extends SectorTicketInfo> SectorTicket<T> createSectorTicket(Disposable.Multi disposer, T info) {
		return disposer.attach(createSectorTicketManual(info));
	}

	public <T extends SectorTicketInfo> SectorTicket<T> createSectorTicketManual(T info) {
		final var ticket = new SectorTicket<>(this, info == null ? null : info.copy());
		this.trackedTickets.push(new SectorTicketTracker(ticket));
		applyTickets(InactiveProfiler.INSTANCE);
		return (SectorTicket<T>) ticket;
	}

	public void removeSectorTicket(SectorTicket<?> ticket) {
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
		applyTickets(InactiveProfiler.INSTANCE);
		return ticket;
	}

	public void removeSystemTicket(SystemTicket ticket) {
		Mod.LOGGER.info("removing system ticket for {}", ticket.id);
		this.trackedSystemTickets.retain(tracked -> tracked.loanedTicket != ticket);
		this.removedSystemTickets.push(ticket);
		applyTickets(InactiveProfiler.INSTANCE);
	}

	public Option<GalaxySector> getSector(SectorPos pos) {
		final var slot = this.sectorMap.get(pos.rootCoords());
		return slot.map(s -> s.sector.lookupSubtree(pos)).filter(sector -> sector.isComplete());
	}

	public Option<GalaxySector.InitialElement> getInitial(GalaxySectorId id) {
		return this.sectorMap.get(id.sectorPos().rootCoords()).flatMap(slot -> {
			final var sector = slot.sector.lookupSubtree(id.sectorPos());
			if (!sector.isComplete())
				return Option.none();
			return Option.some(sector.lookupInitial(id.elementIndex()));
		});
	}

	public Option<StarSystem> getSystem(GalaxySectorId id) {
		return this.systemMap.get(id).flatMap(sys -> Option.fromNullable(sys.system));
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
		ticket.info.enumerateAffectedSectors(pos -> this.sectorMap.get(pos.rootCoords()).ifSome(slot -> {
			final var sector = slot.sector.lookupSubtree(pos);
			if (sector != null && sector.isComplete())
				sectorConsumer.accept(sector);
		}));
	}

}
