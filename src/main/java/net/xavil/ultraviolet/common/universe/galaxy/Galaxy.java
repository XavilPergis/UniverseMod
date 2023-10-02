package net.xavil.ultraviolet.common.universe.galaxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.Rng;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.DensityFields;
import net.xavil.ultraviolet.common.universe.id.GalaxySectorId;
import net.xavil.ultraviolet.common.universe.id.UniverseSectorId;
import net.xavil.ultraviolet.common.universe.system.StarSystem;
import net.xavil.ultraviolet.common.universe.universe.Universe;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.math.matrices.Vec3;

public class Galaxy {

	public static class Info {
		public GalaxyType type;
		public double ageMya;

		public double maxMetallicity() {
			return 0.2;
		}
	}

	public final Universe parentUniverse;
	public final Info info;
	public final DensityFields densityFields;
	public final UniverseSectorId galaxyId;

	private final List<GalaxyGenerationLayer> generationLayers = new ArrayList<>();

	public final SectorManager sectorManager = new SectorManager(this);

	public Galaxy(Universe parentUniverse, UniverseSectorId galaxyId, Info info, DensityFields densityFields) {
		this.parentUniverse = parentUniverse;
		this.galaxyId = galaxyId;
		this.info = info;
		this.densityFields = densityFields;

		addGenerationLayer(new BaseGalaxyGenerationLayer(this, densityFields));
	}

	public void addGenerationLayer(GalaxyGenerationLayer layer) {
		for (final var other : this.generationLayers) {
			if (other.layerId == layer.layerId) {
				Mod.LOGGER.warn("tried to insert a galaxy generation layer with id {}, but it was already inserted!",
						layer.layerId);
				return;
			}
		}
		this.generationLayers.add(layer);
	}

	private long sectorSeed(SectorPos pos) {
		return FastHasher.withSeed(this.parentUniverse.getCommonUniverseSeed()).append(pos).currentHash();
	}

	public void tick(ProfilerFiller profiler) {
		this.sectorManager.tick(profiler);
	}

	public void generateSectorElements(GalaxySector.PackedElements out, SectorPos pos) {
		final var random = Rng.wrap(new Random(sectorSeed(pos)));
		final var ctx = new GalaxyGenerationLayer.Context(this, random, pos);

		for (int i = 0; i < this.generationLayers.size(); ++i) {
			final var genLayer = this.generationLayers.get(i);
			genLayer.generateInto(ctx, out);
		}

		out.shrinkToFit();
	}

	public GalaxySector.PackedElements generateSectorElements(SectorPos pos) {
		final var elements = new GalaxySector.PackedElements(pos.minBound());
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
