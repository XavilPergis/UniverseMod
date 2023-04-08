package net.xavil.universal.common.universe.system;

import java.util.Random;

import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.galaxy.GalaxySector;
import net.xavil.universal.common.universe.system.StarSystem.Info;
import net.xavil.util.FastHasher;
import net.xavil.util.Rng;

public interface StarSystemGenerator {

	static final class Context {
		public final Rng rng;
		public final Galaxy galaxy;
		public final GalaxySector sector;
		public final StarSystem.Info info;

		public Context(Rng rng, Galaxy galaxy, GalaxySector sector, Info info) {
			this.rng = rng;
			this.galaxy = galaxy;
			this.sector = sector;
			this.info = info;
		}

		public Context(Galaxy galaxy, GalaxySector sector, Info info) {
			final var seed = FastHasher.withSeed(galaxy.parentUniverse.getCommonUniverseSeed())
					.appendDouble(sector.x).appendDouble(sector.y).appendDouble(sector.z).currentHash();
			this.rng = Rng.wrap(new Random(seed));
			this.galaxy = galaxy;
			this.sector = sector;
			this.info = info;
		}
	}

	StarSystem generate(Context ctx);

}
