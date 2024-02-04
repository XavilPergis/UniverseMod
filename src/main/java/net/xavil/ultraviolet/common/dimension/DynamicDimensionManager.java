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
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
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

	public @Nullable ServerLevel getLevel(ResourceKey<Level> name) {
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

		this.server.overworld().getWorldBorder()
				.addListener(new BorderChangeListener.DelegateBorderChangeListener(newLevel.getWorldBorder()));
		MinecraftServerAccessor.getLevels(this.server).put(name, newLevel);

		return newLevel;
	}

}
