package net.xavil.ultraviolet.common.universe.galaxy;

import net.minecraft.util.Mth;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;

public final class GalaxyRegionWeights {

	@FunctionalInterface
	public interface Field {
		void evaluate(Vec3Access pos, GalaxyRegionWeights outMasks);
	}

	public double core, arms, disc, halo;

	public double totalWeight() {
		return this.core + this.arms + this.disc + this.halo;
	}

	public static void lerp(GalaxyRegionWeights out, double t, GalaxyRegionWeights a, GalaxyRegionWeights b) {
		out.core = Mth.lerp(t, a.core, b.core);
		out.arms = Mth.lerp(t, a.arms, b.arms);
		out.disc = Mth.lerp(t, a.disc, b.disc);
		out.halo = Mth.lerp(t, a.halo, b.halo);
	}

	public static double dot(GalaxyRegionWeights a, GalaxyRegionWeights b) {
		double res = 0.0;
		res += a.core * b.core;
		res += a.arms * b.arms;
		res += a.disc * b.disc;
		res += a.halo * b.halo;
		return res;
	}

	public <T> T pick(double t, T core, T arms, T disc, T halo) {
		double momentum = t * this.totalWeight();

		if (this.core > momentum)
			return core;
		momentum -= this.core;
		if (this.arms > momentum)
			return arms;
		momentum -= this.arms;
		if (this.disc > momentum)
			return disc;
		momentum -= this.disc;
		if (this.halo > momentum)
			return halo;
		momentum -= this.halo;

		// shouldnt get here, but im not sure what happens when t is exactly 1, so i
		// return something just in case~
		return disc;
	}
}