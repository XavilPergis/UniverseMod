package net.xavil.universal;

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.material.Material;
import net.xavil.universal.common.TestBlock;
import net.xavil.universal.common.dimension.DimensionCreationProperties;
import net.xavil.universal.common.dimension.DynamicDimensionManager;
import net.xavil.universal.common.item.StarmapItem;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;
import net.xavil.universal.networking.ModPacket;
import net.xavil.universal.networking.ModServerNetworking;
import net.xavil.universal.networking.c2s.ServerboundTeleportToPlanetPacket;
import net.xavil.universal.networking.s2c.ClientboundChangeSystemPacket;

public class Mod implements ModInitializer {

	public static final String MOD_ID = "universal";

	public static ResourceLocation namespaced(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final StarmapItem STARMAP_ITEM = new StarmapItem(
			new FabricItemSettings().group(CreativeModeTab.TAB_MISC));

	@Override
	public void onInitialize() {
		ModServerNetworking.register();

		Registry.register(Registry.BLOCK, namespaced("test_block"),
				new TestBlock(FabricBlockSettings.of(Material.METAL)));
		Registry.register(Registry.ITEM, namespaced("starmap"), STARMAP_ITEM);
	}

	public static void handlePacket(MinecraftServer server, ServerPlayer sender, ModPacket packetUntyped) {
		if (packetUntyped instanceof ServerboundTeleportToPlanetPacket packet) {
			var opLevel = server.getOperatorUserPermissionLevel();
			if (!sender.hasPermissions(opLevel))
				return;

			if (packet.planetId == null)
				return;

			var universe = MinecraftServerAccessor.getUniverse(server);
			var systemNode = universe.getSystemNode(packet.planetId);
			if (systemNode == null)
				return;

			// FIXME: save which planet each player is on and use that to sync.

			if (packet.planetId.equals(universe.getStartingSystemGenerator().getStartingSystemId())) {
				var p = server.overworld().getSharedSpawnPos();
				var yaw = sender.getRespawnAngle();
				sender.teleportTo(server.overworld(), p.getX(), p.getY(), p.getZ(), yaw, 0);
			} else {
				final var key = DynamicDimensionManager
						.getKey(new ResourceLocation("dynamic", packet.planetId.uniqueName()));
				final var manager = DynamicDimensionManager.get(server);
				final var newLevel = manager.getOrCreateLevel(key, () -> {
					var registryAccess = server.registryAccess();
					var type = registryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)
							.getOrCreateHolder(DimensionType.OVERWORLD_LOCATION);
					var generator = WorldGenSettings.makeDefaultOverworld(registryAccess, new Random().nextLong());
					return DimensionCreationProperties.basic(new LevelStem(type, generator));
				});

				LevelAccessor.setUniverseId(newLevel, packet.planetId);

				// TODO: find actual spawn pos
				sender.teleportTo(newLevel, 0, 100, 0, 0, 0);
			}

			var changeSystemPacket = new ClientboundChangeSystemPacket();
			changeSystemPacket.id = packet.planetId;
			ModServerNetworking.send(sender, changeSystemPacket);

			Mod.LOGGER.info("teleport to planet! " + packet.planetId);
		}
	}
}
