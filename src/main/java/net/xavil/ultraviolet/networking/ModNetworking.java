package net.xavil.ultraviolet.networking;

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

public class ModNetworking {

	public static final Event<RegisterPacketsCallback> REGISTER_PACKETS_EVENT = EventFactory
			.createArrayBacked(RegisterPacketsCallback.class, callbacks -> packetAcceptor -> {
				for (var callback : callbacks) {
					callback.register(packetAcceptor);
				}
			});

	public static Consumer<ModPacket<ClientGamePacketListener>> CLIENTBOUND_PLAY_HANDLER = null;
	public static BiConsumer<ServerPlayer, ModPacket<ServerGamePacketListener>> SERVERBOUND_PLAY_HANDLER = null;

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
			if (CLIENTBOUND_PLAY_HANDLER != null)
				CLIENTBOUND_PLAY_HANDLER.accept((ModPacket<ClientGamePacketListener>) packet);
		} else if (listener instanceof ServerGamePacketListenerImpl impl) {
			if (SERVERBOUND_PLAY_HANDLER != null)
				SERVERBOUND_PLAY_HANDLER.accept(impl.player, (ModPacket<ServerGamePacketListener>) packet);
		}
	}

}
