package net.xavil.universal.common.universe.universe;

import net.xavil.universal.common.universe.id.UniverseSectorId;
import net.xavil.util.Disposable;
import net.xavil.util.ThreadSignal;

public final class GalaxyTicket implements Disposable {

	public final UniverseSectorManager attachedManager;
	public UniverseSectorId id;
	public final ThreadSignal generationListener = new ThreadSignal();

	public GalaxyTicket(UniverseSectorManager attachedManager, UniverseSectorId id) {
		this.attachedManager = attachedManager;
		this.id = id;
	}

	public void remove() {
		this.attachedManager.removeGalaxyTicket(this);
	}

	@Override
	public void dispose() {
		remove();
	}
	
}
