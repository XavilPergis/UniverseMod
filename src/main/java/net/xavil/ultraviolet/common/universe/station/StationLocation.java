package net.xavil.ultraviolet.common.universe.station;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.Units;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.galaxy.SystemTicket;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.system.BinaryCelestialNode;
import net.xavil.ultraviolet.common.universe.system.PlanetaryCelestialNode;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;
import net.xavil.hawklib.math.matrices.Vec3;
public abstract sealed class StationLocation implements Disposable {

	public abstract Vec3 getPos();

	public abstract StationLocation update(Universe universe);

	public abstract boolean isJump();

	private static <T> Tag encodeNbt(Codec<T> codec, T value) {
		return codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow(false, Mod.LOGGER::error);
	}

	private static <T> T decodeNbt(Codec<T> codec, Tag value) {
		return codec.parse(NbtOps.INSTANCE, value).getOrThrow(false, Mod.LOGGER::error);
	}

	// the station is currently orbiting a celestial body
	public static final class OrbitingCelestialBody extends StationLocation {
		public final SystemNodeId id;

		private final SystemTicket ticket;
		private OrbitalPlane plane;
		private OrbitalShape shape;

		private Vec3 systemPos;
		private Vec3 pos;
		private boolean needsLoading = true;

		private OrbitingCelestialBody(Universe universe, SystemTicket ticket, SystemNodeId id) {
			this.id = id;
			this.ticket = ticket;
			update(universe);
		}

		// this is all kinda awful!
		private OrbitingCelestialBody(Universe universe, CompoundTag nbt) {
			this.id = decodeNbt(SystemNodeId.CODEC, nbt.get("id"));

			try (final var disposer = Disposable.scope()) {
				this.ticket = universe.loadGalaxy(disposer, id.universeSector())
						.map(galaxy -> galaxy.sectorManager.createSystemTicketManual(id.galaxySector()))
						.unwrap();
			}

			if (nbt.contains("plane"))
				this.plane = decodeNbt(OrbitalPlane.CODEC, nbt.get("plane"));
			if (nbt.contains("shape"))
				this.shape = decodeNbt(OrbitalShape.CODEC, nbt.get("shape"));

			this.ticket.forceLoad();
			this.systemPos = universe.getSystemPos(id.system()).unwrap();
			this.needsLoading = false;

			update(universe);
		}

		public void writeNbt(CompoundTag nbt) {
			nbt.put("id", encodeNbt(SystemNodeId.CODEC, this.id));
			if (this.plane != null)
				nbt.put("plane", encodeNbt(OrbitalPlane.CODEC, this.plane));
			if (this.shape != null)
				nbt.put("shape", encodeNbt(OrbitalShape.CODEC, this.shape));
		}

		public static Maybe<OrbitingCelestialBody> createDefault(Universe universe, SystemNodeId id) {
			try (final var disposer = Disposable.scope()) {
				final var galaxy = universe.loadGalaxy(disposer, id.universeSector()).unwrapOrNull();
				if (galaxy == null)
					return Maybe.none();
				final var ticket = galaxy.sectorManager.createSystemTicketManual(id.galaxySector());
				if (ticket.forceLoad().isNone())
					return Maybe.none();
				return Maybe.some(new OrbitingCelestialBody(universe, ticket, id));
			}
		}

		@Override
		public Vec3 getPos() {
			return this.pos;
		}

		public void forceLoad(Universe universe) {
			this.ticket.forceLoad();
			update(universe);
		}

		public boolean failedToLoad() {
			return this.ticket.failedToLoad();
		}

		private void tryLoad(Universe universe) {
			if (!this.needsLoading)
				return;
			if (!this.ticket.isLoaded())
				return;

			final var node = universe.getSystemNode(id).unwrapOrNull();
			if (node == null)
				return;

			final var time = universe.getCelestialTime();
			node.updatePositions(time);

			this.systemPos = universe.getSystemPos(id.system()).unwrap();

			double semiMajor = 1.0;
			if (node instanceof PlanetaryCelestialNode planetNode) {
				// semiMajor = 1.06 * planetNode.radiusRearth * (Units.m_PER_Rearth /
				// Units.TERA);
				semiMajor = 2.0 * Units.Tu_PER_ku * planetNode.radius;
			} else if (node instanceof StellarCelestialNode starNode) {
				semiMajor = 5.0 * Units.Tu_PER_ku * starNode.radius;
			} else if (node instanceof BinaryCelestialNode binaryNode) {
				semiMajor = 1.1 * binaryNode.orbitalShapeOuter.semiMajor();
			}

			this.plane = OrbitalPlane.ZERO.withReferencePlane(node.referencePlane);
			this.shape = OrbitalShape.fromEccentricity(0.0, semiMajor);
			this.needsLoading = false;
		}

		@Override
		public StationLocation update(Universe universe) {
			tryLoad(universe);
			if (this.needsLoading)
				return this;
			final var time = universe.getCelestialTime();
			final var localPos = universe.getSystemNode(this.id)
					.map(node -> node.getOrbitalPosition(new Vec3.Mutable(), this.plane, this.shape, false, time).xyz())
					.unwrapOrNull();
			this.pos = this.systemPos.add(localPos);
			return this;
		}

