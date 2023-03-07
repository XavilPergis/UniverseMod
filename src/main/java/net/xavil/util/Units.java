package net.xavil.util;

public final class Units {

	// ----- unit conversion -----
	public static final double Tm_PER_ly = 9.461e+3;
	public static final double Tm_PER_au = 0.149598;
	public static final double ly_PER_Tm = 1 / Tm_PER_ly;

	public static final double km_PER_au = 1.496e+8;

	public static final double W_PER_Lsol = 3.827e26;

	public static final double m_PER_Rsol = 6.957e8;
	public static final double m_PER_Rearth = 6.371e6;
	public static final double m_PER_Rjupiter = 6.6854e7;

	// ----- constants -----
	// W m^-2 K^-4
	public static final double BOLTZMANN_CONSTANT_W_PER_m2K4 = 5.670373e-8;
	public static final double GRAVITATIONAL_CONSTANT_m3_PER_kg_s2 = 6.6743e-11;

	// ----- known quantities -----
	public static final double Yg_PER_Msol = 1.989e+9;
	public static final double Yg_PER_Mearth = 5.97219e+3;
	public static final double Yg_PER_Mjupiter = 1.899e+6;
	public static final double Mm_PER_Rsol = 695.7;
	public static final double Mm_PER_Rearth = 0.156786;
	public static final double Mm_PER_Rjupiter = 69.91100;
	public static final double SOL_LIFETIME_MYA = 10e4;
	public static final double K_PER_Tsol = 5780;

	// ----- metric prefixes -----

	// @formatter:off
	public static final double QUECTO = 1e-30; // q
	public static final double RONTO  = 1e-27; // r
	public static final double YOCTO  = 1e-24; // y
	public static final double ZEPTO  = 1e-21; // z
	public static final double ATTO   = 1e-18; // a
	public static final double FEMTO  = 1e-15; // f
	public static final double PICO   = 1e-12; // p
	public static final double NANO   = 1e-9;  // n
	public static final double MICRO  = 1e-6;  // μ/u
	public static final double MILLI  = 1e-3;  // m
	public static final double CENTI  = 1e-2;  // c
	public static final double DECI   = 1e-1;  // d
	// base unit
	public static final double DEKA   = 1e1;   // da
	public static final double HECTO  = 1e2;   // h
	public static final double KILO   = 1e3;   // k
	public static final double MEGA   = 1e6;   // M
	public static final double GIGA   = 1e9;   // G
	public static final double TERA   = 1e12;  // T
	public static final double PETA   = 1e15;  // P
	public static final double EXA    = 1e18;  // E
	public static final double ZETTA  = 1e21;  // Z
	public static final double YOTTA  = 1e24;  // Y
	public static final double RONNA  = 1e27;  // R
	public static final double QUETTA = 1e30;  // Q
	// @formatter:on

	public static double fromLy(double ly) {
		return Tm_PER_ly * ly;
	}

	public static double fromAu(double au) {
		return Tm_PER_au * au;
	}

	public static double fromMsol(double msol) {
		return Yg_PER_Msol * msol;
	}

	public static double fromMearth(double mearth) {
		return Yg_PER_Mearth * mearth;
	}

	public static double fromMjupiter(double mjupiter) {
		return Yg_PER_Mjupiter * mjupiter;
	}

}
