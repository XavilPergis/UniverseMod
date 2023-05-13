package net.xavil.ultraviolet.common.universe.system.gen;

import java.util.Set;
import java.util.function.BiConsumer;

import net.xavil.util.Units;
import net.xavil.util.math.Interval;

public abstract sealed class AccreteDebugEvent {

	public interface Consumer {
		boolean shouldEmitEvents();

		void accept(AccreteDebugEvent event);

		static Consumer wrap(java.util.function.Consumer<AccreteDebugEvent> consumer) {
			return new Consumer() {

				@Override
				public boolean shouldEmitEvents() {
					return true;
				}

				@Override
				public void accept(AccreteDebugEvent event) {
					consumer.accept(event);
				}

			};
		}

		static Consumer DUMMY = new Consumer() {
			@Override
			public boolean shouldEmitEvents() {
				return false;
			}

			@Override
			public void accept(AccreteDebugEvent event) {
			}
		};
	}

	public abstract String kind();

	public abstract void addInfoLines(BiConsumer<String, String> consumer);

	public static final class Initialize extends AccreteDebugEvent {
		public final Interval dustBandInterval;
		public final Interval planetesimalPlacementInterval;

		public Initialize(Interval dustBandRadius, Interval planetesimalPlacementInterval) {
			this.dustBandInterval = dustBandRadius;
			this.planetesimalPlacementInterval = planetesimalPlacementInterval;
		}

		@Override
		public String kind() {
			return "INIT";
		}

		@Override
		public void addInfoLines(BiConsumer<String, String> consumer) {
			consumer.accept("dust band interval",
					String.format("%.2f au to %.2f au", dustBandInterval.lower(), dustBandInterval.higher()));
			consumer.accept("placement interval", String.format("%.2f au to %.2f au",
					planetesimalPlacementInterval.lower(), planetesimalPlacementInterval.higher()));
		}
	}

	public static final class PlanetesimalCreated extends AccreteDebugEvent {
		public final int id;
		public final double mass;
		public final double radius;
		public final double distance;
		public final Interval effectInterval;

		public PlanetesimalCreated(int id, double mass, double radius, double distance, Interval effectInterval) {
			this.id = id;
			this.mass = mass;
			this.radius = radius;
			this.distance = distance;
			this.effectInterval = effectInterval;
		}

		public PlanetesimalCreated(AccreteContext ctx, Planetesimal planetesimal) {
			this.id = planetesimal.getId();
			this.mass = planetesimal.getMass();
			this.radius = planetesimal.getRadius();
			this.distance = planetesimal.distanceToStar();
			this.effectInterval = planetesimal.effectLimits();
		}

		@Override
		public String kind() {
			return "CREATE";
		}

		@Override
		public void addInfoLines(BiConsumer<String, String> consumer) {
			consumer.accept("id", "" + id);
			consumer.accept("mass",
					String.format("%.2f M☉, %.2f M♃, %.2f Mⴲ", mass, mass * Units.Yg_PER_Msol / Units.Yg_PER_Mjupiter,
							mass * Units.Yg_PER_Msol / Units.Yg_PER_Mearth));
			consumer.accept("radius", String.format("%.2f km", radius));
			consumer.accept("distance", String.format("%.2f au", distance));
			consumer.accept("swept interval",
					String.format("%.2f au to %.2f au", effectInterval.lower(), effectInterval.higher()));
		}
	}

	public static final class PlanetesimalUpdated extends AccreteDebugEvent {
		public final int id;
		public final double mass;
		public final double radius;
		public final double distance;
		public final Interval effectInterval;

		public PlanetesimalUpdated(int id, double mass, double radius, double distance, Interval effectInterval) {
			this.id = id;
			this.mass = mass;
			this.radius = radius;
			this.distance = distance;
			this.effectInterval = effectInterval;
		}

		public PlanetesimalUpdated(Planetesimal planetesimal) {
			this.id = planetesimal.getId();
			this.mass = planetesimal.getMass();
			this.radius = planetesimal.getRadius();
			this.distance = planetesimal.getOrbitalShape().semiMajor();
			this.effectInterval = planetesimal.effectLimits();
		}