		@Override
		public void close() {
			this.ticket.close();
		}

		@Override
		public boolean isJump() {
			return false;
		}
	}

	// the station is currently jumping between star systems
	public static final class JumpingSystem extends StationLocation {
		private Vec3 sourcePos;
		private StationLocation targetLocation;
		private double distanceTravelled = 0.0;

		public final SystemNodeId targetNode;
		private Vec3 pos;

		private JumpingSystem(Universe universe, SystemTicket ticket, StationLocation current, SystemNodeId target) {
			this.sourcePos = current.getPos();
			Assert.isTrue(ticket.isLoaded());
			final var id = target;
			final var dest = new StationLocation.OrbitingCelestialBody(universe, ticket, id);
			dest.forceLoad(universe);
			this.targetLocation = dest;
			this.targetNode = id;
		}

		public static Maybe<JumpingSystem> create(Universe universe, StationLocation current, SystemNodeId target) {
			try (final var disposer = Disposable.scope()) {
				final var galaxy = universe.loadGalaxy(disposer, target.universeSector()).unwrapOrNull();
				if (galaxy == null)
					return Maybe.none();
				final var ticket = galaxy.sectorManager.createSystemTicketManual(target.galaxySector());
				if (ticket.forceLoad().isNone())
					return Maybe.none();
				return Maybe.some(new JumpingSystem(universe, ticket, current, target));
			}
		}

		private JumpingSystem(Universe universe, CompoundTag nbt) {
			this.sourcePos = decodeNbt(Vec3.CODEC, nbt.get("src_pos"));
			this.targetLocation = fromNbt(universe, nbt.getCompound("target"));
			this.distanceTravelled = nbt.getDouble("travelled");
			this.targetNode = decodeNbt(SystemNodeId.CODEC, nbt.get("target_node"));
		}

		public void writeNbt(CompoundTag nbt) {
			nbt.put("src_pos", encodeNbt(Vec3.CODEC, this.sourcePos));
			nbt.put("target", toNbt(this.targetLocation));
			nbt.putDouble("travelled", this.distanceTravelled);
			nbt.put("target_node", encodeNbt(SystemNodeId.CODEC, this.targetNode));
		}

		@Override
		public Vec3 getPos() {
			return this.pos;
		}

		public void travel(double distance) {
			this.distanceTravelled += distance;
		}

		public double getCompletion() {
			final var target = this.targetLocation.getPos();
			final var totalDistance = this.sourcePos.distanceTo(target);
			return this.distanceTravelled / totalDistance;
		}

		public boolean isComplete() {
			return getCompletion() >= 1;
		}

		@Override
		public StationLocation update(Universe universe) {
			this.targetLocation.update(universe);
			final var completion = getCompletion();
			if (completion >= 1.0)
				return this.targetLocation;
			this.pos = Vec3.lerp(completion, this.sourcePos, this.targetLocation.getPos());
			return this;
		}

		@Override
		public void close() {
		}

		@Override
		public boolean isJump() {
			return true;
		}
	}

	// the station is currently located within a star system, but not orbiting any
	// specific system node.
	public static final class SystemRelative extends StationLocation {
		public SystemId system;
		public Vec3 pos;

		public SystemRelative(SystemId system, Vec3 pos) {
			this.system = system;
			this.pos = pos;
		}

		private SystemRelative(Universe universe, CompoundTag nbt) {
			this.system = decodeNbt(SystemId.CODEC, nbt.get("system"));
			this.pos = decodeNbt(Vec3.CODEC, nbt.get("pos"));
		}

		public void writeNbt(CompoundTag nbt) {
			nbt.put("system", encodeNbt(SystemId.CODEC, this.system));
			nbt.put("pos", encodeNbt(Vec3.CODEC, this.pos));
		}

		@Override
		public void close() {
		}

		@Override
		public Vec3 getPos() {
			return pos;
		}

		@Override
		public StationLocation update(Universe universe) {
			return this;
		}

		@Override
		public boolean isJump() {
			return false;
		}
	}

	// public static final class GalaxyRelative extends StationLocation {
	// 	public UniverseSectorId id;
	// 	public Vec3 pos;
	// }

	public static CompoundTag toNbt(StationLocation location) {
		final var nbt = new CompoundTag();
		if (location instanceof OrbitingCelestialBody loc) {
			nbt.putString("type", "orbit");
			loc.writeNbt(nbt);
		} else if (location instanceof JumpingSystem loc) {
			nbt.putString("type", "system_jump");
			loc.writeNbt(nbt);
		} else if (location instanceof SystemRelative loc) {
			nbt.putString("type", "system_relative");
			loc.writeNbt(nbt);
		}
		return nbt;
	}

	public static StationLocation fromNbt(Universe universe, CompoundTag nbt) {
		final var type = nbt.getString("type");
		if (type.equals("orbit")) {
			return new OrbitingCelestialBody(universe, nbt);
		} else if (type.equals("system_jump")) {
			return new JumpingSystem(universe, nbt);
		} else if (type.equals("system_relative")) {
			return new SystemRelative(universe, nbt);
		}
		return null;
	}

}
