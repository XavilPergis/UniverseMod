package net.xavil.hawklib;

public class Units {

	protected Units() {
	}

	// ----- metric prefixes -----

	// @formatter:off

	// base unit conversions
	public static final double
	u_PER_nu  = 1e-9, nu_PER_u  = 1 / u_PER_nu,  // n
	u_PER_uu  = 1e-6, uu_PER_u  = 1 / u_PER_uu,  // μ/u
	u_PER_mu  = 1e-3, mu_PER_u  = 1 / u_PER_mu,  // m
	u_PER_cu  = 1e-2, cu_PER_u  = 1 / u_PER_cu,  // c
	u_PER_du  = 1e-1, du_PER_u  = 1 / u_PER_du,  // d
	u_PER_ku  = 1e3,  ku_PER_u  = 1 / u_PER_ku,  // k
	u_PER_Mu  = 1e6,  Mu_PER_u  = 1 / u_PER_Mu,  // M
	u_PER_Gu  = 1e9,  Gu_PER_u  = 1 / u_PER_Gu,  // G
	u_PER_Tu  = 1e12, Tu_PER_u  = 1 / u_PER_Tu,  // T
	u_PER_Pu  = 1e15, Pu_PER_u  = 1 / u_PER_Pu,  // P
	u_PER_Eu  = 1e18, Eu_PER_u  = 1 / u_PER_Eu,  // E
	u_PER_Zu  = 1e21, Zu_PER_u  = 1 / u_PER_Zu,  // Z
	u_PER_Yu  = 1e24, Yu_PER_u  = 1 / u_PER_Yu;  // Y

	// nano conversions
	public static final double
	nu_PER_uu  = nu_PER_u * u_PER_uu,
	nu_PER_mu  = nu_PER_u * u_PER_mu,
	nu_PER_cu  = nu_PER_u * u_PER_cu,
	nu_PER_du  = nu_PER_u * u_PER_du,
	nu_PER_ku  = nu_PER_u * u_PER_ku,
	nu_PER_Mu  = nu_PER_u * u_PER_Mu,
	nu_PER_Gu  = nu_PER_u * u_PER_Gu,
	nu_PER_Tu  = nu_PER_u * u_PER_Tu,
	nu_PER_Pu  = nu_PER_u * u_PER_Pu,
	nu_PER_Eu  = nu_PER_u * u_PER_Eu,
	nu_PER_Zu  = nu_PER_u * u_PER_Zu,
	nu_PER_Yu  = nu_PER_u * u_PER_Yu;

	// micro conversions
	public static final double
	uu_PER_nu  = uu_PER_u * u_PER_nu,
	uu_PER_mu  = uu_PER_u * u_PER_mu,
	uu_PER_cu  = uu_PER_u * u_PER_cu,
	uu_PER_du  = uu_PER_u * u_PER_du,
	uu_PER_ku  = uu_PER_u * u_PER_ku,
	uu_PER_Mu  = uu_PER_u * u_PER_Mu,
	uu_PER_Gu  = uu_PER_u * u_PER_Gu,
	uu_PER_Tu  = uu_PER_u * u_PER_Tu,
	uu_PER_Pu  = uu_PER_u * u_PER_Pu,
	uu_PER_Eu  = uu_PER_u * u_PER_Eu,
	uu_PER_Zu  = uu_PER_u * u_PER_Zu,
	uu_PER_Yu  = uu_PER_u * u_PER_Yu;

	// milli conversions
	public static final double
	mu_PER_nu  = mu_PER_u * u_PER_nu,
	mu_PER_uu  = mu_PER_u * u_PER_uu,
	mu_PER_cu  = mu_PER_u * u_PER_cu,
	mu_PER_du  = mu_PER_u * u_PER_du,
	mu_PER_ku  = mu_PER_u * u_PER_ku,
	mu_PER_Mu  = mu_PER_u * u_PER_Mu,
	mu_PER_Gu  = mu_PER_u * u_PER_Gu,
	mu_PER_Tu  = mu_PER_u * u_PER_Tu,
	mu_PER_Pu  = mu_PER_u * u_PER_Pu,
	mu_PER_Eu  = mu_PER_u * u_PER_Eu,
	mu_PER_Zu  = mu_PER_u * u_PER_Zu,
	mu_PER_Yu  = mu_PER_u * u_PER_Yu;

