package net.xavil.ultraviolet.common.universe.universe;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;

public final class GalaxyTicket implements Disposable {

	public final UniverseSectorManager attachedManager;
	public UniverseSectorId id;

	public GalaxyTicket(UniverseSectorManager attachedManager, UniverseSectorId id) {
		this.attachedManager = attachedManager;
		this.id = id;
	}

	public void remove() {
		this.attachedManager.removeGalaxyTicket(this);
	}

	@Override
	public void close() {
		remove();
	}

	public Maybe<Galaxy> forceLoad() {
		return this.attachedManager.forceLoad(this);
	}
	
}
