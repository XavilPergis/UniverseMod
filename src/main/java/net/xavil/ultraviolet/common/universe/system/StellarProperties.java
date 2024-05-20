package net.xavil.ultraviolet.common.universe.system;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.collections.impl.VectorFloat;
import net.xavil.hawklib.collections.impl.VectorInt;
import net.xavil.hawklib.math.Interval;
import net.xavil.ultraviolet.Mod;

public final class StellarProperties {
	public double massYg;
	public double ageMyr;

	public double luminosityLsol;
	public double radiusRsol;
	public double temperatureK;
	public double phase;

	private static final Grid GRID;

	static final class Track {
		public final int length;
		public final float[] age;
		public final float[] mass;
		public final float[] radius;
		public final float[] luminosity;
		public final float[] temperature;

		public Track(int length, float[] age, float[] mass, float[] radius, float[] luminosity,
				float[] temperature) {
			this.length = length;
			this.age = age;
			this.mass = mass;
			this.radius = radius;
			this.luminosity = luminosity;
			this.temperature = temperature;
		}

		public Track(int length) {
			this.length = length;
			this.age = new float[length];
			this.mass = new float[length];
			this.radius = new float[length];
			this.luminosity = new float[length];
			this.temperature = new float[length];
		}
	}

	static final class Grid {
		// the metallicity value each top-level array index is associated with
		public final float[] metallicities;
		// the mass value each middle-level array index is associated with
		public final float[] initialMasses;

		// derived from above
		// outermost level: metallicity, middle level: mass, innermost level: age
		public final Track[][] tracks;

		public Grid(float[] metallicities, float[] initialMasses, Track[][] tracks) {
			this.metallicities = metallicities;
			this.initialMasses = initialMasses;
			this.tracks = tracks;
		}
	}

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

	private static Grid loadInMemory() {
		final var buf = loadStellarEvolutionTracks();
		if (buf == null)
			return null;

		final var metallicityCount = buf.getInt();
		final var metallicities = new float[metallicityCount];
		for (int i = 0; i < metallicityCount; ++i) {
			metallicities[i] = buf.getFloat();
		}

		final var massCount = buf.getInt();
		final var masses = new float[massCount];
		for (int i = 0; i < massCount; ++i) {
			masses[i] = buf.getFloat();
		}

		// yknow, id expect it to be like `new T[inner][outer]`, but its actually `new
		// T[outer][inner]` for some reason. i guess it mirrors how you access it? idk.
		final var trackPointers = new int[metallicityCount][massCount];
		Mod.LOGGER.error("mass count = {}", massCount);
		Mod.LOGGER.error("metallicity count = {}", metallicityCount);
		Mod.LOGGER.error("trackPointers.length = {}", trackPointers.length);
		Mod.LOGGER.error("trackPointers[0].length = {}", trackPointers[0].length);
		for (int i = 0; i < metallicityCount; ++i) {
			for (int j = 0; j < massCount; ++j) {
				trackPointers[i][j] = buf.getInt();
			}
		}

		final var tracks = new Track[metallicityCount][massCount];
		for (int i = 0; i < metallicityCount; ++i) {
			for (int j = 0; j < massCount; ++j) {
				buf.position(trackPointers[i][j]);
				final var track = new Track(buf.getShort());
				tracks[i][j] = track;
				for (int k = 0; k < track.length; ++k) {
					track.age[k] = buf.getFloat();
					track.mass[k] = buf.getFloat();
					track.luminosity[k] = buf.getFloat();
					track.temperature[k] = buf.getFloat();
					track.radius[k] = buf.getFloat();
					// skip phase for now
					buf.get();
				}
			}
		}

		return new Grid(metallicities, masses, tracks);
	}

	static {
		GRID = loadInMemory();
	}

	static final class BinarySearchResultDouble {
		public boolean found;
		public int index;
	}

