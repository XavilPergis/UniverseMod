package net.xavil.ultraviolet.common.universe.system.gen;

import net.xavil.hawklib.Rng;
import net.xavil.hawklib.math.Interval;

public class AccreteContext {

	public final SimulationParameters params;
	public final Rng rng;
	public final double stellarLuminosityLsol;
	public final double stellarMassMsol;
	public final Interval stableOrbitInterval;
	public int nextPlanetesimalId = 0;
	
	public final double systemAgeMya;
	public double currentSystemAgeMya;

	public AccreteContext(SimulationParameters params, Rng rng, double stellarLuminosityLsol, double stellarMassMsol,
			double stellarAgeMyr, Interval stableOrbitInterval) {
		this.params = params;
		this.rng = rng;
		this.stellarLuminosityLsol = stellarLuminosityLsol;
		this.stellarMassMsol = stellarMassMsol;
		this.systemAgeMya = stellarAgeMyr;
		this.currentSystemAgeMya = stellarAgeMyr;
		this.stableOrbitInterval = stableOrbitInterval;
	}

}
