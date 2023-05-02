package net.xavil.universal.common.universe.galaxy;

import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.util.Disposable;
import net.xavil.util.Option;

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

	public Option<StarSystem> forceLoad() {
		return this.attachedManager.forceLoad(this);
	}

	public boolean isLoaded() {
		return this.attachedManager.isSystemLoaded(this.id);
	}

	public boolean failedToLoad() {
		return isLoaded() && this.attachedManager.getSystem(this.id).isNone();
	}

	@Override
	public void dispose() {
		remove();
	}

}
