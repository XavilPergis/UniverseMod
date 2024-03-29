package net.xavil.ultraviolet.mixin.impl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.ultraviolet.client.SkyRenderer;
import net.xavil.hawklib.client.HawkTextureManager;
import net.xavil.hawklib.client.flexible.RenderTexture;
import net.xavil.ultraviolet.common.universe.universe.ClientUniverse;
import net.xavil.ultraviolet.mixin.accessor.MinecraftClientAccessor;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin implements MinecraftClientAccessor {

	private ClientUniverse universe = null;

	@Shadow
	private volatile boolean pause;
	@Shadow
	@Final
	private RenderTarget mainRenderTarget;
	@Shadow
	private Screen screen;
	@Shadow
	@Final
	private ReloadableResourceManager resourceManager;
	@Shadow
	private ProfilerFiller profiler;

	@Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;<init>(Lnet/minecraft/client/Minecraft;)V"))
	private void registerReloaders(GameConfig config, CallbackInfo info) {
		HawkTextureManager.INSTANCE.registerAtlases();
		this.resourceManager.registerReloadListener(HawkTextureManager.INSTANCE);
	}

	@Inject(method = "tick", at = @At("TAIL"))
	private void onTick(CallbackInfo info) {
		if (this.universe != null) {
			this.universe.tick(this.profiler, this.pause);
		}
		SkyRenderer.INSTANCE.tick();
	}

	@ModifyArg(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V"), index = 2)
	private boolean modifyRenderLevel(boolean shouldRender) {
		if (this.screen instanceof HawkScreen modScreen) {
			shouldRender &= modScreen.shouldRenderWorld();
		}
		if (!shouldRender) {
			this.mainRenderTarget.clear(false);
			this.mainRenderTarget.bindWrite(false);
		}
		return shouldRender;
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;push(Ljava/lang/String;)V", args = "ldc=blit"))
	private void updateTemporaryTextures(boolean renderLevel, CallbackInfo info) {
		RenderTexture.tick();
	}

	@Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("TAIL"))
	private void onClearLevel(Screen screen, CallbackInfo info) {
		if (this.universe != null)
			this.universe.close();
		this.universe = null;
		SkyRenderer.INSTANCE.disposeTickets();
	}

	@Override
	public ClientUniverse ultraviolet_getUniverse() {
		return this.universe;
	}

	@Override
	public void ultraviolet_setUniverse(ClientUniverse universe) {
		this.universe = universe;
	}

}
