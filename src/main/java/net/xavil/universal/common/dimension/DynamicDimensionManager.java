package net.xavil.universal.common.dimension;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.mojang.serialization.Lifecycle;

import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;

public class DynamicDimensionManager {

	protected final MinecraftServer server;

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
			Supplier<DimensionCreationProperties> propertiesFactory) {
		final var level = getLevel(name);
		return level != null ? level : createLevel(name, propertiesFactory.get());
	}

	public ServerLevel createLevel(ResourceKey<Level> name, DimensionCreationProperties properties) {
		final var worldData = MinecraftServerAccessor.getWorldData(this.server);

		final var seed = BiomeManager.obfuscateSeed(worldData.worldGenSettings().seed());
		final var derivedLevelData = new DerivedLevelData(worldData, worldData.overworldData());

		// vanilla uses a value of 11 here, which is the radius of chunks to listen for.
		final var explicitListener = properties.progressListener();
		final var progressListener = explicitListener != null ? explicitListener
				: MinecraftServerAccessor.getProgressListenerFactory(this.server).create(11);

		final var stem = properties.levelStem();

		// TODO: what is the significance of this? is this registry synced to clients
		// upon modification?
		// direct holders don't even go through any registry, and are directly
		// replicated to clients, and as far as i can tell, reference holders point into
		// frozen registries, so they can only reference builtin and json dimensions. so
		// it seems like this shouldnt have any effect?
		if (worldData.worldGenSettings().dimensions() instanceof WritableRegistry<LevelStem> stemRegistry) {
			final var stemKey = ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, name.location());
			stemRegistry.register(stemKey, stem, Lifecycle.experimental());
		}

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
				properties.customSpawners(),
				properties.shouldTickTime());

		// TODO: inform client of new world (seems to only be needed for commands that
		// need to know which dimensions exist)

		this.server.overworld().getWorldBorder()
				.addListener(new BorderChangeListener.DelegateBorderChangeListener(newLevel.getWorldBorder()));
		MinecraftServerAccessor.getLevels(this.server).put(name, newLevel);

		return newLevel;
	}

}
