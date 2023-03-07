package net.xavil.universal.common.universe;

public class DensityFields {
	
	public final double galaxyRadius;
	// how many stars per unit volume there are.
	public final DoubleField3 stellarDensity;
	public final DoubleField3 minAgeFactor;

	public DensityFields(double galaxyRadius, DoubleField3 stellarDensity, DoubleField3 minAgeFactor) {
		this.galaxyRadius = galaxyRadius;
		this.stellarDensity = stellarDensity;
		this.minAgeFactor = minAgeFactor;
	}

}
