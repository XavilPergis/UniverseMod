package net.xavil.universal.client;

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

	public static final Feature SHOW_ORBIT_PATH_SUBDIVISIONS = new Feature();
	public static final Feature SHOW_ALL_ORBIT_PATH_LEVELS = new Feature();
	public static final Feature SHOW_SECTOR_BOUNDARIES = new Feature();
	public static final Feature SECTOR_TICKET_AROUND_FOCUS = new Feature();

}
