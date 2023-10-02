package net.xavil.ultraviolet;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.xavil.hawklib.Assert;
import net.xavil.hawklib.Disposable;
import net.xavil.hawklib.Maybe;
import net.xavil.ultraviolet.common.PerLevelData;
import net.xavil.ultraviolet.common.block.ModBlocks;
import net.xavil.ultraviolet.common.dimension.DynamicDimensionManager;
import net.xavil.ultraviolet.common.item.StarmapItem;
import net.xavil.ultraviolet.common.level.EmptyChunkGenerator;
import net.xavil.ultraviolet.common.level.ModChunkGenerator;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.id.SystemNodeId;
import net.xavil.ultraviolet.common.universe.station.SpaceStation;
import net.xavil.ultraviolet.debug.ModDebugCommand;
import net.xavil.ultraviolet.mixin.accessor.EntityAccessor;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftServerAccessor;
import net.xavil.ultraviolet.networking.ModNetworking;
import net.xavil.ultraviolet.networking.c2s.ServerboundDebugValueSetPacket;
import net.xavil.ultraviolet.networking.c2s.ServerboundStationJumpPacket;
import net.xavil.ultraviolet.networking.c2s.ServerboundTeleportToLocationPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundDebugValueSetPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundOpenStarmapPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundSpaceStationInfoPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundStationJumpBeginPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundSyncCelestialTimePacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundUniverseSyncPacket;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.hawklib.math.matrices.Vec3;

public class Mod implements ModInitializer {

	public static final String MOD_ID = "ultraviolet";

	public static ResourceLocation namespaced(String path) {
		return new ResourceLocation(MOD_ID, path);
	}

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final StarmapItem STARMAP_ITEM = new StarmapItem(
			new FabricItemSettings().group(CreativeModeTab.TAB_MISC));

