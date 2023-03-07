package net.xavil.universegen.system;

import net.xavil.util.math.Ellipse;
import net.xavil.util.math.OrbitalPlane;
import net.xavil.util.math.OrbitalShape;

public class CelestialNodeChild<T extends CelestialNode> {
	public final CelestialNode parentNode;
	public final T node;
	public final OrbitalShape orbitalShape;
	// FIXME: this needs to be mutable for StarSystemGenerator
	public OrbitalPlane orbitalPlane;
	public final double offset;

	public CelestialNodeChild(CelestialNode parentNode, T node, OrbitalShape orbitalShape,
			OrbitalPlane orbitalPlane, double offset) {
		this.parentNode = parentNode;
		this.node = node;
		this.orbitalShape = orbitalShape;
		this.orbitalPlane = orbitalPlane;
		this.offset = offset;
	}

	public Ellipse getEllipse(OrbitalPlane referencePlane) {
		var plane = this.orbitalPlane.withReferencePlane(referencePlane);
		return Ellipse.fromOrbit(this.parentNode.position, plane, this.orbitalShape);
	}
}