	static final class FindIndicesResult {
		public boolean inBounds;
		public int metallicityIndex, massIndex;
		public double metallicityDist, massDist;
	}

	static void binarySearch(BinarySearchResultDouble out, float[] arr, double x) {
		binarySearch(out, arr, x, 0, arr.length);
	}

	static void binarySearch(BinarySearchResultDouble out, float[] arr, double x, int il, int ih) {
		while (ih > il) {
			final var ic = (il + ih) / 2;
			final var vc = arr[ic];
			if (vc < x) {
				il = ic + 1;
			} else if (vc > x) {
				ih = ic;
			} else {
				// assume that `arr` doesnt contain any NaNs
				out.found = true;
				out.index = ic;
				return;
			}
		}

		out.found = false;
		out.index = il;
	}

	static void findIndices(FindIndicesResult out, double metallicity, double initialMass,
			float[] metallicities, float[] initialMasses) {
		out.inBounds = true;
		out.inBounds &= metallicity >= metallicities[0] && metallicity <= metallicities[metallicities.length - 1];
		out.inBounds &= initialMass >= initialMasses[0] && initialMass <= initialMasses[initialMasses.length - 1];

		if (!out.inBounds)
			return;

		final var searchRes = new BinarySearchResultDouble();

		binarySearch(searchRes, metallicities, metallicity);
		if (searchRes.found) {
			out.metallicityIndex = searchRes.index;
			out.metallicityDist = 0;
		} else {
			out.metallicityIndex = searchRes.index - 1;
			final var hi = metallicities[searchRes.index];
			final var lo = metallicities[searchRes.index - 1];
			out.metallicityDist = Mth.inverseLerp(metallicity, lo, hi);
		}

		binarySearch(searchRes, initialMasses, initialMass);
		if (searchRes.found) {
			out.massIndex = searchRes.index;
			out.massDist = 0;
		} else {
			out.massIndex = searchRes.index - 1;
			final var hi = initialMasses[searchRes.index];
			final var lo = initialMasses[searchRes.index - 1];
			out.massDist = Mth.inverseLerp(initialMass, lo, hi);
		}
	}

	public static final double[] PRIMARY_EEPS = {
			1, 202, 353, 454, 605, 631, 707, 808, 1409, 1710 };
	public static final String[] PRIMARY_EEP_LABELS_LOWMASS = {
			"PMS", "ZAMS", "IAMS", "TAMS", "RGBTip", "ZAHB", "TAHB", "TPAGB", "post-AGB", "WDCS" };
	public static final String[] PRIMARY_EEP_LABELS_HIGHMASS = {
			"PMS", "ZAMS", "IAMS", "TAMS", "RGBTip", "ZACHeB", "TACHeB", "C-burn" };

	public static final Interval LOG_AGE_BOUNDS = new Interval(5, 10.13);
	public static final Interval FEH_BOUNDS = new Interval(-4, 0.5);
	public static final Interval EEP_BOUNDS = new Interval(0, 1710);
	public static final Interval MASS_BOUNDS = new Interval(0.1, 300);

