package net.xavil.universal.common.universe.system;

// The position of this node represents the barycenter of the binary system
public final class BinaryNode extends StarSystemNode {
	public StarSystemNode a;
	public StarSystemNode b;

	// binary orbits always share a common orbital plane
	public OrbitalPlane orbitalPlane;

	// each individual orbit in a binary can have its own ellipse shape
	public double eccentricity;
	public double semimajorAxisATm;
	public double semimajorAxisBTm;

	public BinaryNode(StarSystemNode a, StarSystemNode b, OrbitalPlane orbitalPlane, double eccentricity,
			double maxDistanceTm) {
		super(a.massYg + b.massYg);

		this.a = a;
		this.b = b;
		this.orbitalPlane = orbitalPlane;
		this.eccentricity = eccentricity;

		this.semimajorAxisATm = maxDistanceTm;
		this.semimajorAxisBTm = maxDistanceTm;
		// this.shapeA = shapeA;
		// this.shapeB = shapeB;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("BinaryNode " + this.id);
		builder.append(" [");
		builder.append("massYg=" + this.massYg + ", ");
		builder.append("orbitalPlane=" + this.orbitalPlane + ", ");
		builder.append("eccentricity=" + this.eccentricity + ", ");
		builder.append("semimajorAxisATm=" + this.semimajorAxisATm + ", ");
		builder.append("semimajorAxisBTm=" + this.semimajorAxisBTm);
		builder.append("]");
		return builder.toString();
	}

}