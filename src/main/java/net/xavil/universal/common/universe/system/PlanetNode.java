package net.xavil.universal.common.universe.system;

public non-sealed class PlanetNode extends StarSystemNode {

	public enum Type {
		GAS_GIANT,
		ICE_WORLD,
		ROCKY_WORLD,
		ROCKY_ICE_WORLD,
		WATER_WORLD,
		EARTH_LIKE_WORLD,
	}

	public Type type;
	public double radiusRearth;
	public double temperatureK;

	public PlanetNode(Type type, double massYg, double radiusRearth, double temperatureK) {
		super(massYg);
		this.type = type;
		this.radiusRearth = radiusRearth;
		this.temperatureK = temperatureK;
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("PlanetaryBodyNode " + this.id);
		builder.append(" [");
		builder.append("massYg=" + this.massYg + ", ");
		builder.append("radiusRearth=" + this.radiusRearth + ", ");
		builder.append("temperatureK=" + this.temperatureK + ", ");
		builder.append("]");
		return builder.toString();
	}

	// planet type (gas giant, icy world, rocky world, earth-like world, etc)
	// mass, surface gravity, atmosphere type, landable
	// cloud coverage, greenhouse effect, volcanism, plate tectonics
	// asteroid belt/rings? perhaps a single disc object?

}