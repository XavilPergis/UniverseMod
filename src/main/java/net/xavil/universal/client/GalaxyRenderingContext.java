package net.xavil.universal.client;

import java.util.Random;

import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.DensityFields;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.util.Units;
import net.xavil.util.math.matrices.Vec3;

public class GalaxyRenderingContext {

	private Octree<Double> galaxyPoints = null;
	private final Galaxy galaxy;

	public GalaxyRenderingContext(Galaxy galaxy) {
		this.galaxy = galaxy;
	}

	private static double aabbVolume(Vec3 a, Vec3 b) {
		double res = 1;
		res *= Math.abs(a.x - b.x);
		res *= Math.abs(a.y - b.y);
		res *= Math.abs(a.z - b.z);
		return res;
	}

	private void buildGalaxyPoints(DensityFields densityFields) {
		var random = new Random();

		var volumeMin = Vec3.from(-densityFields.galaxyRadius, -densityFields.galaxyRadius * 0.25,
				-densityFields.galaxyRadius);
		var volumeMax = Vec3.from(densityFields.galaxyRadius, densityFields.galaxyRadius * 0.25, densityFields.galaxyRadius);

		this.galaxyPoints = new Octree<Double>(volumeMin, volumeMax);

		var attemptCount = 1000000;

		int successfulPlacements = 0;
		double highestSeenDensity = Double.NEGATIVE_INFINITY;
		double lowestSeenDensity = Double.POSITIVE_INFINITY;
		var maxDensity = 1e21 * attemptCount / aabbVolume(volumeMin, volumeMax);
		for (var i = 0; i < attemptCount; ++i) {
			if (successfulPlacements > 2500)
				break;

			var pos = Vec3.random(random, volumeMin, volumeMax);

			// density is specified in Tm^-3 (ie, number of stars per cubic terameter)
			var density = densityFields.stellarDensity.sample(pos)
					/ (Units.ly_PER_Tm * Units.ly_PER_Tm * Units.ly_PER_Tm);

			if (density > highestSeenDensity)
				highestSeenDensity = density;
			if (density < lowestSeenDensity)
				lowestSeenDensity = density;

			if (density >= random.nextDouble(0, maxDensity)) {
				this.galaxyPoints.insert(pos, 0, density);
				successfulPlacements += 1;
			}
		}

		Mod.LOGGER.info("placed " + successfulPlacements + " sample points");
		Mod.LOGGER.info("max " + maxDensity);
		Mod.LOGGER.info("lowest " + lowestSeenDensity);
		Mod.LOGGER.info("highest " + highestSeenDensity);
	}

	public void build() {
		if (this.galaxyPoints == null) {
			buildGalaxyPoints(this.galaxy.densityFields);
		}
	}

	@FunctionalInterface
	public interface PointConsumer {
		void accept(Vec3 pos, double size);
	}

	public void enumerate(PointConsumer consumer) {
		this.galaxyPoints.enumerateElements(elem -> {
			double s = 4e7 * (elem.value / 5.0);
			if (s < 5e6) s = 5e6;
			if (s > 3e7) s = 3e7;
			consumer.accept(elem.pos, s);
		});
	}

}
