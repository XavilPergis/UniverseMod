package net.xavil.ultraviolet.client.screen.layer;

import net.xavil.ultraviolet.client.GalaxyRenderingContext;

import java.util.Random;

import org.lwjgl.glfw.GLFW;

import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.ultraviolet.common.universe.galaxy.GalaxyType;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.client.screen.HawkScreen.Keypress;
import net.xavil.hawklib.math.matrices.Vec3;

public class ScreenLayerGalaxy extends HawkScreen3d.Layer3d {
	private GalaxyRenderingContext galaxyRenderingContext;
	private final Vec3 originOffset;

	public ScreenLayerGalaxy(HawkScreen3d screen, Galaxy galaxy, Vec3 originOffset) {
		super(screen, new CameraConfig(1e3, false, 1e15, false));
		this.galaxyRenderingContext = new GalaxyRenderingContext(galaxy.densityFields);
		this.originOffset = originOffset;
	}

	@Override
	public boolean handleKeypress(Keypress keypress) {
		if (keypress.keyCode == GLFW.GLFW_KEY_P && keypress.hasModifiers(GLFW.GLFW_MOD_CONTROL)) {
			this.galaxyRenderingContext.close();
			final var df = GalaxyType.SPIRAL.createDensityFields(13000, new Random());
			this.galaxyRenderingContext = new GalaxyRenderingContext(df);
			return true;
		}
		return false;
	}

	@Override
	public void render3d(OrbitCamera.Cached camera, float partialTick) {
		this.galaxyRenderingContext.draw(camera, this.originOffset);
	}

	@Override
	public void close() {
		super.close();
		this.galaxyRenderingContext.close();
	}

}
