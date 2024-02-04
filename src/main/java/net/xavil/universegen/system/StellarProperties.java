package net.xavil.universegen.system;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.VectorFloat;
import net.xavil.hawklib.collections.impl.VectorInt;
import net.xavil.hawklib.collections.interfaces.ImmutableListFloat;
import net.xavil.ultraviolet.Mod;

public final class StellarProperties {
	public double massYg;
	public double ageMyr;

	public double luminosityLsol;
	public double radiusRsol;
	public double temperatureK;
	public double phase;

	private static final VectorFloat ISO_AGE_INDEX_KEYS = new VectorFloat();
	private static final VectorInt ISO_AGE_INDEX = new VectorInt(); // (start, end)
	private static final VectorFloat ISO_MASS_INDEX_KEYS = new VectorFloat();
	private static final VectorInt ISO_MASS_INDEX = new VectorInt(); // entry_index

	private static final int ISO_FIELDS_PER_ENTRY = 5;
	private static final VectorFloat ISO_ENTRIES = new VectorFloat(); // (final_mass, log_luminosity, log_temperature,
																		// log_radius, phase)

	@Nullable
	private static ByteBuffer loadStellarEvolutionTracks() {
		try {
			final var stream = StellarProperties.class.getResourceAsStream("/star_evolution_tracks.bin");
			return ByteBuffer.wrap(stream.readAllBytes()).order(ByteOrder.BIG_ENDIAN);
		} catch (IOException ex) {
			Mod.LOGGER.error("Failed to load stellar evolution tracks");
			ex.printStackTrace();
			return null;
		}
	}

	private static void loadInMemory() {
		final var buf = loadStellarEvolutionTracks();
		if (buf == null)
			return;

		ISO_AGE_INDEX_KEYS.clear();
		ISO_AGE_INDEX.clear();
		ISO_MASS_INDEX_KEYS.clear();
		ISO_MASS_INDEX.clear();
		ISO_ENTRIES.clear();

		final var isoCount = buf.getInt();
		Mod.LOGGER.info("isochrone count: {}", isoCount);
		for (int i = 0; i < isoCount; ++i) {
			final var logAge = buf.getFloat();
			final var entryCount = buf.getInt();
			Mod.LOGGER.info("entry count: {}", entryCount);
			ISO_AGE_INDEX_KEYS.push(logAge);
			ISO_AGE_INDEX.push(ISO_MASS_INDEX.size() / 2);
			for (int j = 0; j < entryCount; ++j) {
				final var initialMass = buf.getFloat();
				ISO_MASS_INDEX_KEYS.push(initialMass);
				ISO_MASS_INDEX.push(ISO_ENTRIES.size() / ISO_FIELDS_PER_ENTRY);

				ISO_ENTRIES.push(buf.getFloat());
				ISO_ENTRIES.push(buf.getFloat());
				ISO_ENTRIES.push(buf.getFloat());
				ISO_ENTRIES.push(buf.getFloat());
				ISO_ENTRIES.push(buf.get());
			}
			ISO_AGE_INDEX.push(ISO_MASS_INDEX.size() / 2);
		}

	}

	static {
		loadInMemory();
	}

	private int binarySearch(ImmutableListFloat list, float value) {
		return binarySearch(list, 0, list.size(), value);
	}

	private int binarySearch(ImmutableListFloat list, int start, int end, float value) {
		int boundL = start, boundH = end - 1;
		while (boundL < boundH && boundH >= start) {
			final var midpointIndex = (boundL + boundH) / 2;
			final var midpointValue = list.get(midpointIndex);

			if (midpointValue < value) {
				boundL = midpointIndex + 1;
			} else if (midpointValue > value) {
				boundH = midpointIndex - 1;
			} else {
				boundL = boundH = midpointIndex;
			}
		}

		final var listValue = list.get(boundL);
		if (value > listValue)
			return boundL + 1;

		return boundL;
	}

