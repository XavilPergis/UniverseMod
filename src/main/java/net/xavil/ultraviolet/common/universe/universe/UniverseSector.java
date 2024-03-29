package net.xavil.ultraviolet.common.universe.universe;

import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.hawklib.collections.interfaces.ImmutableList;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.Vec3i;

public final class UniverseSector {

	public final Vec3i pos;
	public volatile ImmutableList<InitialElement> initialElements = null;

	public record InitialElement(Vec3 pos, Galaxy.Info info) {
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
