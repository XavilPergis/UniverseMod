package net.xavil.ultraviolet.common.universe.system.gen;

import net.xavil.hawklib.Rng;
import net.xavil.hawklib.StableRandom;
import net.xavil.hawklib.math.Interval;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;

public class AccreteContext {

	public final SimulationParameters params;
	public final StableRandom rng;
	public final Galaxy galaxy;
	public final double stellarLuminosityLsol;
	public final double stellarMassMsol;
	public final double systemMetallicity;
	public final Interval stableOrbitInterval;
	public int nextPlanetesimalId = 0;

	public final double systemAgeMya;
	public double currentSystemAgeMya;

	public AccreteContext(SimulationParameters params, StableRandom rng, Galaxy galaxy,
			double stellarLuminosityLsol,
			double stellarMassMsol, double stellarAgeMyr, double systemMetallicity, Interval stableOrbitInterval) {
		this.params = params;
		this.rng = rng;
		this.galaxy = galaxy;
		this.stellarLuminosityLsol = stellarLuminosityLsol;
		this.stellarMassMsol = stellarMassMsol;
		this.systemAgeMya = stellarAgeMyr;
		this.currentSystemAgeMya = 0;
		this.systemMetallicity = systemMetallicity;
		this.stableOrbitInterval = stableOrbitInterval;
	}

}
