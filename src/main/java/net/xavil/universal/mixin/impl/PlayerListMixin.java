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
import net.xavil.universal.mixin.accessor.MinecraftServerAccessor;
import net.xavil.universal.networking.ModNetworking;
import net.xavil.universal.networking.s2c.ClientboundUniverseInfoPacket;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
	
	@Inject(method = "placeNewPlayer", at = @At("TAIL"))
	private void onPlaceNewPlayer(Connection connection, ServerPlayer serverPlayer, CallbackInfo info) {
		var universe = MinecraftServerAccessor.getUniverse(serverPlayer.server);

		// Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, compoundTag.get("SpawnDimension")).resultOrPartial(LOGGER::error).orElse(Level.OVERWORLD)

		var packet = new ClientboundUniverseInfoPacket();
		packet.commonSeed = universe.getCommonUniverseSeed();
		packet.uniqueSeed = universe.getUniqueUniverseSeed();

		packet.startingGalaxyId = universe.getStartingGalaxyId();
		var galaxyVolume = universe.getOrGenerateGalaxyVolume(packet.startingGalaxyId.sectorPos());
		var galaxy = galaxyVolume.fullById(packet.startingGalaxyId.sectorId());
		
		packet.startingSystemId = universe.getStartingSystemId(galaxy);
		
		var buf = PacketByteBufs.create();
		packet.write(buf);
		ServerPlayNetworking.send(serverPlayer, ModNetworking.CLIENTBOUND_UNIVERSE_INFO, buf);
	}

}
