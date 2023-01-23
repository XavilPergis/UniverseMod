package net.xavil.universal.common.universe.system;

public non-sealed class PlanetNode extends StarSystemNode {
	public PlanetNode(double massYg) {
		super(massYg);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("PlanetaryBodyNode " + this.id);
		builder.append(" [");
		builder.append("massYg=" + this.massYg + ", ");
		builder.append("]");
		return builder.toString();
	}
	// planet type (gas giant, icy world, rocky world, earth-like world, etc)
	// mass, surface gravity, atmosphere type, landable
	// asteroid belt/rings? perhaps a single disc object?

}