package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;
import net.xavil.universal.networking.ModServerNetworking;
import net.xavil.universal.networking.s2c.ClientboundChangeSystemPacket;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

	@Inject(method = "placeNewPlayer", at = @At("TAIL"))
	private void onPlaceNewPlayer(Connection connection, ServerPlayer serverPlayer, CallbackInfo info) {
		var universe = MinecraftServerAccessor.getUniverse(serverPlayer.server);

		var universeInfoPacket = new ClientboundUniverseInfoPacket();
		universeInfoPacket.commonSeed = universe.getCommonUniverseSeed();
		universeInfoPacket.uniqueSeed = universe.getUniqueUniverseSeed();

		var startingId = universe.getStartingSystemGenerator().getStartingSystemId();
		universeInfoPacket.startingGalaxyId = startingId.galaxySector();
		universeInfoPacket.startingSystemVolumePos = startingId.systemSector().sectorPos();
		universeInfoPacket.startingNodeId = startingId.systemNodeId();
		universeInfoPacket.startingSystem = universe.getStartingSystemGenerator().startingSystem;

		ModServerNetworking.send(serverPlayer, universeInfoPacket);

		var changeSystemPacket = new ClientboundChangeSystemPacket();
		changeSystemPacket.id = LevelAccessor.getUniverseId(serverPlayer.level);

		ModServerNetworking.send(serverPlayer, changeSystemPacket);
	}

}
