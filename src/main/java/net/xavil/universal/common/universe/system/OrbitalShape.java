package net.xavil.universal.common.universe.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.universal.common.Ellipse;

// defines the shape of the orbital ellipse
public record OrbitalShape(double eccentricity, double semiMajor) {

	public static OrbitalShape fromEccentricity(double eccentricity, double semiMajor) {
		return new OrbitalShape(eccentricity, semiMajor);
	}

	public static OrbitalShape fromAxes(double semiMajor, double semiMinor) {
		return new OrbitalShape(Ellipse.eccentricity(semiMajor, semiMinor), semiMajor);
	}

	public double semiMinor() {
		return this.semiMajor * Math.sqrt(1 - this.eccentricity * this.eccentricity);
	}

	// distance from the center of the ellipse to one of its foci
	public double focalDistance() {
		double a = this.semiMajor, b = this.semiMinor();
		return Math.sqrt(a * a - b * b);
	}

	public static final Codec<OrbitalShape> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.DOUBLE.fieldOf("eccentricity").forGetter(OrbitalShape::eccentricity),
			Codec.DOUBLE.fieldOf("semi_major_axis").forGetter(OrbitalShape::semiMajor))
			.apply(inst, OrbitalShape::new));

}