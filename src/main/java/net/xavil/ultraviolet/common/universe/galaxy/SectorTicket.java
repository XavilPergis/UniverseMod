package net.xavil.ultraviolet.common.universe.galaxy;

import net.xavil.hawklib.Disposable;

public final class SectorTicket<T extends SectorTicketInfo> implements Disposable {
	public final SectorManager attachedManager;
	public T info;

	public SectorTicket(SectorManager attachedManager, T info) {
		this.attachedManager = attachedManager;
		this.info = info;
	}

	public void remove() {
		this.attachedManager.removeSectorTicket(this);
	}

	@Override
	public void close() {
		this.remove();
	}
}
