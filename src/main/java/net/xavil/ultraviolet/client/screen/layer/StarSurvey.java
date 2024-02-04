package net.xavil.ultraviolet.client.screen.layer;

import java.util.function.Consumer;

import net.xavil.hawklib.Units;
import net.xavil.hawklib.math.Interval;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicketInfo;

public final class StarSurvey {
	// these are just based on mass, not on actual color
	public int starCountL = 0;
	public int starCountM = 0;
	public int starCountK = 0;
	public int starCountG = 0;
	public int starCountF = 0;
	public int starCountA = 0;
	public int starCountB = 0;
	public int starCountO = 0;
	public double totalMassL = 0, totalTemperatureL = 0, totalLuminosityL = 0, totalAgeL = 0;
	public double totalMassM = 0, totalTemperatureM = 0, totalLuminosityM = 0, totalAgeM = 0;
	public double totalMassK = 0, totalTemperatureK = 0, totalLuminosityK = 0, totalAgeK = 0;
	public double totalMassG = 0, totalTemperatureG = 0, totalLuminosityG = 0, totalAgeG = 0;
	public double totalMassF = 0, totalTemperatureF = 0, totalLuminosityF = 0, totalAgeF = 0;
	public double totalMassA = 0, totalTemperatureA = 0, totalLuminosityA = 0, totalAgeA = 0;
	public double totalMassB = 0, totalTemperatureB = 0, totalLuminosityB = 0, totalAgeB = 0;
	public double totalMassO = 0, totalTemperatureO = 0, totalLuminosityO = 0, totalAgeO = 0;
	public int starsPerLevel[] = new int[GalaxySector.ROOT_LEVEL + 1];
	public int sectorsPerLevel[] = new int[GalaxySector.ROOT_LEVEL + 1];

	public final Histogram massDistribution = new Histogram("Mass (M☉)", 32,
			new AxisMapping.Log(10, 1e-3, 1e3));
	public final Histogram temperatureDistribution = new Histogram("Temperature (K)", 32,
			new AxisMapping.Linear(0, 60000));
	public final Histogram luminosityDistribution = new Histogram("Luminosity (L☉)", 32,
			new AxisMapping.Log(10, 1e-3, 1e5));
	public final Histogram ageDistribution = new Histogram("Age (Mya)", 32,
			new AxisMapping.Linear(0, 10000));

	public StarSurvey() {
	}

	public void init(SectorTicket<SectorTicketInfo.Multi> ticket) {
		final var elem = new GalaxySector.ElementHolder();
		ticket.attachedManager.enumerate(ticket, sector -> {
			// if (sector.level != 3)
			// return;
			final var radius = ticket.info.radiusForLevel(sector.level);
			this.starsPerLevel[sector.level] += sector.elements.size();
			this.sectorsPerLevel[sector.level] += 1;
			for (int i = 0; i < sector.elements.size(); ++i) {
				sector.elements.load(elem, i);
				if (elem.systemPosTm.distanceTo(ticket.info.centerPos) <= radius)
					insert(sector, elem);
			}
		});
	}

	public void reset() {
		for (int i = 0; i < this.starsPerLevel.length; ++i)
			this.starsPerLevel[i] = 0;
		for (int i = 0; i < this.sectorsPerLevel.length; ++i)
			this.sectorsPerLevel[i] = 0;
		this.starCountL = this.starCountM = this.starCountK = this.starCountG = this.starCountF = this.starCountA = this.starCountB = this.starCountO = 0;
		this.totalMassL = this.totalTemperatureL = this.totalLuminosityL = this.totalAgeL = 0;
		this.totalMassM = this.totalTemperatureM = this.totalLuminosityM = this.totalAgeM = 0;
		this.totalMassK = this.totalTemperatureK = this.totalLuminosityK = this.totalAgeK = 0;
		this.totalMassG = this.totalTemperatureG = this.totalLuminosityG = this.totalAgeG = 0;
		this.totalMassF = this.totalTemperatureF = this.totalLuminosityF = this.totalAgeF = 0;
		this.totalMassA = this.totalTemperatureA = this.totalLuminosityA = this.totalAgeA = 0;
		this.totalMassB = this.totalTemperatureB = this.totalLuminosityB = this.totalAgeB = 0;
		this.totalMassO = this.totalTemperatureO = this.totalLuminosityO = this.totalAgeO = 0;
		this.massDistribution.reset();
		this.temperatureDistribution.reset();
		this.luminosityDistribution.reset();
		this.ageDistribution.reset();
	}

