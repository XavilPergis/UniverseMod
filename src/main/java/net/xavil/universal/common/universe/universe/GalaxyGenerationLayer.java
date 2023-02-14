package net.xavil.universal.common.universe.universe;

import java.util.Random;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.Octree;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystem.Info;

public abstract class GalaxyGenerationLayer {

	public final int layerId;

	public GalaxyGenerationLayer(int layerId) {
		this.layerId = layerId;
	}

	public static final class Context {
		public final Galaxy galaxy;
		public final Random random;
		public final Vec3i volumeCoords;
		public final Vec3 volumeMin;
		public final Vec3 volumeMax;

		public Context(Galaxy galaxy, Random random, Vec3i volumeCoords, Octree<Lazy<Info, StarSystem>> volume) {
			this.galaxy = galaxy;
			this.random = random;
			this.volumeCoords = volumeCoords;
			this.volumeMin = volume.rootNode.min;
			this.volumeMax = volume.rootNode.max;
		}
	}

	public interface Sink {
		void accept(Vec3 pos, Lazy<StarSystem.Info, StarSystem> system);
	}

	public abstract void generateInto(Context ctx, Sink sink);

}
