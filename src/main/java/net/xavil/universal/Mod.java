package net.xavil.universal;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.xavil.universal.common.block.ModBlocks;
import net.xavil.universal.common.dimension.DimensionCreationProperties;
import net.xavil.universal.common.dimension.DynamicDimensionManager;
import net.xavil.universal.common.item.StarmapItem;
import net.xavil.universal.common.universe.Location;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;
import net.xavil.universal.networking.ModNetworking;
import net.xavil.universal.networking.ModPacket;
import net.xavil.universal.networking.c2s.ServerboundTeleportToLocationPacket;
import net.xavil.universal.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.universal.networking.s2c.ClientboundOpenStarmapPacket;
import net.xavil.universal.networking.s2c.ClientboundSyncCelestialTimePacket;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.util.Assert;
import net.xavil.util.Disposable;
import net.xavil.util.Option;

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
		ModBlocks.register();

		ModNetworking.SERVERBOUND_PLAY_HANDLER = (player, packet) -> player.server
				.execute(() -> handlePacket(player, packet));

		ModNetworking.REGISTER_PACKETS_EVENT.register(acceptor -> {
			acceptor.clientboundPlay.register(ClientboundUniverseInfoPacket.class, ClientboundUniverseInfoPacket::new);
			acceptor.clientboundPlay.register(ClientboundOpenStarmapPacket.class, ClientboundOpenStarmapPacket::new);
			acceptor.clientboundPlay.register(ClientboundChangeSystemPacket.class, ClientboundChangeSystemPacket::new);
			acceptor.clientboundPlay.register(ClientboundSyncCelestialTimePacket.class,
					ClientboundSyncCelestialTimePacket::new);

			acceptor.serverboundPlay.register(ServerboundTeleportToLocationPacket.class,
					ServerboundTeleportToLocationPacket::new);
		});

		Registry.register(Registry.ITEM, namespaced("starmap"), STARMAP_ITEM);

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			dispatcher.register(literal("universal")
					.then(literal("station")
							.then(literal("add")
									.then(argument("name", StringArgumentType.string()))
									.executes(this::executeStationAdd))
							.then(literal("remove")
									.then(argument("name", StringArgumentType.string()))
									.executes(this::executeStationRemove))
							.then(literal("tp")
									.then(argument("entity", EntityArgument.entity()))
									.then(argument("name", StringArgumentType.string()))
									.executes(this::executeStationTp))
							.then(literal("move")
									.then(argument("name", StringArgumentType.string()))
									.executes(this::executeStationTp))));
		});
	}

	private int executeStationAdd(CommandContext<CommandSourceStack> ctx) {
		final var level = ctx.getSource().getLevel();
		return 1;
	}

	private int executeStationRemove(CommandContext<CommandSourceStack> ctx) {
		return 1;
	}

	private int executeStationTp(CommandContext<CommandSourceStack> ctx) {
		return 1;
	}

	private static void teleportToStation(ServerPlayer sender, int id) {
		final var server = sender.server;
		final var universe = MinecraftServerAccessor.getUniverse(server);

		final var station = universe.getStation(id);
		if (station.isNone()) {
			final var playerName = sender.getGameProfile().getName();
			final var playerUuid = sender.getStringUUID();
			Mod.LOGGER.warn("{} ({}) tried to teleport to a non-existent station with id {}.", playerName, playerUuid,
					id);
			return;
		}

		if (station.unwrap().level instanceof ServerLevel level) {
			sender.teleportTo(level, 0, 0, 0, 0, 0);
		} else {
			Assert.isUnreachable();
		}
	}

	private static Option<Supplier<DimensionCreationProperties>> getDimProperties(MinecraftServer server,
			CelestialNode node) {
		if (node instanceof PlanetaryCelestialNode planetNode)
			return Option.fromNullable(planetNode.dimensionProperties(server));
		return Option.none();
	}

	private static ServerLevel createWorld(MinecraftServer server, SystemNodeId id,
			Supplier<DimensionCreationProperties> props) {
		final var manager = DynamicDimensionManager.get(server);
		final var universe = MinecraftServerAccessor.getUniverse(server);

		final var key = DynamicDimensionManager.getKey(id.uniqueName());
		final var newLevel = manager.getOrCreateLevel(key, props);

		((LevelAccessor) newLevel).universal_setUniverse(universe);
		((LevelAccessor) newLevel).universal_setLocation(new Location.World(id));

		return newLevel;
	}

	private static Option<ServerLevel> getOrCreateWorld(MinecraftServer server, SystemNodeId id) {
		final var universe = MinecraftServerAccessor.getUniverse(server);
		if (id.equals(universe.getStartingSystemGenerator().getStartingSystemId())) {
			return Option.some(server.overworld());
		}

		return Disposable.scope(disposer -> {
			return universe.loadSystem(disposer, id.system())
					.flatMap(system -> Option.fromNullable(system.rootNode.lookup(id.nodeId())))
					.flatMap(node -> getDimProperties(server, node))
					.map(props -> createWorld(server, id, props));
		});
	}

	private static void teleportToWorld(ServerPlayer sender, SystemNodeId id) {
		final var server = sender.server;
		final var level = getOrCreateWorld(server, id).unwrapOrNull();
		if (level != null) {
			final var p = level.getSharedSpawnPos();
			final var yaw = sender.getRespawnAngle();
			sender.teleportTo(level, p.getX(), p.getY(), p.getZ(), yaw, 0);
		} else {
			final var playerName = sender.getGameProfile().getName();
			final var playerUuid = sender.getStringUUID();
			Mod.LOGGER.warn("{} ({}) tried to teleport to non-landable node {}", playerName, playerUuid, id);
		}
	}

	public static void handlePacket(ServerPlayer sender, ModPacket<?> packetUntyped) {
		final var server = sender.server;
		if (packetUntyped instanceof ServerboundTeleportToLocationPacket packet) {
			final var playerName = sender.getGameProfile().getName();
			final var playerUuid = sender.getStringUUID();

			var opLevel = server.getOperatorUserPermissionLevel();
			if (!sender.hasPermissions(opLevel))
				return;

			if (packet.location == null) {
				Mod.LOGGER.warn("{} ({}) tried to teleport to null location!", playerName, playerUuid);
				return;
			}

			if (packet.location instanceof Location.Unknown) {
				Mod.LOGGER.warn("{} ({}) tried to teleport to an unknown location!", playerName, playerUuid);
				return;
			} else if (packet.location instanceof Location.World world) {
				teleportToWorld(sender, world.id);
			} else if (packet.location instanceof Location.Station station) {
				teleportToStation(sender, station.id);
			}

			var changeSystemPacket = new ClientboundChangeSystemPacket(packet.location);
			sender.connection.send(changeSystemPacket);

			Mod.LOGGER.info("teleport to celestial location! " + packet.location);
		}
	}
}
