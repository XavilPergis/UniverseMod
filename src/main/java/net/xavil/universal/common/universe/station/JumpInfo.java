package net.xavil.universal.common.universe.station;

import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.util.Disposable;

public final class JumpInfo implements Disposable {

	private final SpaceStation station;
	private final StationLocation.JumpingSystem jump;

	public JumpInfo(SpaceStation station, StationLocation.JumpingSystem jump) {
		this.station = station;
		this.jump = jump;
	}

	public void tick() {
		this.jump.travel(100000);
	}

	public boolean complete() {
		return this.jump.isComplete();
	}

	@Override
	public void close() {
	}

}
