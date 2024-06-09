package net.xavil.ultraviolet.common.universe.system;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.annotation.Nullable;

import net.minecraft.util.Mth;
import net.xavil.hawklib.Units;
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

		public final float minMetallicity, maxMetallicity;
		public final float minInitialMass, maxInitialMass;

		// derived from above
		// outermost level: metallicity, middle level: mass, innermost level: age
		public final Track[][] tracks;

		public Grid(float[] metallicities, float[] initialMasses, Track[][] tracks) {
			this.metallicities = metallicities;
			this.initialMasses = initialMasses;
			this.tracks = tracks;
			this.minMetallicity = metallicities[0];
			this.maxMetallicity = metallicities[metallicities.length - 1];
			this.minInitialMass = initialMasses[0];
			this.maxInitialMass = initialMasses[initialMasses.length - 1];
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

	static final class GridCell {
		public Track trackNN, trackNP, trackPN, trackPP;
		public double metallicityT, massT;

		public GridCell(double metallicityT, double massT, Track trackNN, Track trackNP, Track trackPN, Track trackPP) {
			this.trackNN = trackNN;
			this.trackNP = trackNP;
			this.trackPN = trackPN;
			this.trackPP = trackPP;
			this.metallicityT = metallicityT;
			this.massT = massT;
		}
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

	// find the 4 tracks that the initial (mass, age, metallicity) triple is
	// enclosed by
	static void findIndices(FindIndicesResult out, double metallicity, double initialMass, Grid grid) {
		out.inBounds = true;
		out.inBounds &= metallicity >= grid.minMetallicity && metallicity <= grid.maxMetallicity;
		out.inBounds &= initialMass >= grid.minInitialMass && initialMass <= grid.maxInitialMass;

		if (!out.inBounds)
			return;

		final var searchRes = new BinarySearchResultDouble();

		binarySearch(searchRes, grid.metallicities, metallicity);
		if (searchRes.found) {
			out.metallicityIndex = searchRes.index;
			out.metallicityDist = 0;
		} else {
			out.metallicityIndex = searchRes.index - 1;
			final var hi = grid.metallicities[searchRes.index];
			final var lo = grid.metallicities[searchRes.index - 1];
			out.metallicityDist = Mth.inverseLerp(metallicity, lo, hi);
		}

		binarySearch(searchRes, grid.initialMasses, initialMass);
		if (searchRes.found) {
			out.massIndex = searchRes.index;
			out.massDist = 0;
		} else {
			out.massIndex = searchRes.index - 1;
			final var hi = grid.initialMasses[searchRes.index];
			final var lo = grid.initialMasses[searchRes.index - 1];
			out.massDist = Mth.inverseLerp(initialMass, lo, hi);
		}
	}

	// find the 4 tracks that the initial (mass, metallicity) pair is enclosed by
	@Nullable
	static GridCell findTracks(Grid grid, double metallicity, double initialMass) {
		// boolean inBounds = true;
		// inBounds &= metallicity >= grid.minMetallicity && metallicity <=
		// grid.maxMetallicity;
		// inBounds &= initialMass >= grid.minInitialMass && initialMass <=
		// grid.maxInitialMass;
		// if (!inBounds)
		// return null;

		metallicity = Mth.clamp(metallicity, grid.minMetallicity, grid.maxMetallicity);
		initialMass = Mth.clamp(initialMass, grid.minInitialMass, grid.maxInitialMass);

		final var searchRes = new BinarySearchResultDouble();
		final int metallicityIndex, massIndex;
		final double metallicityT, massT;

		binarySearch(searchRes, grid.metallicities, metallicity);
		if (metallicity == grid.maxMetallicity) {
			metallicityIndex = grid.metallicities.length - 2;
			metallicityT = 1;
		} else if (searchRes.found) {
			metallicityIndex = searchRes.index;
			metallicityT = 0;
		} else {
			if (searchRes.index >= grid.metallicities.length || searchRes.index <= 0) {
				return null;
			}
			metallicityIndex = searchRes.index - 1;
			final var hi = grid.metallicities[searchRes.index];
			final var lo = grid.metallicities[searchRes.index - 1];
			metallicityT = Mth.inverseLerp(metallicity, lo, hi);
		}

		binarySearch(searchRes, grid.initialMasses, initialMass);
		if (initialMass == grid.maxInitialMass) {
			massIndex = grid.initialMasses.length - 2;
			massT = 1;
		} else if (searchRes.found) {
			massIndex = searchRes.index;
			massT = 0;
		} else {
			if (searchRes.index >= grid.initialMasses.length || searchRes.index <= 0) {
				return null;
			}
			massIndex = searchRes.index - 1;
			final var hi = grid.initialMasses[searchRes.index];
			final var lo = grid.initialMasses[searchRes.index - 1];
			massT = Mth.inverseLerp(initialMass, lo, hi);
		}

		return new GridCell(metallicityT, massT,
				grid.tracks[metallicityIndex][massIndex],
				grid.tracks[metallicityIndex][massIndex + 1],
				grid.tracks[metallicityIndex + 1][massIndex],
				grid.tracks[metallicityIndex + 1][massIndex + 1]);
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



@nb.jit(nopython=True)
def interp_eep(age, feh, mass, fehs, masses, n1, ages, lengths):
    if age != age or feh != feh or mass != mass:
        return np.nan

    (feh_idx, mass_idx), (feh_dist, mass_dist), oob = find_indices_2d(feh, mass, fehs, masses)

    if oob:
        return np.nan

    ind_nn = feh_idx * n1 + mass_idx
    ind_np = feh_idx * n1 + (mass_idx + 1)
    ind_pn = (feh_idx + 1) * n1 + mass_idx
    ind_pp = (feh_idx + 1) * n1 + (mass_idx + 1)

    # The EEP value is just the same as the index + 1
    i_eep_nn, _ = searchsorted(ages[ind_nn, :], age, N=lengths[ind_nn])
    i_eep_np, _ = searchsorted(ages[ind_np, :], age, N=lengths[ind_np])
    i_eep_pn, _ = searchsorted(ages[ind_pn, :], age, N=lengths[ind_pn])
    i_eep_pp, _ = searchsorted(ages[ind_pp, :], age, N=lengths[ind_pp])

    max_i_eep = weight_arrays.shape[1] - 1
    if (i_eep_nn > max_i_eep) or (i_eep_np > max_i_eep) or (i_eep_pn > max_i_eep) or (i_eep_pp > max_i_eep):
        return np.nan

    eep_nn = i_eep_nn + 1
    eep_np = i_eep_np + 1
    eep_pn = i_eep_pn + 1
    eep_pp = i_eep_pp + 1

    if i_eep_nn >= lengths[ind_nn]:
        eep_nn = eep_np
    if i_eep_np >= lengths[ind_np]:
        eep_np = eep_nn
    if i_eep_pn >= lengths[ind_pn]:
        eep_pn = eep_pp
    if i_eep_pp >= lengths[ind_pp]:
        eep_pp = eep_pn

	# bilinear interpolation
    eep_0 = lerp(mass_dist, eep_nn, eep_np)
    eep_1 = lerp(mass_dist, eep_pn, eep_pp)
    return lerp(feh_dist, eep_0, eep_1)

	*/
	// @formatter:on

	private double findEep(GridCell cell, double age) {
		final var searchRes = new BinarySearchResultDouble();
		binarySearch(searchRes, cell.trackNN.age, age);
		final int iEepNN = searchRes.index;
		binarySearch(searchRes, cell.trackNP.age, age);
		final int iEepNP = searchRes.index;
		binarySearch(searchRes, cell.trackPN.age, age);
		final int iEepPN = searchRes.index;
		binarySearch(searchRes, cell.trackPP.age, age);
		final int iEepPP = searchRes.index;

		int eepNN = iEepNN + 1;
		int eepNP = iEepNP + 1;
		int eepPN = iEepPN + 1;
		int eepPP = iEepPP + 1;
		if (iEepNN >= cell.trackNN.length)
			eepNN = eepNP;
		if (iEepNP >= cell.trackNP.length)
			eepNP = eepNN;
		if (iEepPN >= cell.trackPN.length)
			eepPN = eepPP;
		if (iEepPP >= cell.trackPP.length)
			eepPP = eepPN;

		final var eepN = Mth.lerp(cell.massT, eepNN, eepNP);
		final var eepP = Mth.lerp(cell.massT, eepPN, eepPP);
		return Mth.lerp(cell.metallicityT, eepN, eepP);
	}

	private double bilinear(double tx, double ty, double nn, double np, double pn, double pp) {
		final var massN = Mth.lerp(tx, nn, np);
		final var massP = Mth.lerp(tx, pn, pp);
		return Mth.lerp(ty, massN, massP);
	}

	private double trilinear(
			double massT, double metallicityT, double eepT,
			int iEepL, int iEepH,
			float[] nn, float[] np, float[] pn, float[] pp) {
		return Mth.lerp(
				eepT,
				bilinear(massT, metallicityT, nn[iEepL], np[iEepL], pn[iEepL], pp[iEepL]),
				bilinear(massT, metallicityT, nn[iEepH], np[iEepH], pn[iEepH], pp[iEepH]));
	}

	private void loadInner2(Grid grid, double age, double metallicity, double initialMass) {
		final var tracks = findTracks(grid, metallicity, initialMass);

		if (tracks == null) {
			// FIXME
			this.massYg = Units.Yg_PER_Msol * 0;
			this.radiusRsol = 1;
			this.luminosityLsol = 0;
			this.temperatureK = 20000;
			return;
		}

		final var eep = findEep(tracks, age);

		final int eepL = Mth.floor(eep - 1), eepH = Mth.ceil(eep - 1);
		final double eepT = eep - eepL;

		if (eepH >= tracks.trackNN.length ||
				eepH >= tracks.trackNP.length ||
				eepH >= tracks.trackPN.length ||
				eepH >= tracks.trackPP.length) {

			// there's a few reasons we might get here - either a star is at the end of its
			// life, or it is straddling the low mass and high mass tracks.

			// 7.5 Msol

			// FIXME
			this.massYg = Units.Yg_PER_Msol * 1;
			this.radiusRsol = 0.5;
			this.luminosityLsol = 0;
			this.temperatureK = 0;
			return;
		}

		final var mass = trilinear(
				tracks.massT, tracks.metallicityT, eepT,
				eepL, eepH,
				tracks.trackNN.mass, tracks.trackNP.mass,
				tracks.trackPN.mass, tracks.trackPP.mass);
		final var radius = trilinear(
				tracks.massT, tracks.metallicityT, eepT,
				eepL, eepH,
				tracks.trackNN.radius, tracks.trackNP.radius,
				tracks.trackPN.radius, tracks.trackPP.radius);
		final var luminosity = trilinear(
				tracks.massT, tracks.metallicityT, eepT,
				eepL, eepH,
				tracks.trackNN.luminosity, tracks.trackNP.luminosity,
				tracks.trackPN.luminosity, tracks.trackPP.luminosity);
		final var temperature = trilinear(
				tracks.massT, tracks.metallicityT, eepT,
				eepL, eepH,
				tracks.trackNN.temperature, tracks.trackNP.temperature,
				tracks.trackPN.temperature, tracks.trackPP.temperature);

		this.massYg = Units.Yg_PER_Msol * mass;
		this.radiusRsol = radius;
		this.luminosityLsol = luminosity;
		this.temperatureK = temperature;
	}

	private void loadInner(Grid grid, double age, double metallicity, double initialMass) {
		final var findIndicesRes = new FindIndicesResult();
		findIndices(findIndicesRes, metallicity, initialMass, grid);
		if (!findIndicesRes.inBounds)
			return;

		final int metallicityIndex = findIndicesRes.metallicityIndex, massIndex = findIndicesRes.massIndex;
		final double metallicityT = findIndicesRes.metallicityDist, massT = findIndicesRes.massDist;

		// metallicity,mass
		final var trackNN = grid.tracks[metallicityIndex + 0][massIndex + 0];
		final var trackNP = grid.tracks[metallicityIndex + 0][massIndex + 1];
		final var trackPN = grid.tracks[metallicityIndex + 1][massIndex + 0];
		final var trackPP = grid.tracks[metallicityIndex + 1][massIndex + 1];

		// find the EEP number for each track
		final var searchRes = new BinarySearchResultDouble();
		binarySearch(searchRes, trackNN.age, age);
		int iEepNN = searchRes.index;
		binarySearch(searchRes, trackNP.age, age);
		int iEepNP = searchRes.index;
		binarySearch(searchRes, trackPN.age, age);
		int iEepPN = searchRes.index;
		binarySearch(searchRes, trackPP.age, age);
		int iEepPP = searchRes.index;

		if (iEepNN >= trackNN.age.length)
			iEepNN = trackNN.age.length - 1;
		if (iEepNP >= trackNP.age.length)
			iEepNP = trackNP.age.length - 1;
		if (iEepPN >= trackPN.age.length)
			iEepPN = trackPN.age.length - 1;
		if (iEepPP >= trackPP.age.length)
			iEepPP = trackPP.age.length - 1;

		final var mass = bilinear(massT, metallicityT,
				trackNN.mass[iEepNN], trackNP.mass[iEepNP],
				trackPN.mass[iEepPN], trackPP.mass[iEepPP]);
		final var radius = bilinear(massT, metallicityT,
				trackNN.radius[iEepNN], trackNP.radius[iEepNP],
				trackPN.radius[iEepPN], trackPP.radius[iEepPP]);
		final var luminosity = bilinear(massT, metallicityT,
				trackNN.luminosity[iEepNN], trackNP.luminosity[iEepNP],
				trackPN.luminosity[iEepPN], trackPP.luminosity[iEepPP]);
		final var temperature = bilinear(massT, metallicityT,
				trackNN.temperature[iEepNN], trackNP.temperature[iEepNP],
				trackPN.temperature[iEepPN], trackPP.temperature[iEepPP]);

		this.massYg = Units.Yg_PER_Msol * mass;
		this.radiusRsol = radius;
		this.luminosityLsol = luminosity;
		this.temperatureK = temperature;
	}

	public void load(double massYg, double ageMyr, double metallicity) {
		// loadInner(GRID, 1e6 * ageMyr, metallicity, Units.Msol_PER_Yg * massYg);
		loadInner2(GRID, 1e6 * ageMyr, metallicity, Units.Msol_PER_Yg * massYg);
	}

}
