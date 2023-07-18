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
import net.xavil.ultraviolet.Mod;

public final class DebugKey<T> {

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

	private static final MutableMap<String, DebugKey<?>> KEY_MAP = MutableMap.hashMap();
	public final String keyId;
	public final Type<T> type;
	public final T defaultValue;
	public final Side side;

	public DebugKey(String keyId, Type<T> type, T defaultValue, Side side) {
		this.keyId = keyId;
		this.type = type;
		this.defaultValue = defaultValue;
		this.side = side;
	}

	public static <T> DebugKey<T> register(DebugKey<T> key) {
		KEY_MAP.insert(key.keyId, key);
		return key;
	}

	@Nullable
	public static DebugKey<?> lookup(String keyId) {
		return KEY_MAP.get(keyId).unwrapOrNull();
	}

	public static void enumerate(Consumer<DebugKey<?>> consumer) {
		KEY_MAP.values().forEach(consumer);
	}

	public static DebugKey<Boolean> booleanKey(String id, boolean defaultValue, Side side) {
		return register(new DebugKey<Boolean>(id, Type.BOOLEAN, defaultValue, side));
	}

	// @formatter:off
	public static final DebugKey<Boolean> SHOW_SECTOR_BOUNDARIES       = booleanKey("showSectorBoundaries",              false, Side.CLIENT);
	public static final DebugKey<Boolean> SECTOR_TICKET_AROUND_FOCUS   = booleanKey("sectorTicketAroundFocus",           false, Side.CLIENT);
	public static final DebugKey<Boolean> SHOW_ORBIT_PATH_SUBDIVISIONS = booleanKey("showOrbitPathSubdivisions",         false, Side.CLIENT);
	public static final DebugKey<Boolean> SHOW_ALL_ORBIT_PATH_LEVELS   = booleanKey("showAllOrbitPathSubdivisionLevels", false, Side.CLIENT);
	// @formatter:on

}
