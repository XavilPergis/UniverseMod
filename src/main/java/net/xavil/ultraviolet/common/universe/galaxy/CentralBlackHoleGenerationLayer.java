package net.xavil.ultraviolet.common.universe.galaxy;

import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector.PackedSectorElements;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxySector.SectorElementHolder;
import net.xavil.ultraviolet.common.universe.system.StarSystem;

// TODO
public final class CentralBlackHoleGenerationLayer extends GalaxyGenerationLayer {

	public CentralBlackHoleGenerationLayer(Galaxy parentGalaxy, int layerId) {
		super(parentGalaxy, layerId);
	}

	@Override
	public void generateInto(Context ctx, PackedSectorElements elements) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'generateInto'");
	}

	@Override
	public StarSystem generateFullSystem(SectorElementHolder elem) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'generateFullSystem'");
	}
	
}
