package net.xavil.universal.mixin.impl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.authlib.GameProfile;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.xavil.universal.mixin.accessor.LevelAccessor;
import net.xavil.universal.networking.s2c.ClientboundChangeSystemPacket;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

	@Shadow
	private ServerGamePacketListenerImpl connection;

	public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
		super(level, blockPos, f, gameProfile);
	}

	@Inject(method = "setLevel", at = @At("TAIL"))
	public void changeLevel(ServerLevel level, CallbackInfo info) {
		if (this.connection != null) {
			final var packet = new ClientboundChangeSystemPacket(LevelAccessor.getLocation(level));
			this.connection.send(packet);
		}
	}

}
