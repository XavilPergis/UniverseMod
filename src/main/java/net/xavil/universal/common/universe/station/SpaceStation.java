package net.xavil.universal.common.universe.station;

import javax.annotation.Nullable;

import net.minecraft.world.level.Level;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.MutableList;
import net.xavil.util.math.Quat;
import net.xavil.util.math.matrices.Vec3;
import net.xavil.util.math.matrices.interfaces.Vec3Access;

public final class SpaceStation {

	public final Universe universe;
	public final Level level;
	public Quat orientation = Quat.IDENTITY;
	public String name;
	private StationLocation location;

	private JumpInfo jumpInfo = null;

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

	private void setJumpInfo(@Nullable JumpInfo info) {
		if (this.jumpInfo != null)
			this.jumpInfo.close();
		this.jumpInfo = info;
	}

	public Vec3 getGavityAt(Vec3Access pos) {
		return Vec3.YN.mul(0.05);
	}

	public void tick() {
		this.location = this.location.update(this.universe);
		this.prevPos = pos;
		this.pos = this.location.getPos();

		if (this.jumpInfo != null) {
			this.jumpInfo.tick();
			if (this.jumpInfo.complete()) {
				setJumpInfo(null);
			}
		}
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
	public void prepareForJump(SystemId system, boolean isJumpInstant) {
		if (this.jumpInfo != null)
			return;
		final var jump = StationLocation.JumpingSystem.create(this.universe, this.location, system).unwrapOrNull();
		if (jump != null) {
			setJumpInfo(new JumpInfo(this, jump));
			setLocation(jump);
		}
	}

}
