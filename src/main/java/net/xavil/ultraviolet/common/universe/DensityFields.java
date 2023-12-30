package net.xavil.ultraviolet.common.universe;

public class DensityFields {

	public final double galaxyRadius;
	public final double galaxyAge;

	// how many stars per unit volume there are.
	public final ScalarField stellarDensity;
	public final ScalarField minAgeFactor;
	public final ScalarField metallicity;

	public DensityFields(double galaxyRadius, double galaxyAge, ScalarField stellarDensity,
			ScalarField minAgeFactor, ScalarField metallicity) {
		this.galaxyRadius = galaxyRadius;
		this.galaxyAge = galaxyAge;
		this.stellarDensity = stellarDensity;
		this.minAgeFactor = minAgeFactor;
		this.metallicity = metallicity;
	}

}
