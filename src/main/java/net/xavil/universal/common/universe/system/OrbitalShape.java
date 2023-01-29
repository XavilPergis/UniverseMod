package net.xavil.universal.common.universe.system;

// defines the shape of the orbital ellipse
public record OrbitalShape(
		double eccentricity,
		double semimajorAxisTm) {

	public double semiminorAxisTm() {
		return this.semimajorAxisTm * Math.sqrt(1 - this.eccentricity * this.eccentricity);
	}

}