package net.xavil.ultraviolet.client.screen;

import net.xavil.ultraviolet.client.screen.layer.Signal;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.hawklib.collections.Blackboard;
import net.xavil.hawklib.math.ColorRgba;

public final class BlackboardKeys {
	private BlackboardKeys() {
	}

	public static final Blackboard.Key<String, GalaxySectorId> SELECTED_STAR_SYSTEM = Blackboard.Key
			.create("selected_star_system");
	public static final Blackboard.Key<String, Integer> SELECTED_STAR_SYSTEM_NODE = Blackboard.Key
			.create("selected_star_system_node");
	public static final Blackboard.Key<String, Integer> FOLLOWING_STAR_SYSTEM_NODE = Blackboard.Key
			.create("following_star_system_node");
	public static final Blackboard.Key<String, Boolean> SHOW_GUIDES = Blackboard.Key
			.create("show_guides");

	public static final Blackboard.Key<String, ColorRgba> BINARY_PATH_COLOR = Blackboard.Key
			.create("binary_path_color", new ColorRgba(0.1f, 0.4f, 0.5f, 0.5f));
	public static final Blackboard.Key<String, ColorRgba> UNARY_PATH_COLOR = Blackboard.Key
			.create("unary_path_color", new ColorRgba(0.5f, 0.4f, 0.1f, 0.5f));
	public static final Blackboard.Key<String, ColorRgba> SELECTED_PATH_COLOR = Blackboard.Key
			.create("selected_path_color", new ColorRgba(0.2f, 1.0f, 0.2f, 1.0f));

	public static final Signal TEST_SIGNAL = new Signal();

}
