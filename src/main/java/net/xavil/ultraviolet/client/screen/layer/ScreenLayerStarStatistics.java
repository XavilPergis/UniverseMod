package net.xavil.ultraviolet.client.screen.layer;

import net.xavil.hawklib.client.flexible.RenderTexture;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;

public class ScreenLayerStarStatistics extends HawkScreen.Layer2d {

	public ScreenLayerStarStatistics(HawkScreen attachedScreen) {
		super(attachedScreen);
	}

	private void renderScatterPlot(RenderContext ctx, ScatterPlot plot, RenderTexture target) {
		target.framebuffer.bind();

		

		ctx.currentTexture.framebuffer.bind();
	}

	@Override
	public void render(RenderContext ctx) {
	}
	
}
