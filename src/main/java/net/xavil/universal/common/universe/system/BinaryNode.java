package net.xavil.universal.common.universe.system;

// The position of this node represents the barycenter of the binary system
public final class BinaryNode extends StarSystemNode {
	private StarSystemNode a;
	private StarSystemNode b;

	// binary orbits always share a common orbital plane
	public OrbitalPlane orbitalPlane;

	// distance from the barycenter to the furthest edge of either orbit
	public double maxOrbitalRadiusTm;
	// the percentage of the semi-major axis that the semi-minor is.
	public double squishFactor;

	public BinaryNode(StarSystemNode a, StarSystemNode b, OrbitalPlane orbitalPlane,
			double squishFactor, double maxOrbitalRadiusTm) {
		super(a.massYg + b.massYg);

		this.a = a;
		this.b = b;
		this.orbitalPlane = orbitalPlane;
		this.maxOrbitalRadiusTm = maxOrbitalRadiusTm;
		this.squishFactor = squishFactor;
	}

	// F -> distance between center of ellipse and one of its foci
	// F = sqrt(a^2 - b^2)
	// e = sqrt(1 - (b^2 / a^2))

	private double getSemiMajorLarger() {
		// r - F
		return this.maxOrbitalRadiusTm * (1 - (1 - this.squishFactor * this.squishFactor) / 2);
	}

	private double getSemiMinorLarger() {
		// S * r
		return this.squishFactor * this.maxOrbitalRadiusTm;
	}

	// distance from center of the larger ellipse to the barycenter (one of its
	// foci)
	private double getFocalDistanceLarger() {
		// a = r - F, b = S * r, F = sqrt(a^2 - b^2)
		// solving for F yields F = r * (1 - S^2) / 2
		return this.maxOrbitalRadiusTm * (1 - this.squishFactor * this.squishFactor) / 2;
	}

	public double getSemiMajorAxisA() {
		var res = getSemiMajorLarger();
		if (this.b.massYg < this.a.massYg)
			res *= this.b.massYg / this.a.massYg;
		return res;
	}

	public double getSemiMajorAxisB() {
		var res = getSemiMajorLarger();
		if (this.a.massYg < this.b.massYg)
			res *= this.a.massYg / this.b.massYg;
		return res;
	}

	public double getSemiMinorAxisA() {
		var res = getSemiMinorLarger();
		if (this.b.massYg < this.a.massYg)
			res *= this.b.massYg / this.a.massYg;
		return res;
	}

	public double getSemiMinorAxisB() {
		var res = getSemiMinorLarger();
		if (this.a.massYg < this.b.massYg)
			res *= this.a.massYg / this.b.massYg;
		return res;
	}

	public double getFocalDistanceA() {
		var res = getFocalDistanceLarger();
		// the focal distance of the smaller orbit is just a scaled version of the
		// larger orbit's:
		// a = k*r-F_s, b = k*S*r, F_s = sqrt(a^2 - b^2)
		// solving for F_s yields F_s = k * r * (1 - S^2) / 2, which is k * F
		if (this.b.massYg < this.a.massYg)
			res *= this.b.massYg / this.a.massYg;
		return res;
	}

	public double getFocalDistanceB() {
		var res = getFocalDistanceLarger();
		if (this.a.massYg < this.b.massYg)
			res *= this.a.massYg / this.b.massYg;
		return res;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("BinaryNode " + this.id);
		builder.append(" [");
		builder.append("massYg=" + this.massYg);
		builder.append(", orbitalPlane=" + this.orbitalPlane);
		builder.append(", squishFactor=" + this.squishFactor);
		builder.append(", maxOrbitalRadiusTm=" + this.maxOrbitalRadiusTm);
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