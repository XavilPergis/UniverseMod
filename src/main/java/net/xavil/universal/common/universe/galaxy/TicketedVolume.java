package net.xavil.universal.common.universe.galaxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.Vec3;
import net.xavil.universal.common.universe.Vec3i;
import net.xavil.universal.common.universe.id.SectorId;

public abstract class TicketedVolume<T> {

	private static class Sector<T> {
		public final Vec3i sectorPos;
		public final List<TicketedVolume<?>> childVolumes = new ArrayList<>();
		public final Octree<T> volume;
		public final Set<Ticket> attachedTickets = Collections.newSetFromMap(new IdentityHashMap<>());

		public Sector(Vec3i sectorPos, Octree<T> volume) {
			this.sectorPos = sectorPos;
			this.volume = volume;
		}
	}

	public static class Ticket {
		private int lifetime;
		private int age = 0;
		private Vec3i sectorPos;
		private int sectorRadius;

		// we might want to keep a volume loaded
		// private Ticket parentTicket;

		public Ticket(Vec3i sectorPos, int sectorRadius, int lifetime) {
			this.lifetime = lifetime;
			this.sectorPos = sectorPos;
			this.sectorRadius = sectorRadius;
		}

		public void enumerate(Consumer<Vec3i> consumer) {
			var center = this.sectorPos;
			var radius = this.sectorRadius;
			for (var x = center.x - radius; x <= center.x + radius; ++x) {
				for (var y = center.y - radius; y <= center.y + radius; ++y) {
					for (var z = center.z - radius; z <= center.z + radius; ++z) {
						consumer.accept(Vec3i.from(x, y, z));
					}
				}
			}
		}

		public void tick() {
		}

		public Vec3i getSectorPos() {
			return this.sectorPos;
		}

		// public void setSectorPos(Vec3i sectorPos) {
		// this.sectorPos = sectorPos;
		// }

		public int getSectorRadius() {
			return this.sectorRadius;
		}

		// public void setSectorRadius(int sectorRadius) {
		// this.sectorRadius = sectorRadius;
		// }

	}

	private final Map<Vec3i, Sector<T>> sectorMap = new HashMap<>();
	private final Set<Ticket> tickets = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<Ticket> tickableTickets = Collections.newSetFromMap(new IdentityHashMap<>());

	public final Ticket addTicket(Vec3i center, int radius, int lifetime) {
		var ticket = new Ticket(center, radius, lifetime);
		this.tickets.add(ticket);
		if (lifetime >= 0) {
			this.tickableTickets.add(ticket);
		}

		// Mod.LOGGER.info("[ticket] ADD pos=" + ticket.sectorPos + ", lifetime=" + ticket.lifetime);

		ticket.enumerate(pos -> {
			var sector = this.sectorMap.get(pos);
			if (sector == null) {
				sector = new Sector<>(pos, generateVolume(pos));
				this.sectorMap.put(pos, sector);
			}
			sector.attachedTickets.add(ticket);
		});

		return ticket;
	}

	public final void removeTicket(Ticket ticket) {
		// Mod.LOGGER.info("[ticket] REMOVE pos=" + ticket.sectorPos + ", age=" + ticket.age);
		ticket.enumerate(pos -> {
			var sector = this.sectorMap.get(pos);
			sector.attachedTickets.remove(ticket);
			if (sector.attachedTickets.isEmpty()) {
				this.sectorMap.remove(pos);
			}
		});
		this.tickets.remove(ticket);
		this.tickableTickets.remove(ticket);
	}

	public final Stream<Vec3i> streamLoadedSectors() {
		return this.sectorMap.keySet().stream();
	}

	public final boolean isLoaded(Vec3i sectorPos) {
		return this.sectorMap.containsKey(sectorPos);
	}

	public final Octree<T> get(Vec3i sectorPos) {
		if (!isLoaded(sectorPos))
			return null;
		return this.sectorMap.get(sectorPos).volume;
	}

	public final T get(SectorId sectorId) {
		if (!isLoaded(sectorId.sectorPos()))
			return null;
		return this.sectorMap.get(sectorId.sectorPos()).volume.getById(sectorId.sectorId());
	}

	public static void enumerateSectors(Vec3 centerPos, double radius, double distancePerSector,
			Consumer<Vec3i> posConsumer) {
		var minSectorX = (int) Math.floor((centerPos.x - radius) / distancePerSector);
		var maxSectorX = (int) Math.floor((centerPos.x + radius) / distancePerSector);
		var minSectorY = (int) Math.floor((centerPos.y - radius) / distancePerSector);
		var maxSectorY = (int) Math.floor((centerPos.y + radius) / distancePerSector);
		var minSectorZ = (int) Math.floor((centerPos.z - radius) / distancePerSector);
		var maxSectorZ = (int) Math.floor((centerPos.z + radius) / distancePerSector);

		for (int x = minSectorX; x <= maxSectorX; ++x) {
			for (int y = minSectorY; y <= maxSectorY; ++y) {
				for (int z = minSectorZ; z <= maxSectorZ; ++z) {
					posConsumer.accept(Vec3i.from(x, y, z));
				}
			}
		}
	}

	public void tick() {
		Set<Ticket> toRemove = Collections.newSetFromMap(new IdentityHashMap<>());
		this.tickableTickets.forEach(ticket -> {
			ticket.age += 1;
			if (ticket.age > ticket.lifetime)
				toRemove.add(ticket);
		});
		toRemove.forEach(this::removeTicket);
	}

	public abstract Octree<T> generateVolume(Vec3i sectorPos);

}
