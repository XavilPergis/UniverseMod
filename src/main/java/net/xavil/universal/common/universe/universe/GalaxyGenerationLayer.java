package net.xavil.universal.common.universe.universe;

import java.util.Random;

import net.minecraft.core.Vec3i;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystem.Info;

public abstract class GalaxyGenerationLayer {

	public static final class Context {
		public final Galaxy galaxy;
		public final Random random;
		public final Vec3i volumeCoords;
		public final Octree<Lazy<StarSystem.Info, StarSystem>> volume;

		public Context(Galaxy galaxy, Random random, Vec3i volumeCoords, Octree<Lazy<Info, StarSystem>> volume) {
			this.galaxy = galaxy;
			this.random = random;
			this.volumeCoords = volumeCoords;
			this.volume = volume;
		}
	}

	public abstract void generateInto(Context ctx);

}
