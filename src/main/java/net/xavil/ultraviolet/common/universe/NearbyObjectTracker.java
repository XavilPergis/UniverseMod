package net.xavil.ultraviolet.common.universe;

import java.util.Comparator;
import java.util.Random;

import javax.annotation.Nullable;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.UniversePosition;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.universe.GalaxyTicket;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.ultraviolet.common.universe.universe.UniverseSector;
import net.xavil.ultraviolet.common.universe.universe.UniverseSectorManager;
import net.xavil.ultraviolet.common.universe.universe.UniverseSectorTicket;
import net.xavil.ultraviolet.common.universe.universe.UniverseSectorTicketInfo;

public final class NearbyObjectTracker implements Disposable {
	private final Universe universe;
	private UniverseSectorTicket<UniverseSectorTicketInfo.Cube> universeTicket;

	private GalaxyTicket galaxyTicket;
	private Galaxy galaxyIn;
	private UniversePosition lastOutsideGalaxyPos;
	private double lastOutsideGalaxyDistance;
	private boolean waitingOnUniverseSectors;
	private SectorTicket<SectorTicketInfo.Multi> systemSectorTicket;

	private SystemTicket systemTicket;
	private StarSystem systemIn;
	private UniversePosition lastOutsideSystemPos;
	private double lastOutsideSystemDistance;
	private boolean waitingOnSystemSectors;
	private boolean systemFailedToLoad;

	private static final double SYSTEM_NEARBY_RANGE = 150;

	public NearbyObjectTracker(Universe universe) {
		this.universe = universe;
	}

	@Nullable
	public Galaxy getGalaxyIn() {
		return this.galaxyIn;
	}

	@Nullable
	public StarSystem getSystemIn() {
		return this.systemIn;
	}

	@Override
	public void close() {
		if (this.universeTicket != null)
			this.universeTicket.close();
		if (this.galaxyTicket != null)
			this.galaxyTicket.close();
		if (this.systemSectorTicket != null)
			this.systemSectorTicket.close();
	}

	private void resetGalaxy() {
		resetSystem();

		this.lastOutsideGalaxyPos = null;
		this.lastOutsideGalaxyDistance = Double.POSITIVE_INFINITY;
		this.galaxyIn = null;
		if (this.galaxyTicket != null)
			this.galaxyTicket.close();
		this.galaxyTicket = null;
		if (this.systemSectorTicket != null)
			this.systemSectorTicket.close();
		this.systemSectorTicket = null;
		this.waitingOnUniverseSectors = false;
	}

	private void resetSystem() {
		this.lastOutsideSystemPos = null;
		this.lastOutsideSystemDistance = Double.POSITIVE_INFINITY;
		this.systemIn = null;
		if (this.systemTicket != null)
			this.systemTicket.close();
		this.systemTicket = null;
		this.waitingOnSystemSectors = false;
		this.systemFailedToLoad = false;
	}

	private Vector<UniverseSectorId> scanForNearbyGalaxies(UniversePosition position) {
		final var ids = new Vector<UniverseSectorId>();
		final var sectors = new Vector<UniverseSector>();
		final var centerSector = UniverseSectorManager.getSectorForPos(position);

		this.waitingOnUniverseSectors = false;
		for (int x = centerSector.x - 1; x <= centerSector.x + 1; ++x) {
			for (int y = centerSector.y - 1; y <= centerSector.y + 1; ++y) {
				for (int z = centerSector.z - 1; z <= centerSector.z + 1; ++z) {
					final var sectorPos = new Vec3i(x, y, z);
					final var sector = this.universe.sectorManager.getSector(sectorPos).unwrapOrNull();
					if (sector == null)
						continue;
					if (sector.initialElements == null) {
						this.waitingOnUniverseSectors = true;
						return new Vector<>();
					}
					sectors.push(sector);
				}
			}
		}

		for (final var sector : sectors.iterable()) {
			for (int i = 0; i < sector.initialElements.size(); ++i) {
				final var elem = sector.initialElements.get(i);
				final var distanceTm = position.relativeTo(elem.pos(), Units.Tu_PER_u).length();
				final var elemRadius = 2.0 * elem.info().radius;
				this.lastOutsideGalaxyDistance = Math.min(this.lastOutsideGalaxyDistance, distanceTm - elemRadius);
				if (distanceTm < elemRadius) {
					ids.push(new UniverseSectorId(sector.pos, i));
				}
			}
		}

		ids.sort(Comparator.comparingDouble(id -> {
			final var sector = this.universe.sectorManager.getSector(id.sectorPos()).unwrap();
			if (sector.initialElements == null)
				return Double.POSITIVE_INFINITY;
			final var pos = sector.initialElements.get(id.id()).pos();
			final var distanceTm = position.relativeTo(pos, Units.Tu_PER_u).length();
			return distanceTm;
		}));
		return ids;
	}

	private Vector<GalaxySectorId> scanForNearbySystems(UniversePosition position) {
		final var ids = new Vector<GalaxySectorId>();
		this.waitingOnSystemSectors = !this.systemSectorTicket.isLoaded();
		this.systemSectorTicket.attachedManager.enumerate(this.systemSectorTicket, sector -> {
			final var elem = new GalaxySector.ElementHolder();
			for (int i = 0; i < sector.elements.size(); ++i) {
				sector.elements.load(elem, i);
				final var elemPos = this.galaxyIn.position.add(elem.systemPosTm, Units.u_PER_Tu);
				final var distanceTm = position.relativeTo(elemPos, Units.Tu_PER_u).length();
				final var elemRadius = SYSTEM_NEARBY_RANGE;
				// subtract off SYSTEM_NEARBY_RANGE because otherwise this value would represent
				// the distance to the center of the nearest system, and not the distance to its
				// edge.
				this.lastOutsideSystemDistance = Math.min(this.lastOutsideSystemDistance, distanceTm - elemRadius);
				if (distanceTm < elemRadius) {
					ids.push(GalaxySectorId.from(sector.pos(), i));
				}
			}
		});
		return ids;
	}

