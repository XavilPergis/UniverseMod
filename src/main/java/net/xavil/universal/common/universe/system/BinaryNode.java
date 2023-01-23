package net.xavil.universal.common.universe.system;

// The position of this node represents the barycenter of the binary system
public final class BinaryNode extends StarSystemNode {
	private StarSystemNode a;
	private StarSystemNode b;

	// binary orbits always share a common orbital plane
	public OrbitalPlane orbitalPlane;

	// the barycenter and thus semimajor axes of the two ellipses are derived from
	// these quantities and the mass ratios of a and b.
	public double apastronDistanceTm;
	public double eccentricity;

	public BinaryNode(StarSystemNode a, StarSystemNode b, OrbitalPlane orbitalPlane, double eccentricity,
			double apastronDistanceTm) {
		super(a.massYg + b.massYg);

		this.a = a;
		this.b = b;
		this.orbitalPlane = orbitalPlane;
		this.apastronDistanceTm = apastronDistanceTm;
		this.eccentricity = eccentricity;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("BinaryNode " + this.id);
		builder.append(" [");
		builder.append("massYg=" + this.massYg);
		builder.append(", orbitalPlane=" + this.orbitalPlane);
		builder.append(", eccentricity=" + this.eccentricity);
		builder.append(", apastronDistance=" + this.apastronDistanceTm);
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