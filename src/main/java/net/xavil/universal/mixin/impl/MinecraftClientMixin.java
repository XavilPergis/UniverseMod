package net.xavil.universal.mixin.impl;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.xavil.universal.common.universe.universe.ClientUniverse;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin implements MinecraftClientAccessor {

	private @Nullable ClientUniverse universe = null;

	@Shadow
	private volatile boolean pause;

	@Inject(method = "tick", at = @At("TAIL"))
	private void onTick(CallbackInfo info) {
		if (!this.pause && this.universe != null) {
			this.universe.tick();
		}
	}

	// id prefer to just inject at TAIL but one of the parameters is an unnameable
	// type :(
	// TODO: use an access widener perhaps?
	@Redirect(method = "doLoadLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"))
	private void onInit(Connection connection, Packet<?> packet) {
		// we don't actually want to fuck with the packet or anything
		connection.send(packet);
		this.universe = new ClientUniverse((Minecraft) (Object) this);
	}

	@Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("TAIL"))
	private void onClearLevel(Screen screen, CallbackInfo info) {
		this.universe = null;
	}

	@Override
	public ClientUniverse universal_getUniverse() {
		return universe;
	}

}
