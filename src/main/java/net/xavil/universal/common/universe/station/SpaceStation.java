package net.xavil.universal.common.universe.station;

import net.minecraft.world.level.Level;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.util.math.Quat;
import net.xavil.util.math.Vec3;

public final class SpaceStation {
	
	public final Universe universe;
	public final Level level;
	public Quat orientation = Quat.IDENTITY;
	public String name;
	private StationLocation location;

	private Vec3 pos = Vec3.ZERO;
	private Vec3 prevPos = Vec3.ZERO;

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
			this.location.dispose();
		this.location = location;
	}

	public Vec3 getGavityAt(Vec3 pos) {
		return Vec3.YN.mul(0.5);
	}

	public void tick() {
		this.location.update(this.universe);
		this.prevPos = pos;
		this.pos = this.location.getPos();
	}

	public Vec3 getPos(float partialTick) {
		return Vec3.lerp(partialTick, this.prevPos, this.pos);
	}

	public void jumpTo(SystemId system) {}

}