	// @formatter:off
	/*
name = "mist"
eep_col = "EEP"
age_col = "log10_isochrone_age_yr"
feh_col = "[Fe/H]"
mass_col = "star_mass"
initial_mass_col = "initial_mass"
logTeff_col = "log_Teff"
logg_col = "log_g"
logL_col = "log_L"

default_kwargs = {"version": "1.2", "vvcrit": 0.4, "kind": "full_isos"}
default_columns = StellarModelGrid.default_columns + ("delta_nu", "nu_max", "phase")

bounds = (("age", (5, 10.13)), ("feh", (-4, 0.5)), ("eep", (0, 1710)), ("mass", (0.1, 300)))

fehs = [-4.00, -3.50, -3.00, -2.50, -2.00, -1.75, -1.50, -1.25, -1.00, -0.75, -0.50, -0.25, 0.00, 0.25, 0.50]
n_fehs = 15

primary_eeps        = (    1,    202,    353,    454,      605,      631,      707,      808,       1409,   1710)
eep_labels          = ("PMS", "ZAMS", "IAMS", "TAMS", "RGBTip",   "ZAHB",   "TAHB",  "TPAGB", "post-AGB", "WDCS")
eep_labels_highmass = ("PMS", "ZAMS", "IAMS", "TAMS", "RGBTip", "ZACHeB", "TACHeB", "C-burn")
n_eep = 1710

def get_array_grids(self, recalc=False):
	n = len(self.fehs) * len(self.masses)

	# flattened 2d indices for first dimension, 
	age_arrays = np.zeros((n, self.n_eep)) * np.nan
	dt_deep_arrays = np.zeros((n, self.n_eep)) * np.nan

	lengths = np.zeros(n) * np.nan

	for i, (feh, mass) in enumerate(itertools.product(self.fehs, self.masses)):
		# index_cols = ("log10_isochrone_age_yr", "feh", "EEP")
		subdf = self.df.xs((feh, mass), level=(0, 1))
		ages = subdf.age.values
		lengths[i] = len(ages)
		age_arrays[i, :len(ages)] = ages
		dt_deep_arrays[i, :len(ages)] = subdf.dt_deep.values

	return age_arrays, dt_deep_arrays, lengths

	*/
	// @formatter:on

	// estimate EEP value for a given (age, FeH, mass) triple
	private double findEep(Grid grid, double age, double metallicity, double initialMass) {
		final var findIndicesRes = new FindIndicesResult();
		findIndices(findIndicesRes, metallicity, initialMass, grid.metallicities, grid.initialMasses);
		if (!findIndicesRes.inBounds)
			return Double.NaN;

		final int metallicityIndex = findIndicesRes.metallicityIndex, massIndex = findIndicesRes.massIndex;
		final double metallicityT = findIndicesRes.metallicityDist, massT = findIndicesRes.massDist;

		// metallicity,mass
		final var ageGridNN = grid.tracks[metallicityIndex + 0][massIndex + 0].age;
		final var ageGridNP = grid.tracks[metallicityIndex + 0][massIndex + 1].age;
		final var ageGridPN = grid.tracks[metallicityIndex + 1][massIndex + 0].age;
		final var ageGridPP = grid.tracks[metallicityIndex + 1][massIndex + 1].age;

		final var searchRes = new BinarySearchResultDouble();
		binarySearch(searchRes, ageGridNN, age);
		final int iEepNN = searchRes.index;
		binarySearch(searchRes, ageGridNP, age);
		final int iEepNP = searchRes.index;
		binarySearch(searchRes, ageGridPN, age);
		final int iEepPN = searchRes.index;
		binarySearch(searchRes, ageGridPP, age);
		final int iEepPP = searchRes.index;

		// max_i_eep = weight_arrays.shape[1] - 1
		// if (iEepNN > max_i_eep) or (iEepNP > max_i_eep) or (iEepPN > max_i_eep)
		// or (iEepPP > max_i_eep):
		// return np.nan

		int eepNN = iEepNN + 1;
		int eepNP = iEepNP + 1;
		int eepPN = iEepPN + 1;
		int eepPP = iEepPP + 1;
		if (iEepNN >= ageGridNN.length)
			eepNN = eepNP;
		if (iEepNP >= ageGridNP.length)
			eepNP = eepNN;
		if (iEepPN >= ageGridPN.length)
			eepPN = eepPP;
		if (iEepPP >= ageGridPP.length)
			eepPP = eepPN;

		final var eepN = Mth.lerp(massT, eepNN, eepNP);
		final var eepP = Mth.lerp(massT, eepPN, eepPP);
		return Mth.lerp(metallicityT, eepN, eepP);
	}

