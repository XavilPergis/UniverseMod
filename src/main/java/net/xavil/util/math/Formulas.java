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
	 * @param e (unitless) The eccentricity of the ellipse.
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

}
