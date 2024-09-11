package net.xavil.ultraviolet.mixin.accessor;

import net.minecraft.client.Minecraft;

public interface MouseHandlerAccessor {
    
    void ultraviolet_grabMouse(boolean clearScreen);

    @SuppressWarnings("resource")
    static void grabMouse(boolean clearScreen) {
        final var handler = Minecraft.getInstance().mouseHandler;
        ((MouseHandlerAccessor) handler).ultraviolet_grabMouse(clearScreen);
    }

}