	private double interp(double tx, double ty, double nn, double np, double pn, double pp) {
		final var massN = Mth.lerp(tx, Math.pow(nn, 0.2), Math.pow(np, 0.2));
		final var massP = Mth.lerp(tx, Math.pow(pn, 0.2), Math.pow(pp, 0.2));
		return Math.pow(Mth.lerp(ty, massN, massP), 5.0);
	}

	private void loadInner(Grid grid, double age, double metallicity, double initialMass) {
		final var findIndicesRes = new FindIndicesResult();
		findIndices(findIndicesRes, metallicity, initialMass, grid.metallicities, grid.initialMasses);
		if (!findIndicesRes.inBounds)
			return;

		final int metallicityIndex = findIndicesRes.metallicityIndex, massIndex = findIndicesRes.massIndex;
		final double metallicityT = findIndicesRes.metallicityDist, massT = findIndicesRes.massDist;

		// metallicity,mass
		final var trackNN = grid.tracks[metallicityIndex + 0][massIndex + 0];
		final var trackNP = grid.tracks[metallicityIndex + 0][massIndex + 1];
		final var trackPN = grid.tracks[metallicityIndex + 1][massIndex + 0];
		final var trackPP = grid.tracks[metallicityIndex + 1][massIndex + 1];

		final var searchRes = new BinarySearchResultDouble();
		binarySearch(searchRes, trackNN.age, age);
		int iEepNN = searchRes.index;
		binarySearch(searchRes, trackNP.age, age);
		int iEepNP = searchRes.index;
		binarySearch(searchRes, trackPN.age, age);
		int iEepPN = searchRes.index;
		binarySearch(searchRes, trackPP.age, age);
		int iEepPP = searchRes.index;

		// max_i_eep = weight_arrays.shape[1] - 1
		// if (iEepNN > max_i_eep) or (iEepNP > max_i_eep) or (iEepPN > max_i_eep)
		// or (iEepPP > max_i_eep):
		// return np.nan

		// if (iEepNN >= trackNN.age.length)
		// 	eepNN = eepNP;
		// if (iEepNP >= trackNP.age.length)
		// 	eepNP = eepNN;
		// if (iEepPN >= trackPN.age.length)
		// 	eepPN = eepPP;
		// if (iEepPP >= trackPP.age.length)
		// 	eepPP = eepPN;
		if (iEepNN >= trackNN.age.length)
			iEepNN = trackNN.age.length - 1;
		if (iEepNP >= trackNP.age.length)
			iEepNP = trackNP.age.length - 1;
		if (iEepPN >= trackPN.age.length)
			iEepPN = trackPN.age.length - 1;
		if (iEepPP >= trackPP.age.length)
			iEepPP = trackPP.age.length - 1;

		final var mass = interp(massT, metallicityT,
				trackNN.mass[iEepNN], trackNP.mass[iEepNP],
				trackPN.mass[iEepPN], trackPP.mass[iEepPP]);
		final var radius = interp(massT, metallicityT,
				trackNN.radius[iEepNN], trackNP.radius[iEepNP],
				trackPN.radius[iEepPN], trackPP.radius[iEepPP]);
		final var luminosity = interp(massT, metallicityT,
				trackNN.luminosity[iEepNN], trackNP.luminosity[iEepNP],
				trackPN.luminosity[iEepPN], trackPP.luminosity[iEepPP]);
		final var temperature = interp(massT, metallicityT,
				trackNN.temperature[iEepNN], trackNP.temperature[iEepNP],
				trackPN.temperature[iEepPN], trackPP.temperature[iEepPP]);

		this.massYg = Units.Yg_PER_Msol * mass;
		this.radiusRsol = radius;
		this.luminosityLsol = luminosity;
		this.temperatureK = temperature;
	}

	public void load(double massYg, double ageMyr, double metallicity) {
		loadInner(GRID, 1e6 * ageMyr, metallicity, Units.Msol_PER_Yg * massYg);
	}

}
