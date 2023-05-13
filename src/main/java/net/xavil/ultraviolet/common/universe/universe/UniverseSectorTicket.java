package net.xavil.ultraviolet.common.universe.universe;

import net.xavil.util.Disposable;

public final class UniverseSectorTicket implements Disposable {
	
	public final UniverseSectorManager attachedManager;
	public final UniverseSectorTicketInfo info;

	public UniverseSectorTicket(UniverseSectorManager attachedManager, UniverseSectorTicketInfo ticket) {
		this.attachedManager = attachedManager;
		this.info = ticket;
	}

	public void remove() {
		this.attachedManager.removeSectorTicket(this);
	}

	@Override
	public void close() {
		this.remove();
	}

}
