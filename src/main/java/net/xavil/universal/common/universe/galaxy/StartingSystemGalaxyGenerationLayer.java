package net.xavil.universal.common.universe.galaxy;

import java.util.Random;

import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.universal.common.universe.id.SystemId;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.system.StarSystem.Info;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.math.Interval;
import net.xavil.util.math.Vec3;
import net.xavil.util.math.Vec3i;

public class StartingSystemGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public static final int STARTING_LOCATION_SAMPLE_ATTEMPTS = 1000;
	public static final Interval STARTING_LOCATION_ACCEPTABLE_DENSITY = new Interval(1, 2);

	public final int startingNodeId;
	public final StarSystem startingSystem;
	public final StarSystem.Info startingSystemInfo;

	private boolean isLocationChosen = false;
	private Vec3 startingSystemPos = Vec3.ZERO;
	private Vec3i startingSystemCoords = Vec3i.ZERO;
	private int elementIndex = -1;

	public StartingSystemGalaxyGenerationLayer(Galaxy parentGalaxy, CelestialNode startingNode, int startingNodeId) {
		super(parentGalaxy, 1);
		this.startingNodeId = startingNodeId;
		this.startingSystem = new StarSystem(parentGalaxy, startingNode);
		this.startingSystemInfo = StarSystem.Info.custom(4600, "Sol", this.startingSystem);
	}

	private void chooseStartingLocation() {
		if (this.isLocationChosen)
			return;
		this.isLocationChosen = true;
		final var random = new Random(this.parentGalaxy.parentUniverse.getUniqueUniverseSeed());
		final var densityFields = this.parentGalaxy.densityFields;

		for (int i = 0; i < STARTING_LOCATION_SAMPLE_ATTEMPTS; ++i) {
			final var samplePos = Vec3.random(random,
					Vec3.broadcast(-densityFields.galaxyRadius),
					Vec3.broadcast(densityFields.galaxyRadius));
			final var density = densityFields.stellarDensity.sampleDensity(samplePos);
			if (STARTING_LOCATION_ACCEPTABLE_DENSITY.contains(density)) {
				this.startingSystemPos = samplePos;
				this.startingSystemCoords = GalaxySector.levelCoordsForPos(GalaxySector.ROOT_LEVEL, samplePos);
				return;
			}
		}

		Mod.LOGGER.error("could not find suitable starting system location!");
	}

	private void findElementIndex() {
		chooseStartingLocation();
		if (this.elementIndex == -1)
			this.parentGalaxy.generateSectorElements(new SectorPos(GalaxySector.ROOT_LEVEL, this.startingSystemCoords));
	}

	@Override
	public void generateInto(Context ctx, Sink sink) {
		chooseStartingLocation();
		final var pos = this.startingSystemPos;
		if (ctx.level != GalaxySector.ROOT_LEVEL)
			return;
		if (pos.x < ctx.volumeMin.x || pos.x >= ctx.volumeMax.x
				|| pos.y < ctx.volumeMin.y || pos.y >= ctx.volumeMax.y
				|| pos.z < ctx.volumeMin.z || pos.z >= ctx.volumeMax.z)
			return;
		this.elementIndex = sink.accept(pos, this.startingSystemInfo, 0);
	}

	public SystemNodeId getStartingSystemId() {
		findElementIndex();
		final var systemSectorId = GalaxySectorId.from(GalaxySector.ROOT_LEVEL, this.startingSystemCoords, this.layerId, this.elementIndex);
		final var system = new SystemId(this.parentGalaxy.galaxyId, systemSectorId);
		return new SystemNodeId(system, this.startingNodeId);
	}

	@Override
	public StarSystem generateFullSystem(Info systemInfo, long systemSeed) {
		return this.startingSystem;
	}

}
