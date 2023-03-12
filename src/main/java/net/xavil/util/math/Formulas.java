package net.xavil.util.math;

import net.xavil.util.Units;

public final class Formulas {

	/**
	 * @param semiMajor (Tm) The semi-major axis
	 * @param mu        (Yg) The standard gravitational pramater. For unary orbits,
	 *                  this is the mass of the larger body. For binary orbits, this
	 *                  is the combined mass of each body.
	 * @return (s) The orbital period.
	 */
	public static double orbitalPeriod(double semiMajor, double mu) {
		final var a = Units.TERA * semiMajor;
		return 2 * Math.PI * Math.sqrt(a * a * a / (Units.GRAVITATIONAL_CONSTANT_m3_PER_kg_s2 * Units.YOTTA * mu));
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
	 * @param childRadius (Mm) The radius of the child body.
	 * @return (Tm) The radius of the roche sphere.
	 */
	public static double rocheLimit(double parentMass, double childMass, double childRadius) {
		final var au_PER_Mm = (Units.MEGA / Units.KILO) / Units.km_PER_au;
		final var childRadiusAu = childRadius * au_PER_Mm;
		final var parentMassMsol = parentMass / Units.Yg_PER_Msol;
		final var childMassMsol = childMass / Units.Yg_PER_Msol;
		final var limitAu = childRadiusAu * Math.cbrt(2 * (parentMassMsol / childMassMsol));
		return limitAu * Units.Tm_PER_au;
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
		var massMsol = mass / Units.Yg_PER_Msol;
		var m = Math.pow(massMsol / (1 + massMsol), 0.25);
		var inner = (1 - m) * shape.periapsisDistance();
		var outer = (1 + m) * shape.apoapsisDistance();
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
		final var parentMassMsol = parentMass / Units.Yg_PER_Msol;
		final var childMassMsol = childMass / Units.Yg_PER_Msol;
		final var radiusAu = shape.semiMajor() * (1 - shape.eccentricity())
				* Math.cbrt(childMassMsol / (3 * parentMassMsol));
		return radiusAu * Units.Tm_PER_au;
	}

}
