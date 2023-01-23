package net.xavil.universal.common.universe;

public final class Units {

	// main units:
	// Mass: Yg (Yottagrams)
	// Distance: Tm (Terameters)

	public static final double YG_PER_MSOL = 1.989e+9;
	public static final double YG_PER_MEARTH = 5.97219e+3;
	public static final double TM_PER_LY = 9.461e+3;
	public static final double TM_PER_AU = 0.149598;
	public static final double LY_PER_TM = 1 / TM_PER_LY;

	// Sol's effective temperature
	public static final double K_PER_TSOL = 5780;

	public static double ly(double ly) {
		return TM_PER_LY * ly;
	}

	public static double au(double au) {
		return TM_PER_AU * au;
	}

	public static double msol(double msol) {
		return YG_PER_MSOL * msol;
	}

	public static double mearth(double mearth) {
		return YG_PER_MEARTH * mearth;
	}
	
}
