package net.xavil.universal.common.universe.system.gen;

import java.util.Random;

public class AccreteContext {

	public final SimulationParameters params;
	public final Random random;
	public final double stellarLuminosityLsol;
	public final double stellarMassMsol;

	public AccreteContext(SimulationParameters params, Random random, double stellarLuminosityLsol, double stellarMassMsol) {
		this.params = params;
		this.random = random;
		this.stellarLuminosityLsol = stellarLuminosityLsol;
		this.stellarMassMsol = stellarMassMsol;
	}

}
