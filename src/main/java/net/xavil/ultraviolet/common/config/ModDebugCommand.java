package net.xavil.ultraviolet.common.config;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.common.universe.WorldType;
import net.xavil.ultraviolet.common.universe.station.StationLocation;
import net.xavil.ultraviolet.common.universe.station.StationLocation.OrbitingCelestialBody;
import net.xavil.ultraviolet.common.universe.universe.ServerUniverse;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftServerAccessor;

public final class ModDebugCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, boolean dedicated) {
		final var baseCommand = literal("ultraviolet").requires(src -> src.hasPermission(2));

		baseCommand.then(createConfigSubcommand());

		baseCommand.then(literal("debug")
				.then(argument("action", StringArgumentType.string())
						.executes(ctx -> executeDebugAction(ctx, false))
						.then(argument("payload", NbtTagArgument.nbtTag())
								.executes(ctx -> executeDebugAction(ctx, true)))));

		baseCommand.then(literal("station")
				.then(literal("add")
						.then(argument("name", StringArgumentType.string())
								.executes(ModDebugCommand::executeStationAdd)))
				.then(literal("remove")
						.then(argument("name", StringArgumentType.string())
								.executes(ModDebugCommand::executeStationRemove)))
				.then(literal("tp")
						.then(argument("name", StringArgumentType.string())
								.executes(ModDebugCommand::executeStationTpImplicit))
						.then(argument("entities", EntityArgument.entities())
								.then(argument("name", StringArgumentType.string())
										.executes(ModDebugCommand::executeStationTpExplicit))))
				.then(literal("move")
						.then(argument("name", StringArgumentType.string())
								.executes(ModDebugCommand::executeStationMove))));

		baseCommand.then(literal("time")
				.then(literal("scale").then(argument("seconds_per_second", DoubleArgumentType.doubleArg())
						.executes(ModDebugCommand::executeTimeScale)))
				.then(literal("set").then(argument("seconds", DoubleArgumentType.doubleArg())
						.executes(ModDebugCommand::executeTimeSet)))
				.then(literal("add").then(argument("seconds", DoubleArgumentType.doubleArg())
						.executes(ModDebugCommand::executeTimeAdd))));

