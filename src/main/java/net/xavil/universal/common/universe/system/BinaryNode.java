package net.xavil.universal.common.universe.system;

import net.xavil.universal.common.Ellipse;

// The position of this node represents the barycenter of the binary system
public final class BinaryNode extends StarSystemNode {
	private StarSystemNode a;
	private StarSystemNode b;

	// binary orbits always share a common orbital plane
	public OrbitalPlane orbitalPlane;
	public OrbitalShape orbitalShapeA;
	public OrbitalShape orbitalShapeB;
	public double offset;

	public BinaryNode(double massYg, StarSystemNode a, StarSystemNode b, OrbitalPlane orbitalPlane,
			OrbitalShape orbitalShapeA, OrbitalShape orbitalShapeB, double offset) {
		super(massYg);
		this.a = a;
		this.b = b;
		this.orbitalPlane = orbitalPlane;
		this.orbitalShapeA = orbitalShapeA;
		this.orbitalShapeB = orbitalShapeB;
		this.offset = offset;
	}

	public BinaryNode(StarSystemNode a, StarSystemNode b, OrbitalPlane orbitalPlane,
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

	public Ellipse getEllipseA(OrbitalPlane referencePlane) {
		final var plane = this.orbitalPlane.withReferencePlane(referencePlane);
		return Ellipse.fromOrbit(this.position, plane, orbitalShapeA, false);
	}

	public Ellipse getEllipseB(OrbitalPlane referencePlane) {
		final var plane = this.orbitalPlane.withReferencePlane(referencePlane);
		return Ellipse.fromOrbit(this.position, plane, orbitalShapeB, true);
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

	public void setA(StarSystemNode node) {
		this.a = node;
	}

	public void setB(StarSystemNode node) {
		this.b = node;
	}

	public void replace(StarSystemNode oldNode, StarSystemNode newNode) {
		if (oldNode == this.a) {
			this.a = newNode;
		} else if (oldNode == this.b) {
			this.b = newNode;
		} else {
			throw new IllegalArgumentException("tried to replace binary child that was not in the binary node");
		}
	}

	public StarSystemNode getA() {
		return this.a;
	}

	public StarSystemNode getB() {
		return this.b;
	}

}