package net.xavil.universal.client.screen;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class UniversalScreen extends Screen {

	protected final Minecraft client = Minecraft.getInstance();
	protected final @Nullable Screen previousScreen;

	protected UniversalScreen(Component component, @Nullable Screen previousScreen) {
		super(component);
		this.previousScreen = previousScreen;
	}

	/**
	 * Controls whether the client will render the world. This should be overridden
	 * to return false when the screen is completely covered by a background.
	 */
	public boolean shouldRenderWorld() {
		return true;
	}

	@Override
	public void onClose() {
		// NOTE: explicitly not calling super's onClose because we want to set the
		// screen to the previous scrren instead of always setting it to null.
		this.client.setScreen(previousScreen);
	}

}
