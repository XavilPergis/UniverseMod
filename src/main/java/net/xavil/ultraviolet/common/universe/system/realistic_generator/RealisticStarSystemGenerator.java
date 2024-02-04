package net.xavil.ultraviolet.common.universe.system.realistic_generator;

import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.StableRandom;
import net.xavil.hawklib.Units;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.system.CelestialNode;
import net.xavil.ultraviolet.common.universe.system.StarSystemGenerator;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode;
import net.xavil.ultraviolet.common.universe.system.StarSystemGenerator.Context;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode.Properties;

public final class RealisticStarSystemGenerator implements StarSystemGenerator {

	public static final double MAX_METALLICITY = 0.2;

	public static ProtoplanetaryDisc createDisc(Galaxy galaxy, GalaxySector.ElementHolder info) {
		final var rootRng = new StableRandom(info.systemSeed);

		final var stellarProperties = new StellarCelestialNode.Properties();
		stellarProperties.load(new SplittableRng(rootRng.uniformLong("star_props")), info.massYg, 0.0);
		final var node = StellarCelestialNode.fromMassAndAge(new SplittableRng(rootRng.uniformLong("star_props")),
				info.massYg, info.systemAgeMyr);

		final var remainingMass = rootRng.uniformDouble("remaining_mass", 0.05, 0.9 * info.massYg);

		var metallicity = galaxy.densityFields.metallicity.sample(info.systemPosTm);
		if (metallicity > MAX_METALLICITY) {
			// Mod.LOGGER.warn(
			// "Tried to generate star system with a metallicity of '{}', which is greater
			// than the limit of '{}'",
			// metallicity, MAX_METALLICITY);
			metallicity = MAX_METALLICITY;
		}

		// TODO: actually generate metallicity values instead of just doing this
		metallicity = rootRng.weightedDouble("metallicity", 4, 0.001, 0.1);

		final var params = new SimulationParameters();
		params.randomize(rootRng.split("params"));

		final var nodeMass = Units.Msol_PER_Yg * node.massYg;
		final var actx = new AccreteContext(params, rootRng.split("system_gen"), galaxy,
				stellarProperties.luminosityLsol, nodeMass,
				info.systemAgeMyr,
				metallicity);

		return new ProtoplanetaryDisc(actx, remainingMass);
	}

	// lmao this is ass
	@Override
	public CelestialNode generate(Context ctx) {
		try {
			return createDisc(ctx.galaxy, ctx.info).collapseDisc();
		} catch (Throwable t) {
			t.printStackTrace();
			Mod.LOGGER.error("Failed to generate system!");
			throw t;
		}
	}

}
