package net.xavil.universal.common.universe.galaxy;

import net.xavil.util.Disposable;

public final class SectorTicket implements Disposable {
	public final SectorManager attachedManager;
	public final SectorTicketInfo info;

	public SectorTicket(SectorManager attachedManager, SectorTicketInfo ticket) {
		this.attachedManager = attachedManager;
		this.info = ticket;
	}

	public void remove() {
		this.attachedManager.removeSectorTicket(this);
	}

	@Override
	public void dispose() {
		this.remove();
	}
}
