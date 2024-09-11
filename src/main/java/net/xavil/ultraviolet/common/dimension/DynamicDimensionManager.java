package net.xavil.ultraviolet.common.dimension;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.GlobalData;
import net.xavil.ultraviolet.common.PerLevelData;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.system.PlanetaryCelestialNode;
import net.xavil.ultraviolet.mixin.accessor.MinecraftServerAccessor;

public class DynamicDimensionManager {

	protected final MinecraftServer server;
	private final MutableList<ResourceKey<Level>> levelsToUnload = new Vector<>();

	public DynamicDimensionManager(MinecraftServer server) {
		this.server = server;
	}

	public static DynamicDimensionManager get(MinecraftServer server) {
		return MinecraftServerAccessor.getDimensionManager(server);
	}

	public static ResourceKey<Level> getKey(ResourceLocation location) {
		return ResourceKey.create(Registry.DIMENSION_REGISTRY, location);
	}

	public static ResourceKey<Level> getKey(String location) {
		return ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation("dynamic", location));
	}

	@Nullable
	public ServerLevel getLevel(ResourceKey<Level> name) {
		// NOTE: we cant just call MinecraftServer.getLevel because that contains a
		// mixin that may in turn reach this function, resulting in an infinite loop.
		return MinecraftServerAccessor.getLevels(this.server).get(name);
	}

	public ServerLevel getOrCreateLevel(ResourceKey<Level> name,
			Supplier<LevelStem> propertiesFactory) {
		final var level = getLevel(name);
		return level != null ? level : createLevel(name, propertiesFactory.get());
	}

	public void tick() {
		// TODO
		// for (final var key : this.levelsToUnload.iterable()) {
		// }
		this.levelsToUnload.clear();
	}

	public void scheduleUnload(ResourceKey<Level> name) {
		this.levelsToUnload.push(name);
	}

	// basically lifted from
	// https://github.com/McJtyMods/RFToolsDimensions/blob/1.18/src/main/java/mcjty/rftoolsdim/dimension/tools/DynamicDimensionManager.java
	public ServerLevel createLevel(ResourceKey<Level> name, LevelStem stem) {
		final var worldData = MinecraftServerAccessor.getWorldData(this.server);

		final var seed = BiomeManager.obfuscateSeed(worldData.worldGenSettings().seed());
		final var derivedLevelData = new DerivedLevelData(worldData, worldData.overworldData());

		// vanilla uses a value of 11 here, which is the radius of chunks to listen for.
		final var progressListener = MinecraftServerAccessor.getProgressListenerFactory(this.server).create(11);

		// writing to the level stem registry is *supposed* to make minecraft load the
		// registered world upon startup, but for some reason, it doesn't work. Dynamic
		// entries *are* saved to disk, but they seem to be ignored when loading, and i
		// cant figure out why. The load on startup behavior is a double-edged sword,
		// too. It can take a significant amount of time to load lots of worlds, many of
		// which may not need to be loaded.
		//
		// So, instead of this, we handle dynamic dimension creation ourselves. There is
		// a fundamental loss of imformation here, however, since we have to generate
		// the LevelStem from scratch every time. This isn't a huge deal for out mod,
		// since the level properties are deived from their galactic environment
		// anyways, which is pretty easy to query.

		final var newLevel = new ServerLevel(
				this.server,
				MinecraftServerAccessor.getExecutor(this.server),
				MinecraftServerAccessor.getStorageSource(this.server),
				derivedLevelData,
				name,
				stem.typeHolder(),
				progressListener,
				stem.generator(),
				false,
				seed,
				List.of(),
				false);

		// TODO: inform client of new world (seems to only be needed for commands that
		// need to know which dimensions exist)

		final var perLevelData = PerLevelData.get(newLevel);
		perLevelData.levelStem = stem;
		perLevelData.setDirty();

		final var borderListener = new BorderChangeListener.DelegateBorderChangeListener(newLevel.getWorldBorder());
		this.server.overworld().getWorldBorder().addListener(borderListener);
		MinecraftServerAccessor.getLevels(this.server).put(name, newLevel);

		final var globalData = GlobalData.get(this.server);
		final var dynamicLevels = globalData.dynamicLevels;
		if (dynamicLevels.indexOf(name.location()) < 0) {
			Mod.LOGGER.info("adding new dynamic level '{}' to saved list", name.location());
			dynamicLevels.push(name.location());
		}
		globalData.setDirty();

		return newLevel;
	}

	// the dynamic level should already be created (but not necessarily loaded)
	// before this function is called.
	public ServerLevel loadDynamicLevel(ResourceKey<Level> key) {
		// already loaded!
		if (getLevel(key) != null)
			return getLevel(key);

		Mod.LOGGER.info("loading dynamic level '{}'", key.location());

		// mirrors the creation of DimensionDataStorage in ServerChunkCache. allows us
		// to get at a world's saved data before the world is actually loaded.
		final var storageSource = MinecraftServerAccessor.getStorageSource(this.server);
		final var dataFolder = storageSource.getDimensionPath(key).resolve("data").toFile();
		final var dataStorage = new DimensionDataStorage(dataFolder, this.server.getFixerUpper());

		final var perLevelData = dataStorage.get(PerLevelData::load, PerLevelData.ID);
		if (perLevelData == null) {
			return null;
		}

		if (perLevelData.levelStem == null)
			recoverStem(perLevelData, key);
		if (perLevelData.levelStem == null)
			stemRecoveryFailed(perLevelData, key);

		return getOrCreateLevel(key, () -> perLevelData.levelStem);
	}

	private void stemRecoveryFailed(PerLevelData perLevelData, ResourceKey<Level> key) {
		if (perLevelData.levelStem != null) {
			throw new IllegalStateException("failed stem recovery with non-null stem?");
		}

		// FIXME: this is a destructive operation, it should not be automatically
		// applied! But it seems like a lot of work to give an option to exit out and
		// fix the world file...
		Mod.LOGGER.error(
				"Unable to recover level stem for level '{}'! Defaulting to overworld level stem...",
				key.location());
		final var stemRegistry = this.server.getWorldData().worldGenSettings().dimensions();
		perLevelData.levelStem = stemRegistry.get(LevelStem.OVERWORLD);
		perLevelData.setDirty();
	}

	private void recoverStem(PerLevelData perLevelData, ResourceKey<Level> key) {
		if (perLevelData.levelStem != null) {
			throw new IllegalStateException("trying to recover stem when it already exists?");
		}

		Mod.LOGGER.error("Level '{}' has no saved level stem!", key.location());

		// FIXME: recover stations with no level stems. This needs station data to be
		// persisted first, though.
		if (perLevelData.worldType instanceof WorldType.SystemNode type) {
			final var universe = MinecraftServerAccessor.getUniverse(this.server);
			try (final var disposer = Disposable.scope()) {
				final var node = universe.loadSystem(disposer, type.id.system())
						.flatMap(system -> Maybe.fromNullable(system.rootNode.lookup(type.id.nodeId())))
						.unwrapOrNull();

				if (node == null) {
					Mod.LOGGER.error("Node ID '{}' did not correspond to any system node!");
					return;
				}
				if (node instanceof PlanetaryCelestialNode planetNode) {
					final var props = planetNode.dimensionProperties(this.server);
					if (props == null) {
						Mod.LOGGER.error("Node ID '{}' was not a landable planet node!");
						return;
					}
					perLevelData.levelStem = props.get();
					perLevelData.setDirty();
					Mod.LOGGER.info("Successfully recovered level stem for level '{}'!", key.location());
				} else {
					Mod.LOGGER.error("Node ID '{}' was not a planet node!");
					return;
				}
			}
		}
	}

	public void loadSavedDynamicLevels() {
		final var levelIds = GlobalData.get(this.server).dynamicLevels;
		Mod.LOGGER.debug("all dynamic levels: {}", levelIds);
		final var serverLevelMap = MinecraftServerAccessor.getLevels(this.server);
		for (final var id : levelIds.iterable()) {
			final var levelKey = getKey(id);
			if (!serverLevelMap.containsKey(levelKey)) {
				final var level = loadDynamicLevel(levelKey);
				serverLevelMap.put(levelKey, level);
			}
		}
	}

}
