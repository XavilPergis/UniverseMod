package net.xavil.universegen.system;

import net.xavil.hawklib.math.Ellipse;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;

// The position of this node represents the barycenter of the binary system
public final class BinaryCelestialNode extends CelestialNode {
	private CelestialNode a;
	private CelestialNode b;

	// binary orbits always share a common orbital plane
	public OrbitalPlane orbitalPlane;
	public OrbitalShape orbitalShapeA;
	// this is always the larger of the two
	public OrbitalShape orbitalShapeB;
	public double offset;

	public BinaryCelestialNode(double massYg, CelestialNode a, CelestialNode b, OrbitalPlane orbitalPlane,
			OrbitalShape orbitalShapeA, OrbitalShape orbitalShapeB, double offset) {
		super(massYg);
		this.a = a;
		this.b = b;
		this.orbitalPlane = orbitalPlane;
		this.orbitalShapeA = orbitalShapeA;
		this.orbitalShapeB = orbitalShapeB;
		this.offset = offset;
	}

	public BinaryCelestialNode(CelestialNode a, CelestialNode b, OrbitalPlane orbitalPlane,
			double squishFactor, double maxOrbitalRadiusTm, double offset) {
		super(a.massYg + b.massYg);

		if (a.massYg > b.massYg) {
			this.a = a;
			this.b = b;
		} else {
			this.a = b;
			this.b = a;
		}

		// object with the smaller mass has the larger orbit.
		final var majorB = maxOrbitalRadiusTm;
		final var minorB = squishFactor * maxOrbitalRadiusTm;
		final var shapeB = OrbitalShape.fromAxes(majorB, minorB);
		final var shapeA = OrbitalShape.fromEccentricity(shapeB.eccentricity(),
				(this.b.massYg / this.a.massYg) * shapeB.semiMajor());

		this.orbitalPlane = orbitalPlane;
		this.orbitalShapeA = shapeA;
		this.orbitalShapeB = shapeB;
		this.offset = offset;
	}

	public OrbitalShape getCombinedShape() {
		return new OrbitalShape(this.orbitalShapeB.eccentricity(),
				this.orbitalShapeA.semiMajor() + this.orbitalShapeB.semiMajor());
	}

	public Ellipse getEllipseA(OrbitalPlane referencePlane, double celestialTime) {
		final var plane = this.orbitalPlane.withReferencePlane(referencePlane);
		return Ellipse.fromOrbit(this.position, plane, orbitalShapeA, this.apsidalRate * celestialTime, false);
	}

	public Ellipse getEllipseB(OrbitalPlane referencePlane, double celestialTime) {
		final var plane = this.orbitalPlane.withReferencePlane(referencePlane);
		return Ellipse.fromOrbit(this.position, plane, orbitalShapeB, this.apsidalRate * celestialTime, true);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("BinaryNode " + this.id);
		builder.append(" [");
		builder.append("massYg=" + this.massYg);
		builder.append(", orbitalPlane=" + this.orbitalPlane);
		builder.append(", orbitalShapeA=" + this.orbitalShapeA);
		builder.append(", orbitalShapeB=" + this.orbitalShapeB);
		builder.append("]");
		return builder.toString();
	}

	public void setA(CelestialNode node) {
		this.a = node;
	}

	public void setB(CelestialNode node) {
		this.b = node;
	}

	public void replace(CelestialNode oldNode, CelestialNode newNode) {
		if (oldNode == this.a) {
			this.a = newNode;
		} else if (oldNode == this.b) {
			this.b = newNode;
		} else {
			throw new IllegalArgumentException("tried to replace binary child that was not in the binary node");
		}
	}

	public CelestialNode getA() {
		return this.a;
	}

	public CelestialNode getB() {
		return this.b;
	}

}