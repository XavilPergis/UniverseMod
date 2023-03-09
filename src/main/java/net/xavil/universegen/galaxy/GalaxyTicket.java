package net.xavil.universegen.galaxy;

import net.xavil.universegen.id.SystemId;
import net.xavil.util.math.Vec3;

public abstract sealed class GalaxyTicket {

	public static final class SingleSystem extends GalaxyTicket {
		public final SystemId system;

		public SingleSystem(SystemId system) {
			this.system = system;
		}
	}
	
	public static final class Visual extends GalaxyTicket {
		public Vec3 position;
		public Vec3 lastPosition;
	}

}
