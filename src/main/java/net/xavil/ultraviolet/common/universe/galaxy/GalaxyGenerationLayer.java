package net.xavil.ultraviolet.common.universe.galaxy;

import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.hawklib.math.matrices.Vec3;

public abstract class GalaxyGenerationLayer {

	public final Galaxy parentGalaxy;
	public int layerId;

	public static final int BASE_LAYER_ID = 0;
	public static final int STARTING_SYSTEM_LAYER_ID = 1;
	public static final int STAR_CATALOG_LAYER_ID = 2;
	public static final int CENTRAL_BLACK_HOLE_LAYER_ID = 3;

	public GalaxyGenerationLayer(Galaxy parentGalaxy) {
		this.parentGalaxy = parentGalaxy;
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
