package net.xavil.universal.common.universe.station;

import net.minecraft.world.level.Level;
import net.xavil.util.math.Vec3;

public final class SpaceStation {
	
	public final Level level;
	public StationLocation location;

	public SpaceStation(Level level, StationLocation location) {
		this.level = level;
		this.location = location;
	}

	public double getGavityStrength(Vec3 pos) {
		return 0.5;
	}

}
