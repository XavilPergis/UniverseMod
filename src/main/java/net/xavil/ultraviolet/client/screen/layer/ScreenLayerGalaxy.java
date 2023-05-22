package net.xavil.ultraviolet.client.screen.layer;

import net.xavil.ultraviolet.client.GalaxyRenderingContext;
import net.xavil.ultraviolet.client.ModRendering;
import static net.xavil.ultraviolet.client.Shaders.*;
import static net.xavil.ultraviolet.client.DrawStates.*;
import net.xavil.ultraviolet.client.camera.CameraConfig;
import net.xavil.ultraviolet.client.camera.OrbitCamera;
import net.xavil.ultraviolet.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.client.flexible.FlexibleVertexMode;
import net.xavil.ultraviolet.client.gl.texture.GlTexture2d;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.client.screen.Ultraviolet3dScreen;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.util.math.Color;
import net.xavil.util.math.TransformStack;
import net.xavil.util.math.matrices.Vec3;

public class ScreenLayerGalaxy extends Ultraviolet3dScreen.Layer3d {
	private final GalaxyRenderingContext galaxyRenderingContext;
	private final Vec3 originOffset;
	
	public ScreenLayerGalaxy(Ultraviolet3dScreen screen, Galaxy galaxy, Vec3 originOffset) {
		// m/ly = 1e12 m/Tm * Tm/ly = m/ly
		super(screen, new CameraConfig(0.01, 1e6));
		this.galaxyRenderingContext = new GalaxyRenderingContext(galaxy);
		this.originOffset = originOffset;
	}
	
	@Override
	public void render3d(OrbitCamera.Cached camera, float partialTick) {
		final var builder = BufferRenderer.immediateBuilder();

		this.galaxyRenderingContext.build();
		builder.begin(FlexibleVertexMode.POINTS, ModRendering.BILLBOARD_FORMAT);
		this.galaxyRenderingContext.enumerate((pos, size) -> {
			RenderHelper.addBillboard(builder, camera, new TransformStack(),
					pos.sub(this.originOffset),
					size * (1e12 / camera.metersPerUnit),
					Color.WHITE.withA(0.1));
		});
		builder.end();

		final var shader = getShader(SHADER_GALAXY_PARTICLE);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
		builder.draw(shader, DRAW_STATE_ADDITIVE_BLENDING);
	}

}
