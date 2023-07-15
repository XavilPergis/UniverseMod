package net.xavil.universegen.system;

import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.OrbitalPlane;

public class CelestialRing {
	public final OrbitalPlane orbitalPlane;
	public final double eccentricity;
	public final Interval interval;
	public final double mass;

	public CelestialRing(OrbitalPlane orbitalPlane, double eccentricity, Interval interval, double mass) {
		this.orbitalPlane = orbitalPlane;
		this.eccentricity = eccentricity;
		this.interval = interval;
		this.mass = mass;
	}
}
