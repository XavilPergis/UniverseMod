package net.xavil.universal.common.universe.galaxy;

import java.util.Random;

import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.util.math.Vec3;

public abstract class GalaxyGenerationLayer {

	public final Galaxy parentGalaxy;
	public final int layerId;

	public GalaxyGenerationLayer(Galaxy parentGalaxy, int layerId) {
		this.parentGalaxy = parentGalaxy;
		this.layerId = layerId;
	}

	public static final class Context {
		public final Galaxy galaxy;
		public final Random random;
		public final double starCountFactor;
		public final SectorPos pos;
		public final int level;
		public final Vec3 volumeMin;
		public final Vec3 volumeMax;

		public Context(Galaxy galaxy, Random random, double starCountFactor, SectorPos pos) {
			this.galaxy = galaxy;
			this.random = random;
			this.starCountFactor = starCountFactor;
			this.pos = pos;
			this.level = pos.level();
			this.volumeMin = pos.minBound();
			this.volumeMax = pos.maxBound();

		}
	}

	public interface Sink {
		int accept(Vec3 pos, StarSystem.Info systemInfo, long systemSeed);
	}

	// TODO: maybe we could cache some things like stellar density in the corners of
	// some sort of "sector info" type and use trilinear interpolation to avoid
	// sampling the density field over and over again. this may not work so great
	// for higher levels of the sector trees, idk.

	public abstract void generateInto(Context ctx, Sink sink);

	public abstract StarSystem generateFullSystem(StarSystem.Info systemInfo, long systemSeed);

}