		dispatcher.register(baseCommand);
	}

	private static CommonConfig getCommonDebug(CommandContext<CommandSourceStack> ctx) {
		return ((MinecraftServerAccessor) ctx.getSource().getServer()).ultraviolet_getCommonDebug();
	}

	private static int executeDebugAction(CommandContext<CommandSourceStack> ctx, boolean hasPayload)
			throws CommandSyntaxException {
		final var action = StringArgumentType.getString(ctx, "action");
		final var payload = hasPayload ? NbtTagArgument.getNbtTag(ctx, "payload") : null;
		Mod.dispatchDebugAction(ctx.getSource().getPlayerOrException(), action, payload, true);
		return 1;
	}

	private static <T> void executeConfigSetCommand(ConfigKey<T> key, CommandContext<CommandSourceStack> ctx)
			throws CommandSyntaxException {
		final T value = ctx.getArgument("value", key.type.containedClass);
		final var oldValue = getCommonDebug(ctx).get(key, ctx.getSource().getPlayerOrException());
		getCommonDebug(ctx).setPlayer(key, ctx.getSource().getPlayerOrException(), value);
		final var message = String.format(
				"config value '%s' changed from '%s' to '%s'",
				key.keyId, oldValue, value);
		ctx.getSource().sendSuccess(new TextComponent(message), true);
	}

	private static <T> void executeConfigGetCommand(ConfigKey<T> key, CommandContext<CommandSourceStack> ctx)
			throws CommandSyntaxException {
		final var value = getCommonDebug(ctx).get(key, ctx.getSource().getPlayerOrException());
		final var message = String.format(
				"config value '%s' is set to '%s'",
				key.keyId, value);
		ctx.getSource().sendSuccess(new TextComponent(message), true);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> createConfigSubcommand() {
		final var builders = new Object() {
			LiteralArgumentBuilder<CommandSourceStack> set = literal("set");
			LiteralArgumentBuilder<CommandSourceStack> get = literal("get");
			// LiteralArgumentBuilder<CommandSourceStack> unset = literal("unset");
		};

		ConfigKey.enumerate(key -> {
			builders.set.then(literal(key.keyId).then(argument("value", key.type.argumentType).executes(ctx -> {
				executeConfigSetCommand(key, ctx);
				return 1;
			})));
			builders.get.then(literal(key.keyId).executes(ctx -> {
				executeConfigGetCommand(key, ctx);
				return 1;
			}));
		});
		final var builder = literal("config").then(builders.set).then(builders.get);
		return builder;
	}

	private static int executeTimeScale(CommandContext<CommandSourceStack> ctx) {
		final var level = ctx.getSource().getLevel();
		final var rate = DoubleArgumentType.getDouble(ctx, "seconds_per_second");
		if (LevelAccessor.getUniverse(level) instanceof ServerUniverse universe) {
			universe.celestialTimeRate = rate;
			universe.syncTime(false);
		}
		return 1;
	}

	private static int executeTimeSet(CommandContext<CommandSourceStack> ctx) {
		final var level = ctx.getSource().getLevel();
		final var time = DoubleArgumentType.getDouble(ctx, "seconds");
		if (LevelAccessor.getUniverse(level) instanceof ServerUniverse universe) {
			universe.celestialTime = time;
			universe.syncTime(true);
		}
		return 1;
	}

	private static int executeTimeAdd(CommandContext<CommandSourceStack> ctx) {
		final var level = ctx.getSource().getLevel();
		final var time = DoubleArgumentType.getDouble(ctx, "seconds");
		if (LevelAccessor.getUniverse(level) instanceof ServerUniverse universe) {
			universe.celestialTime += time;
			universe.syncTime(true);
		}
		return 1;
	}

	private static int executeStationAdd(CommandContext<CommandSourceStack> ctx) {
		final var level = ctx.getSource().getLevel();
		final var universe = LevelAccessor.getUniverse(level);
		final var location = LevelAccessor.getWorldType(level);
		final var name = StringArgumentType.getString(ctx, "name");

		if (universe.getStationByName(name).isSome()) {
			ctx.getSource().sendFailure(
					new TextComponent("could not create station '" + name + "' because it already exists!"));
			return 1;
		}

		if (location instanceof WorldType.SystemNode loc) {
			final var sloc = OrbitingCelestialBody.createDefault(universe, loc.id);
			if (sloc.isSome()) {
				universe.createStation(name, sloc.unwrap());
				ctx.getSource().sendSuccess(new TextComponent("created station around node " + loc.id), true);
			}
		} else if (location instanceof WorldType.Station loc) {
			universe.getStation(loc.id).ifSome(station -> {
				if (station.getLocation() instanceof StationLocation.OrbitingCelestialBody sloc) {
					final var newSloc = OrbitingCelestialBody.createDefault(universe, sloc.id);
					if (newSloc.isSome()) {
						universe.createStation(name, newSloc.unwrap());
						ctx.getSource().sendSuccess(new TextComponent("created station around node " + sloc.id), true);
					}
				}
			});
		} else {
			ctx.getSource().sendFailure(new TextComponent("connot create station: location invalid"));
		}
		return 1;
	}

	private static int executeStationRemove(CommandContext<CommandSourceStack> ctx) {
		ctx.getSource().sendFailure(new TextComponent("station removal is unimplemented."));
		return 1;
	}

	private static int executeStationTpExplicit(CommandContext<CommandSourceStack> ctx) {
		try {
			final var name = StringArgumentType.getString(ctx, "name");
			final var entities = EntityArgument.getEntities(ctx, "entities");
			return executeStationTp(ctx, name, entities);
		} catch (CommandSyntaxException ex) {
		}
		return 1;
	}

	private static int executeStationTpImplicit(CommandContext<CommandSourceStack> ctx) {
		final var name = StringArgumentType.getString(ctx, "name");
		final var entities = List.of(ctx.getSource().getEntity());
		return executeStationTp(ctx, name, entities);
	}

	private static int executeStationTp(CommandContext<CommandSourceStack> ctx, String name,
			Collection<? extends Entity> entities) {
		final var level = ctx.getSource().getLevel();
		final var universe = LevelAccessor.getUniverse(level);
		final var stationOpt = universe.getStationByName(name);
		stationOpt.ifSome(station -> {
			if (station.level instanceof ServerLevel newLevel) {
				for (final var entity : entities) {
					final var spawnPos = new Vec3(0, 128, 0);
					Mod.teleportEntityToWorld(entity, newLevel, spawnPos, 0, 0);
				}
			}
			ctx.getSource().sendSuccess(new TextComponent("teleported to station '" + station.name + "'"), true);
		});
		if (stationOpt.isNone()) {
			ctx.getSource()
					.sendFailure(new TextComponent("cannot teleport to station: '" + name + "' could not be found"));
		}
		return 1;
	}

	private static int executeStationMove(CommandContext<CommandSourceStack> ctx) {
		ctx.getSource().sendFailure(new TextComponent("station movement is unimplemented."));
		return 1;
	}

}
