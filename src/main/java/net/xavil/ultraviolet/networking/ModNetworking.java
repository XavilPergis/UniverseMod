package net.xavil.ultraviolet.networking;

import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ServerStatusPacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.thread.BlockableEventLoop;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.collections.interfaces.MutableMap;
import net.xavil.ultraviolet.Mod;

public class ModNetworking {

	public static final Event<RegisterPacketsCallback> REGISTER_PACKETS_EVENT = EventFactory
			.createArrayBacked(RegisterPacketsCallback.class, callbacks -> packetAcceptor -> {
				for (var callback : callbacks) {
					callback.register(packetAcceptor);
				}
			});

	private static class ClientboundHandler<T> {
		private final Class<T> packetType;
		private final MutableList<Consumer<T>> consumers = new Vector<>();

		public ClientboundHandler(Class<T> packetType) {
			this.packetType = packetType;
		}

		public void dispatch(ModPacket<?> packet) {
			if (packet != null && packet.getClass() == this.packetType) {
				@SuppressWarnings("unchecked")
				final T typedPacket = (T) packet;
				this.consumers.forEach(consumer -> consumer.accept(typedPacket));
			}
		}
	}

	private ModNetworking() {
	}

	private static class ServerboundHandler<T> {
		private final Class<T> packetType;
		private final MutableList<BiConsumer<ServerPlayer, T>> consumers = new Vector<>();

		public ServerboundHandler(Class<T> packetType) {
			this.packetType = packetType;
		}

		public void dispatch(ServerPlayer player, ModPacket<?> packet) {
			if (packet != null && packet.getClass() == this.packetType) {
				@SuppressWarnings("unchecked")
				final T typedPacket = (T) packet;
				this.consumers.forEach(consumer -> consumer.accept(player, typedPacket));
			}
		}
	}

	private static MutableMap<Class<?>, ClientboundHandler<?>> CLIENTBOUND_PLAY_HANDLERS = MutableMap.hashMap();
	private static MutableMap<Class<?>, ServerboundHandler<?>> SERVERBOUND_PLAY_HANDLERS = MutableMap.hashMap();

	public static <T extends ModPacket<ClientGamePacketListener>> void addClientboundHandlerRaw(Class<T> clazz,
			Consumer<T> packetHandler) {
		if (!CLIENTBOUND_PLAY_HANDLERS.containsKey(clazz)) {
			CLIENTBOUND_PLAY_HANDLERS.insert(clazz, new ClientboundHandler<>(clazz));
		}
		@SuppressWarnings("unchecked")
		final var handlers = (ClientboundHandler<T>) CLIENTBOUND_PLAY_HANDLERS.get(clazz).unwrap();
		handlers.consumers.push(packetHandler);
	}

	public static <T extends ModPacket<ServerGamePacketListener>> void addServerboundHandlerRaw(Class<T> clazz,
			BiConsumer<ServerPlayer, T> packetHandler) {
		if (!SERVERBOUND_PLAY_HANDLERS.containsKey(clazz)) {
			SERVERBOUND_PLAY_HANDLERS.insert(clazz, new ServerboundHandler<>(clazz));
		}
		@SuppressWarnings("unchecked")
		final var handlers = (ServerboundHandler<T>) SERVERBOUND_PLAY_HANDLERS.get(clazz).unwrap();
		handlers.consumers.push(packetHandler);
	}

	public static <T extends ModPacket<ClientGamePacketListener>> void addClientboundHandler(Class<T> clazz,
			Executor executor, Consumer<T> packetHandler) {
		addClientboundHandlerRaw(clazz, packet -> executor.execute(() -> {
			packetHandler.accept(packet);
		}));
	}

	public static <T extends ModPacket<ServerGamePacketListener>> void addServerboundHandler(Class<T> clazz,
			BiConsumer<ServerPlayer, T> packetHandler) {
		addServerboundHandlerRaw(clazz, (player, packet) -> player.server.execute(() -> {
			packetHandler.accept(player, packet);
		}));
	}

	// public static void addClientboundHandler(ClientboundPlayHandler handler) {
	// CLIENTBOUND_PLAY_HANDLERS.push(handler);
	// }

	// public static void addServerboundHandler(ServerboundPlayHandler handler) {
	// SERVERBOUND_PLAY_HANDLERS.push(handler);
	// }

	public static final class PacketSet<T extends PacketListener> {
		private final ConnectionProtocol.PacketSet<T> packetSet;

		public PacketSet(ConnectionProtocol.PacketSet<T> packetSet) {
			this.packetSet = packetSet;
		}

		public <P extends Packet<T>> void registerRaw(Class<P> type, Function<FriendlyByteBuf, P> packetFactory) {
			this.packetSet.addPacket(type, packetFactory);
		}

		public <P extends ModPacket<T>> void register(Class<P> type, Supplier<P> defaultPacketFactory) {
			this.packetSet.addPacket(type, buf -> {
				var instance = defaultPacketFactory.get();
				instance.read(buf);
				return instance;
			});
		}

	}

	public static final class PacketRegistrationAcceptor {
		public final PacketSet<ClientLoginPacketListener> clientboundLogin;
		public final PacketSet<ServerLoginPacketListener> serverboundLogin;
		public final PacketSet<ClientStatusPacketListener> clientboundStatus;
		public final PacketSet<ServerStatusPacketListener> serverboundStatus;
		public final PacketSet<ClientGamePacketListener> clientboundPlay;
		public final PacketSet<ServerGamePacketListener> serverboundPlay;

		public PacketRegistrationAcceptor(ConnectionProtocol.PacketSet<ClientLoginPacketListener> clientboundLogin,
				ConnectionProtocol.PacketSet<ServerLoginPacketListener> serverboundLogin,
				ConnectionProtocol.PacketSet<ClientStatusPacketListener> clientboundStatus,
				ConnectionProtocol.PacketSet<ServerStatusPacketListener> serverboundStatus,
				ConnectionProtocol.PacketSet<ClientGamePacketListener> clientboundPlay,
				ConnectionProtocol.PacketSet<ServerGamePacketListener> serverboundPlay) {
			this.clientboundLogin = new PacketSet<>(clientboundLogin);
			this.serverboundLogin = new PacketSet<>(serverboundLogin);
			this.clientboundStatus = new PacketSet<>(clientboundStatus);
			this.serverboundStatus = new PacketSet<>(serverboundStatus);
			this.clientboundPlay = new PacketSet<>(clientboundPlay);
			this.serverboundPlay = new PacketSet<>(serverboundPlay);
		}
	}

	public interface RegisterPacketsCallback {
		void register(PacketRegistrationAcceptor acceptor);
	}

	public interface PacketReceivedCallback<T extends PacketListener> {
		void register(Packet<T> packet, T vanillaHandler);
	}

	public static <T extends PacketListener> void dispatch(ModPacket<T> packet, T listener) {
		if (listener instanceof ClientGamePacketListener) {
			final var handler = CLIENTBOUND_PLAY_HANDLERS.get(packet.getClass()).unwrapOrNull();
			if (handler != null) {
				handler.dispatch(packet);
			} else {
				Mod.LOGGER.warn("Packet handler (Client Play) not found for packet class '{}'!",
						packet.getClass().getName());
			}
		} else if (listener instanceof ServerGamePacketListenerImpl impl) {
			final var handler = SERVERBOUND_PLAY_HANDLERS.get(packet.getClass()).unwrapOrNull();
			if (handler != null) {
				handler.dispatch(impl.player, packet);
			} else {
				Mod.LOGGER.warn("Packet handler (Server Play) not found for packet class '{}'!",
						packet.getClass().getName());
			}
		}
	}

}
