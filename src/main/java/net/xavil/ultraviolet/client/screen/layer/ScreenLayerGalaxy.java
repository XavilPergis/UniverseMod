package net.xavil.ultraviolet.client.screen.layer;

import net.xavil.ultraviolet.client.GalaxyRenderingContext;
import net.xavil.ultraviolet.client.UltravioletVertexFormats;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.camera.CameraConfig;
import net.xavil.hawklib.client.camera.OrbitCamera;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.ultraviolet.client.screen.RenderHelper;
import net.xavil.ultraviolet.common.universe.galaxy.Galaxy;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.client.screen.HawkScreen3d;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Vec3;

public class ScreenLayerGalaxy extends HawkScreen3d.Layer3d {
	private final GalaxyRenderingContext galaxyRenderingContext;
	private final Vec3 originOffset;
	
	public ScreenLayerGalaxy(HawkScreen3d screen, Galaxy galaxy, Vec3 originOffset) {
		// m/ly = 1e12 m/Tm * Tm/ly = m/ly
		super(screen, new CameraConfig(0.01, 1e6));
		this.galaxyRenderingContext = new GalaxyRenderingContext(galaxy);
		this.originOffset = originOffset;
	}
	
	@Override
	public void render3d(OrbitCamera.Cached camera, float partialTick) {
		final var builder = BufferRenderer.IMMEDIATE_BUILDER;

		final var shader = getShader(SHADER_GALAXY_PARTICLE);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));

		this.galaxyRenderingContext.build();
		builder.begin(PrimitiveType.POINTS, UltravioletVertexFormats.BILLBOARD_FORMAT);
		this.galaxyRenderingContext.enumerate((pos, size) -> {
			RenderHelper.addBillboard(builder, camera, new TransformStack(),
					pos.sub(this.originOffset),
					size * (1e12 / camera.metersPerUnit),
					Color.WHITE.withA(0.1));
		});
		builder.end().draw(shader, DRAW_STATE_ADDITIVE_BLENDING);
	}

}
