package net.xavil.ultraviolet.common.universe;

import javax.annotation.Nonnull;

import net.xavil.hawklib.ProbabilityDistribution;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxyRegionWeights;

public class GalaxyParameters {

	public final double galaxyRadius;
	public final double galaxyAge;

	public final GalaxyRegionWeights.Field masks;

	// star formation histories
	public final ProbabilityDistribution coreSfh;
	public final ProbabilityDistribution armsSfh;
	public final ProbabilityDistribution discSfh;
	public final ProbabilityDistribution haloSfh;

	// M_sol pc^-3
	public final GalaxyRegionWeights stellarDensityWeights;

	public GalaxyParameters(double galaxyRadius, double galaxyAge,
			GalaxyRegionWeights.Field masks,
			GalaxyRegionWeights stellarDensityWeights,
			ProbabilityDistribution coreSfh,
			ProbabilityDistribution armsSfh,
			ProbabilityDistribution discSfh,
			ProbabilityDistribution haloSfh) {
		this.galaxyRadius = galaxyRadius;
		this.galaxyAge = galaxyAge;
		this.masks = masks;
		this.stellarDensityWeights = stellarDensityWeights;
		this.coreSfh = coreSfh;
		this.armsSfh = armsSfh;
		this.discSfh = discSfh;
		this.haloSfh = haloSfh;
	}

	/**
	 * Blend between star formation histories such that the chance of picking any
	 * sfh is proportional to the local region weights. This method is meant to be
	 * called once for each sample, instead of being called once and having its
	 * result drawn from multiple times.
	 * 
	 * @param weights The galaxy region weights.
	 * @param t       A uniformly-distributed random value.
	 * @return The sampled sfh.
	 */
	@Nonnull
	public ProbabilityDistribution pickSfh(GalaxyRegionWeights weights, double t) {
		double momentum = t * weights.totalWeight();

		if (weights.core > momentum)
			return this.coreSfh;
		momentum -= weights.core;
		if (weights.arms > momentum)
			return this.armsSfh;
		momentum -= weights.arms;
		if (weights.disc > momentum)
			return this.discSfh;
		momentum -= weights.disc;
		if (weights.halo > momentum)
			return this.haloSfh;
		momentum -= weights.halo;

		// shouldnt get here, but im not sure what happens when t is exactly 1, so i
		// return something just in case~
		return this.discSfh;
	}

	public double sampleDensity(Vec3Access pos) {
		final var tmp = new GalaxyRegionWeights();
		this.masks.evaluate(pos, tmp);
		return GalaxyRegionWeights.dot(this.stellarDensityWeights, tmp);
	}

}
