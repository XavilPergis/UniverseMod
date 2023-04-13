package net.xavil.universal.client.screen;

import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.util.collections.Blackboard;

public final class BlackboardKeys {
	private BlackboardKeys() {
	}

	public static final Blackboard.Key<GalaxySectorId> SELECTED_STAR_SYSTEM = Blackboard.Key
			.create("selected_star_system");
}