	@Override
	public void onInitialize() {
		ModBlocks.register();

		Registry.register(Registry.CHUNK_GENERATOR, namespaced("empty"), EmptyChunkGenerator.CODEC);
		Registry.register(Registry.CHUNK_GENERATOR, namespaced("planet"), ModChunkGenerator.CODEC);

		ModNetworking.addServerboundHandler(ServerboundTeleportToLocationPacket.class, Mod::handlePacket);
		ModNetworking.addServerboundHandler(ServerboundStationJumpPacket.class, Mod::handlePacket);
		ModNetworking.addServerboundHandler(ServerboundDebugValueSetPacket.class, Mod::handlePacket);

		ModNetworking.REGISTER_PACKETS_EVENT.register(acceptor -> {
			// @formatter:off
			acceptor.clientboundPlay.register(ClientboundUniverseSyncPacket.class, ClientboundUniverseSyncPacket::new);
			acceptor.clientboundPlay.register(ClientboundOpenStarmapPacket.class, ClientboundOpenStarmapPacket::new);
			acceptor.clientboundPlay.register(ClientboundChangeSystemPacket.class, ClientboundChangeSystemPacket::new);
			acceptor.clientboundPlay.register(ClientboundSyncCelestialTimePacket.class, ClientboundSyncCelestialTimePacket::new);
			acceptor.clientboundPlay.register(ClientboundSpaceStationInfoPacket.class, ClientboundSpaceStationInfoPacket::new);
			acceptor.clientboundPlay.register(ClientboundStationJumpBeginPacket.class, ClientboundStationJumpBeginPacket::new);
			acceptor.clientboundPlay.register(ClientboundDebugValueSetPacket.class, ClientboundDebugValueSetPacket::new);

			acceptor.serverboundPlay.register(ServerboundTeleportToLocationPacket.class, ServerboundTeleportToLocationPacket::new);
			acceptor.serverboundPlay.register(ServerboundStationJumpPacket.class, ServerboundStationJumpPacket::new);
			acceptor.serverboundPlay.register(ServerboundDebugValueSetPacket.class, ServerboundDebugValueSetPacket::new);
			// @formatter:on
		});

		Registry.register(Registry.ITEM, namespaced("starmap"), STARMAP_ITEM);

		CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
			ModDebugCommand.register(dispatcher, dedicated);
		});

		ServerWorldEvents.LOAD.register((server, level) -> {
			Mod.LOGGER.error("LOAD {}", level);
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

	private static Maybe<Supplier<LevelStem>> getDimProperties(MinecraftServer server,
			CelestialNode node) {
		if (node instanceof PlanetaryCelestialNode planetNode)
			return Maybe.fromNullable(planetNode.dimensionProperties(server));
		return Maybe.none();
	}

	private static ServerLevel createWorld(MinecraftServer server, SystemNodeId id,
			Supplier<LevelStem> props) {
		final var manager = DynamicDimensionManager.get(server);
		final var universe = MinecraftServerAccessor.getUniverse(server);

		final var key = DynamicDimensionManager.getKey(id.uniqueName());
		final var newLevel = manager.getOrCreateLevel(key, props);

		LevelAccessor.setUniverse(newLevel, universe);
		LevelAccessor.setWorldType(newLevel, new WorldType.SystemNode(id));

		return newLevel;
	}

	private static Maybe<ServerLevel> getOrCreateWorld(MinecraftServer server, SystemNodeId id) {
		final var universe = MinecraftServerAccessor.getUniverse(server);
		if (id.equals(universe.getStartingSystemGenerator().getStartingSystemId())) {
			return Maybe.some(server.overworld());
		}

		try (final var disposer = Disposable.scope()) {
			return universe.loadSystem(disposer, id.system())
					.flatMap(system -> Maybe.fromNullable(system.rootNode.lookup(id.nodeId())))
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

	public static void handlePacket(ServerPlayer sender, ServerboundDebugValueSetPacket packet) {
		final var opLevel = sender.server.getOperatorUserPermissionLevel();
		if (!sender.hasPermissions(opLevel))
			return;
	}

	public static void handlePacket(ServerPlayer sender, ServerboundTeleportToLocationPacket packet) {
		final var server = sender.server;
		final var info = new PacketInfo(sender);

		var opLevel = server.getOperatorUserPermissionLevel();
		if (!sender.hasPermissions(opLevel))
			return;

		if (packet.location == null) {
			info.warn("tried to teleport to null location!");
			return;
		}

		if (packet.location instanceof WorldType.Unknown) {
			info.warn("tried to teleport to an unknown location!");
			return;
		} else if (packet.location instanceof WorldType.SystemNode world) {
			teleportToWorld(sender, world.id);
		} else if (packet.location instanceof WorldType.Station station) {
			teleportToStation(sender, station.id);
		}

		var changeSystemPacket = new ClientboundChangeSystemPacket(packet.location);
		sender.connection.send(changeSystemPacket);

		info.info("teleport to celestial location! {}", packet.location);
	}

	public static void handlePacket(ServerPlayer sender, ServerboundStationJumpPacket packet) {
		final var server = sender.server;
		final var info = new PacketInfo(sender);
		final var universe = MinecraftServerAccessor.getUniverse(server);

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

		station.prepareForJump(packet.target, packet.isJumpInstant);

		final var beginPacket = new ClientboundStationJumpBeginPacket(packet.stationId, packet.target,
				packet.isJumpInstant);
		server.getPlayerList().broadcastAll(beginPacket);
	}

	private static class PacketInfo {
		private String prefix;

		public PacketInfo(ServerPlayer sender) {
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

	public static void syncPlayer(ServerPlayer player) {
		final var universe = MinecraftServerAccessor.getUniverse(player.server);

		final var syncPacket = new ClientboundUniverseSyncPacket();
		// seeds
		syncPacket.commonSeed = universe.getCommonUniverseSeed();
		syncPacket.uniqueSeed = universe.getUniqueUniverseSeed();
		// starting system info
		final var ssg = universe.getStartingSystemGenerator();
		syncPacket.startingSystemAge = ssg.systemAge;
		syncPacket.startingSystemName = ssg.systemName;
		syncPacket.startingId = ssg.getStartingSystemId();
		syncPacket.startingSystemNbt = CelestialNode.writeNbt(ssg.startingSystem.rootNode);
		// current info
		syncPacket.worldType = EntityAccessor.getWorldType(player);
		player.connection.send(syncPacket);

		universe.syncTime(player, true);
	}

	private static void stemRecoveryFailed(MinecraftServer server, PerLevelData perLevelData, ResourceKey<Level> key) {
		if (perLevelData.levelStem != null) {
			throw new IllegalStateException();
		}

		// FIXME: this is a destructive operation, it should not be automatically
		// applied! But it seems like a lot of work to give an option to exit out and
		// fix the world file...
		Mod.LOGGER.error(
				"Unable to recover level stem for level '{}'! Defaulting to overworld level stem...",
				key.location());
		final var stemRegistry =server.getWorldData().worldGenSettings().dimensions();
		perLevelData.levelStem = stemRegistry.get(LevelStem.OVERWORLD);
		perLevelData.setDirty();
	}

	private static void recoverStem(MinecraftServer server, PerLevelData perLevelData, ResourceKey<Level> key) {
		if (perLevelData.levelStem != null) {
			throw new IllegalStateException();
		}

		Mod.LOGGER.error("Level '{}' has no saved level stem!", key.location());

		// FIXME: recover stations with no level stems. This needs station data to be
		// persisted first, though.
		if (perLevelData.worldType instanceof WorldType.SystemNode type) {
			final var universe = MinecraftServerAccessor.getUniverse(server);
			try (final var disposer = Disposable.scope()) {
				final var node = universe.loadSystem(disposer, type.id.system())
						.flatMap(system -> Maybe.fromNullable(system.rootNode.lookup(type.id.nodeId())))
						.unwrapOrNull();

				if (node == null) {
					Mod.LOGGER.error("Node ID '{}' did not correspond to any system node!");
					return;
				}
				if (node instanceof PlanetaryCelestialNode planetNode) {
					final var props = planetNode.dimensionProperties(server);
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

	public static ServerLevel loadDynamicLevel(MinecraftServer server, ResourceKey<Level> key) {
		final var storageSource = MinecraftServerAccessor.getStorageSource(server);
		final var dataFolder = storageSource.getDimensionPath(key).resolve("data").toFile();
		final var dataStorage = new DimensionDataStorage(dataFolder, server.getFixerUpper());

		final var perLevelData = dataStorage.get(PerLevelData::load, PerLevelData.ID);
		if (perLevelData == null) {
			return null;
		}

		if (perLevelData.levelStem == null)
			recoverStem(server, perLevelData, key);
		if (perLevelData.levelStem == null)
			stemRecoveryFailed(server, perLevelData, key);

		final var dimManager = MinecraftServerAccessor.getDimensionManager(server);
		return dimManager.getOrCreateLevel(key, () -> perLevelData.levelStem);
	}

}