	public void insert(GalaxySector sector, GalaxySector.ElementHolder elem) {
		final var massMsol = Units.Msol_PER_Yg * elem.massYg;
		if (elem.temperatureK >= 30000) {
			this.starCountO += 1;
			this.totalMassO += massMsol;
			this.totalTemperatureO += elem.temperatureK;
			this.totalLuminosityO += elem.luminosityLsol;
			this.totalAgeO += elem.systemAgeMyr;
		} else if (elem.temperatureK >= 10000) {
			this.starCountB += 1;
			this.totalMassB += massMsol;
			this.totalTemperatureB += elem.temperatureK;
			this.totalLuminosityB += elem.luminosityLsol;
			this.totalAgeB += elem.systemAgeMyr;
		} else if (elem.temperatureK >= 7500) {
			this.starCountA += 1;
			this.totalMassA += massMsol;
			this.totalTemperatureA += elem.temperatureK;
			this.totalLuminosityA += elem.luminosityLsol;
			this.totalAgeA += elem.systemAgeMyr;
		} else if (elem.temperatureK >= 6000) {
			this.starCountF += 1;
			this.totalMassF += massMsol;
			this.totalTemperatureF += elem.temperatureK;
			this.totalLuminosityF += elem.luminosityLsol;
			this.totalAgeF += elem.systemAgeMyr;
		} else if (elem.temperatureK >= 5200) {
			this.starCountG += 1;
			this.totalMassG += massMsol;
			this.totalTemperatureG += elem.temperatureK;
			this.totalLuminosityG += elem.luminosityLsol;
			this.totalAgeG += elem.systemAgeMyr;
		} else if (elem.temperatureK >= 3700) {
			this.starCountK += 1;
			this.totalMassK += massMsol;
			this.totalTemperatureK += elem.temperatureK;
			this.totalLuminosityK += elem.luminosityLsol;
			this.totalAgeK += elem.systemAgeMyr;
		} else if (elem.temperatureK >= 2400) {
			this.starCountM += 1;
			this.totalMassM += massMsol;
			this.totalTemperatureM += elem.temperatureK;
			this.totalLuminosityM += elem.luminosityLsol;
			this.totalAgeM += elem.systemAgeMyr;
		} else {
			this.starCountL += 1;
			this.totalMassL += massMsol;
			this.totalTemperatureL += elem.temperatureK;
			this.totalLuminosityL += elem.luminosityLsol;
			this.totalAgeL += elem.systemAgeMyr;
		}
		this.massDistribution.insert(massMsol);
		this.temperatureDistribution.insert(elem.temperatureK);
		this.luminosityDistribution.insert(elem.luminosityLsol);
		this.ageDistribution.insert(elem.systemAgeMyr);
	}

	public static void printSurvey(StarSurvey survey, Consumer<String> printer) {
		final var starCountTotal = survey.starCountO + survey.starCountB + survey.starCountA + survey.starCountF
				+ survey.starCountG + survey.starCountK + survey.starCountM + survey.starCountL;
		printer.accept(String.format("Total Star Count: %d", starCountTotal));
		for (int i = 0; i <= GalaxySector.ROOT_LEVEL; ++i) {
			printer.accept(String.format("%d stars across %d sectors for level %d (%f stars/sector)",
					survey.starsPerLevel[i],
					survey.sectorsPerLevel[i], i, survey.starsPerLevel[i] / (double) survey.sectorsPerLevel[i]));
		}
		printer.accept("Star Class Averages:");
		if (survey.starCountO > 0)
			printer.accept(String.format("O: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountO,
					100.0 * survey.starCountO / (double) starCountTotal,
					survey.totalMassO / survey.starCountO,
					survey.totalTemperatureO / survey.starCountO,
					survey.totalLuminosityO / survey.starCountO,
					survey.totalAgeO / survey.starCountO));
		if (survey.starCountB > 0)
			printer.accept(String.format("B: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountB,
					100.0 * survey.starCountB / (double) starCountTotal,
					survey.totalMassB / survey.starCountB,
					survey.totalTemperatureB / survey.starCountB,
					survey.totalLuminosityB / survey.starCountB,
					survey.totalAgeB / survey.starCountB));
		if (survey.starCountA > 0)
			printer.accept(String.format("A: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountA,
					100.0 * survey.starCountA / (double) starCountTotal,
					survey.totalMassA / survey.starCountA,
					survey.totalTemperatureA / survey.starCountA,
					survey.totalLuminosityA / survey.starCountA,
					survey.totalAgeA / survey.starCountA));
		if (survey.starCountF > 0)
			printer.accept(String.format("F: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountF,
					100.0 * survey.starCountF / (double) starCountTotal,
					survey.totalMassF / survey.starCountF,
					survey.totalTemperatureF / survey.starCountF,
					survey.totalLuminosityF / survey.starCountF,
					survey.totalAgeF / survey.starCountF));
		if (survey.starCountG > 0)
			printer.accept(String.format("G: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountG,
					100.0 * survey.starCountG / (double) starCountTotal,
					survey.totalMassG / survey.starCountG,
					survey.totalTemperatureG / survey.starCountG,
					survey.totalLuminosityG / survey.starCountG,
					survey.totalAgeG / survey.starCountG));
		if (survey.starCountK > 0)
			printer.accept(String.format("K: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountK,
					100.0 * survey.starCountK / (double) starCountTotal,
					survey.totalMassK / survey.starCountK,
					survey.totalTemperatureK / survey.starCountK,
					survey.totalLuminosityK / survey.starCountK,
					survey.totalAgeK / survey.starCountK));
		if (survey.starCountM > 0)
			printer.accept(String.format("M: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountM,
					100.0 * survey.starCountM / (double) starCountTotal,
					survey.totalMassM / survey.starCountM,
					survey.totalTemperatureM / survey.starCountM,
					survey.totalLuminosityM / survey.starCountM,
					survey.totalAgeM / survey.starCountM));
		if (survey.starCountL > 0)
			printer.accept(String.format("L: %d (%f%%), %f M☉, %f K, %f L☉, %f Mya", survey.starCountL,
					100.0 * survey.starCountL / (double) starCountTotal,
					survey.totalMassL / survey.starCountL,
					survey.totalTemperatureL / survey.starCountL,
					survey.totalLuminosityL / survey.starCountL,
					survey.totalAgeL / survey.starCountL));

		printer.accept("Mass Distribution");
		survey.massDistribution.display(printer, 100);
		printer.accept("Temperature Distribution");
		survey.temperatureDistribution.display(printer, 100);
		printer.accept("Luminosity Distribution");
		survey.luminosityDistribution.display(printer, 100);
		printer.accept("Age Distribution");
		survey.ageDistribution.display(printer, 100);
	}
}