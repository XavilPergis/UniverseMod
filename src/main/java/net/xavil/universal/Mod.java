package net.xavil.universal;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.material.Material;
import net.xavil.universal.common.TestBlock;
import net.xavil.universal.common.item.StarmapItem;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;
import net.xavil.universal.networking.ModPacket;
import net.xavil.universal.networking.ModServerNetworking;
import net.xavil.universal.networking.c2s.ServerboundTeleportToPlanetPacket;

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

			var universe = MinecraftServerAccessor.getUniverse(server);

			// universe.getSystemNode(packet.planetId);

			Mod.LOGGER.info("teleport to planet! " + packet.planetId);
		}
	}
}
