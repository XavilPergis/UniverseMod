package net.xavil.ultraviolet.common.universe.system;

import net.xavil.hawklib.math.Ellipse;
import net.xavil.hawklib.math.OrbitalPlane;
import net.xavil.hawklib.math.OrbitalShape;

public class CelestialNodeChild<T extends CelestialNode> {
	public final CelestialNode parentNode;
	public final T node;
	public final OrbitalShape orbitalShape;
	// FIXME: this needs to be mutable for StarSystemGenerator
	public OrbitalPlane orbitalPlane;
	public final double phase;

	public CelestialNodeChild(CelestialNode parentNode, T node, OrbitalShape orbitalShape,
			OrbitalPlane orbitalPlane, double phase) {
		this.parentNode = parentNode;
		this.node = node;
		this.orbitalShape = orbitalShape;
		this.orbitalPlane = orbitalPlane;
		this.phase = phase;
	}

	public Ellipse getEllipse(OrbitalPlane referencePlane, double celestialTime) {
		var plane = this.orbitalPlane.withReferencePlane(referencePlane);
		return Ellipse.fromOrbit(this.parentNode.position, plane, this.orbitalShape, this.node.apsidalRate * celestialTime, false);
	}

	public Ellipse getEllipse(double celestialTime) {
		return getEllipse(this.parentNode.referencePlane, celestialTime);
	}
}