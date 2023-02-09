package net.xavil.universal.common.universe.galaxy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.UniverseId;

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
			for (var x = center.getX() - radius; x <= center.getX() + radius; ++x) {
				for (var y = center.getY() - radius; y <= center.getY() + radius; ++y) {
					for (var z = center.getZ() - radius; z <= center.getZ() + radius; ++z) {
						consumer.accept(new Vec3i(x, y, z));
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

	public final boolean isLoaded(Vec3i sectorPos) {
		return this.sectorMap.containsKey(sectorPos);
	}

	public final @Nullable Octree<T> get(Vec3i sectorPos) {
		if (!isLoaded(sectorPos))
			return null;
		return this.sectorMap.get(sectorPos).volume;
	}

	public final @Nullable T get(UniverseId.SectorId sectorId) {
		if (!isLoaded(sectorId.sectorPos()))
			return null;
		return this.sectorMap.get(sectorId.sectorPos()).volume.getById(sectorId.sectorId());
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
