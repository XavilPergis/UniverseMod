package net.xavil.ultraviolet.debug;

import java.util.UUID;

import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.xavil.hawklib.Maybe;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.collections.interfaces.MutableSet;
import net.xavil.ultraviolet.networking.s2c.ClientboundDebugValueSetPacket;

public final class CommonConfig {

	private final MinecraftServer server;

	private static final class Slot<T> {
		public final ConfigKey<T> key;
		public T value;

		public Slot(ConfigKey<T> key) {
			this.key = key;
		}

		public Tag toNbt() {
			return this.key.type.writeNbt.apply(this.value);
		}
	}

	private final MutableSet<String> dirtyGlobalSlots = MutableSet.hashSet();
	private final MutableMap<String, Slot<?>> globalSlots = MutableMap.hashMap();
	private final MutableMap<UUID, MutableSet<String>> dirtyPlayerSlots = MutableMap.hashMap();
	private final MutableMap<UUID, MutableMap<String, Slot<?>>> playerSlots = MutableMap.hashMap();

	public CommonConfig(MinecraftServer server) {
		this.server = server;
	}

	public <T> T get(ConfigKey<T> key) {
		@SuppressWarnings("unchecked")
		final var slot = (Slot<T>) this.globalSlots.getOrNull(key.keyId);
		if (slot != null) {
			return slot.value;
		}
		return key.defaultValue;
	}

	public <T> T get(ConfigKey<T> key, ServerPlayer player) {
		final var slots = this.playerSlots.getOrNull(player.getUUID());
		if (slots != null) {
			@SuppressWarnings("unchecked")
			final var slot = (Slot<T>) slots.getOrNull(key.keyId);
			if (slot != null) {
				return slot.value;
			}
		}
		return get(key);
	}

	public <T> Maybe<T> setGlobal(ConfigKey<T> key, T value) {
		final var hadPrevious = this.globalSlots.containsKey(key.keyId);
		@SuppressWarnings("unchecked")
		final var slot = (Slot<T>) this.globalSlots.entry(key.keyId).orInsertWith(k -> new Slot<>(key));

		final var oldValue = slot.value;
		slot.value = value;
		this.dirtyGlobalSlots.insert(key.keyId);
		return hadPrevious ? Maybe.some(oldValue) : Maybe.none();
	}

	public <T> Maybe<T> setPlayer(ConfigKey<T> key, ServerPlayer player, T value) {
		if (key.side != ConfigKey.Side.CLIENT) {
			return setGlobal(key, value);
			// Mod.LOGGER.error("Tried to set global debug value '{}' for player '{}'", key.keyId,
			// 		player.getGameProfile().getName());
			// return Maybe.none();
		}
		final var slots = this.playerSlots.entry(player.getUUID()).orInsertWith(k -> MutableMap.hashMap());
		final var hadPrevious = slots.containsKey(key.keyId);
		@SuppressWarnings("unchecked")
		final var slot = (Slot<T>) slots.entry(key.keyId).orInsertWith(k -> new Slot<>(key));

		final var oldValue = slot.value;
		slot.value = value;
		this.dirtyPlayerSlots.entry(player.getUUID()).orInsertWith(k -> MutableSet.hashSet()).insert(key.keyId);
		return hadPrevious ? Maybe.some(oldValue) : Maybe.none();
	}

	public void flush() {
		this.dirtyGlobalSlots.forEach(keyId -> {
			final var slot = this.globalSlots.getOrNull(keyId);
			if (slot == null)
				return;
			final var packet = new ClientboundDebugValueSetPacket(keyId, slot.toNbt(), 0);
			this.server.getPlayerList().broadcastAll(packet);
		});
		this.dirtyGlobalSlots.clear();

		this.dirtyPlayerSlots.entries().forEach(entry -> {
			final var player = this.server.getPlayerList().getPlayer(entry.key);
			final var slots = this.playerSlots.getOrNull(entry.key);
			if (slots == null)
				return;
			entry.get().unwrap().forEach(keyId -> {
				final var slot = slots.getOrNull(keyId);
				if (slot == null)
					return;
				final var packet = new ClientboundDebugValueSetPacket(keyId, slot.toNbt(), 0);
				player.connection.send(packet);
			});
		});
		this.dirtyPlayerSlots.clear();
	}

	// public static void applyPacket(ServerboundDebugValueSetPacket packet) {
	// 	final var key = DebugKey.lookup(packet.keyId);
	// 	if (key == null) {
	// 		Mod.LOGGER.error("[client] debug packet has unknown key ID of '{}'", packet.keyId);
	// 		return;
	// 	}

	// 	final var slot = VALUE_MAP.entry(key).orInsertWith(Slot::new);
	// 	slot.update(packet.nbt);
	// }

}
