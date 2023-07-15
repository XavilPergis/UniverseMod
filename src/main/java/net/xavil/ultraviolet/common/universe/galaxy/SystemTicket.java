package net.xavil.ultraviolet.common.universe.galaxy;

import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;

public final class SystemTicket implements Disposable {

	public final SectorManager attachedManager;
	public GalaxySectorId id;

	public SystemTicket(SectorManager attachedManager, GalaxySectorId id) {
		this.attachedManager = attachedManager;
		this.id = id;
	}

	public void remove() {
		this.attachedManager.removeSystemTicket(this);
	}

	public Maybe<StarSystem> forceLoad() {
		return this.attachedManager.forceLoad(this);
	}

	public boolean isLoaded() {
		return this.attachedManager.isSystemLoaded(this.id);
	}

	public boolean failedToLoad() {
		return isLoaded() && this.attachedManager.getSystem(this.id).isNone();
	}

	@Override
	public void close() {
		remove();
	}

}
