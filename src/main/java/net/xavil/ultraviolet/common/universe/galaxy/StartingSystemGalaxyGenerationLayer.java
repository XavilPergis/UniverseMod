package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.Random;

import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.SystemId;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.matrices.Vec3;

public class StartingSystemGalaxyGenerationLayer extends GalaxyGenerationLayer {

	public static final int STARTING_LOCATION_SAMPLE_ATTEMPTS = 1000;
	public static final Interval STARTING_LOCATION_ACCEPTABLE_DENSITY = new Interval(0.01, 0.1);

	public final int startingNodeId;
	public StarSystem startingSystem;
	private final GalaxySector.SectorElementHolder startingSystemInfo = new GalaxySector.SectorElementHolder();

	private boolean isLocationChosen = false;
	private final CelestialNode startingNode;
	private SectorPos startingSystemSectorPos;
	private int elementIndex = -1;

	public StartingSystemGalaxyGenerationLayer(Galaxy parentGalaxy, CelestialNode startingNode, int startingNodeId) {
		super(parentGalaxy, 1);
		this.startingNodeId = startingNodeId;
		this.startingNode = startingNode;
	}

	private static StellarCelestialNode findPrimaryStar(StarSystem system) {
		StellarCelestialNode primaryStar = null;
		for (var child : system.rootNode.iterable()) {
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
		final var random = new Random(this.parentGalaxy.parentUniverse.getUniqueUniverseSeed() + 10);
		final var densityFields = this.parentGalaxy.densityFields;

		// TODO: fix this. currently, it always fails
		for (int i = 0; i < STARTING_LOCATION_SAMPLE_ATTEMPTS; ++i) {
			final var samplePos = Vec3.random(random,
					Vec3.broadcast(-densityFields.galaxyRadius),
					Vec3.broadcast(densityFields.galaxyRadius));
			final var density = densityFields.stellarDensity.sample(samplePos);
			if (STARTING_LOCATION_ACCEPTABLE_DENSITY.contains(density)) {
				Vec3.set(out, samplePos);
				return true;
			}
		}
		Mod.LOGGER.error("could not find suitable starting system location!");
		Vec3.set(out, Vec3.ZERO);
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

		this.startingSystem = new StarSystem("Sol", this.parentGalaxy, startingSystemPos.xyz(), this.startingNode);
		final var primaryStar = findPrimaryStar(this.startingSystem);
		this.startingSystemInfo.luminosityLsol = primaryStar.luminosityLsol;
		this.startingSystemInfo.massYg = primaryStar.massYg;
		this.startingSystemInfo.systemAgeMyr = 4600;
		this.startingSystemInfo.systemSeed = 0;
		this.startingSystemInfo.temperatureK = primaryStar.temperatureK;
	}

	private void findElementIndex() {
		chooseStartingLocation();
		if (this.elementIndex == -1)
			this.parentGalaxy.generateSectorElements(this.startingSystemSectorPos);
	}

	@Override
	public void generateInto(Context ctx, GalaxySector.PackedSectorElements elements) {
		chooseStartingLocation();
		if (!this.startingSystemSectorPos.equals(ctx.pos))
			return;

		elements.beginWriting(1);
		final var info = new GalaxySector.SectorElementHolder();
		info.loadCopyOf(this.startingSystemInfo);
		info.generationLayer = this.layerId;
		elements.store(info, elements.size());
		this.elementIndex = elements.size();
		elements.endWriting(1);
	}

	public SystemNodeId getStartingSystemId() {
		findElementIndex();
		final var systemSectorId = GalaxySectorId.from(this.startingSystemSectorPos, this.elementIndex);
		final var system = new SystemId(this.parentGalaxy.galaxyId, systemSectorId);
		return new SystemNodeId(system, this.startingNodeId);
	}

	@Override
	public StarSystem generateFullSystem(GalaxySector.SectorElementHolder elem) {
		return this.startingSystem;
	}

}
