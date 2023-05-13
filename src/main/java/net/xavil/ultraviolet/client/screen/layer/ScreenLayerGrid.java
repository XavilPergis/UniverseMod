package net.xavil.ultraviolet.client.screen.layer;

import net.xavil.ultraviolet.client.camera.CameraConfig;
import net.xavil.ultraviolet.client.camera.OrbitCamera;
import net.xavil.ultraviolet.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.client.screen.Ultraviolet3dScreen;

public class ScreenLayerGrid extends Ultraviolet3dScreen.Layer3d {

	public ScreenLayerGrid(Ultraviolet3dScreen attachedScreen) {
		super(attachedScreen, new CameraConfig(0.01, 1e6));
	}

	@Override
	public void render3d(OrbitCamera.Cached camera, float partialTick) {
		final var builder = BufferRenderer.immediateBuilder();
		// TODO: configurable grid
		RenderHelper.renderGrid(builder, camera, camera.metersPerUnit / 1e12, 1, 10, 40, partialTick);
	}

}