		@Override
		public String kind() {
			return "UPDATE";
		}

		@Override
		public void addInfoLines(BiConsumer<String, String> consumer) {
			consumer.accept("id", "" + id);
			consumer.accept("mass",
					String.format("%.2f M☉, %.2f M♃, %.2f Mⴲ", mass, mass * Units.Yg_PER_Msol / Units.Yg_PER_Mjupiter,
							mass * Units.Yg_PER_Msol / Units.Yg_PER_Mearth));
			consumer.accept("radius", String.format("%.2f km", radius));
			consumer.accept("distance", String.format("%.2f au", distance));
			consumer.accept("swept interval",
					String.format("%.2f au to %.2f au", effectInterval.lower(), effectInterval.higher()));
		}
	}

	// public static final class PlanetesimalCollision extends AccreteDebugEvent {
	// 	public final int parentId;
	// 	public final int collidedId;

	// 	public PlanetesimalCollision(int parentId, int collidedId) {
	// 		this.parentId = parentId;
	// 		this.collidedId = collidedId;
	// 	}

	// 	public PlanetesimalCollision(Planetesimal parent, Planetesimal child) {
	// 		this.parentId = parent.getId();
	// 		this.collidedId = child.getId();
	// 	}

	// 	@Override
	// 	public String kind() {
	// 		return "COLLIDE";
	// 	}

	// 	@Override
	// 	public void addInfoLines(BiConsumer<String, String> consumer) {
	// 		consumer.accept("parent", "" + parentId);
	// 		consumer.accept("collided", "" + collidedId);
	// 	}
	// }

	public static final class RingAdded extends AccreteDebugEvent {
		public final int id;
		public final Planetesimal.Ring ring;

		public RingAdded(int id, Planetesimal.Ring ring) {
			this.id = id;
			this.ring = ring;
		}

		public RingAdded(Planetesimal parent, Planetesimal.Ring ring) {
			this.id = parent.getId();
			this.ring = ring;
		}

		@Override
		public String kind() {
			return "RING";
		}

		@Override
		public void addInfoLines(BiConsumer<String, String> consumer) {
			consumer.accept("moon", "" + id);
			consumer.accept("ring start (au)", "" + ring.interval().lower());
			consumer.accept("ring end (au)", "" + ring.interval().higher());
			consumer.accept("ring eccentricity", "" + ring.eccentricity());
		}
	}

	public static final class PlanetesimalRemoved extends AccreteDebugEvent {
		public final int id;

		public PlanetesimalRemoved(int id) {
			this.id = id;
		}

		public PlanetesimalRemoved(Planetesimal planetesimal) {
			this.id = planetesimal.getId();
		}

		@Override
		public String kind() {
			return "REMOVE";
		}

		@Override
		public void addInfoLines(BiConsumer<String, String> consumer) {
			consumer.accept("id", "" + id);
		}
	}

	public static final class Sweep extends AccreteDebugEvent {
		public final Interval sweepInterval;
		public final boolean sweptGas, sweptDust;

		public Sweep(Interval sweepInterval, boolean sweptGas, boolean sweptDust) {
			this.sweepInterval = sweepInterval;
			this.sweptGas = sweptGas;
			this.sweptDust = sweptDust;
		}

		@Override
		public String kind() {
			return "SWEEP";
		}

		@Override
		public void addInfoLines(BiConsumer<String, String> consumer) {
			consumer.accept("swept gas", "" + sweptGas);
			consumer.accept("swept dust", "" + sweptDust);
			consumer.accept("swept interval",
					String.format("%.2f au to %.2f au", sweepInterval.lower(), sweepInterval.higher()));
		}
	}

	public static final class UpdateOrbits extends AccreteDebugEvent {
		public final int parentId;
		public final Set<Integer> added, removed;

		public UpdateOrbits(int parentId, Set<Integer> added, Set<Integer> removed) {
			this.parentId = parentId;
			this.added = added;
			this.removed = removed;
		}

		@Override
		public String kind() {
			return "ORBITS";
		}

		@Override
		public void addInfoLines(BiConsumer<String, String> consumer) {
		}
	}

}
