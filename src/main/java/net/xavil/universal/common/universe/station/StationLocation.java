package net.xavil.universal.common.universe.station;

import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.util.math.OrbitalPlane;
import net.xavil.util.math.OrbitalShape;
import net.xavil.util.math.Vec3;

public abstract sealed class StationLocation {

	public static final class OrbitingCelestialBody extends StationLocation {
		public SystemNodeId id;
		public OrbitalPlane plane;
		public OrbitalShape shape;
		public double time;
	}

	public static final class JumpingSystem extends StationLocation {
		public StationLocation from;
		public double completion;
	}

	public static final class SystemRelative extends StationLocation {
		public Vec3 pos;
	}

	public static final class JumpingGalaxy extends StationLocation {
		public StationLocation from, to;
		public double completion;
	}

	public static final class GalaxyRelative extends StationLocation {
		public Vec3 pos;
	}

}
