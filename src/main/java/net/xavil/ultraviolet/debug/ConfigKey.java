package net.xavil.ultraviolet.debug;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.arguments.NbtTagArgument;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.xavil.hawklib.collections.interfaces.MutableMap;

public final class ConfigKey<T> {

	public enum Side {
		CLIENT,
		SERVER,
	}

	public static final class Type<T> {
		public final Class<T> containedClass;
		public final ArgumentType<T> argumentType;
		public final Function<Tag, T> readNbt;
		public final Function<T, Tag> writeNbt;

		public Type(Class<T> containedClass, ArgumentType<T> argumentType, Function<Tag, T> readNbt,
				Function<T, Tag> writeNbt) {
			this.containedClass = containedClass;
			this.argumentType = argumentType;
			this.readNbt = readNbt;
			this.writeNbt = writeNbt;
		}

		public <C> Tag toNbt(CommandContext<C> ctx, String name) {
			return this.writeNbt.apply(ctx.getArgument(name, this.containedClass));
		}

		public static final Type<Boolean> BOOLEAN = new Type<>(
				boolean.class, BoolArgumentType.bool(),
				nbt -> nbt instanceof ByteTag tag ? tag.getAsByte() != 0 : null,
				value -> value == null ? null : ByteTag.valueOf(value));
		public static final Type<Float> FLOAT = new Type<>(
				float.class, FloatArgumentType.floatArg(),
				nbt -> nbt instanceof FloatTag tag ? tag.getAsFloat() : null,
				value -> value == null ? null : FloatTag.valueOf(value));
		public static final Type<Double> DOUBLE = new Type<>(
				double.class, DoubleArgumentType.doubleArg(),
				nbt -> nbt instanceof DoubleTag tag ? tag.getAsDouble() : null,
				value -> value == null ? null : DoubleTag.valueOf(value));
		public static final Type<Integer> INTEGER = new Type<>(
				int.class, IntegerArgumentType.integer(),
				nbt -> nbt instanceof IntTag tag ? tag.getAsInt() : null,
				value -> value == null ? null : IntTag.valueOf(value));
		public static final Type<String> STRING = new Type<>(
				String.class, StringArgumentType.string(),
				nbt -> nbt instanceof StringTag tag ? tag.getAsString() : null,
				value -> value == null ? null : StringTag.valueOf(value));
		public static final Type<Tag> NBT = new Type<>(
				Tag.class, NbtTagArgument.nbtTag(),
				Function.identity(), Function.identity());
	}

	private static final MutableMap<String, ConfigKey<?>> KEY_MAP = MutableMap.hashMap();
	public final String keyId;
	public final Type<T> type;
	public final T defaultValue;
	public final Side side;

	public ConfigKey(String keyId, Type<T> type, T defaultValue, Side side) {
		this.keyId = keyId;
		this.type = type;
		this.defaultValue = defaultValue;
		this.side = side;
	}

	public static <T> ConfigKey<T> register(ConfigKey<T> key) {
		KEY_MAP.insert(key.keyId, key);
		return key;
	}

	@Nullable
	public static ConfigKey<?> lookup(String keyId) {
		return KEY_MAP.get(keyId).unwrapOrNull();
	}

	public static void enumerate(Consumer<ConfigKey<?>> consumer) {
		KEY_MAP.values().forEach(consumer);
	}

	public static ConfigKey<Boolean> booleanKey(String id, boolean defaultValue, Side side) {
		return register(new ConfigKey<Boolean>(id, Type.BOOLEAN, defaultValue, side));
	}

	public static ConfigKey<Float> floatKey(String id, float defaultValue, Side side) {
		return register(new ConfigKey<Float>(id, Type.FLOAT, defaultValue, side));
	}

	public static ConfigKey<Double> doubleKey(String id, double defaultValue, Side side) {
		return register(new ConfigKey<Double>(id, Type.DOUBLE, defaultValue, side));
	}

	public static ConfigKey<Integer> intKey(String id, int defaultValue, Side side) {
		return register(new ConfigKey<Integer>(id, Type.INTEGER, defaultValue, side));
	}

	// @formatter:off
	public static final ConfigKey<Boolean> SHOW_SECTOR_BOUNDARIES             = booleanKey("showSectorBoundaries",          false, Side.CLIENT);
	public static final ConfigKey<Boolean> SECTOR_TICKET_AROUND_FOCUS         = booleanKey("sectorTicketAroundFocus",       false, Side.CLIENT);
	public static final ConfigKey<Boolean> SHOW_LINE_LODS                     = booleanKey("showLineLods",                  false, Side.CLIENT);
	public static final ConfigKey<Boolean> SHOW_ALL_LINE_LODS                 = booleanKey("showAllLineLods",               false, Side.CLIENT);
	public static final ConfigKey<Integer> GRID_LINE_SUBDIVISIONS             = intKey    ("gridLineSubdivisions",          2,     Side.CLIENT);
	public static final ConfigKey<Double>  SKY_CAMERA_NEAR_PLANE              = doubleKey ("skyCameraNearPlane",            1e2,   Side.CLIENT);
	public static final ConfigKey<Double>  SKY_CAMERA_FAR_PLANE               = doubleKey ("skyCameraFarPlane",             1e10,  Side.CLIENT);
	public static final ConfigKey<Boolean> FORCE_STAR_RENDERER_IMMEDIATE_MODE = booleanKey("forceStarRendererImediateMode", false, Side.CLIENT);

	public static final ConfigKey<Integer> GALAXY_PARTILE_ATTEMPT_COUNT = intKey("galaxyParticleAttemptCount", 1000000, Side.CLIENT);
	public static final ConfigKey<Integer> GALAXY_PARTILE_MAX_PARTICLES = intKey("galaxyParticleMaxParticles", 2500,    Side.CLIENT);

	public static final ConfigKey<Float> STAR_SHADER_STAR_MIN_SIZE           = floatKey("starShaderStarMinSize",          5f,    Side.CLIENT);
	public static final ConfigKey<Float> STAR_SHADER_STAR_MAX_SIZE           = floatKey("starShaderStarMaxSize",          8f,    Side.CLIENT);
	public static final ConfigKey<Float> STAR_SHADER_STAR_SIZE_SQUASH_FACTOR = floatKey("starShaderStarSizeSquashFactor", 250f,  Side.CLIENT);
	public static final ConfigKey<Float> STAR_SHADER_STAR_BRIGHTNESS_FACTOR  = floatKey("starShaderStarBrightnessFactor", 2e10f, Side.CLIENT);
	public static final ConfigKey<Float> STAR_SHADER_DIM_STAR_MIN_ALPHA      = floatKey("starShaderDimStarMinAlpha",      0.1f,  Side.CLIENT);
	public static final ConfigKey<Float> STAR_SHADER_DIM_STAR_EXPONENT       = floatKey("starShaderDimStarExponent",      0.1f,  Side.CLIENT);

	public static final ConfigKey<Double>  MIN_GRAVITY       = doubleKey ("minGravity",      0.2,   Side.SERVER);
	public static final ConfigKey<Double>  MAX_GRAVITY       = doubleKey ("maxGravity",      1.2,   Side.SERVER);
	public static final ConfigKey<Boolean> USE_FIXED_GRAVITY = booleanKey("useFixedGravity", false, Side.SERVER);
	public static final ConfigKey<Double>  FIXED_GRAVITY     = doubleKey ("fixedGravity",    1.0,   Side.SERVER);
	// @formatter:on

}
