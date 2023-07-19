package net.xavil.ultraviolet.mixin.impl;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.ServerStatusPacketListener;
import net.xavil.ultraviolet.networking.ModNetworking;
import net.xavil.ultraviolet.networking.ModNetworking.PacketRegistrationAcceptor;

@Mixin(ConnectionProtocol.class)
public abstract class ConnectionProtocolMixin {

	@Shadow
	@Final
	private Map<PacketFlow, ? extends ConnectionProtocol.PacketSet<?>> flows;

	@Inject(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Maps;newHashMap()Ljava/util/HashMap;", remap = false, shift = At.Shift.AFTER))
	private static void registerCustomPackets(CallbackInfo info) {
		// slightly icky
		final var state = (ConnectionProtocolMixin) (Object) ConnectionProtocol.PLAY;

		// NOTE: packets must be defiend on both sides, because for any particular side,
		// that side must decode messages to that side, and encode messages to the other
		// side, which requires global knowlege about the network protocol.

		@SuppressWarnings("unchecked")
		final var acceptor = new PacketRegistrationAcceptor(
				(ConnectionProtocol.PacketSet<ClientLoginPacketListener>) state.flows.get(PacketFlow.CLIENTBOUND),
				(ConnectionProtocol.PacketSet<ServerLoginPacketListener>) state.flows.get(PacketFlow.SERVERBOUND),
				(ConnectionProtocol.PacketSet<ClientStatusPacketListener>) state.flows.get(PacketFlow.CLIENTBOUND),
				(ConnectionProtocol.PacketSet<ServerStatusPacketListener>) state.flows.get(PacketFlow.SERVERBOUND),
				(ConnectionProtocol.PacketSet<ClientGamePacketListener>) state.flows.get(PacketFlow.CLIENTBOUND),
				(ConnectionProtocol.PacketSet<ServerGamePacketListener>) state.flows.get(PacketFlow.SERVERBOUND));

		ModNetworking.REGISTER_PACKETS_EVENT.invoker().register(acceptor);
	}

}
