package net.xavil.universal.common.universe.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// defines the shape of the orbital ellipse
public record OrbitalShape(
		double eccentricity,
		double semimajorAxisTm) {

	public double semiminorAxisTm() {
		return this.semimajorAxisTm * Math.sqrt(1 - this.eccentricity * this.eccentricity);
	}

	public static final Codec<OrbitalShape> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.DOUBLE.fieldOf("eccentricity").forGetter(OrbitalShape::eccentricity),
			Codec.DOUBLE.fieldOf("semi_major_axis").forGetter(OrbitalShape::semimajorAxisTm))
			.apply(inst, OrbitalShape::new));

}