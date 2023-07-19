package net.xavil.universegen.system;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.math.Color;

public non-sealed class StellarCelestialNode extends CelestialNode {

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
			initialMass0 = Units.fromMsol(initialMass0);
			finalMass0 = Units.fromMsol(finalMass0);
			initialMass1 = Units.fromMsol(initialMass1);
			finalMass1 = Units.fromMsol(finalMass1);
			this.hasSpectralClass = hasSpectralClass;
			this.curveSlope = (finalMass1 - finalMass0) / (initialMass1 - initialMass0);
			this.curveYIntercept = finalMass0 - this.curveSlope * initialMass0;
		}

		private static final double C2_m2_PER_s2 = Units.SPEED_OF_LIGHT_m_PER_s * Units.SPEED_OF_LIGHT_m_PER_s;
		private static final double SCHWARZSCHILD_FACTOR_m_PER_kg = 2.0 * Units.GRAVITATIONAL_CONSTANT_m3_PER_kg_s2
				/ C2_m2_PER_s2;
		private static final double SCHWARZSCHILD_FACTOR_Rsol_PER_Yg = SCHWARZSCHILD_FACTOR_m_PER_kg
				* (Units.YOTTA / Units.KILO) / Units.m_PER_Rsol;

		public void transformToFinalProperties(Rng rng, double ageMyr, double mainSequenceLifetimeMyr,
				StellarCelestialNode node) {
			node.massYg = curveMass(rng, node);

			if (this == BLACK_HOLE) {
				node.luminosityLsol = 0;
				node.radiusRsol = node.massYg * SCHWARZSCHILD_FACTOR_Rsol_PER_Yg;
				node.temperatureK = 0;
			} else if (this == WHITE_DWARF) {
				// TODO: conservation of angular momentum (fast spinny :3)
				// no active heating, so they radiate their heat away over time.
				// i have no idea how the fuck this actually works so im just gonna fudge
				// something

				final double coolingTime = ageMyr - mainSequenceLifetimeMyr;
				final double Te = 2e-7;
				final double coolingFactor = (1 - Te) * Math.exp(-0.004 * coolingTime) + Te;
				node.temperatureK *= 1e3;
				node.temperatureK *= coolingFactor;

				node.radiusRsol *= 1e-2 + rng.uniformDouble(-2.5e-6, 2.5e-6);
				final var temperatureTsol = node.temperatureK / Units.K_PER_Tsol;
				node.luminosityLsol = Math.pow(node.radiusRsol, 2) * Math.pow(temperatureTsol, 4);
			} else if (this == NEUTRON_STAR) {
				final double coolingTime = ageMyr - mainSequenceLifetimeMyr;
				final double Te = 2e-7;
				final double coolingFactor = (1 - Te) * Math.exp(-0.004 * coolingTime) + Te;
				node.temperatureK *= 1e4;
				node.temperatureK *= coolingFactor;

				node.radiusRsol *= 1e-5 + rng.uniformDouble(-2.5e-8, 2.5e-8);
				final var temperatureTsol = node.temperatureK / Units.K_PER_Tsol;
				node.luminosityLsol = Math.pow(node.radiusRsol, 2) * Math.pow(temperatureTsol, 4);
			} else if (this == GIANT) {
				// idk
				node.radiusRsol *= rng.uniformDouble(100, 200);
				node.temperatureK *= 0.1;
				node.luminosityLsol *= 10;
			}
		}

		private double curveMass(Rng rng, StellarCelestialNode node) {
			var variance = Mth.lerp(rng.uniformDouble(), 0.0, -0.1 * node.massYg);
			return this.curveSlope * node.massYg + this.curveYIntercept + variance;
		}
	}

	public static enum StarClass {
		O("O", Color.rgb(0, 0, 1)),
		B("B", Color.rgb(0.1, 0.2, 1)),
		A("A", Color.rgb(0.2, 0.4, 1)),
		F("F", Color.rgb(0.8, 0.8, 0.9)),
		G("G", Color.rgb(1, 0.8, 0.5)),
		K("K", Color.rgb(1, 0.5, 0.1)),
		M("M", Color.rgb(1, 0.2, 0.1));

		public final String name;
		// FIXME: color is based on star temperature, not star class!
		public final Color color;

		private StarClass(String name, Color color) {
			this.name = name;
			this.color = color;
		}
	}

	public StellarCelestialNode.Type type;
	public double luminosityLsol;
	public double radiusRsol;
	public double temperatureK;

	public StellarCelestialNode(StellarCelestialNode.Type type, double massYg,
			double luminosityLsol, double radiusRsol, double temperatureK) {
		super(massYg);
		this.type = type;
		this.luminosityLsol = luminosityLsol;
		this.radiusRsol = radiusRsol;
		this.temperatureK = temperatureK;
	}

	public static double mainSequenceLifetimeFromMass(double massYg) {
		final var massMsol = massYg / Units.Yg_PER_Msol;
		return Units.SOL_LIFETIME_MYA * (massMsol / mainSequenceLuminosityFromMass(massYg));
	}

	public static double mainSequenceLuminosityFromMass(double massYg) {
		final var massMsol = massYg / Units.Yg_PER_Msol;
		double luminosityLsol = 0;
		if (massMsol < 0.43) {
			luminosityLsol = 0.23 * Math.pow(massMsol, 2.3);
		} else if (massMsol < 2) {
			luminosityLsol = Math.pow(massMsol, 4);
		} else if (massMsol < 55) {
			luminosityLsol = 1.4 * Math.pow(massMsol, 3.5);
		} else {
			luminosityLsol = 32000 * massMsol;
		}
		return luminosityLsol;
	}

	public static double mainSequenceRadiusFromMass(double massYg) {
		final var massMsol = massYg / Units.Yg_PER_Msol;
		return Math.pow(massMsol, 0.8);
	}

	public static double temperature(double radiusRsol, double luminosityLsol) {
		var r = Units.m_PER_Rsol * radiusRsol;
		var l = Units.W_PER_Lsol * luminosityLsol;
		return Math.pow(l / (4 * Math.PI * r * r * Units.BOLTZMANN_CONSTANT_W_PER_m2_K4), 0.25);
	}

	// private static @Nullable StellarCelestialNode generateStarNode(Random random,
	// double systemAgeMya, double massYg) {
	// final var starLifetime =
	// StellarCelestialNode.mainSequenceLifetimeFromMass(massYg);

	// // angular_momentum/angular_velocity = mass * radius^2

	// // TODO: conservation of angular momentum when star changes mass or radius
	// var targetType = StellarCelestialNode.Type.MAIN_SEQUENCE;
	// if (systemAgeMya > starLifetime) {
	// if (massYg < NEUTRON_STAR_MIN_INITIAL_MASS_YG) {
	// targetType = StellarCelestialNode.Type.WHITE_DWARF;
	// } else if (massYg < BLACK_HOLE_MIN_INITIAL_MASS_YG) {
	// targetType = StellarCelestialNode.Type.NEUTRON_STAR;
	// } else {
	// targetType = StellarCelestialNode.Type.BLACK_HOLE;
	// }
	// } else if (systemAgeMya > starLifetime * 0.8) {
	// targetType = StellarCelestialNode.Type.GIANT;
	// }

	// var node = StellarCelestialNode.fromMass(random, targetType, massYg);
	// return node;
	// }

	public static final double NEUTRON_STAR_MIN_INITIAL_MASS_YG = Units.fromMsol(10);
	public static final double BLACK_HOLE_MIN_INITIAL_MASS_YG = Units.fromMsol(25);

	public static StellarCelestialNode fromMassAndAge(Rng rng, double massYg, double ageMyr) {
		final var mainSequenceLifetimeMyr = StellarCelestialNode.mainSequenceLifetimeFromMass(massYg);

		var targetType = StellarCelestialNode.Type.MAIN_SEQUENCE;
		if (ageMyr <= mainSequenceLifetimeMyr) {
			targetType = StellarCelestialNode.Type.MAIN_SEQUENCE;
		} else if (ageMyr <= mainSequenceLifetimeMyr * 2) {
			targetType = StellarCelestialNode.Type.GIANT;
		} else {
			if (massYg < NEUTRON_STAR_MIN_INITIAL_MASS_YG) {
				targetType = StellarCelestialNode.Type.WHITE_DWARF;
			} else if (massYg < BLACK_HOLE_MIN_INITIAL_MASS_YG) {
				targetType = StellarCelestialNode.Type.NEUTRON_STAR;
			} else {
				targetType = StellarCelestialNode.Type.BLACK_HOLE;
			}
		}

		final var node = StellarCelestialNode.fromMass(rng, targetType, massYg);
		targetType.transformToFinalProperties(rng, ageMyr, mainSequenceLifetimeMyr, node);
		return node;
	}

	public static StellarCelestialNode fromMass(Rng rng, StellarCelestialNode.Type type, double massYg) {
		var m = massYg;
		var l = mainSequenceLuminosityFromMass(massYg);
		var r = mainSequenceRadiusFromMass(massYg);
		return new StellarCelestialNode(type, m, l, r, temperature(r, l));
	}

	public final @Nullable StellarCelestialNode.StarClass starClass() {
		// @formatter:off
		if (!this.type.hasSpectralClass)        return null;
		if (this.massYg < Units.fromMsol(0.45)) return StarClass.M;
		if (this.massYg < Units.fromMsol(0.8))  return StarClass.K;
		if (this.massYg < Units.fromMsol(1.04)) return StarClass.G;
		if (this.massYg < Units.fromMsol(1.4))  return StarClass.F;
		if (this.massYg < Units.fromMsol(2.1))  return StarClass.A;
		if (this.massYg < Units.fromMsol(16))   return StarClass.B;
		                                        return StarClass.O;
		// @formatter:on
	}

	public final double apparentBrightness(double distanceTm) {
		var powerW = Units.W_PER_Lsol * luminosityLsol;
		return powerW / (4 * Math.PI * distanceTm * distanceTm);
		// P / 4 * pi * r^2
	}

	public Color getColor() {
		var starClass = starClass();

		// TODO: color based on temperature and not on star class!
		if (starClass != null) {
			// @formatter:off
			if (this.massYg < Units.fromMsol(0.45)) return Color.rgb(1, 0.14, 0.05);
			if (this.massYg < Units.fromMsol(0.8))  return Color.rgb(1, 0.2, 0.05);
			if (this.massYg < Units.fromMsol(1.04)) return Color.rgb(0.6, 0.36, 0.12);
			if (this.massYg < Units.fromMsol(1.4))  return Color.rgb(0.4, 0.4, 0.5);
			if (this.massYg < Units.fromMsol(2.1))  return Color.rgb(0.3, 0.4, 0.7);
			if (this.massYg < Units.fromMsol(16))   return Color.rgb(0.2, 0.3, 0.9);
													return Color.rgb(0.1, 0.2, 1);
			// @formatter:on
			// return starClass.color;
		} else if (this.type == StellarCelestialNode.Type.WHITE_DWARF) {
			return Color.rgb(1, 1, 1);
		} else if (this.type == StellarCelestialNode.Type.NEUTRON_STAR) {
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