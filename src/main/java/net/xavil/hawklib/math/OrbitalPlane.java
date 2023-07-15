package net.xavil.hawklib.math;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.hawklib.math.matrices.Vec3;

public record OrbitalPlane(Quat rotationFromReference) {

	public static final OrbitalPlane ZERO = new OrbitalPlane(Quat.IDENTITY);

	public static OrbitalPlane fromOrbitalElements(
			double inclinationRad,
			double longitudeOfAscendingNodeRad,
			double argumentOfPeriapsisRad) {

		var x = Quat.axisAngle(Vec3.YP, longitudeOfAscendingNodeRad);
		var y = Quat.axisAngle(Vec3.XP, inclinationRad);
		var z = Quat.axisAngle(Vec3.YP, argumentOfPeriapsisRad);

		var q = x.hamiltonProduct(y).hamiltonProduct(z);
		return new OrbitalPlane(q);
	}

	public OrbitalPlane withReferencePlane(OrbitalPlane reference) {
		return new OrbitalPlane(reference.rotationFromReference.hamiltonProduct(this.rotationFromReference));
	}

	public static final Codec<OrbitalPlane> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Quat.CODEC.fieldOf("value").forGetter(OrbitalPlane::rotationFromReference))
			.apply(inst, OrbitalPlane::new));

}