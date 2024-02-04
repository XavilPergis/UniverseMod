package net.xavil.universegen.system;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.Nullable;

import com.google.common.base.Charsets;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Constants;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.system.BasicStarSystemGenerator;
import net.xavil.ultraviolet.common.universe.system.RealisticStarSystemGenerator;
import net.xavil.ultraviolet.common.universe.system.StarSystemGenerator;

public non-sealed class StellarCelestialNode extends UnaryCelestialNode {

	public static enum Type {
		STAR,
		WHITE_DWARF,
		NEUTRON_STAR,
		BLACK_HOLE,
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

	public static double radiusFromLuminosityAndTemperature(double luminosityLsol, double temperatureK) {
		final var L = Units.W_PER_Lsol * luminosityLsol;
		final var T4 = Math.pow(temperatureK, 4);
		final var r = Math.sqrt(L / (4 * Math.PI * Constants.BOLTZMANN_CONSTANT_W_PER_m2_K4 * T4));
		return Units.ku_PER_u * r;
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
		var t = Math.pow(l / (4 * Math.PI * r * r * Constants.BOLTZMANN_CONSTANT_W_PER_m2_K4), 0.25);
		t = Math.min(t, 50000);
		return t;
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
		public void load(SplittableRng rng, double massYg, double ageMyr) {
			this.massYg = massYg;
			this.ageMyr = ageMyr;

			// not a star lol
			if (massYg < 0.08 * Units.Yg_PER_Msol) {
				this.type = null;
				return;
			}

			// final var massMsol = massYg / Units.Yg_PER_Msol;
			// return Units.SOL_LIFETIME_MYA * (massMsol /
			// mainSequenceLuminosityFromMass(massYg));

			this.luminosityLsol = mainSequenceLuminosityFromMass(massYg);
			double lumT = rng.weightedDouble("luminosity_offset", 2);
			if (rng.chance("luminosity_negate", 0.5))
				lumT *= -1;
			this.luminosityLsol *= 1.0 + 0.1 * lumT;

			this.mainSequenceLifetimeMyr = StellarCelestialNode.mainSequenceLifetimeFromMass(massYg);
			double lifetimeT = rng.weightedDouble("lifetime_offset", 2);
			if (rng.chance("lifetime_negate", 0.5))
				lifetimeT *= -1;
			this.mainSequenceLifetimeMyr *= 1.0 + 0.1 * lifetimeT;

			this.radiusRsol = mainSequenceRadiusFromMass(massYg);
			double radiusT = rng.weightedDouble("radius_offset", 2);
			if (rng.chance("radius_negate", 0.5))
				radiusT *= -1;
			this.radiusRsol *= 1.0 + 0.1 * radiusT;

			this.temperatureK = temperature(this.radiusRsol, this.luminosityLsol);
			double tempT = rng.weightedDouble("temperature_offset", 2);
			if (rng.chance("temperature_negate", 0.5))
				tempT *= -1;
			this.temperatureK *= 1.0 + 0.1 * tempT;

			this.temperatureK = Mth.map(this.temperatureK, 2000, 10000, 2600, 7000);

			rng.push("tracks");
			if (ageMyr <= this.mainSequenceLifetimeMyr) {
				doMainSequenceTrack(rng);
			} else if (ageMyr <= this.mainSequenceLifetimeMyr * 1.4) {
				doGiantTrack(rng);
			} else {
				if (massYg < NEUTRON_STAR_MIN_INITIAL_MASS_YG) {
					doWhiteDwarfTrack(rng);
				} else if (massYg < BLACK_HOLE_MIN_INITIAL_MASS_YG) {
					doNeutronStarTrack(rng);
				} else {
					doBlackHoleTrack(rng);
				}
			}
			rng.pop();

		}

		private void doMainSequenceTrack(SplittableRng rng) {
			this.type = Type.STAR;
		}

		private void doGiantTrack(SplittableRng rng) {
			this.type = Type.STAR;

			final var t = Mth.inverseLerp(this.ageMyr, this.mainSequenceLifetimeMyr,
					this.mainSequenceLifetimeMyr * 1.4);

			// TODO: figure out actual quantities here. tbh this whole thing should be
			// rewritten.
			this.radiusRsol *= Mth.lerp(t, 3.0,
					rng.weightedDouble("radius1", 5, 30, 100) * rng.uniformDouble("radius2", 0.3, 1.0));
			this.temperatureK *= Mth.lerp(t, 3.0, 0.4 + 0.1 * rng.weightedDouble("temperature", 5));
			this.luminosityLsol *= Mth.lerp(t, 3.0, rng.weightedDouble("temperature", 3, 1.5, 3));
		}

		private void doWhiteDwarfTrack(SplittableRng rng) {
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

			this.temperatureK *= rng.uniformDouble("temperature", 5, 10);
			this.temperatureK *= coolingCurve;
			this.temperatureK *= 0.2;
			this.radiusRsol *= 4e-3;

			// treat the white dwarf as an ideal black body and apply the Stefan–Boltzmann
			// law
			final var radiantExitance = Constants.BOLTZMANN_CONSTANT_W_PER_m2_K4 * Math.pow(this.temperatureK, 4.0);
			final var radiusM = this.radiusRsol * Units.m_PER_Rsol;
			final var surfaceArea = 4.0 * Math.PI * radiusM * radiusM;
			this.luminosityLsol = radiantExitance * surfaceArea / Units.W_PER_Lsol;
		}

		private void doNeutronStarTrack(SplittableRng rng) {
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

		private void doBlackHoleTrack(SplittableRng rng) {
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

	public static StellarCelestialNode fromMassAndAge(SplittableRng rng, double massYg, double ageMyr) {
		final var properties = new Properties();
		properties.load(rng, massYg, ageMyr);
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

	// TODO: do something slightly better than this garbage
	public final @Nullable String getSpectralClassification() {
		if (this.type == null)
			return "[Unknown]";

		// final var logLum = Math.log10(this.luminosityLsol);
		// final var bvIndex = Math.pow(5601 / this.temperature, 1.5) - 0.4;

		// this is dum, write an actual star classifier and use the HR diagram instead
		for (final var interval : CLASSIFICATION_TABLE) {
			if (this.temperature >= interval.minBound && this.temperature < interval.maxBound) {
				var num = Mth.inverseLerp(this.temperature, interval.minBound, interval.maxBound);
				num = 10 * (1 - num);
				var res = String.format("%s%.1f", interval.name, num);
				return switch (this.type) {
					case STAR -> res + " V";
					case WHITE_DWARF -> "WD* " + res;
					case NEUTRON_STAR -> "N* " + res;
					case BLACK_HOLE -> "Bh";
				};
			}
		}

		return "O0 Ia";
	}

	public final double apparentBrightness(double distanceTm) {
		var powerW = Units.W_PER_Lsol * luminosityLsol;
		return powerW / (4 * Math.PI * distanceTm * distanceTm);
		// P / 4 * pi * r^2
	}

	public final CelestialNode generateSystem(long systemSeed, Galaxy galaxy, GalaxySector sector,
			GalaxySectorId sectorId, GalaxySector.ElementHolder elem) {

		final var rng = new SplittableRng(systemSeed);

		final var ctx = new StarSystemGenerator.Context(
				rng.uniformLong("generation_seed"),
				galaxy, sector, sectorId, elem);
		// final var systemGenerator = new RealisticStarSystemGenerator();
		final var systemGenerator = new BasicStarSystemGenerator(this);
		final var rootNode = systemGenerator.generate(ctx);

		rootNode.build();
		rootNode.assignSeeds(rng.uniformLong("node_seed"));

		return rootNode;
	}

	public static final BlackbodyTable BLACK_BODY_COLOR_TABLE;

	public static final class BlackbodyTable {
		public final int rowCount;
		private final float[] colorRgb;
		private final float[] visualBrightnessMultiplier;
		public final float temperatureMin, temperatureMax;

		private static final class Entry {
			public float temperature;
			public float bolometricRatio;
			public float efficencyY;
			public float x, y, Y;
			public float r, g, b;

			private void storeColumn(String column, float value) {
				switch (column) {
					case "temperature" -> this.temperature = value;
					case "bolometric_ratio" -> this.bolometricRatio = value;
					case "eff_Y" -> this.efficencyY = value;
					case "x" -> this.x = value;
					case "y" -> this.y = value;
					case "Y" -> this.Y = value;
					case "r" -> this.r = value;
					case "g" -> this.g = value;
					case "b" -> this.b = value;
					default -> {
					}
				}
			}
		}

		private static String readString(ByteBuffer buf) {
			final var byteCount = buf.getInt();
			final var startPos = buf.position();
			buf.position(startPos + byteCount);
			return new String(buf.array(), startPos, byteCount, Charsets.UTF_8);
		}

		// assumes that each row is a constant delta temperature from each other, and
		// that the first row's temperature is temperatureMin, and the last row's
		// temperature is temperatureMax
		public BlackbodyTable(ByteBuffer buf) {
			final var columnCount = buf.getInt();
			final var columns = new Vector<String>(columnCount);
			for (int i = 0; i < columnCount; ++i)
				columns.push(readString(buf));

			this.rowCount = buf.getInt();
			this.temperatureMin = buf.getFloat();
			this.temperatureMax = buf.getFloat();
			final var entry = new Entry();

			this.colorRgb = new float[3 * this.rowCount];
			this.visualBrightnessMultiplier = new float[this.rowCount];

			for (int i = 0; i < this.rowCount; ++i) {
				for (final var column : columns.iterable()) {
					entry.storeColumn(column, buf.getFloat());

					this.visualBrightnessMultiplier[i] = entry.bolometricRatio * entry.efficencyY;
					this.colorRgb[3 * i + 0] = entry.r;
					this.colorRgb[3 * i + 1] = entry.g;
					this.colorRgb[3 * i + 2] = entry.b;
				}
			}
		}

		public float lookupBrightnessMultiplier(double temperatureK) {
			final var fi = this.rowCount * Mth.inverseLerp(temperatureK, this.temperatureMin, this.temperatureMax);
			final int i = Mth.floor(fi);
			final var frac = fi - i;
			if (i >= this.rowCount - 1) {
				return this.visualBrightnessMultiplier[this.visualBrightnessMultiplier.length - 1];
			} else if (i < 0) {
				return this.visualBrightnessMultiplier[0];
			}
			return Mth.lerp((float) frac, this.visualBrightnessMultiplier[i], this.visualBrightnessMultiplier[i + 1]);
		}

		public void lookupColor(Vec3.Mutable out, double temperatureK) {
			final var fi = this.rowCount * Mth.inverseLerp(temperatureK, this.temperatureMin, this.temperatureMax);
			final int i = Mth.floor(fi);
			final var frac = fi - i;
			if (i >= this.rowCount - 1) {
				out.x = this.colorRgb[this.colorRgb.length - 3];
				out.y = this.colorRgb[this.colorRgb.length - 2];
				out.z = this.colorRgb[this.colorRgb.length - 1];
				return;
			} else if (i < 0) {
				out.x = this.colorRgb[0];
				out.y = this.colorRgb[1];
				out.z = this.colorRgb[2];
				return;
			}
			out.x = Mth.lerp(frac, this.colorRgb[3 * i + 0], this.colorRgb[3 * i + 3]);
			out.y = Mth.lerp(frac, this.colorRgb[3 * i + 1], this.colorRgb[3 * i + 4]);
			out.z = Mth.lerp(frac, this.colorRgb[3 * i + 2], this.colorRgb[3 * i + 5]);
		}
	}

	@Nullable
	private static ByteBuffer loadBlackbodyLut() {
		try {
			final var stream = StellarCelestialNode.class.getResourceAsStream("/black_body_lut.bin");
			return ByteBuffer.wrap(stream.readAllBytes()).order(ByteOrder.BIG_ENDIAN);
		} catch (IOException ex) {
			Mod.LOGGER.error("Failed to load star catalog");
			ex.printStackTrace();
			return null;
		}
	}

	static {
		BLACK_BODY_COLOR_TABLE = new BlackbodyTable(loadBlackbodyLut());
	}

	public float getBrightnessMultiplier() {
		return BLACK_BODY_COLOR_TABLE.lookupBrightnessMultiplier(this.temperature);
	}

	public ColorRgba getColor() {
		final var res = new Vec3.Mutable();
		BLACK_BODY_COLOR_TABLE.lookupColor(res, this.temperature);
		return ColorRgba.fromDoubles(res.x, res.y, res.z, 1);
	}

}