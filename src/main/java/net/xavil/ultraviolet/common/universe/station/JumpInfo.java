package net.xavil.ultraviolet.common.universe.station;

import net.xavil.hawklib.Constants;
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
		final double speed_c = 10000;
		final double speed_ly_PER_s = speed_c * (Constants.SPEED_OF_LIGHT_m_PER_s * Units.Tu_PER_u * Units.ly_PER_Tm);
		this.jump.travel(speed_ly_PER_s / Constants.Tick_PER_s);
	}

	public boolean complete() {
		return this.jump.isComplete();
	}

	@Override
	public void close() {
		this.jump.close();
	}

}
