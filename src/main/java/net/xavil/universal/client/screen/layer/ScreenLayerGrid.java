package net.xavil.universal.client.screen.layer;

import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.screen.OrbitCamera;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.client.screen.Universal3dScreen;

public class ScreenLayerGrid extends Universal3dScreen.Layer3d {

	public ScreenLayerGrid(Universal3dScreen attachedScreen) {
		super(attachedScreen);
	}

	@Override
	public void render3d(OrbitCamera.Cached camera, float partialTick) {
		final var builder = BufferRenderer.immediateBuilder();
		RenderHelper.renderGrid(builder, camera, camera.camera.renderScaleFactor, 1, 10, 40, partialTick);
	}

}
