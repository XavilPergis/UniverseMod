package net.xavil.universegen.system;

public non-sealed class OtherCelestialNode extends CelestialNode {
	public OtherCelestialNode(double massYg) {
		super(massYg);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("OtherNode " + this.id);
		builder.append(" [");
		builder.append("massYg=" + this.massYg + ", ");
		builder.append("]");
		return builder.toString();
	}

}