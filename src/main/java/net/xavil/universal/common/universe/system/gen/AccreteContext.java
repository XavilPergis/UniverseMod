package net.xavil.universal.common.universe.system.gen;

import net.xavil.util.Rng;

public class AccreteContext {

	public final SimulationParameters params;
	public final Rng rng;
	public final double stellarLuminosityLsol;
	public final double stellarMassMsol;
	public final double systemDistanceLimit;
	public int nextPlanetesimalId = 0;

	public final AccreteDebugEvent.Consumer debugConsumer;

	public AccreteContext(SimulationParameters params, Rng rng, double stellarLuminosityLsol, double stellarMassMsol,
			double systemDistanceLimit, AccreteDebugEvent.Consumer debugConsumer) {
		this.params = params;
		this.rng = rng;
		this.stellarLuminosityLsol = stellarLuminosityLsol;
		this.stellarMassMsol = stellarMassMsol;
		this.systemDistanceLimit = systemDistanceLimit;
		this.debugConsumer = debugConsumer;
	}

}
