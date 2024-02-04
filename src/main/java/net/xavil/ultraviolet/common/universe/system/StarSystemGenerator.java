package net.xavil.ultraviolet.common.universe.system;

import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;

public interface StarSystemGenerator {

	static final class Context {
		public final long seed;
		public final Galaxy galaxy;
		// not really meant to be mutated!
		public final GalaxySector sector;
		public final GalaxySectorId sectorId;
		public final GalaxySector.ElementHolder info;

		public Context(long seed, Galaxy galaxy, GalaxySector sector, GalaxySectorId sectorId,
				GalaxySector.ElementHolder info) {
			this.seed = seed;
			this.galaxy = galaxy;
			this.sector = sector;
			this.sectorId = sectorId;
			this.info = info;
		}

		public SystemId systemId() {
			return new SystemId(this.galaxy.galaxyId, this.sectorId);
		}
	}

	CelestialNode generate(Context ctx);

}
