package net.xavil.ultraviolet.common.universe.universe;

import net.xavil.hawklib.Disposable;

public final class UniverseSectorTicket<T extends UniverseSectorTicketInfo> implements Disposable {
	
	public final UniverseSectorManager attachedManager;
	public final T info;

	public UniverseSectorTicket(UniverseSectorManager attachedManager, T ticket) {
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
