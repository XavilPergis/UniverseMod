package net.xavil.universal.common.universe.galaxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.DensityFields;
import net.xavil.universal.common.universe.id.GalaxySectorId;
import net.xavil.universal.common.universe.id.UniverseSectorId;
import net.xavil.universal.common.universe.system.StarSystem;
import net.xavil.universal.common.universe.universe.Universe;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.util.FastHasher;
import net.xavil.util.Option;
import net.xavil.util.Disposable;
import net.xavil.util.collections.Vector;
import net.xavil.util.collections.interfaces.ImmutableList;
import net.xavil.util.math.Interval;

public class Galaxy {

	public static class Info {
		public GalaxyType type;
		public double ageMya;
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

	public ImmutableList<GalaxySector.InitialElement> generateSectorElements(SectorPos pos) {
		final var random = new Random(sectorSeed(pos));

		final var starFactor = 1.0 / Math.pow(1 << pos.level(), 3.0);
		final var ctx = new GalaxyGenerationLayer.Context(this, random, starFactor, pos, new Interval(0, 1));

		final var elements = new Vector<GalaxySector.InitialElement>();
		for (int i = 0; i < this.generationLayers.size(); ++i) {
			final var genLayer = this.generationLayers.get(i);
			final var i2 = i;
			genLayer.generateInto(ctx, (systemPos, info, seed) -> {
				final var index = elements.size();
				elements.push(new GalaxySector.InitialElement(systemPos, info, seed, i2));
				return index;
			});
		}
		elements.optimize();
		return elements;
	}

	public StarSystem generateFullSystem(GalaxySector sector, GalaxySector.InitialElement elem) {
		return this.generationLayers.get(elem.generationLayer()).generateFullSystem(elem.info(), elem.seed());
	}

	public Option<StarSystem> loadSystem(Disposable.Multi disposer, GalaxySectorId id) {
		final var systemTicket = this.sectorManager.createSystemTicket(disposer, id);
		return this.sectorManager.forceLoad(systemTicket);
	}

	public Option<StarSystem> getSystem(GalaxySectorId id) {
		return this.sectorManager.getSystem(id);
	}

	public Option<CelestialNode> getSystemNode(GalaxySectorId id, int nodeId) {
		return this.sectorManager.getSystem(id).flatMap(system -> Option.fromNullable(system.rootNode.lookup(nodeId)));
	}

}
