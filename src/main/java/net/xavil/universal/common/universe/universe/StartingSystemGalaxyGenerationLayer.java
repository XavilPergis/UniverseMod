package net.xavil.universal.common.universe.universe;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.common.universe.Lazy;
import net.xavil.universal.common.universe.UniverseId;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystemNode;

public class StartingSystemGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public final UniverseId.SectorId startingGalaxyId;
	public final Vec3i startingSystemVolumePos;
	public final StarSystemNode startingSystem;
	public final int startingNodeId;
	private int startingSystemId = -1;

	public StartingSystemGalaxyGenerationLayer(UniverseId.SectorId startingGalaxyId,
			Vec3i startingSystemVolumePos, StarSystemNode startingSystem, int startingNodeId) {
		this.startingGalaxyId = startingGalaxyId;
		this.startingSystemVolumePos = startingSystemVolumePos;
		this.startingSystem = startingSystem;
		this.startingNodeId = startingNodeId;
	}

	private static Vec3 randomVec(Random random) {
		var x = random.nextDouble(0, Galaxy.TM_PER_SECTOR);
		var y = random.nextDouble(0, Galaxy.TM_PER_SECTOR);
		var z = random.nextDouble(0, Galaxy.TM_PER_SECTOR);
		return new Vec3(x, y, z);
	}

	@Override
	public void generateInto(Context ctx) {
		if (!ctx.galaxy.galaxyId.equals(this.startingGalaxyId))
			return;
		if (!ctx.volumeCoords.equals(this.startingSystemVolumePos))
			return;
		var pos = ctx.volume.rootNode.min.add(randomVec(ctx.random));
		var system = new StarSystem(ctx.galaxy, this.startingSystem);
		var init = new StarSystem.Info();
		init.remainingHydrogenYg = 0;
		init.name = "Sol";
		init.systemAgeMya = 4600;
		init.stars = new ArrayList<>();
		this.startingSystem.visit(node -> {
			if (node instanceof StarNode starNode) {
				init.stars.add(starNode);
			}
		});
		this.startingSystemId = ctx.volume.insert(pos, new Lazy<>(init, n -> system));
	}

	public UniverseId getStartingSystemId() {
		return new UniverseId(this.startingGalaxyId,
				new UniverseId.SectorId(this.startingSystemVolumePos, this.startingSystemId),
				this.startingNodeId);
	}

}
