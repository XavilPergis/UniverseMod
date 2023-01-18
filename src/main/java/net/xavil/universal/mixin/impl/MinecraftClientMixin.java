package net.xavil.universal.mixin.impl;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.xavil.universal.common.universe.universe.ClientUniverse;
import net.xavil.universal.mixin.accessor.MinecraftClientAccessor;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin implements MinecraftClientAccessor {
	
	private @Nullable ClientUniverse universe = null;

	@Inject(method = "setLevel", at = @At("TAIL"))
	private void onSetLevel(ClientLevel clientLevel, CallbackInfo info) {
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
