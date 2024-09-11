package net.xavil.ultraviolet.mixin.impl;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.Screen;
import net.xavil.ultraviolet.mixin.accessor.MouseHandlerAccessor;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin implements MouseHandlerAccessor {

    private boolean shouldClearCurrentScreen = false;

    @Shadow
    public abstract void grabMouse();

    @Redirect(method = "grabMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void handleSetScreen(Minecraft minecraft, Screen screen) {
        if (this.shouldClearCurrentScreen)
            minecraft.setScreen(screen);
    }

    // vanilla unconditionally clears the current screen for some reason
    @Override
    public void ultraviolet_grabMouse(boolean clearScreen) {
        final var old = this.shouldClearCurrentScreen;
        try {
            this.shouldClearCurrentScreen = clearScreen;
            grabMouse();
        } finally {
            this.shouldClearCurrentScreen = old;
        }
    }

}
