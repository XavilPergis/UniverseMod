package net.xavil.hawklib;

public class Constants {

	protected Constants() {
	}

	public static final double Tick_PER_s = 20.0;

	public static final double BOLTZMANN_CONSTANT_W_PER_m2_K4 = 5.670373e-8;
	public static final double GRAVITATIONAL_CONSTANT_m3_PER_kg_s2 = 6.6743e-11;
	public static final double SPEED_OF_LIGHT_m_PER_s = 299792458;
	public static final double SCHWARZSCHILD_FACTOR_m_PER_kg = 2.0 * GRAVITATIONAL_CONSTANT_m3_PER_kg_s2
			/ Math.pow(SPEED_OF_LIGHT_m_PER_s, 2);

}
