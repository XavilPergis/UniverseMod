package net.xavil.ultraviolet.client.screen;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TranslatableComponent;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerBackground;
import net.xavil.ultraviolet.client.screen.layer.ScreenLayerStarStatistics;
import net.xavil.ultraviolet.common.universe.galaxy.SectorTicket;

public class StarStatisticsScreen extends HawkScreen {

	public StarStatisticsScreen(Screen previousScreen, SectorTicket<?> ticket) {
		super(new TranslatableComponent("narrator.screen.star_statistics"), previousScreen);

		this.layers.push(new ScreenLayerBackground(this, ColorRgba.BLACK));
		this.layers.push(new ScreenLayerStarStatistics(this, ticket));
	}
	
}
