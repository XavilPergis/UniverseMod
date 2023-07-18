package net.xavil.ultraviolet.debug;

import net.minecraft.nbt.Tag;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.hawklib.math.Color;
import net.xavil.ultraviolet.Mod;
import net.xavil.ultraviolet.networking.s2c.ClientboundDebugValueSetPacket;

public final class ClientDebug {

	public static final Color[] DEBUG_COLORS = { Color.RED, Color.GREEN, Color.BLUE, Color.CYAN, Color.MAGENTA,
			Color.YELLOW, };

	public static Color getDebugColor(int i) {
		return DEBUG_COLORS[i % DEBUG_COLORS.length];
	}

	private static final class Slot<T> {
		private final DebugKey<T> key;
		private T value;

		public Slot(DebugKey<T> key) {
			this.key = key;
		}

		public void update(Tag nbt) {
			try {
				final T oldValue = this.value;
				this.value = this.key.type.readNbt.apply(nbt);
				Mod.LOGGER.info("[client] debug value '{}' updated from '{}' to '{}'", this.key.keyId, oldValue,
						this.value);
			} catch (Throwable t) {
				Mod.LOGGER.error("[client] caught exception while reading NBT data of debug value '{}':",
						this.key.keyId);
				t.printStackTrace();
			}
		}
	}

	private static final MutableMap<DebugKey<?>, Slot<?>> VALUE_MAP = MutableMap.hashMap();

	@SuppressWarnings("unchecked")
	public static <T> T get(DebugKey<T> key) {
		final var slot = VALUE_MAP.getOrNull(key);
		return slot == null ? key.defaultValue : (T) slot.value;
	}

	public static void applyPacket(ClientboundDebugValueSetPacket packet) {
		final var key = DebugKey.lookup(packet.keyId);
		if (key == null) {
			Mod.LOGGER.error("[client] debug packet has unknown key ID of '{}'", packet.keyId);
			return;
		}

		final var slot = VALUE_MAP.entry(key).orInsertWith(Slot::new);
		slot.update(packet.nbt);
	}

}
