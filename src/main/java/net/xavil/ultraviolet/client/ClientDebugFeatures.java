package net.xavil.ultraviolet.client;

import net.xavil.hawklib.math.Color;

public class ClientDebugFeatures {

	public static class Feature {
		private boolean enabled = false;

		public void toggle() {
			this.enabled = !this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return this.enabled;
		}
	}

	public static final Color[] DEBUG_COLORS = { Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA,
			Color.YELLOW, };

	public static Color getDebugColor(int i) {
		return DEBUG_COLORS[i % DEBUG_COLORS.length];
	}

	public static final Feature SHOW_ORBIT_PATH_SUBDIVISIONS = new Feature();
	public static final Feature SHOW_ALL_ORBIT_PATH_LEVELS = new Feature();
	public static final Feature SHOW_SECTOR_BOUNDARIES = new Feature();
	public static final Feature SECTOR_TICKET_AROUND_FOCUS = new Feature();

}
