package net.xavil.ultraviolet.common.universe.station;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Units;

public final class JumpInfo implements Disposable {

	// private final SpaceStation station;
	private final StationLocation.JumpingSystem jump;

	public JumpInfo(SpaceStation station, StationLocation.JumpingSystem jump) {
		// this.station = station;
		this.jump = jump;
	}

	public void tick() {
		// 299792458
		final double speed = 10;
		this.jump.travel(speed * Units.Tm_PER_ly * 0.05);
	}

	public boolean complete() {
		return this.jump.isComplete();
	}

	@Override
	public void close() {
	}

}
