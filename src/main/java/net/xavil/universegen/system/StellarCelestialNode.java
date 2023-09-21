package net.xavil.universegen.system;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Constants;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec3;

public non-sealed class StellarCelestialNode extends UnaryCelestialNode {

	public static enum Type {
		MAIN_SEQUENCE(true),
		GIANT(true),
		WHITE_DWARF(true),
		NEUTRON_STAR(false),
		BLACK_HOLE(false);

		public final boolean hasSpectralClass;

		private Type(boolean hasSpectralClass) {
			this.hasSpectralClass = hasSpectralClass;
		}
	}

	public StellarCelestialNode.Type type;
	public double luminosityLsol;

	public StellarCelestialNode() {
	}

	public StellarCelestialNode(StellarCelestialNode.Type type, double massYg,
			double luminosityLsol, double radiusRsol, double temperatureK) {
		super(massYg);
		this.type = type;
		this.luminosityLsol = luminosityLsol;
		this.radius = Units.km_PER_Rsol * radiusRsol;
		this.temperature = temperatureK;
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
		return Math.pow(l / (4 * Math.PI * r * r * Constants.BOLTZMANN_CONSTANT_W_PER_m2_K4), 0.25);
	}

	public static final double NEUTRON_STAR_MIN_INITIAL_MASS_YG = 10 * Units.Yg_PER_Msol;
	public static final double BLACK_HOLE_MIN_INITIAL_MASS_YG = 25 * Units.Yg_PER_Msol;
	private static final double SCHWARZSCHILD_FACTOR_Rsol_PER_Yg = Constants.SCHWARZSCHILD_FACTOR_m_PER_kg
			* Units.ku_PER_Yu / Units.m_PER_Rsol;

	public static final class Properties {
		public double massYg;
		public double ageMyr;

		public Type type;
		public double mainSequenceLifetimeMyr;
		public double luminosityLsol;
		public double radiusRsol;
		public double temperatureK;

		// TODO: redo this to be less bad lol
		public void load(double massYg, double ageMyr) {
			this.massYg = massYg;
			this.ageMyr = ageMyr;

			// not a star lol
			if (massYg < 0.08 * Units.Yg_PER_Msol) {
				this.type = null;
				return;
			}

			this.mainSequenceLifetimeMyr = StellarCelestialNode.mainSequenceLifetimeFromMass(massYg);

			this.luminosityLsol = mainSequenceLuminosityFromMass(massYg);
			this.radiusRsol = mainSequenceRadiusFromMass(massYg);
			this.temperatureK = temperature(this.radiusRsol, this.luminosityLsol);

			if (ageMyr <= this.mainSequenceLifetimeMyr) {
				doMainSequenceTrack();
			} else if (ageMyr <= this.mainSequenceLifetimeMyr * 2) {
				doGiantTrack();
			} else {
				if (massYg < NEUTRON_STAR_MIN_INITIAL_MASS_YG) {
					doWhiteDwarfTrack();
				} else if (massYg < BLACK_HOLE_MIN_INITIAL_MASS_YG) {
					doNeutronStarTrack();
				} else {
					doBlackHoleTrack();
				}
			}

		}

		private void doMainSequenceTrack() {
			this.type = Type.MAIN_SEQUENCE;
		}

		private void doGiantTrack() {
			this.type = Type.GIANT;
			// TODO: figure out actual quantities here. tbh this whole thing should be
			// rewritten.
			this.radiusRsol *= 100;
			this.temperatureK *= 0.3;
			this.luminosityLsol *= 3.0;
		}

		private void doWhiteDwarfTrack() {
			this.type = Type.WHITE_DWARF;
			// TODO: conservation of angular momentum (fast spinny :3)
			// white dwarf and neutron star cooling is apparently very complicated, and i
			// have birdbrain, so im just completely winging (hehe) it. As far as i
			// understand, at least for neutron stars, they have a period of very rapid
			// cooling followed by a period of much slower cooling.

			Assert.isTrue(this.ageMyr > this.mainSequenceLifetimeMyr);

			final double coolingTime = ageMyr - this.mainSequenceLifetimeMyr;
			final double fastCurve = Math.exp(-0.007 * coolingTime);
			final double slowCurve = Math.exp(-0.0002 * coolingTime);
			final double coolingCurve = (fastCurve + 0.5 * slowCurve) / 1.5;

			this.temperatureK *= 10;
			this.temperatureK *= coolingCurve;
			this.radiusRsol *= 1e-2;

			// treat the white dwarf as an ideal black body and apply the Stefan–Boltzmann
			// law
			final var radiantExitance = Constants.BOLTZMANN_CONSTANT_W_PER_m2_K4 * Math.pow(this.temperatureK, 4.0);
			final var radiusM = this.radiusRsol * Units.m_PER_Rsol;
			final var surfaceArea = 4.0 * Math.PI * radiusM * radiusM;
			this.luminosityLsol = radiantExitance * surfaceArea / Units.W_PER_Lsol;
		}

		private void doNeutronStarTrack() {
			this.type = Type.NEUTRON_STAR;
			Assert.isTrue(ageMyr > this.mainSequenceLifetimeMyr);

			final double coolingTime = ageMyr - this.mainSequenceLifetimeMyr;
			final double fastCurve = Math.exp(-0.007 * coolingTime);
			final double slowCurve = Math.exp(-0.0002 * coolingTime);
			final double coolingCurve = (fastCurve + 0.5 * slowCurve) / 1.5;

			this.temperatureK *= 20;
			this.temperatureK *= coolingCurve;
			this.radiusRsol *= 1e-5;

			final var radiantExitance = Constants.BOLTZMANN_CONSTANT_W_PER_m2_K4 * Math.pow(this.temperatureK, 4.0);
			final var radiusM = this.radiusRsol * Units.m_PER_Rsol;
			final var surfaceArea = 4.0 * Math.PI * radiusM * radiusM;
			this.luminosityLsol = radiantExitance * surfaceArea / Units.W_PER_Lsol;
		}

		private void doBlackHoleTrack() {
			this.type = Type.BLACK_HOLE;
			this.luminosityLsol = 0;
			this.radiusRsol = this.massYg * SCHWARZSCHILD_FACTOR_Rsol_PER_Yg;
			this.temperatureK = 0;
		}
	}

	public static StellarCelestialNode fromProperties(Properties properties) {
		final var node = StellarCelestialNode.fromMass(properties.type, properties.massYg);
		node.luminosityLsol = properties.luminosityLsol;
		node.radius = Units.km_PER_Rsol * properties.radiusRsol;
		node.temperature = properties.temperatureK;
		return node;
	}

	public static StellarCelestialNode fromMassAndAge(double massYg, double ageMyr) {
		final var properties = new Properties();
		properties.load(massYg, ageMyr);
		return fromProperties(properties);
	}

	public static StellarCelestialNode fromMass(StellarCelestialNode.Type type, double massYg) {
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

		// this is dum, write an actual star classifier and use the HR diagram instead
		for (final var interval : CLASSIFICATION_TABLE) {
			if (this.temperature >= interval.minBound && this.temperature < interval.maxBound) {
				var num = Mth.inverseLerp(this.temperature, interval.minBound, interval.maxBound);
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

	private static final double[] RED_POLYNOMIAL_COEFFICENTS = { 4.93596077e0, -1.29917429e0, 1.64810386e-01,
			-1.16449912e-02, 4.86540872e-04, -1.19453511e-05, 1.59255189e-07, -8.89357601e-10 };
	private static final double[] GREEN1_POLYNOMIAL_COEFFICENTS = { -4.95931720e-01, 1.08442658e0, -9.17444217e-01,
			4.94501179e-01, -1.48487675e-01, 2.49910386e-02, -2.21528530e-03, 8.06118266e-05 };
	private static final double[] GREEN2_POLYNOMIAL_COEFFICENTS = { 3.06119745e0, -6.76337896e-01, 8.28276286e-02,
			-5.72828699e-03, 2.35931130e-04, -5.73391101e-06, 7.58711054e-08, -4.21266737e-10 };
	private static final double[] BLUE_POLYNOMIAL_COEFFICENTS = { 4.93997706e-01, -8.59349314e-01, 5.45514949e-01,
			-1.81694167e-01, 4.16704799e-02, -6.01602324e-03, 4.80731598e-04, -1.61366693e-05 };

	// this value should be as small as possible while still giving good results.
	// smaller values mean more cache hits when doing lots of random table lookups.
	// i did not test 256 for fitness, i just picked it at random.
	private static final int BLACK_BODY_COLOR_TABLE_SIZE = 256;
	private static final float[] BLACK_BODY_COLOR_TABLE;

	private static double poly(double[] coefficients, double x) {
		double result = coefficients[0];
		double xn = x;
		for (int i = 1; i < coefficients.length; i++) {
			result += xn * coefficients[i];
			xn *= x;
		}
		return result;
	}

	static {
		// adapted from https://gist.github.com/stasikos/06b02d18f570fc1eaa9f
		BLACK_BODY_COLOR_TABLE = new float[3 * BLACK_BODY_COLOR_TABLE_SIZE];
		for (int i = 0; i < BLACK_BODY_COLOR_TABLE_SIZE; ++i) {
			double x = 40.0 * (i / (float) BLACK_BODY_COLOR_TABLE_SIZE);
			double temperatureK = 1000.0 * x;

			double red, green, blue;

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

			int idx = 3 * i;
			BLACK_BODY_COLOR_TABLE[idx++] = (float) red;
			BLACK_BODY_COLOR_TABLE[idx++] = (float) green;
			BLACK_BODY_COLOR_TABLE[idx++] = (float) blue;
		}
	}

	public static void blackBodyColorFromTable(Vec3.Mutable out, double temperatureK) {
		// clamp to 39999 so `fi` is never exactly 1, which would cause an out of bounds
		// access to BLACK_BODY_COLOR_TABLE
		final var fi = BLACK_BODY_COLOR_TABLE_SIZE * Math.min(temperatureK, 40000) / 40000;
		final int i = Mth.floor(fi);
		final var frac = fi - i;
		if (i >= BLACK_BODY_COLOR_TABLE_SIZE - 1) {
			out.x = BLACK_BODY_COLOR_TABLE[BLACK_BODY_COLOR_TABLE.length - 3];
			out.y = BLACK_BODY_COLOR_TABLE[BLACK_BODY_COLOR_TABLE.length - 2];
			out.z = BLACK_BODY_COLOR_TABLE[BLACK_BODY_COLOR_TABLE.length - 1];
			return;
		}
		out.x = Mth.lerp(frac, BLACK_BODY_COLOR_TABLE[3 * i + 0], BLACK_BODY_COLOR_TABLE[3 * i + 3]);
		out.y = Mth.lerp(frac, BLACK_BODY_COLOR_TABLE[3 * i + 1], BLACK_BODY_COLOR_TABLE[3 * i + 4]);
		out.z = Mth.lerp(frac, BLACK_BODY_COLOR_TABLE[3 * i + 2], BLACK_BODY_COLOR_TABLE[3 * i + 5]);
	}

	public Color getColor() {
		final var res = new Vec3.Mutable();
		blackBodyColorFromTable(res, this.temperature);
		return Color.fromDoubles(res.x, res.y, res.z, 1);
	}

}