	// centi conversions
	public static final double
	cu_PER_nu  = cu_PER_u * u_PER_nu,
	cu_PER_uu  = cu_PER_u * u_PER_uu,
	cu_PER_mu  = cu_PER_u * u_PER_mu,
	cu_PER_du  = cu_PER_u * u_PER_du,
	cu_PER_ku  = cu_PER_u * u_PER_ku,
	cu_PER_Mu  = cu_PER_u * u_PER_Mu,
	cu_PER_Gu  = cu_PER_u * u_PER_Gu,
	cu_PER_Tu  = cu_PER_u * u_PER_Tu,
	cu_PER_Pu  = cu_PER_u * u_PER_Pu,
	cu_PER_Eu  = cu_PER_u * u_PER_Eu,
	cu_PER_Zu  = cu_PER_u * u_PER_Zu,
	cu_PER_Yu  = cu_PER_u * u_PER_Yu;

	// deci conversions
	public static final double
	du_PER_nu  = du_PER_u * u_PER_nu,
	du_PER_uu  = du_PER_u * u_PER_uu,
	du_PER_mu  = du_PER_u * u_PER_mu,
	du_PER_cu  = du_PER_u * u_PER_cu,
	du_PER_ku  = du_PER_u * u_PER_ku,
	du_PER_Mu  = du_PER_u * u_PER_Mu,
	du_PER_Gu  = du_PER_u * u_PER_Gu,
	du_PER_Tu  = du_PER_u * u_PER_Tu,
	du_PER_Pu  = du_PER_u * u_PER_Pu,
	du_PER_Eu  = du_PER_u * u_PER_Eu,
	du_PER_Zu  = du_PER_u * u_PER_Zu,
	du_PER_Yu  = du_PER_u * u_PER_Yu;

	// kilo conversions
	public static final double
	ku_PER_nu  = ku_PER_u * u_PER_nu,
	ku_PER_uu  = ku_PER_u * u_PER_uu,
	ku_PER_mu  = ku_PER_u * u_PER_mu,
	ku_PER_cu  = ku_PER_u * u_PER_cu,
	ku_PER_du  = ku_PER_u * u_PER_du,
	ku_PER_Mu  = ku_PER_u * u_PER_Mu,
	ku_PER_Gu  = ku_PER_u * u_PER_Gu,
	ku_PER_Tu  = ku_PER_u * u_PER_Tu,
	ku_PER_Pu  = ku_PER_u * u_PER_Pu,
	ku_PER_Eu  = ku_PER_u * u_PER_Eu,
	ku_PER_Zu  = ku_PER_u * u_PER_Zu,
	ku_PER_Yu  = ku_PER_u * u_PER_Yu;

	// mega conversions
	public static final double
	Mu_PER_nu  = Mu_PER_u * u_PER_nu,
	Mu_PER_uu  = Mu_PER_u * u_PER_uu,
	Mu_PER_mu  = Mu_PER_u * u_PER_mu,
	Mu_PER_cu  = Mu_PER_u * u_PER_cu,
	Mu_PER_du  = Mu_PER_u * u_PER_du,
	Mu_PER_ku  = Mu_PER_u * u_PER_ku,
	Mu_PER_Gu  = Mu_PER_u * u_PER_Gu,
	Mu_PER_Tu  = Mu_PER_u * u_PER_Tu,
	Mu_PER_Pu  = Mu_PER_u * u_PER_Pu,
	Mu_PER_Eu  = Mu_PER_u * u_PER_Eu,
	Mu_PER_Zu  = Mu_PER_u * u_PER_Zu,
	Mu_PER_Yu  = Mu_PER_u * u_PER_Yu;

	// giga conversions
	public static final double
	Gu_PER_nu  = Gu_PER_u * u_PER_nu,
	Gu_PER_uu  = Gu_PER_u * u_PER_uu,
	Gu_PER_mu  = Gu_PER_u * u_PER_mu,
	Gu_PER_cu  = Gu_PER_u * u_PER_cu,
	Gu_PER_du  = Gu_PER_u * u_PER_du,
	Gu_PER_ku  = Gu_PER_u * u_PER_ku,
	Gu_PER_Mu  = Gu_PER_u * u_PER_Mu,
	Gu_PER_Tu  = Gu_PER_u * u_PER_Tu,
	Gu_PER_Pu  = Gu_PER_u * u_PER_Pu,
	Gu_PER_Eu  = Gu_PER_u * u_PER_Eu,
	Gu_PER_Zu  = Gu_PER_u * u_PER_Zu,
	Gu_PER_Yu  = Gu_PER_u * u_PER_Yu;

