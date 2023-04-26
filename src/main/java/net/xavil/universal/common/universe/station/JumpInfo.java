package net.xavil.universal.common.universe.station;

import net.xavil.universal.common.universe.galaxy.SystemTicket;
import net.xavil.util.Disposable;

public final class JumpInfo implements Disposable {

	private final SpaceStation station;
	private final SystemTicket ticket;

	public JumpInfo(SpaceStation station, SystemTicket ticket) {
		this.station = station;
		this.ticket = ticket;
	}

	public void tick() {
		if (!ticket.isLoaded() || ticket.failedToLoad())
			return;
		// this.countdownToJumpTicks -= 1;
		// if (this.countdownToJumpTicks < 0)
		// 	startJump(this.jumpingSystem);
	} 

	public boolean failed() {
		return ticket.failedToLoad();
	}

	@Override
	public void dispose() {
		this.ticket.dispose();
	}

}
