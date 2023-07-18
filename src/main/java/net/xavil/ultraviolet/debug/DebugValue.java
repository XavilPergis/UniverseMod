package net.xavil.ultraviolet.debug;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.util.Objects;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;

public final class DebugValue<T> {
	public final String name;
	public final int flags;
	public final DebugValueType<T> type;

	private boolean maybeHasModifications = false;
	private T value;
	private T oldValue;
	private int syncId = 0;

	public static final int FLAG_BROADCAST = 1;

	public static final class DebugValueType<T> {
		public final Class<T> containedClass;
		public final ArgumentType<T> argumentType;

		public DebugValueType(Class<T> containedClass, ArgumentType<T> argumentType) {
			this.containedClass = containedClass;
			this.argumentType = argumentType;
		}

		public static final DebugValueType<Boolean> BOOLEAN = new DebugValueType<>(
				boolean.class, BoolArgumentType.bool());
		public static final DebugValueType<Float> FLOAT = new DebugValueType<>(
				float.class, FloatArgumentType.floatArg());
	}

	public DebugValue(String name, int flags, DebugValueType<T> type) {
		this.name = name;
		this.flags = flags;
		this.type = type;
	}

	@Nullable
	public T get() {
		return this.value;
	}

	public T get(T defaultValue) {
		return this.value == null ? defaultValue : this.value;
	}

	public T set(T newValue) {
		T old = this.value;
		this.value = newValue;
		this.maybeHasModifications = true;
		return old;
	}

	public boolean maybeHasModifications() {
		return this.maybeHasModifications;
	}

	public boolean hasModifications() {
		return this.maybeHasModifications && Objects.equals(this.oldValue, this.value);
	}

	public void resetModificationTracking() {
		this.oldValue = this.value;
		this.maybeHasModifications = false;
	}

	public LiteralArgumentBuilder<CommandSourceStack> createGetCommand() {
		return literal(this.name).executes(ctx -> {
			final var message = String.format(
					"debug value '%s' is currently '%s'",
					this.name, this.value);
			ctx.getSource().sendSuccess(new TextComponent(message), true);
			return 1;
		});
	}

	public LiteralArgumentBuilder<CommandSourceStack> createSetCommand() {
		return literal(this.name).then(argument("value", this.type.argumentType)).executes(ctx -> {
			final var message = String.format(
					"debug value '%s' changed to '%s' (was '%s')",
					this.name, this.value);
			set(ctx.getArgument("value", this.type.containedClass));
			// final var packet = new ClientboundDebugValueSetPacket(this.name, toNbt());
			// ctx.getSource().getServer().getPlayerList().broadcastAll(packet);
			ctx.getSource().sendSuccess(new TextComponent(message), true);
			return 1;
		});
	}

	public LiteralArgumentBuilder<CommandSourceStack> createUnsetCommand() {
		return literal(this.name).executes(ctx -> {
			final var message = String.format(
					"debug value '%s' unset (was '%s')",
					this.name, this.value);
			// final var packet = new ClientboundDebugValueSetPacket(this.name, null);
			// ctx.getSource().getServer().getPlayerList().broadcastAll(packet);
			ctx.getSource().sendSuccess(new TextComponent(message), true);
			return 1;
		});
	}

}