	// tera conversions
	public static final double
	Tu_PER_nu  = Tu_PER_u * u_PER_nu,
	Tu_PER_uu  = Tu_PER_u * u_PER_uu,
	Tu_PER_mu  = Tu_PER_u * u_PER_mu,
	Tu_PER_cu  = Tu_PER_u * u_PER_cu,
	Tu_PER_du  = Tu_PER_u * u_PER_du,
	Tu_PER_ku  = Tu_PER_u * u_PER_ku,
	Tu_PER_Mu  = Tu_PER_u * u_PER_Mu,
	Tu_PER_Gu  = Tu_PER_u * u_PER_Gu,
	Tu_PER_Pu  = Tu_PER_u * u_PER_Pu,
	Tu_PER_Eu  = Tu_PER_u * u_PER_Eu,
	Tu_PER_Zu  = Tu_PER_u * u_PER_Zu,
	Tu_PER_Yu  = Tu_PER_u * u_PER_Yu;

	// peta conversions
	public static final double
	Pu_PER_nu  = Pu_PER_u * u_PER_nu,
	Pu_PER_uu  = Pu_PER_u * u_PER_uu,
	Pu_PER_mu  = Pu_PER_u * u_PER_mu,
	Pu_PER_cu  = Pu_PER_u * u_PER_cu,
	Pu_PER_du  = Pu_PER_u * u_PER_du,
	Pu_PER_ku  = Pu_PER_u * u_PER_ku,
	Pu_PER_Mu  = Pu_PER_u * u_PER_Mu,
	Pu_PER_Gu  = Pu_PER_u * u_PER_Gu,
	Pu_PER_Tu  = Pu_PER_u * u_PER_Tu,
	Pu_PER_Eu  = Pu_PER_u * u_PER_Eu,
	Pu_PER_Zu  = Pu_PER_u * u_PER_Zu,
	Pu_PER_Yu  = Pu_PER_u * u_PER_Yu;

	// exa conversions
	public static final double
	Eu_PER_nu  = Eu_PER_u * u_PER_nu,
	Eu_PER_uu  = Eu_PER_u * u_PER_uu,
	Eu_PER_mu  = Eu_PER_u * u_PER_mu,
	Eu_PER_cu  = Eu_PER_u * u_PER_cu,
	Eu_PER_du  = Eu_PER_u * u_PER_du,
	Eu_PER_ku  = Eu_PER_u * u_PER_ku,
	Eu_PER_Mu  = Eu_PER_u * u_PER_Mu,
	Eu_PER_Gu  = Eu_PER_u * u_PER_Gu,
	Eu_PER_Tu  = Eu_PER_u * u_PER_Tu,
	Eu_PER_Pu  = Eu_PER_u * u_PER_Pu,
	Eu_PER_Zu  = Eu_PER_u * u_PER_Zu,
	Eu_PER_Yu  = Eu_PER_u * u_PER_Yu;

	// zotta conversions
	public static final double
	Zu_PER_nu  = Zu_PER_u * u_PER_nu,
	Zu_PER_uu  = Zu_PER_u * u_PER_uu,
	Zu_PER_mu  = Zu_PER_u * u_PER_mu,
	Zu_PER_cu  = Zu_PER_u * u_PER_cu,
	Zu_PER_du  = Zu_PER_u * u_PER_du,
	Zu_PER_ku  = Zu_PER_u * u_PER_ku,
	Zu_PER_Mu  = Zu_PER_u * u_PER_Mu,
	Zu_PER_Gu  = Zu_PER_u * u_PER_Gu,
	Zu_PER_Tu  = Zu_PER_u * u_PER_Tu,
	Zu_PER_Pu  = Zu_PER_u * u_PER_Pu,
	Zu_PER_Eu  = Zu_PER_u * u_PER_Eu,
	Zu_PER_Yu  = Zu_PER_u * u_PER_Yu;

