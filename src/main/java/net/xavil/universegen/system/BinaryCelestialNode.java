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
	public double phase;

	public BinaryCelestialNode() {
	}

	private BinaryCelestialNode(CelestialNode a, CelestialNode b) {
		super(a.massYg + b.massYg);
		this.a = a;
		this.b = b;
	}

	public BinaryCelestialNode(double massYg, CelestialNode a, CelestialNode b, OrbitalPlane orbitalPlane,
			OrbitalShape orbitalShapeA, OrbitalShape orbitalShapeB, double phase) {
		super(massYg);
		this.a = a;
		this.b = b;
		this.orbitalPlane = orbitalPlane;
		this.orbitalShapeA = orbitalShapeA;
		this.orbitalShapeB = orbitalShapeB;
		this.phase = phase;
	}

	public BinaryCelestialNode(CelestialNode a, CelestialNode b, OrbitalPlane orbitalPlane,
			OrbitalShape orbitalShapeA, OrbitalShape orbitalShapeB, double phase) {
		super(a.massYg + b.massYg);
		this.a = a;
		this.b = b;
		this.orbitalPlane = orbitalPlane;
		this.orbitalShapeA = orbitalShapeA;
		this.orbitalShapeB = orbitalShapeB;
		this.phase = phase;
	}

	public static BinaryCelestialNode fromSquishFactor(CelestialNode a, CelestialNode b, OrbitalPlane orbitalPlane,
			double squishFactor, double maxOrbitalRadiusTm, double phase) {
		final var node = new BinaryCelestialNode(a, b);

		if (a.massYg > b.massYg) {
			node.a = a;
			node.b = b;
		} else {
			node.a = b;
			node.b = a;
		}

		// object with the smaller mass has the larger orbit.
		final var majorB = maxOrbitalRadiusTm;
		final var minorB = squishFactor * maxOrbitalRadiusTm;
		final var shapeB = OrbitalShape.fromAxes(majorB, minorB);
		final var shapeA = OrbitalShape.fromEccentricity(shapeB.eccentricity(),
				(node.b.massYg / node.a.massYg) * shapeB.semiMajor());

		node.orbitalPlane = orbitalPlane;
		node.orbitalShapeA = shapeA;
		node.orbitalShapeB = shapeB;
		node.phase = phase;
		return node;
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