package net.xavil.universal.common.universe.system;

// reference plane is the XZ plane, reference direction is +X
// all angles are in radians
public record OrbitalPlane(
		// the "tilt" of the orbital plane around the ascending node
		double inclinationRad,
		// the "ascending node" is the axis where the tilted plane and the reference
		// plane intersect
		// the angle of the ascending node, measured from the reference direction
		double longitueOfAscendingNodeRad,
		// the "periapsis" is distance of the closest approach of the orbiting pair
		// the angle at which periapsis occurs, measured from the ascending node
		double argumentOfPeriapsisRad) {

	public OrbitalPlane withInclination(double inclinationRad) {
		return new OrbitalPlane(inclinationRad, longitueOfAscendingNodeRad, argumentOfPeriapsisRad);
	}

	public OrbitalPlane withLongitudeOfAscendingNode(double longitueOfAscendingNodeRad) {
		return new OrbitalPlane(inclinationRad, longitueOfAscendingNodeRad, argumentOfPeriapsisRad);
	}

	public OrbitalPlane withArgumentOfPeriapsis(double argumentOfPeriapsisRad) {
		return new OrbitalPlane(inclinationRad, longitueOfAscendingNodeRad, argumentOfPeriapsisRad);
	}

}