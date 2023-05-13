package net.xavil.universal;

import java.lang.annotation.Target;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ChunkPos;
import net.xavil.universal.common.block.ModBlocks;
import net.xavil.universal.common.dimension.DimensionCreationProperties;
import net.xavil.universal.common.dimension.DynamicDimensionManager;
import net.xavil.universal.common.item.StarmapItem;
import net.xavil.universal.common.level.EmptyChunkGenerator;
import net.xavil.universal.common.universe.Location;
import net.xavil.universal.common.universe.id.SystemNodeId;
import net.xavil.universal.common.universe.station.SpaceStation;
import net.xavil.universal.common.universe.station.StationLocation;
import net.xavil.universal.common.universe.station.StationLocation.OrbitingCelestialBody;
import net.xavil.universal.common.universe.universe.ServerUniverse;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;
import net.xavil.universal.networking.ModNetworking;
import net.xavil.universal.networking.ModPacket;
import net.xavil.universal.networking.c2s.ServerboundStationJumpPacket;
import net.xavil.universal.networking.c2s.ServerboundTeleportToLocationPacket;
import net.xavil.universal.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.universal.networking.s2c.ClientboundOpenStarmapPacket;
import net.xavil.universal.networking.s2c.ClientboundSpaceStationInfoPacket;
import net.xavil.universal.networking.s2c.ClientboundStationJumpBeginPacket;
import net.xavil.universal.networking.s2c.ClientboundSyncCelestialTimePacket;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.util.Assert;
import net.xavil.util.Disposable;
import net.xavil.util.Option;
import net.xavil.util.math.matrices.Vec3;

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

		Registry.register(Registry.CHUNK_GENERATOR, namespaced("empty"), EmptyChunkGenerator.CODEC);

		ModNetworking.SERVERBOUND_PLAY_HANDLER = (player, packet) -> player.server
				.execute(() -> handlePacket(player, packet));

		ModNetworking.REGISTER_PACKETS_EVENT.register(acceptor -> {
			// @formatter:off
			acceptor.clientboundPlay.register(ClientboundUniverseInfoPacket.class, ClientboundUniverseInfoPacket::new);
			acceptor.clientboundPlay.register(ClientboundOpenStarmapPacket.class, ClientboundOpenStarmapPacket::new);
			acceptor.clientboundPlay.register(ClientboundChangeSystemPacket.class, ClientboundChangeSystemPacket::new);
			acceptor.clientboundPlay.register(ClientboundSyncCelestialTimePacket.class, ClientboundSyncCelestialTimePacket::new);
			acceptor.clientboundPlay.register(ClientboundSpaceStationInfoPacket.class, ClientboundSpaceStationInfoPacket::new);
			acceptor.clientboundPlay.register(ClientboundStationJumpBeginPacket.class, ClientboundStationJumpBeginPacket::new);

			acceptor.serverboundPlay.register(ServerboundTeleportToLocationPacket.class, ServerboundTeleportToLocationPacket::new);
			acceptor.serverboundPlay.register(ServerboundStationJumpPacket.class, ServerboundStationJumpPacket::new);
			// @formatter:on
		});

		Registry.register(Registry.ITEM, namespaced("starmap"), STARMAP_ITEM);

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			ModDebugCommand.register(dispatcher, dedicated);
		});
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

		try (final var disposer = Disposable.scope()) {
			return universe.loadSystem(disposer, id.system())
					.flatMap(system -> Option.fromNullable(system.rootNode.lookup(id.nodeId())))
					.flatMap(node -> getDimProperties(server, node))
					.map(props -> createWorld(server, id, props));
		}
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
		final var info = new PacketInfo(sender);
		final var universe = MinecraftServerAccessor.getUniverse(server);

		if (packetUntyped instanceof ServerboundTeleportToLocationPacket packet) {

			var opLevel = server.getOperatorUserPermissionLevel();
			if (!sender.hasPermissions(opLevel))
				return;

			if (packet.location == null) {
				info.warn("tried to teleport to null location!");
				return;
			}

			if (packet.location instanceof Location.Unknown) {
				info.warn("tried to teleport to an unknown location!");
				return;
			} else if (packet.location instanceof Location.World world) {
				teleportToWorld(sender, world.id);
			} else if (packet.location instanceof Location.Station station) {
				teleportToStation(sender, station.id);
			}

			var changeSystemPacket = new ClientboundChangeSystemPacket(packet.location);
			sender.connection.send(changeSystemPacket);

			info.info("teleport to celestial location! {}", packet.location);
		} else if (packetUntyped instanceof ServerboundStationJumpPacket packet) {
			var opLevel = server.getOperatorUserPermissionLevel();
			if (packet.isJumpInstant && !sender.hasPermissions(opLevel))
				return;

			if (packet.target == null) {
				info.warn("tried to jump to null target!");
				return;
			}

			final SpaceStation station = universe.getStation(packet.stationId).unwrapOrNull();
			if (station == null) {
				info.warn("cannot jump, station {} does not exist.", packet.stationId);
				return;
			}

			// FIXME: verify that the system we want to jump to actually exists

			station.prepareForJump(packet.target.system(), packet.isJumpInstant);

			final var beginPacket = new ClientboundStationJumpBeginPacket(packet.stationId, packet.target,
					packet.isJumpInstant);
			server.getPlayerList().broadcastAll(beginPacket);
		}
	}

	private static class PacketInfo {
		public final ServerPlayer sender;
		private String prefix;

		public PacketInfo(ServerPlayer sender) {
			this.sender = sender;
			final var playerName = sender.getGameProfile().getName();
			final var playerUuid = sender.getStringUUID();
			this.prefix = "[" + playerUuid + " | " + playerName + "]";
		}

		public void warn(String message, Object... args) {
			log(Mod.LOGGER::warn, message, args);
		}

		public void info(String message, Object... args) {
			log(Mod.LOGGER::info, message, args);
		}

		private void log(BiConsumer<String, Object[]> logger, String message, Object... args) {
			final var args2 = new Object[args.length + 1];
			args2[0] = this.prefix;
			for (int i = 0; i < args.length; ++i)
				args2[i + 1] = args[i];
			logger.accept("{} " + message, args2);
		}

	}

	public static void teleportEntityToWorld(Entity entity, ServerLevel level, Vec3 pos, float yaw, float pitch) {
		double x = pos.x, y = pos.y, z = pos.z;
		yaw = Mth.wrapDegrees(yaw);
		pitch = Mth.wrapDegrees(pitch);

		if (entity instanceof ServerPlayer player) {
			final var chunkPos = new ChunkPos(new BlockPos(x, y, z));
			level.getChunkSource().addRegionTicket(TicketType.POST_TELEPORT, chunkPos, 1, player.getId());
			player.stopRiding();
			if (player.isSleeping())
				player.stopSleepInBed(true, true);
			player.teleportTo(level, x, y, z, yaw, pitch);
			player.setYHeadRot(yaw);
		} else {
			pitch = Mth.clamp(pitch, -90.0f, 90.0f);
			if (level == entity.level) {
				entity.moveTo(x, y, z, yaw, pitch);
				entity.setYHeadRot(yaw);
			} else {
				final var originalEntity = entity;
				entity = originalEntity.getType().create(level);
				if (entity == null)
					return;
				originalEntity.unRide();
				entity.restoreFrom(originalEntity);
				entity.moveTo(x, y, z, yaw, pitch);
				entity.setYHeadRot(yaw);
				originalEntity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
				level.addDuringTeleport(entity);
			}
		}

		if (!(entity instanceof LivingEntity) || !((LivingEntity) entity).isFallFlying()) {
			entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
			entity.setOnGround(true);
		}
		if (entity instanceof PathfinderMob mob) {
			mob.getNavigation().stop();
		}
	}

}
