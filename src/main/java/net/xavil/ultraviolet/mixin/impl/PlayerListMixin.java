package net.xavil.ultraviolet.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.xavil.ultraviolet.mixin.accessor.LevelAccessor;
import net.xavil.ultraviolet.mixin.accessor.MinecraftServerAccessor;
import net.xavil.ultraviolet.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.ultraviolet.networking.s2c.ClientboundUniverseInfoPacket;
import net.xavil.universegen.system.CelestialNode;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

	@Inject(method = "placeNewPlayer", at = @At("TAIL"))
	private void onPlaceNewPlayer(Connection connection, ServerPlayer serverPlayer, CallbackInfo info) {
		var universe = MinecraftServerAccessor.getUniverse(serverPlayer.server);

		var universeInfoPacket = new ClientboundUniverseInfoPacket();
		universeInfoPacket.commonSeed = universe.getCommonUniverseSeed();
		universeInfoPacket.uniqueSeed = universe.getUniqueUniverseSeed();
		universeInfoPacket.startingId = universe.getStartingSystemGenerator().getStartingSystemId();
		universeInfoPacket.startingSystemNbt = CelestialNode
				.writeNbt(universe.getStartingSystemGenerator().startingSystem.rootNode);
		serverPlayer.connection.send(universeInfoPacket);

		var changeSystemPacket = new ClientboundChangeSystemPacket(LevelAccessor.getLocation(serverPlayer.level));
		serverPlayer.connection.send(changeSystemPacket);
	}

}
