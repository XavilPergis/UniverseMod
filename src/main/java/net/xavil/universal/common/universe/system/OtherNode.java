package net.xavil.universal.common.universe.system;

public non-sealed class OtherNode extends StarSystemNode {
	public OtherNode(double massYg) {
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