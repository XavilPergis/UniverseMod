package net.xavil.universal.client.screen;

import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.util.collections.Blackboard;
import net.xavil.util.math.Color;

public final class BlackboardKeys {
	private BlackboardKeys() {
	}

	public static final Blackboard.Key<GalaxySectorId> SELECTED_STAR_SYSTEM = Blackboard.Key
			.create("selected_star_system");
	public static final Blackboard.Key<Integer> SELECTED_STAR_SYSTEM_NODE = Blackboard.Key
			.create("selected_star_system_node");
	public static final Blackboard.Key<Integer> FOLLOWING_STAR_SYSTEM_NODE = Blackboard.Key
			.create("following_star_system_node");

	public static final Blackboard.Key<Color> BINARY_PATH_COLOR = Blackboard.Key.create("binary_path_color",
			new Color(0.1f, 0.4f, 0.5f, 0.5f));
	public static final Blackboard.Key<Color> UNARY_PATH_COLOR = Blackboard.Key.create("unary_path_color",
			new Color(0.5f, 0.4f, 0.1f, 0.5f));
	public static final Blackboard.Key<Color> SELECTED_PATH_COLOR = Blackboard.Key.create("selected_path_color",
			new Color(0.2, 1.0, 0.2, 1.0));

}
