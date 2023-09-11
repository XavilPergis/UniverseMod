package net.xavil.ultraviolet.common.universe.system;

import net.xavil.hawklib.Rng;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.universegen.system.CelestialNode;

public interface StarSystemGenerator {

	static final class Context {
		public final Rng rng;
		public final Galaxy galaxy;
		public final GalaxySector.SectorElementHolder info;

		public Context(Rng rng, Galaxy galaxy, GalaxySector.SectorElementHolder info) {
			this.rng = rng;
			this.galaxy = galaxy;
			this.info = info;
		}
	}

	CelestialNode generate(Context ctx);

}
