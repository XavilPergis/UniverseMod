package net.xavil.universegen.system;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Rng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.math.Color;

public non-sealed class StellarCelestialNode extends CelestialNode {

	public static enum Type {
		MAIN_SEQUENCE(true, 1, 1, 2, 2),
		GIANT(true, 1, 1, 2, 2),
		WHITE_DWARF(true, 1.1, 0.525, 5.5, 0.98),
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
				// white dwarf and neutron star cooling is apparently very complicated, and i
				// have birdbrain, so im just completely winging (hehe) it. As far as i
				// understand, at least for neutron stars, they have a period of very rapid
				// cooling followed by a period of much slower cooling.

				Assert.isTrue(ageMyr > mainSequenceLifetimeMyr);

				final double coolingTime = ageMyr - mainSequenceLifetimeMyr;
				final double fastCurve = Math.exp(-0.007 * coolingTime);
				final double slowCurve = Math.exp(-0.0002 * coolingTime);
				final double coolingCurve = (fastCurve + 0.5 * slowCurve) / 1.5;

				node.temperatureK *= 10;
				node.temperatureK *= coolingCurve;

				node.radiusRsol *= 1e-2 + rng.uniformDouble(-2.5e-6, 2.5e-6);

				// treat the white dwarf as an ideal black body and apply the Stefan–Boltzmann
				// law
				final var radiantExitance = Units.BOLTZMANN_CONSTANT_W_PER_m2_K4 * Math.pow(node.temperatureK, 4.0);
				final var radiusM = node.radiusRsol * Units.m_PER_Rsol;
				final var surfaceArea = 4.0 * Math.PI * radiusM * radiusM;
				node.luminosityLsol = radiantExitance * surfaceArea / Units.W_PER_Lsol;
			} else if (this == NEUTRON_STAR) {
				Assert.isTrue(ageMyr > mainSequenceLifetimeMyr);

				final double coolingTime = ageMyr - mainSequenceLifetimeMyr;
				final double fastCurve = Math.exp(-0.007 * coolingTime);
				final double slowCurve = Math.exp(-0.0002 * coolingTime);
				final double coolingCurve = (fastCurve + 0.5 * slowCurve) / 1.5;

				node.temperatureK *= 20;
				node.temperatureK *= coolingCurve;

				node.radiusRsol *= 1e-5 + rng.uniformDouble(-2.5e-8, 2.5e-8);

				final var radiantExitance = Units.BOLTZMANN_CONSTANT_W_PER_m2_K4 * Math.pow(node.temperatureK, 4.0);
				final var radiusM = node.radiusRsol * Units.m_PER_Rsol;
				final var surfaceArea = 4.0 * Math.PI * radiusM * radiusM;
				node.luminosityLsol = radiantExitance * surfaceArea / Units.W_PER_Lsol;
				// node.luminosityLsol = node.temperatureK / 2000;
			} else if (this == GIANT) {
				// idk
				node.radiusRsol *= rng.uniformDouble(100, 200);
				node.temperatureK *= Mth.lerp(Math.pow(rng.uniformDouble(), 3.0), 0.2, 0.6);
				node.luminosityLsol *= 10;
			}

			node.cachedColor = blackBodyColor(node.temperatureK);
		}

		private double curveMass(Rng rng, StellarCelestialNode node) {
			return this.curveSlope * node.massYg + this.curveYIntercept;
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
	public Color cachedColor;
	public StarClass cachedStarClass;

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

	private static record ClassificationInterval(
			double minBound,
			double maxBound,
			String name) {
	}

	private static final ClassificationInterval[] CLASSIFICATION_TABLE = {
		// @formatter:off
		new ClassificationInterval(0,     2400,   "L"),
		new ClassificationInterval(2400,  3700,   "M"),
		new ClassificationInterval(3700,  5200,   "K"),
		new ClassificationInterval(5200,  6000,   "G"),
		new ClassificationInterval(6000,  6500,   "F"),
		new ClassificationInterval(6500,  10000,  "A"),
		new ClassificationInterval(10000, 30000,  "B"),
		new ClassificationInterval(30000, 100000, "O"),
		// @formatter:on
	};

	public final @Nullable String getSpectralClassification() {
		if (!this.type.hasSpectralClass)
			return "Non-Spectral";

		for (final var interval : CLASSIFICATION_TABLE) {
			if (this.temperatureK >= interval.minBound && this.temperatureK < interval.maxBound) {
				var num = Mth.inverseLerp(this.temperatureK, interval.minBound, interval.maxBound);
				num = 10 * (1 - num);
				var res = String.format("%s%.1f", interval.name, num);
				switch (this.type) {
					case GIANT -> {
						return res + "III";
					}
					case MAIN_SEQUENCE -> {
						return res + "V";
					}
					case WHITE_DWARF -> {
						return "D" + res;
					}
					default -> {
						Assert.isUnreachable();
					}
				}
			}
		}

		return "Class O0";
	}

	public final double apparentBrightness(double distanceTm) {
		var powerW = Units.W_PER_Lsol * luminosityLsol;
		return powerW / (4 * Math.PI * distanceTm * distanceTm);
		// P / 4 * pi * r^2
	}

	public Color getColor() {
		if (this.cachedColor == null)
			this.cachedColor = blackBodyColor(this.temperatureK);
		return this.cachedColor;
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

	private static final double[] RED_POLYNOMIAL_COEFFICENTS = { 4.93596077e0, -1.29917429e0, 1.64810386e-01,
			-1.16449912e-02, 4.86540872e-04, -1.19453511e-05, 1.59255189e-07, -8.89357601e-10 };
	private static final double[] GREEN1_POLYNOMIAL_COEFFICENTS = { -4.95931720e-01, 1.08442658e0, -9.17444217e-01,
			4.94501179e-01, -1.48487675e-01, 2.49910386e-02, -2.21528530e-03, 8.06118266e-05 };
	private static final double[] GREEN2_POLYNOMIAL_COEFFICENTS = { 3.06119745e0, -6.76337896e-01, 8.28276286e-02,
			-5.72828699e-03, 2.35931130e-04, -5.73391101e-06, 7.58711054e-08, -4.21266737e-10 };
	private static final double[] BLUE_POLYNOMIAL_COEFFICENTS = { 4.93997706e-01, -8.59349314e-01, 5.45514949e-01,
			-1.81694167e-01, 4.16704799e-02, -6.01602324e-03, 4.80731598e-04, -1.61366693e-05 };

	// adapted from https://gist.github.com/stasikos/06b02d18f570fc1eaa9f
	public static Color blackBodyColor(double temperatureK) {
		// Used this: https://gist.github.com/paulkaplan/5184275 at the beginning
		// based on
		// http://stackoverflow.com/questions/7229895/display-temperature-as-a-color-with-c
		// this answer: http://stackoverflow.com/a/24856307
		// (so, just interpretation of pseudocode in Java)

		double x = temperatureK / 1000.0;
		if (x > 40)
			x = 40;
		double red;
		double green;
		double blue;

		// R
		if (temperatureK < 6527) {
			red = 1;
		} else {
			red = poly(RED_POLYNOMIAL_COEFFICENTS, x);

		}
		// G
		if (temperatureK < 850) {
			green = 0;
		} else if (temperatureK <= 6600) {
			green = poly(GREEN1_POLYNOMIAL_COEFFICENTS, x);
		} else {
			green = poly(GREEN2_POLYNOMIAL_COEFFICENTS, x);
		}
		// B
		if (temperatureK < 1900) {
			blue = 0;
		} else if (temperatureK < 6600) {
			blue = poly(BLUE_POLYNOMIAL_COEFFICENTS, x);
		} else {
			blue = 1;
		}

		red = Mth.clamp(red, 0, 1);
		blue = Mth.clamp(blue, 0, 1);
		green = Mth.clamp(green, 0, 1);
		return new Color(red, green, blue, 1).mul(0.5).withA(1);
	}

	private static double poly(double[] coefficients, double x) {
		double result = coefficients[0];
		double xn = x;
		for (int i = 1; i < coefficients.length; i++) {
			result += xn * coefficients[i];
			xn *= x;
		}
		return result;
	}

}