	private void updateGalaxy(UniversePosition position) {
		if (this.universeTicket == null) {
			final var centerSector = UniverseSectorManager.getSectorForPos(position);
			final var ticketInfo = new UniverseSectorTicketInfo.Cube(centerSector, 1);
			this.universeTicket = this.universe.sectorManager.createSectorTicketManual(ticketInfo);
		}

		this.universeTicket.info.centerSector = UniverseSectorManager.getSectorForPos(position);

		if (this.galaxyIn != null) {
			final var relativePos = position.relativeTo(this.galaxyIn.position, Units.Tu_PER_u);
			if (relativePos.length() > 4.0 * this.galaxyIn.info.radius) {
				Mod.LOGGER.info("left galaxy {}", this.galaxyTicket.id);
				resetGalaxy();
			}
		}

		if (this.galaxyIn == null) {
			boolean needsRecheck = true;
			if (this.lastOutsideGalaxyPos != null) {
				// if we're outside of any galaxy, we find the closest one and store that
				// distance, so we know how far we need to travel before we need to re-check
				// whether were in a galaxy or not.
				final var distanceTm = position.relativeTo(this.lastOutsideGalaxyPos, Units.Tu_PER_u).length();
				if (distanceTm <= this.lastOutsideGalaxyDistance) {
					needsRecheck = false;
				}
			}
			needsRecheck |= this.waitingOnUniverseSectors;

			if (needsRecheck) {
				this.lastOutsideGalaxyPos = position;
				this.lastOutsideGalaxyDistance = Double.POSITIVE_INFINITY;
				final var nearbyGalaxies = scanForNearbyGalaxies(position);
				if (!nearbyGalaxies.isEmpty()) {
					this.galaxyTicket = this.universe.sectorManager.createGalaxyTicketManual(nearbyGalaxies.get(0));
					this.galaxyIn = this.galaxyTicket.forceLoad().unwrap();

					final var ticketPos = position.relativeTo(this.galaxyIn.position, Units.Tu_PER_u);
					final var ticketInfo = new SectorTicketInfo.Multi(ticketPos, SYSTEM_NEARBY_RANGE,
							SectorTicketInfo.Multi.SCALES_UNIFORM);
					this.systemSectorTicket = this.galaxyIn.sectorManager.createSectorTicketManual(ticketInfo);
					Mod.LOGGER.info("entered galaxy {}", this.galaxyTicket.id);
				}
			}
		} else {
			this.lastOutsideGalaxyPos = null;
			this.lastOutsideGalaxyDistance = Double.POSITIVE_INFINITY;
		}
	}

	private void updateSystem(UniversePosition position) {
		if (this.galaxyIn == null) {
			resetSystem();
			return;
		}

		// for (int i = 0; i < 1; ++i) {
		// 	final var rng = Rng.wrap(new Random());
		// 	final var pos = Vec3.random(rng, Vec3.NNN, Vec3.PPP).mul(1e1);
		// 	final var pos2 = UniversePosition.from(pos, 1);
		// 	final var pos3 = pos2.relativeTo(UniversePosition.ZERO, 1);

		// 	if (pos.distanceTo(pos3) > 1e-4) {
		// 		Mod.LOGGER.info("AAAAAAAAAAAAAAAAAAAAAAAAAAAA");
		// 	}
		// }

		final var ticketPos = position.relativeTo(this.galaxyIn.position, Units.Tu_PER_u);
		this.systemSectorTicket.info.centerPos = ticketPos;

		if (this.systemIn != null) {
			final var relativePos = position.relativeTo(this.systemIn.position, Units.Tu_PER_u);
			if (relativePos.length() > 2.0 * SYSTEM_NEARBY_RANGE) {
				Mod.LOGGER.info("left system {}", this.systemIn.id);
				resetSystem();
			}
		}

		if (this.systemIn == null) {
			boolean needsRecheck = true;
			needsRecheck &= !this.systemFailedToLoad;
			if (this.lastOutsideSystemPos != null) {
				final var distanceTm = position.relativeTo(this.lastOutsideSystemPos, Units.Tu_PER_u).length();
				if (distanceTm <= this.lastOutsideSystemDistance) {
					needsRecheck = false;
				}
			}
			needsRecheck |= this.waitingOnSystemSectors;

			if (needsRecheck) {
				this.lastOutsideSystemPos = position;
				this.lastOutsideSystemDistance = Double.POSITIVE_INFINITY;
				final var nearbySystems = scanForNearbySystems(position);
				if (!nearbySystems.isEmpty()) {
					this.systemTicket = this.galaxyIn.sectorManager.createSystemTicketManual(nearbySystems.get(0));
					this.systemIn = this.systemTicket.forceLoad().unwrapOrNull();

					if (this.systemIn != null) {
						Mod.LOGGER.info("entered system {}", this.systemTicket.id);
					} else {
						this.systemFailedToLoad = true;
					}

				}
			}
		} else {
			this.lastOutsideSystemPos = null;
			this.lastOutsideSystemDistance = Double.POSITIVE_INFINITY;
		}
	}

	public void update(UniversePosition position) {
		updateGalaxy(position);
		updateSystem(position);
	}
}