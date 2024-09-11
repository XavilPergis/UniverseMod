package net.xavil.ultraviolet.common.universe.galaxy;

import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.SplittableRng;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.ultraviolet.common.universe.GalaxyParameters;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.UniversePosition;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.ultraviolet.common.universe.system.CelestialNode;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.hawklib.math.Interval;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.matrices.Vec3;

public class Galaxy {

	public static final Interval METALLICITY_RANGE = new Interval(1.42857e-06, 4.51753e-02);

	public static class Info {
		public final GalaxyType type;
		public final long seed;
		public final Quat orientation;
		public final double ageMyr;
		public final double radius; // Tm
		public final double irregularity;

		public Info(GalaxyType type, long seed, Quat orientation, double ageMyr, double radius, double irregularity) {
			this.type = type;
			this.seed = seed;
			this.orientation = orientation;
			this.ageMyr = ageMyr;
			this.radius = radius;
			this.irregularity = irregularity;
		}

		public double maxMetallicity() {
			return 0.05;
		}

		public GalaxyParameters createGalaxyParameters() {
			final var rng = new SplittableRng(this.seed);
			return this.type.createGalaxyParameters(this, rng);
		}
	}

	public final Universe parentUniverse;
	public final GalaxyParameters parameters;
	public final Info info;
	public final UniverseSectorId galaxyId;
	public final UniversePosition position;

	private final Vector<GalaxyGenerationLayer> generationLayers = new Vector<>();

	public final SectorManager sectorManager = new SectorManager(this);

	public Galaxy(Universe parentUniverse, UniverseSectorId galaxyId, UniversePosition position, Info info,
			GalaxyParameters parameters) {
		this.parentUniverse = parentUniverse;
		this.galaxyId = galaxyId;
		this.position = position;
		this.info = info;
		this.parameters = parameters;

		this.parentUniverse.addBaseGenerationLayers(this);
	}

	// don't add generation layers after the generation layers have been used to
	// generate stars.
	public void addGenerationLayer(GalaxyGenerationLayer layer) {
		this.generationLayers.reserveExact(1);
		if (layer instanceof StartingSystemGalaxyGenerationLayer) {
			this.generationLayers.insert(0, layer);
			layer.layerId = 0;
			for (int i = 1; i < this.generationLayers.size(); ++i) {
				final var otherLayer = this.generationLayers.get(i);
				otherLayer.layerId = i;
			}
		} else {
			layer.layerId = this.generationLayers.size();
			this.generationLayers.push(layer);
		}
	}

	public void tick(ProfilerFiller profiler) {
		this.sectorManager.tick(profiler);
	}

	public void generateSectorElements(GalaxySector.PackedElements out, SectorPos pos) {
		final var ctx = new GalaxyGenerationLayer.Context(this, pos);

		for (int i = 0; i < this.generationLayers.size(); ++i) {
			final var genLayer = this.generationLayers.get(i);
			genLayer.generateInto(ctx, out);
		}

		out.shrinkToFit();
	}

	public GalaxySector.PackedElements generateSectorElements(SectorPos pos) {
		final var elements = new GalaxySector.PackedElements(pos.minBound(), false);
		generateSectorElements(elements, pos);
		return elements;
	}

	public StarSystem generateFullSystem(GalaxySector sector, GalaxySectorId id, GalaxySector.ElementHolder elem) {
		return this.generationLayers.get(elem.generationLayer).generateFullSystem(sector, id, elem);
	}

	public Maybe<StarSystem> loadSystem(Disposable.Multi disposer, GalaxySectorId id) {
		final var systemTicket = this.sectorManager.createSystemTicket(disposer, id);
		return this.sectorManager.forceLoad(systemTicket);
	}

	public Maybe<StarSystem> getSystem(GalaxySectorId id) {
		return this.sectorManager.getSystem(id);
	}

	public Maybe<Vec3> getSystemPos(GalaxySectorId id) {
		final var elem = new GalaxySector.ElementHolder();
		if (this.sectorManager.loadElement(elem, id)) {
			return Maybe.some(elem.systemPosTm.xyz());
		}
		return Maybe.none();
	}

	public Maybe<CelestialNode> getSystemNode(GalaxySectorId id, int nodeId) {
		return this.sectorManager.getSystem(id).flatMap(system -> Maybe.fromNullable(system.rootNode.lookup(nodeId)));
	}

}