	// very naive interpolation of stellar isochrone data - hopefully it's good
	// enough for this mod. it doesnt need to be super precise or anything, we only
	// need a vague gesture at reality.
	public void load(SplittableRng rng, double massYg, double ageMyr) {
		this.massYg = massYg;
		this.ageMyr = ageMyr;

		// binary search age -> find index of lower and higher bounds for age
		// binary search mass bin -> find index of lower and higher bounds for mass
		// index 4 properties and interpolate

		final var age = (float) Math.log10(ageMyr * 1e6);
		final var mass = (float) (Units.Msol_PER_Yg * this.massYg);

		// what a fucking disaster
		int ageIndexL = 0;
		for (int i = 0; i < ISO_AGE_INDEX_KEYS.size(); ++i) {
			if (ISO_AGE_INDEX_KEYS.get(i) > age) {
				ageIndexL = i - 1;
				break;
			}
		}
		// final var ageIndexL = binarySearch(ISO_AGE_INDEX_KEYS, age);
		final var ageIndexH = ageIndexL + 1;

		final var ageL = ISO_AGE_INDEX_KEYS.get(ageIndexL);
		final var massRangeLIndexL = ISO_AGE_INDEX.get(2 * ageIndexL);
		final var massRangeLIndexH = ISO_AGE_INDEX.get(2 * ageIndexL + 1);

		final var ageH = ISO_AGE_INDEX_KEYS.get(ageIndexH);
		final var massRangeHIndexL = ISO_AGE_INDEX.get(2 * ageIndexH);
		final var massRangeHIndexH = ISO_AGE_INDEX.get(2 * ageIndexH + 1);

		int massIndexLL = 0;
		for (int i = massRangeLIndexL; i < massRangeLIndexH; ++i) {
			if (ISO_MASS_INDEX_KEYS.get(i) > mass) {
				massIndexLL = i - 1;
				break;
			}
		}
		// final var massIndexLL = binarySearch(ISO_MASS_INDEX_KEYS, massRangeLIndexL, massRangeLIndexH, mass);
		final var massIndexLH = massIndexLL + 1;

		int massIndexHL = 0;
		for (int i = massRangeHIndexL; i < massRangeHIndexH; ++i) {
			if (ISO_MASS_INDEX_KEYS.get(i) > mass) {
				massIndexHL = i - 1;
				break;
			}
		}
		// final var massIndexHL = binarySearch(ISO_MASS_INDEX_KEYS, massRangeHIndexL, massRangeHIndexH, mass);
		final var massIndexHH = massIndexHL + 1;

		final var massLL = ISO_MASS_INDEX_KEYS.get(massIndexLL);
		final var massLH = ISO_MASS_INDEX_KEYS.get(massIndexLH);
		final var massHL = ISO_MASS_INDEX_KEYS.get(massIndexHL);
		final var massHH = ISO_MASS_INDEX_KEYS.get(massIndexHH);

		// corner points are
		// (ageL, massLL), (ageL, massLH), (ageH, massHL), (ageH, massHH)

		// derive t-values for interpolation
		final var massTL = Mth.inverseLerp(mass, massLL, massLH);
		final var massTH = Mth.inverseLerp(mass, massHL, massHH);
		final var ageT = Mth.inverseLerp(age, ageL, ageH);

		final var lli = ISO_MASS_INDEX.get(massIndexLL);
		final var lhi = ISO_MASS_INDEX.get(massIndexLH);
		final var hli = ISO_MASS_INDEX.get(massIndexHL);
		final var hhi = ISO_MASS_INDEX.get(massIndexHH);

		this.massYg = interpolate(lli, lhi, hli, hhi, massTL, massTH, ageT, 0);
		this.luminosityLsol = interpolate(lli, lhi, hli, hhi, massTL, massTH, ageT, 1);
		this.temperatureK = interpolate(lli, lhi, hli, hhi, massTL, massTH, ageT, 2);
		this.radiusRsol = interpolate(lli, lhi, hli, hhi, massTL, massTH, ageT, 3);
		this.phase = interpolate(lli, lhi, hli, hhi, massTL, massTH, ageT, 4);

		this.massYg = Units.Yg_PER_Msol * this.massYg;
		this.luminosityLsol = Math.pow(10, this.luminosityLsol);
		this.temperatureK = Math.pow(10, this.temperatureK);
		this.radiusRsol = Math.pow(10, this.radiusRsol);
		// this.luminosityLsol = this.luminosityLsol;
		// this.temperatureK = this.temperatureK;
		// this.radiusRsol = this.radiusRsol;
	}

	private static float interpolate(
			int lli, int lhi, int hli, int hhi,
			float massTL, float massTH, float ageT,
			int offset) {
		var ll = ISO_ENTRIES.get(ISO_FIELDS_PER_ENTRY * lli + offset);
		var lh = ISO_ENTRIES.get(ISO_FIELDS_PER_ENTRY * lhi + offset);
		var hl = ISO_ENTRIES.get(ISO_FIELDS_PER_ENTRY * hli + offset);
		var hh = ISO_ENTRIES.get(ISO_FIELDS_PER_ENTRY * hhi + offset);

		// // HACK: awful hack for debugging cuz i dont wanna restart for hot reloading purposes
		// if (offset != 0) {
		// 	ll = (float) Math.pow(10, ll);
		// 	lh = (float) Math.pow(10, lh);
		// 	hl = (float) Math.pow(10, hl);
		// 	hh = (float) Math.pow(10, hh);
		// }

		final var massL = Mth.lerp(massTL, ll, lh);
		final var massH = Mth.lerp(massTH, hl, hh);
		return Mth.lerp(ageT, massL, massH);
	}

}
