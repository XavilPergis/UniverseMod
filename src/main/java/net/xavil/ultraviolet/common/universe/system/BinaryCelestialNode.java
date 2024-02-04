package net.xavil.ultraviolet.common.universe.system;

import net.xavil.hawklib.math.Ellipse;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;

// The position of this node represents the barycenter of the binary system
public final class BinaryCelestialNode extends CelestialNode {
	public CelestialNode inner;
	public CelestialNode outer;

	// binary orbits always share a common orbital plane
	public OrbitalPlane orbitalPlane;
	public OrbitalShape orbitalShapeInner;
	// this is always the larger of the two
	public OrbitalShape orbitalShapeOuter;
	public double phase;

	public BinaryCelestialNode() {
	}

	public BinaryCelestialNode(CelestialNode a, CelestialNode b) {
		super(a.massYg + b.massYg);
		this.inner = a;
		this.outer = b;
	}

	public BinaryCelestialNode(double massYg, CelestialNode a, CelestialNode b, OrbitalPlane orbitalPlane,
			OrbitalShape orbitalShapeA, OrbitalShape orbitalShapeB, double phase) {
		super(massYg);
		this.inner = a;
		this.outer = b;
		this.orbitalPlane = orbitalPlane;
		this.orbitalShapeInner = orbitalShapeA;
		this.orbitalShapeOuter = orbitalShapeB;
		this.phase = phase;
	}

	public BinaryCelestialNode(CelestialNode a, CelestialNode b, OrbitalPlane orbitalPlane,
			OrbitalShape orbitalShapeA, OrbitalShape orbitalShapeB, double phase) {
		super(a.massYg + b.massYg);
		this.inner = a;
		this.outer = b;
		this.orbitalPlane = orbitalPlane;
		this.orbitalShapeInner = orbitalShapeA;
		this.orbitalShapeOuter = orbitalShapeB;
		this.phase = phase;
	}

	public static BinaryCelestialNode fromSquishFactor(CelestialNode a, CelestialNode b, OrbitalPlane orbitalPlane,
			double squishFactor, double maxOrbitalRadiusTm, double phase) {
		final var node = new BinaryCelestialNode(a, b);

		node.setSiblings(a, b);

		// object with the smaller mass has the larger orbit.
		final var majorB = maxOrbitalRadiusTm;
		final var minorB = squishFactor * maxOrbitalRadiusTm;
		node.setOrbitalShapes(OrbitalShape.fromAxes(majorB, minorB));
		node.orbitalPlane = orbitalPlane;
		node.phase = phase;
		return node;
	}

	public void setSiblings(CelestialNode a, CelestialNode b) {
		if (a.massYg > b.massYg) {
			this.inner = a;
			this.outer = b;
		} else {
			this.inner = b;
			this.outer = a;
		}
		this.massYg = this.inner.massYg + this.outer.massYg;
	}

	public void setOrbitalShapes(OrbitalShape outerShape) {
		final var innerShape = new OrbitalShape(outerShape.eccentricity(),
				(this.outer.massYg / this.inner.massYg) * outerShape.semiMajor());

		this.orbitalShapeInner = innerShape;
		this.orbitalShapeOuter = outerShape;
	}

	public OrbitalShape getCombinedShape() {
		return new OrbitalShape(this.orbitalShapeOuter.eccentricity(),
				this.orbitalShapeInner.semiMajor() + this.orbitalShapeOuter.semiMajor());
	}

	public Ellipse getEllipseA(OrbitalPlane referencePlane, double celestialTime) {
		final var plane = this.orbitalPlane.withReferencePlane(referencePlane);
		return Ellipse.fromOrbit(this.position, plane, orbitalShapeInner, this.apsidalRate * celestialTime, false);
	}

	public Ellipse getEllipseB(OrbitalPlane referencePlane, double celestialTime) {
		final var plane = this.orbitalPlane.withReferencePlane(referencePlane);
		return Ellipse.fromOrbit(this.position, plane, orbitalShapeOuter, this.apsidalRate * celestialTime, true);
	}

	public void setInner(CelestialNode node) {
		this.inner = node;
	}

	public void setOuter(CelestialNode node) {
		this.outer = node;
	}

	public void replace(CelestialNode oldNode, CelestialNode newNode) {
		if (oldNode == this.inner) {
			this.inner = newNode;
		} else if (oldNode == this.outer) {
			this.outer = newNode;
		} else {
			throw new IllegalArgumentException("tried to replace binary child that was not in the binary node");
		}
	}

	public CelestialNode getInner() {
		return this.inner;
	}

	public CelestialNode getOuter() {
		return this.outer;
	}

}