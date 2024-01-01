package net.xavil.ultraviolet.common.universe.galaxy;

import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector.PackedElements;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector.ElementHolder;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;

// TODO
public final class CentralBlackHoleGenerationLayer extends GalaxyGenerationLayer {

	public CentralBlackHoleGenerationLayer(Galaxy parentGalaxy) {
		super(parentGalaxy);
	}

	@Override
	public void generateInto(Context ctx, PackedElements elements) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'generateInto'");
	}

	@Override
	public StarSystem generateFullSystem(GalaxySector sector, GalaxySectorId id, ElementHolder elem) {
		// TODO Auto-generated method stub
		return null;
	}
}
