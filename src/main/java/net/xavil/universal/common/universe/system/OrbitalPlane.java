package net.xavil.universal.common.universe.system;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.common.universe.UniverseId.SectorId;

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

	public static final OrbitalPlane ZERO = new OrbitalPlane(0, 0, 0);

	public OrbitalPlane transform(OrbitalPlane child) {
		// FIXME: this is wrong
		return new OrbitalPlane(inclinationRad + child.inclinationRad,
				longitueOfAscendingNodeRad + child.longitueOfAscendingNodeRad,
				argumentOfPeriapsisRad + child.argumentOfPeriapsisRad);
	}

	public OrbitalPlane withInclination(double inclinationRad) {
		return new OrbitalPlane(inclinationRad, longitueOfAscendingNodeRad, argumentOfPeriapsisRad);
	}

	public OrbitalPlane withLongitudeOfAscendingNode(double longitueOfAscendingNodeRad) {
		return new OrbitalPlane(inclinationRad, longitueOfAscendingNodeRad, argumentOfPeriapsisRad);
	}

	public OrbitalPlane withArgumentOfPeriapsis(double argumentOfPeriapsisRad) {
		return new OrbitalPlane(inclinationRad, longitueOfAscendingNodeRad, argumentOfPeriapsisRad);
	}

	public static final Codec<OrbitalPlane> CODEC = RecordCodecBuilder.create(inst -> inst.group(
			Codec.DOUBLE.fieldOf("inclination").forGetter(OrbitalPlane::inclinationRad),
			Codec.DOUBLE.fieldOf("ascending_node_angle").forGetter(OrbitalPlane::longitueOfAscendingNodeRad),
			Codec.DOUBLE.fieldOf("periapsis_angle").forGetter(OrbitalPlane::argumentOfPeriapsisRad))
			.apply(inst, OrbitalPlane::new));

}