	// yotta conversions
	public static final double
	Yu_PER_nu  = Yu_PER_u * u_PER_nu,
	Yu_PER_uu  = Yu_PER_u * u_PER_uu,
	Yu_PER_mu  = Yu_PER_u * u_PER_mu,
	Yu_PER_cu  = Yu_PER_u * u_PER_cu,
	Yu_PER_du  = Yu_PER_u * u_PER_du,
	Yu_PER_ku  = Yu_PER_u * u_PER_ku,
	Yu_PER_Mu  = Yu_PER_u * u_PER_Mu,
	Yu_PER_Gu  = Yu_PER_u * u_PER_Gu,
	Yu_PER_Tu  = Yu_PER_u * u_PER_Tu,
	Yu_PER_Pu  = Yu_PER_u * u_PER_Pu,
	Yu_PER_Eu  = Yu_PER_u * u_PER_Eu,
	Yu_PER_Zu  = Yu_PER_u * u_PER_Zu;
	// @formatter:on

	// ----- unit conversion -----
	public static final double Tm_PER_pc = 3.08568e4;
	public static final double pc_PER_Tm = 1 / Tm_PER_pc;
	public static final double Tm_PER_ly = 9.461e+3;
	public static final double ly_PER_Tm = 1 / Tm_PER_ly;
	public static final double ly_PER_pc = Tm_PER_pc * ly_PER_Tm;
	public static final double pc_PER_ly = 1 / ly_PER_pc;

	public static final double m_PER_au = 1.496e+11;
	public static final double Tm_PER_au = 0.149598;
	public static final double km_PER_au = 1.496e+8;

	// ----- known quantities -----
	// power
	public static final double W_PER_Lsol = 3.827e26;

	// @formatter:off
	// distance
	public static final double
	m_PER_Rsol      = 6.957e8,                   Rsol_PER_m      = 1 / m_PER_Rsol,
	m_PER_Rjupiter  = 7.1492e7,                  Rjupiter_PER_m  = 1 / m_PER_Rjupiter,
	m_PER_Rearth    = 6.3781e6,                  Rearth_PER_m    = 1 / m_PER_Rearth,

	km_PER_Rsol     = m_PER_Rsol * ku_PER_u,     Rsol_PER_km     = 1 / km_PER_Rsol,
	Mm_PER_Rsol     = m_PER_Rsol * Mu_PER_u,     Rsol_PER_Mm     = 1 / Mm_PER_Rsol,
	Tm_PER_Rsol     = m_PER_Rsol * Tu_PER_u,     Rsol_PER_Tm     = 1 / Tm_PER_Rsol,
	km_PER_Rjupiter = m_PER_Rjupiter * ku_PER_u, Rjupiter_PER_km = 1 / km_PER_Rjupiter,
	Mm_PER_Rjupiter = m_PER_Rjupiter * Mu_PER_u, Rjupiter_PER_Mm = 1 / Mm_PER_Rjupiter,
	Tm_PER_Rjupiter = m_PER_Rjupiter * Tu_PER_u, Rjupiter_PER_Tm = 1 / Tm_PER_Rjupiter,
	km_PER_Rearth   = m_PER_Rearth * ku_PER_u,   Rearth_PER_km   = 1 / km_PER_Rearth,
	Mm_PER_Rearth   = m_PER_Rearth * Mu_PER_u,   Rearth_PER_Mm   = 1 / Mm_PER_Rearth,
	Tm_PER_Rearth   = m_PER_Rearth * Tu_PER_u,   Rearth_PER_Tm   = 1 / Tm_PER_Rearth;
	// @formatter:on

	// @formatter:off
	// mass
	public static final double
	kg_PER_Msol     = 1.98847e30,                  Msol_PER_kg     = 1 / kg_PER_Msol,
	kg_PER_Mjupiter = 1.89813e27,                  Mjupiter_PER_kg = 1 / kg_PER_Mjupiter,
	kg_PER_Mearth   = 5.9722e24,                   Mearth_PER_kg   = 1 / kg_PER_Mearth,

	Yg_PER_Msol     = kg_PER_Msol * Yu_PER_ku,     Msol_PER_Yg     = 1 / Yg_PER_Msol,
	Yg_PER_Mjupiter = kg_PER_Mjupiter * Yu_PER_ku, Mjupiter_PER_Yg = 1 / Yg_PER_Mjupiter,
	Yg_PER_Mearth   = kg_PER_Mearth * Yu_PER_ku,   Mearth_PER_Yg   = 1 / Yg_PER_Mearth;
	// @formatter:on
	
	// @formatter:off
	// temperature
	public static final double
	K_PER_Tsol = 5780, Tsol_PER_K = 1 / K_PER_Tsol;
	// @formatter:on

	public static final double SOL_LIFETIME_MYA = 10e4;

}
