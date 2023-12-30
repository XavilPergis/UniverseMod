package net.xavil.ultraviolet.common.universe.galaxy;

import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.hawklib.math.matrices.Vec3;

public abstract class GalaxyGenerationLayer {

	public final Galaxy parentGalaxy;
	public final int layerId;

	public GalaxyGenerationLayer(Galaxy parentGalaxy, int layerId) {
		this.parentGalaxy = parentGalaxy;
		this.layerId = layerId;
	}

	public static final class Context {
		public final Galaxy galaxy;
		public final SectorPos pos;
		public final int level;
		public final Vec3 volumeMin;
		public final Vec3 volumeMax;

		public Context(Galaxy galaxy, SectorPos pos) {
			this.galaxy = galaxy;
			this.pos = pos;
			this.level = pos.level();
			this.volumeMin = pos.minBound();
			this.volumeMax = pos.maxBound();
		}
	}

	public abstract void generateInto(Context ctx, GalaxySector.PackedElements elements);

	public abstract StarSystem generateFullSystem(GalaxySector sector, GalaxySectorId id, GalaxySector.ElementHolder elem);

}
