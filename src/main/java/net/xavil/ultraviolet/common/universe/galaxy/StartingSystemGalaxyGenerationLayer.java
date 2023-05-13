package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.Random;

import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.math.Interval;
import net.xavil.util.math.matrices.Vec3;

public class StartingSystemGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public static final int STARTING_LOCATION_SAMPLE_ATTEMPTS = 1000;
	public static final Interval STARTING_LOCATION_ACCEPTABLE_DENSITY = new Interval(0.01, 0.1);

	public final int startingNodeId;
	public StarSystem startingSystem;
	public StarSystem.Info startingSystemInfo;

	private boolean isLocationChosen = false;
	private final CelestialNode startingNode;
	private Vec3 startingSystemPos = Vec3.ZERO;
	// private Vec3i startingSystemCoords = Vec3i.ZERO;
	private SectorPos startingSystemSectorPos;
	private int elementIndex = -1;

	public StartingSystemGalaxyGenerationLayer(Galaxy parentGalaxy, CelestialNode startingNode, int startingNodeId) {
		super(parentGalaxy, 1);
		this.startingNodeId = startingNodeId;
		this.startingNode = startingNode;
	}

	private void chooseStartingLocation() {
		if (this.isLocationChosen)
			return;
		this.isLocationChosen = true;
		final var random = new Random(this.parentGalaxy.parentUniverse.getUniqueUniverseSeed() + 10);
		final var densityFields = this.parentGalaxy.densityFields;

		for (int i = 0; i < STARTING_LOCATION_SAMPLE_ATTEMPTS; ++i) {
			final var samplePos = Vec3.random(random,
					Vec3.broadcast(-densityFields.galaxyRadius),
					Vec3.broadcast(densityFields.galaxyRadius));
			final var density = densityFields.stellarDensity.sample(samplePos);
			if (STARTING_LOCATION_ACCEPTABLE_DENSITY.contains(density)) {
				this.startingSystemPos = samplePos;
				this.startingSystemSectorPos = SectorPos.fromPos(GalaxySector.ROOT_LEVEL, samplePos);
				Mod.LOGGER.info("placing starting system in sector at {} (in sector {})",
					this.startingSystemPos, this.startingSystemSectorPos.levelCoords());
				return;
			}
		}

		Mod.LOGGER.error("could not find suitable starting system location!");
		this.startingSystemPos = Vec3.ZERO;
		this.startingSystemSectorPos = SectorPos.fromPos(GalaxySector.ROOT_LEVEL, this.startingSystemPos);
		this.startingSystem = new StarSystem(this.parentGalaxy, this.startingSystemPos, this.startingNode);
		this.startingSystemInfo = StarSystem.Info.custom(4600, "Sol", this.startingSystem);
	}

	private void findElementIndex() {
		chooseStartingLocation();
		if (this.elementIndex == -1)
			this.parentGalaxy.generateSectorElements(this.startingSystemSectorPos);
	}

	@Override
	public void generateInto(Context ctx, Sink sink) {
		chooseStartingLocation();
		final var pos = this.startingSystemPos;
		if (!this.startingSystemSectorPos.equals(ctx.pos))
			return;
		this.elementIndex = sink.accept(pos, this.startingSystemInfo, 0);
	}

	public SystemNodeId getStartingSystemId() {
		findElementIndex();
		final var systemSectorId = GalaxySectorId.from(this.startingSystemSectorPos, this.elementIndex);
		final var system = new SystemId(this.parentGalaxy.galaxyId, systemSectorId);
		return new SystemNodeId(system, this.startingNodeId);
	}

	@Override
	public StarSystem generateFullSystem(GalaxySector.InitialElement elem) {
		return this.startingSystem;
	}

}
