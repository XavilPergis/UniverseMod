package net.xavil.universal.common.universe.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.universal.common.universe.Quat;
import net.xavil.universal.common.universe.Vec3;

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

		// // // https://www.orbiter-forum.com/threads/quaternions-rotations-and-orbital-elements.37264/
		// final var a = inclinationRad / 2;
		// final var b = (longitudeOfAscendingNodeRad + argumentOfPeriapsisRad) / 2;
		// final var c = (longitudeOfAscendingNodeRad - argumentOfPeriapsisRad) / 2;
		// final var w = Math.cos(a) * Math.cos(b);
		// final var i = Math.sin(a) * Math.cos(c);
		// final var j = Math.sin(a) * Math.sin(c);
		// final var k = Math.cos(a) * Math.sin(b);
		// return new OrbitalPlane(Quat.from(w, i, j, k));
	}

	public OrbitalPlane withReferencePlane(OrbitalPlane reference) {
		return new OrbitalPlane(reference.rotationFromReference.hamiltonProduct(this.rotationFromReference));
	}

	public static final Codec<OrbitalPlane> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Quat.CODEC.fieldOf("value").forGetter(OrbitalPlane::rotationFromReference))
			.apply(inst, OrbitalPlane::new));

}