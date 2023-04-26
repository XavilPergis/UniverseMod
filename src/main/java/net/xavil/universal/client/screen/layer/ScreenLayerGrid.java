package net.xavil.universal.client.screen.layer;

import net.xavil.universal.client.camera.CameraConfig;
import net.xavil.universal.client.camera.OrbitCamera;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.client.screen.Universal3dScreen;

public class ScreenLayerGrid extends Universal3dScreen.Layer3d {

	public ScreenLayerGrid(Universal3dScreen attachedScreen) {
		super(attachedScreen, new CameraConfig(0.01, 1e6, 1e12, 1000));
	}

	@Override
	public void render3d(OrbitCamera.Cached camera, float partialTick) {
		final var builder = BufferRenderer.immediateBuilder();
		// TODO: configurable grid
		RenderHelper.renderGrid(builder, camera, camera.renderScale, 1, 10, 40, partialTick);
	}

}
