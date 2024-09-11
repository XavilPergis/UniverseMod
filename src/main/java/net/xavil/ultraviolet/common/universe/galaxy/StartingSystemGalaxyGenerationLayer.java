package net.xavil.ultraviolet.common.universe.galaxy;

import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.system.CelestialNode;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.system.StellarCelestialNode;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Vec3;

public class StartingSystemGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public static final int STARTING_LOCATION_SAMPLE_ATTEMPTS = 1000;
	public static final Interval STARTING_LOCATION_ACCEPTABLE_DENSITY = new Interval(0.003, 0.005);

	public final int startingNodeId;
	public StarSystem startingSystem;
	private final GalaxySector.ElementHolder startingSystemInfo = new GalaxySector.ElementHolder();

	public final double systemAge;
	public final String systemName;

	private boolean isLocationChosen = false;
	private final CelestialNode startingNode;
	private SectorPos startingSystemSectorPos;
	private int elementIndex = -1;

	public StartingSystemGalaxyGenerationLayer(Galaxy parentGalaxy,
			double systemAge, String systemName,
			CelestialNode startingNode, int startingNodeId) {
		super(parentGalaxy, false);
		this.startingNodeId = startingNodeId;
		this.startingNode = startingNode;
		this.systemAge = systemAge;
		this.systemName = systemName;
	}

	private static StellarCelestialNode findPrimaryStar(CelestialNode node) {
		StellarCelestialNode primaryStar = null;
		for (final var child : node.iterable()) {
			if (child instanceof StellarCelestialNode starNode) {
				if (primaryStar == null)
					primaryStar = starNode;
				else if (starNode.luminosityLsol > primaryStar.luminosityLsol)
					primaryStar = starNode;
			}
		}
		return primaryStar;
	}

	private boolean pickLocation(Vec3.Mutable out) {
		final var rng = new SplittableRng(this.galaxy.parentUniverse.getUniqueUniverseSeed());
		rng.advanceWith("pick_starting_system_location");

		final var galaxyParams = this.galaxy.parameters;
		final var volumeMin = Vec3.broadcast(-galaxyParams.galaxyRadius).mul(1, 0.25, 1);
		final var volumeMax = Vec3.broadcast(galaxyParams.galaxyRadius).mul(1, 0.25, 1);

		// TODO: fix this. currently, it always fails
		final var tmp = new GalaxyRegionWeights();
		// final var avoidanceWeights = new GalaxyRegionWeights();
		// avoidanceWeights.halo = 1;
		// avoidanceWeights.core = 0.8;
		// avoidanceWeights.arms = 0.8;
		// avoidanceWeights.disc = 0.0;

		final var bestPosIncludingCore = new Vec3.Mutable();
		for (int i = 0; i < STARTING_LOCATION_SAMPLE_ATTEMPTS; ++i) {
			rng.advance();
			final var samplePos = Vec3.random(rng, volumeMin, volumeMax);
			galaxyParams.stellarDensityField.evaluate(samplePos, tmp);
			final var density = tmp.totalWeight();
			// final var avoidanceScore = GalaxyRegionWeights.dot(avoidanceWeights, tmp);
			if (new Interval(0.003, 0.005).contains(density)) {
				if (tmp.disc / tmp.core > 1e2) {
					Vec3.set(out, samplePos);
					return true;
				}
				Vec3.set(bestPosIncludingCore, samplePos);
			}
		}
		Vec3.set(out, bestPosIncludingCore);
		Mod.LOGGER.error("could not find suitable starting system location!");
		return false;
	}

	private void chooseStartingLocation() {
		if (this.isLocationChosen)
			return;
		this.isLocationChosen = true;

		final var startingSystemPos = new Vec3.Mutable();
		this.startingSystemInfo.systemPosTm = startingSystemPos;

		pickLocation(startingSystemPos);

		this.startingSystemSectorPos = SectorPos.fromPos(GalaxySector.ROOT_LEVEL,
				startingSystemPos.xyz());

		Mod.LOGGER.info("placing starting system in sector at {} (in sector {})",
				startingSystemPos, this.startingSystemSectorPos.levelCoords());

		final var primaryStar = findPrimaryStar(this.startingNode);
		this.startingSystemInfo.luminosityLsol = primaryStar.luminosityLsol;
		this.startingSystemInfo.massYg = primaryStar.massYg;
		this.startingSystemInfo.systemAgeMyr = this.systemAge;
		this.startingSystemInfo.systemSeed = 0xf100f;
		this.startingSystemInfo.temperatureK = primaryStar.temperature;
		this.startingSystemInfo.metallicity = 1.42857e-02;

		// having to specify the ID here is annoying and janky, and is impossible to
		// know unless we guarantee that this generation layer always runs first and
		// thus emits the starting system as sector element 0.

		final var rng = new SplittableRng(this.startingSystemInfo.systemSeed);
		final var orientation = Quat.randomUnit(rng.rng("orientation"));

		final var id = GalaxySectorId.from(this.startingSystemSectorPos, 0);
		this.startingSystem = new StarSystem(id, this.galaxy, this.startingSystemInfo,
				this.startingNode, orientation, this.systemName);
	}

	private void findElementIndex() {
		chooseStartingLocation();
		if (this.elementIndex == -1)
			this.galaxy.generateSectorElements(this.startingSystemSectorPos);
	}

	@Override
	public void generateInto(Context ctx, GalaxySector.PackedElements elements) {
		chooseStartingLocation();
		if (!this.startingSystemSectorPos.equals(ctx.pos))
			return;

		elements.reserve(1);
		final var info = new GalaxySector.ElementHolder();
		info.loadCopyOf(this.startingSystemInfo);
		info.generationLayer = this.layerId;
		elements.store(info, elements.size());
		this.elementIndex = elements.size();
		elements.markWritten(1);
	}

	public SystemNodeId getStartingSystemId() {
		findElementIndex();
		final var systemSectorId = GalaxySectorId.from(this.startingSystemSectorPos, this.elementIndex);
		final var system = new SystemId(this.galaxy.galaxyId, systemSectorId);
		return new SystemNodeId(system, this.startingNodeId);
	}

	public void copyStartingSystemInfo(GalaxySector.ElementHolder out) {
		chooseStartingLocation();
		out.loadCopyOf(this.startingSystemInfo);
	}

	@Override
	public StarSystem generateFullSystem(GalaxySector sector, GalaxySectorId id, GalaxySector.ElementHolder elem) {
		return this.startingSystem;
	}

}
