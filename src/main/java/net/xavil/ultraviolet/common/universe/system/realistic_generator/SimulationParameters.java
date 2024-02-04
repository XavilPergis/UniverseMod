package net.xavil.ultraviolet.common.universe.system.realistic_generator;

import net.xavil.hawklib.StableRandom;

public class SimulationParameters {

	public double cloudEccentricity = 0.1;
	public double b = 1e-5; // "B"
	public double eccentricityCoefficient = 0.077;
	public double initialPlanetesimalMass = 1e-16;
	public double dustDensityCoefficient = 1.5e-3;
	public double dustDensityAlpha = 1;
	public double dustDensityN = 1;

	public void randomize(StableRandom rng) {
		this.cloudEccentricity = rng.uniformDouble("cloud_eccentricity", 0.05, 0.3);
		this.b *= rng.uniformDouble("b", 0.5, 2);
		this.eccentricityCoefficient *= rng.uniformDouble("eccentricity_coefficient", 0.5, 2);
		this.dustDensityAlpha *= rng.uniformDouble("disc_density_alpha", 0.2, 3);
		this.dustDensityN *= rng.uniformDouble("disc_density_n", 0.1, 3);
	}

}
