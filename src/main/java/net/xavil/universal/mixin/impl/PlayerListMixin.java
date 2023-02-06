package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;
import net.xavil.universal.networking.ModNetworking;
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
		
		var buf1 = PacketByteBufs.create();
		universeInfoPacket.write(buf1);
		ServerPlayNetworking.send(serverPlayer, ModNetworking.CLIENTBOUND_UNIVERSE_INFO, buf1);

		var changeSystemPacket = new ClientboundChangeSystemPacket();
		changeSystemPacket.id = LevelAccessor.getUniverseId(serverPlayer.level);
		
		var buf2 = PacketByteBufs.create();
		changeSystemPacket.write(buf2);
		ServerPlayNetworking.send(serverPlayer, ModNetworking.CLIENTBOUND_CHANGE_SYSTEM, buf2);
	}

}
