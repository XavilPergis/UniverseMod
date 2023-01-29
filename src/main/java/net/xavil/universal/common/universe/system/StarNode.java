package net.xavil.universal.common.universe.system;

import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.universal.client.screen.Color;
import net.xavil.universal.common.universe.Units;

public non-sealed class StarNode extends StarSystemNode {

	public static enum Type {
		MAIN_SEQUENCE(true, 1, 1, 2, 2),
		GIANT(true, 1, 1, 2, 2),
		WHITE_DWARF(false, 1.1, 0.525, 5.5, 0.98),
		NEUTRON_STAR(false, 10, 1.4, 25, 1.9),
		BLACK_HOLE(false, 30, 5, 100, 21);

		public final boolean hasSpectralClass;

		private final double curveSlope;
		private final double curveYIntercept;

		private Type(boolean hasSpectralClass,
				double initialMass0, double finalMass0,
				double initialMass1, double finalMass1) {
			initialMass0 = Units.msol(initialMass0);
			finalMass0 = Units.msol(finalMass0);
			initialMass1 = Units.msol(initialMass1);
			finalMass1 = Units.msol(finalMass1);
			this.hasSpectralClass = hasSpectralClass;
			this.curveSlope = (finalMass1 - finalMass0) / (initialMass1 - initialMass0);
			this.curveYIntercept = finalMass0 - this.curveSlope * initialMass0;
		}

		public double curveMass(Random random, double initialMass) {
			var variance = Units.msol(Mth.clamp(0.05 * random.nextGaussian(), -0.1, 0.1));
			return this.curveSlope * initialMass + this.curveYIntercept + variance;
		}

		public double curveLuminosity(Random random, double initialLuminosity) {
			if (this == Type.BLACK_HOLE)
				return 0;
			return initialLuminosity;
		}

		public double curveRadius(Random random, double initialRadius) {
			if (this == Type.WHITE_DWARF)
				return initialRadius * 1e-5 + random.nextDouble(-2.5e-6, 2.5e-6);
			if (this == Type.NEUTRON_STAR)
				return initialRadius * 4e-6 + random.nextDouble(-4e-7, 4e-7);
			if (this == Type.BLACK_HOLE)
				return initialRadius * 3.5e-6 + random.nextDouble(-2.5e-7, 2.5e-7);
			if (this == Type.GIANT)
				return initialRadius * 100 + 0.1 * random.nextGaussian();

			return initialRadius;
		}
	}

	public static enum StarClass {
		O("O", Color.rgb(0.1, 0.1, 1)),
		B("B", Color.rgb(0.6, 0.6, 1)),
		A("A", Color.rgb(0.8, 0.8, 1)),
		F("F", Color.rgb(1, 1, 1)),
		G("G", Color.rgb(1, 1, 0.7)),
		K("K", Color.rgb(1, 0.7, 0.3)),
		M("M", Color.rgb(1, 0.3, 0.05));

		public final String name;
		// FIXME: color is based on star temperature, not star class!
		public final Color color;

		private StarClass(String name, Color color) {
			this.name = name;
			this.color = color;
		}
	}

	public StarNode.Type type;
	public double luminosityLsol;
	public double radiusRsol;
	public double temperatureK;

	public StarNode(StarNode.Type type, double massYg, double luminosityLsol, double radiusRsol, double temperatureK) {
		super(massYg);
		this.type = type;
		this.luminosityLsol = luminosityLsol;
		this.radiusRsol = radiusRsol;
		this.temperatureK = temperatureK;
	}

	public final @Nullable StarNode.StarClass starClass() {
		// @formatter:off
		if (!this.type.hasSpectralClass)    return null;
		if (this.massYg < Units.msol(0.45)) return StarClass.M;
		if (this.massYg < Units.msol(0.8))  return StarClass.K;
		if (this.massYg < Units.msol(1.04)) return StarClass.G;
		if (this.massYg < Units.msol(1.4))  return StarClass.F;
		if (this.massYg < Units.msol(2.1))  return StarClass.A;
		if (this.massYg < Units.msol(16))   return StarClass.B;
		                                    return StarClass.O;
		// @formatter:on
	}

	public Color getColor() {
		var starClass = starClass();

		// TODO: color based on temperature and not on star class!
		if (starClass != null) {
			return starClass.color;
		} else if (this.type == StarNode.Type.WHITE_DWARF) {
			return Color.rgb(1, 1, 1);
		} else if (this.type == StarNode.Type.NEUTRON_STAR) {
			return Color.rgb(0.4, 0.4, 1);
		}
		return Color.rgb(0, 1, 0);
	}

	@Override
	public String toString() {
		var builder = new StringBuilder("StellarBodyNode " + this.id);
		builder.append(" [");
		builder.append("massYg=" + this.massYg + ", ");
		builder.append("type=" + this.type + ", ");
		builder.append("luminosityLsol=" + this.luminosityLsol + ", ");
		builder.append("radiusRsol=" + this.radiusRsol + ", ");
		builder.append("]");
		return builder.toString();
	}

}