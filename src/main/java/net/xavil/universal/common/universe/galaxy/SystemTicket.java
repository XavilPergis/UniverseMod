package net.xavil.universal.common.universe.galaxy;

import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.util.Disposable;
import net.xavil.util.ThreadSignal;

public final class SystemTicket implements Disposable {

	public final SectorManager attachedManager;
	public GalaxySectorId id;
	public final ThreadSignal generationListener = new ThreadSignal();

	public SystemTicket(SectorManager attachedManager, GalaxySectorId id) {
		this.attachedManager = attachedManager;
		this.id = id;
	}

	public void remove() {
		this.attachedManager.removeSystemTicket(this);
	}

	@Override
	public void dispose() {
		remove();
	}
	
}
