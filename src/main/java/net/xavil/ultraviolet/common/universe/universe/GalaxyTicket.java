package net.xavil.ultraviolet.common.universe.universe;

import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.util.Disposable;
import net.xavil.util.Option;

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

	public Option<Galaxy> forceLoad() {
		return this.attachedManager.forceLoad(this);
	}
	
}
