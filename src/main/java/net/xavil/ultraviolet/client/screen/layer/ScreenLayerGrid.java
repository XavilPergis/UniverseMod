package net.xavil.ultraviolet.client.screen.layer;

import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.client.screen.RenderHelper;

public class ScreenLayerGrid extends HawkScreen3d.Layer3d {

	public ScreenLayerGrid(HawkScreen3d attachedScreen) {
		super(attachedScreen, new CameraConfig(0.01, 1e6));
	}

	@Override
	public void render3d(OrbitCamera.Cached camera, float partialTick) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		// TODO: configurable grid
		RenderHelper.renderGrid(builder, camera, camera.metersPerUnit / 1e12, 1, 10, 40, partialTick);
	}

}
