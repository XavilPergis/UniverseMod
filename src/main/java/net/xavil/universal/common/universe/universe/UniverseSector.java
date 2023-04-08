package net.xavil.universal.common.universe.universe;

import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.id.UniverseSectorId;
import net.xavil.util.collections.interfaces.ImmutableList;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec3i;

public final class UniverseSector {

	public final Vec3i pos;
	public volatile ImmutableList<InitialElement> initialElements = null;

	public record InitialElement(Vec3 pos, Galaxy.Info info, long seed) {
	}

	public UniverseSector(Vec3i pos) {
		this.pos = pos;
	}

	public InitialElement lookupInitial(int id) {
		return this.initialElements.get(id);
	}

	public boolean isComplete() {
		return this.initialElements != null;
	}


}
