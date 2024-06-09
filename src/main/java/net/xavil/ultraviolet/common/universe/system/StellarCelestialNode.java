package net.xavil.ultraviolet.common.universe.system;

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

public non-sealed class StellarCelestialNode extends UnaryCelestialNode {

	public static enum Modifier {
		NONE,
		ANOMALOUS, // weird system layouts, weird planets
		QUIET, // nice and peaceful uvu
		VERDANT,
		IRIDESCENT,
	}

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

	public static StellarCelestialNode fromInitialParameters(long seed, double massYg, double ageMyr, double metallicity) {
		final var properties = new StellarProperties();
		properties.load(massYg, ageMyr, metallicity);
		final var node = new StellarCelestialNode();
		node.type = StellarCelestialNode.Type.STAR;
		if (properties.luminosityLsol < 1e-8 && properties.temperatureK < 1e-8) {
			node.type = StellarCelestialNode.Type.BLACK_HOLE;
		}
		node.massYg = properties.massYg;
		node.luminosityLsol = properties.luminosityLsol;
		node.radius = Units.km_PER_Rsol * properties.radiusRsol;
		node.temperature = properties.temperatureK;
		return node;
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
					this.colorRgb[3 * i + 0] = (float) Math.pow(entry.r, 2.0);
					this.colorRgb[3 * i + 1] = (float) Math.pow(entry.g, 2.0);
					this.colorRgb[3 * i + 2] = (float) Math.pow(entry.b, 2.0);
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
				out.x = Math.pow(this.colorRgb[this.colorRgb.length - 3], 1.0);
				out.y = Math.pow(this.colorRgb[this.colorRgb.length - 2], 1.0);
				out.z = Math.pow(this.colorRgb[this.colorRgb.length - 1], 1.0);
				return;
			} else if (i < 0) {
				out.x = Math.pow(this.colorRgb[0], 1.0);
				out.y = Math.pow(this.colorRgb[1], 1.0);
				out.z = Math.pow(this.colorRgb[2], 1.0);
				return;
			}
			out.x = Mth.lerp(frac, Math.pow(this.colorRgb[3 * i + 0], 1.0), Math.pow(this.colorRgb[3 * i + 3], 1.0));
			out.y = Mth.lerp(frac, Math.pow(this.colorRgb[3 * i + 1], 1.0), Math.pow(this.colorRgb[3 * i + 4], 1.0));
			out.z = Mth.lerp(frac, Math.pow(this.colorRgb[3 * i + 2], 1.0), Math.pow(this.colorRgb[3 * i + 5], 1.0));
		}
	}

	@Nullable
	private static ByteBuffer loadBlackbodyLut() {
		try {
			final var stream = StellarCelestialNode.class.getResourceAsStream("/black_body_lut.bin");
			return ByteBuffer.wrap(stream.readAllBytes()).order(ByteOrder.BIG_ENDIAN);
		} catch (IOException ex) {
			Mod.LOGGER.error("Failed to load blackbody LUT");
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