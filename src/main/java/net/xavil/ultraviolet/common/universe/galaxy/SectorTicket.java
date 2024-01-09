package net.xavil.ultraviolet.common.universe.galaxy;

import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Disposable;

public final class SectorTicket<T extends SectorTicketInfo> implements Disposable {
	public final SectorManager attachedManager;
	public T info;
	private boolean unloaded = false;

	public SectorTicket(SectorManager attachedManager, T info) {
		this.attachedManager = attachedManager;
		this.info = info;
	}

	public void remove() {
		Assert.isFalse(this.unloaded);
		this.attachedManager.removeSectorTicket(this);
		this.unloaded = true;
	}

	@Override
	public void close() {
		this.remove();
	}
}
