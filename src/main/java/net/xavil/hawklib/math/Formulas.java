package net.xavil.hawklib.math;

import static net.xavil.hawklib.Units.*;
import static net.xavil.hawklib.Constants.*;

public class Formulas {

	protected Formulas() {
	}

	/**
	 * @param semiMajor (Tm) The semi-major axis
	 * @param mu        (Yg) The standard gravitational pramater. For unary orbits,
	 *                  this is the mass of the larger body. For binary orbits, this
	 *                  is the combined mass of each body.
	 * @return (s) The orbital period.
	 */
	public static double orbitalPeriod(double semiMajor, double mass) {
		final var a = u_PER_Tu * semiMajor;
		final var mu = (GRAVITATIONAL_CONSTANT_m3_PER_kg_s2 * ku_PER_Yu * mass);
		return 2 * Math.PI * Math.sqrt(Math.pow(a, 3.0) / mu);
	}

	/**
	 * @param meanAnomaly (rad) The mean anomaly.
	 * @param e           (unitless) The eccentricity of the ellipse.
	 * @return (rad) The true anomaly.
	 */
	// super stolen from
	// https://github.com/RegrowthStudios/SoACode-Public/blob/c3ddd69355b534d5e70e2e6d0c489b4e93ab1ffe/SoA/OrbitComponentUpdater.cpp#L70
	public static double calculateTrueAnomaly(double meanAnomaly, double e) {
		final var iterationCount = 10;
		var E = meanAnomaly;
		for (var n = 0; n < iterationCount; ++n) {
			double F = E - e * Math.sin(E) - meanAnomaly;
			E -= F / (1 - e * Math.cos(E));
		}
		return Math.atan2(Math.sqrt(1 - e * e) * Math.sin(E), Math.cos(E) - e);
	}

	/**
	 * The radius at which tidal effects of the parent body rip apart the child body
	 * into rings. Could also be used as the minimum bound for finding stable
	 * orbits.
	 * 
	 * @param parentMass  (Yg) The mass of the parent body.
	 * @param childMass   (Yg) The mass of the child body.
	 * @param childRadius (km) The radius of the child body.
	 * @return (Tm) The radius of the roche sphere.
	 */
	public static double rocheLimit(double parentMass, double childMass, double childRadius) {
		final var au_PER_km = 1 / km_PER_au;
		final var childRadiusAu = childRadius * au_PER_km;
		final var parentMassMsol = parentMass / Yg_PER_Msol;
		final var childMassMsol = childMass / Yg_PER_Msol;
		final var limitAu = childRadiusAu * Math.cbrt(2 * (parentMassMsol / childMassMsol));
		return limitAu * Tm_PER_au;
	}

	/**
	 * The interval over which any object with overlapping intervals will
	 * "gravitationally interact", and cannot form longterm stable orbits.
	 * 
	 * @param shape (Tm,unitless) The orbital shape of the object.
	 * @param mass  (Yg) The mass of the object.
	 * @return (Tm,Tm) The effect interval.
	 */
	public static Interval gravitationalEffectLimits(OrbitalShape shape, double mass) {
		final var massMsol = mass / Yg_PER_Msol;
		final var m = Math.pow(massMsol / (1 + massMsol), 0.25);
		final var inner = (1 - m) * shape.periapsisDistance();
		final var outer = (1 + m) * shape.apoapsisDistance();
		return new Interval(inner, outer);
	}

	/**
	 * Calculates the radius of the child's hill sphere in the presence of a
	 * particular orbit around a parent object.
	 * 
	 * @param shape      (Tm,unitless) The orbital shape of the child object.
	 * @param parentMass (Yg) The mass of the parent object.
	 * @param childMass  (Yg) The mass of the child object.
	 * @return (Tm) The radius of the child's hill sphere.
	 */
	public static double hillSphereRadius(OrbitalShape shape, double parentMass, double childMass) {
		final var parentMassMsol = parentMass / Yg_PER_Msol;
		final var childMassMsol = childMass / Yg_PER_Msol;
		final var radiusAu = shape.semiMajor() * (1 - shape.eccentricity())
				* Math.cbrt(childMassMsol / (3 * parentMassMsol));
		return radiusAu * Tm_PER_au;
	}

	/**
	 * An estimate for the amount of time it takes a celestial body to become
	 * tidally locked with another.
	 * 
	 * @param mass       (Yg) The mass of the object.
	 * @param radius     (km) The radius of the object.
	 * @param rigidity   (N/m^2) The rigidity of the object.
	 * @param parentMass (Yg) The mass of the parent/co-orbiting body.
	 * @param semiMajor  (Tm) The semi-major axis of the orbit's ellipse.
	 * @return (Myr) The time until a planet becomes tidally locked.
	 */
	// https://en.wikipedia.org/wiki/Tidal_locking#Timescale
	public static double timeUntilTidallyLocked(double mass, double radius, double rigidity,
			double parentMass, double semiMajor) {
		final var radius_m = u_PER_ku * radius;
		final var denom = ku_PER_Yu * mass * Math.pow(ku_PER_Yu * parentMass, 2);
		final var num = 6 * Math.pow(u_PER_Tu * semiMajor, 6) * radius_m * rigidity;
		return (num / denom) / 1e4;
	}

}
