package net.xavil.ultraviolet.common.universe.station;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public final class SpaceStation {

	public final Universe universe;
	public final Level level;

	// universe-relative
	public Quat orientation = Quat.IDENTITY;

	public String name;
	private StationLocation location;

	private Vec3 pos = Vec3.ZERO;
	private Vec3 prevPos = Vec3.ZERO;

	public MutableList<StationComponent> stationComponents = new Vector<>();

	public SpaceStation(Universe universe, Level level, String name, StationLocation location) {
		this.universe = universe;
		this.level = level;
		this.name = name;
		this.location = location;
	}

	public StationLocation getLocation() {
		return this.location;
	}

	public void setLocation(StationLocation location) {
		if (this.location != null)
			this.location.close();
		this.location = location;
	}

	// private void setJumpInfo(@Nullable JumpInfo info) {
	// if (this.jumpInfo != null)
	// this.jumpInfo.close();
	// this.jumpInfo = info;
	// }

	public Vec3 getGavityAt(Vec3Access pos) {
		return Vec3.YN.mul(0.05);
	}

	public void applyMovement(Vec3Access delta) {
		// -delta.z is forward, relative to the orientation of the station
		// delta.y is up, delta.x is right
		if (this.location instanceof StationLocation.SystemRelative loc) {
			final var transformed = this.orientation.transform(delta);
			loc.pos = loc.pos.add(transformed);
		}
	}

	public void tick() {
		this.location = this.location.update(this.universe);
		this.prevPos = pos;
		this.pos = this.location.getPos();

		// final double speed_c = 10000;
		// final double speed_ly_PER_s = speed_c * (Constants.SPEED_OF_LIGHT_m_PER_s *
		// Units.Tu_PER_u * Units.ly_PER_Tm);
		// this.jump.travel(speed_ly_PER_s / Constants.Tick_PER_s);

		// if (this.jumpInfo != null) {
		// this.jumpInfo.tick();
		// if (this.jumpInfo.complete()) {
		// setJumpInfo(null);
		// }
		// }
	}

	public Vec3 getPos(float partialTick) {
		return Vec3.lerp(partialTick, this.prevPos, this.pos);
	}

	// TODO: make this dynamic! this should require player-built infrastructure to
	// control
	public long getFsdAccumulatorChargeRate() {
		return 100;
	}

	// prepare to jump to system; do countdown n stuff
	// TODO: instant jump
	public void prepareForJump(SystemNodeId id, boolean isJumpInstant) {
		if (this.location.isJump())
			return;
		final var jump = StationLocation.JumpingSystem.create(this.universe, this.location, id).unwrapOrNull();
		if (jump != null) {
			setLocation(jump);
		}
	}

	// public static CompoundTag toNbt(SpaceStation station) {
	// 	final var nbt = new CompoundTag();
	// 	return nbt;
	// }

	// public static SpaceStation fromNbt(Universe universe, CompoundTag nbt) {
	// 	return null;
	// }

}
