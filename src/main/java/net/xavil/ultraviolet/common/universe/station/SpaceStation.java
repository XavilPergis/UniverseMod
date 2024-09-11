package net.xavil.ultraviolet.common.universe.station;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.ultraviolet.common.universe.NearbyObjectTracker;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.id.UniversePosition;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.ultraviolet.common.universe.universe.Universe;

public final class SpaceStation implements Disposable {

	public final Universe universe;
	public final Level level;

	// universe-relative
	public UniversePosition position = UniversePosition.ZERO, prevPosition = UniversePosition.ZERO;
	public Quat orientation = Quat.IDENTITY;

	private final NearbyObjectTracker nearbyTracker;
	private StationOrbit stationOrbit = null;

	public String name;
	public MutableList<StationComponent> stationComponents = new Vector<>();

	public static final class StationOrbit {
		public final SystemNodeId id;
		private OrbitalPlane plane;
		private OrbitalShape shape;

		private StationOrbit(SystemNodeId id, OrbitalPlane plane, OrbitalShape shape) {
			this.id = id;
			this.plane = plane;
			this.shape = shape;
		}
	}

	public SpaceStation(Universe universe, Level level, String name, UniversePosition position) {
		this.universe = universe;
		this.level = level;
		this.name = name;
		this.prevPosition = this.position = position;
		this.nearbyTracker = new NearbyObjectTracker(universe);
	}

	@Override
	public void close() {
		this.nearbyTracker.close();
	}

	public Vec3 getGavityAt(Vec3Access pos) {
		return Vec3.YN.mul(0.05);
	}

	// delta is in meters
	public void applyMovement(Vec3Access delta) {
		// -delta.z is forward, relative to the orientation of the station
		// delta.y is up, delta.x is right
		final var transformed = this.orientation.transform(delta);
		this.position = this.position.add(transformed, 1);
	}

	public void tick() {
		// this.universe.get
		this.nearbyTracker.update(this.position);
		// this.universeTicket.attachedManager.getSector(this.universeTicket.info.affectedSectors());
		this.prevPosition = this.position;
	}

	@Nullable
	public SystemId getSystemIn() {
		return null;
	}

	@Nullable
	public UniverseSectorId getGalaxyIn() {
		return null;
	}

	public static CompoundTag toNbt(SpaceStation station) {
		final var nbt = new CompoundTag();
		return nbt;
	}

	public static SpaceStation fromNbt(Universe universe, CompoundTag nbt) {
		return null;
	}

}
