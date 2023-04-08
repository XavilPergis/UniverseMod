package net.xavil.universal.common.universe;

public class DensityFields {

	public final double galaxyRadius;
	public final double galaxyAge;

	// how many stars per unit volume there are.
	public final DoubleField3 stellarDensity;
	public final DoubleField3 minAgeFactor;
	// public final DoubleField3 metallicity;

	public DensityFields(double galaxyRadius, double galaxyAge, DoubleField3 stellarDensity,
			DoubleField3 minAgeFactor /* , DoubleField3 metallicity */) {
		this.galaxyRadius = galaxyRadius;
		this.galaxyAge = galaxyAge;
		this.stellarDensity = stellarDensity;
		this.minAgeFactor = minAgeFactor;
		// this.metallicity = metallicity;
	}

}
