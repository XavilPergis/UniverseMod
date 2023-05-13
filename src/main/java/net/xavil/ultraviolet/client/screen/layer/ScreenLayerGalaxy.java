package net.xavil.ultraviolet.client.screen.layer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.xavil.ultraviolet.client.GalaxyRenderingContext;
import net.xavil.ultraviolet.client.ModRendering;
import net.xavil.ultraviolet.client.camera.CameraConfig;
import net.xavil.ultraviolet.client.camera.OrbitCamera;
import net.xavil.ultraviolet.client.flexible.BufferRenderer;
import net.xavil.ultraviolet.client.flexible.FlexibleVertexMode;
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

		this.client.getTextureManager().getTexture(RenderHelper.GALAXY_GLOW_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.GALAXY_GLOW_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		builder.draw(ModRendering.getShader(ModRendering.GALAXY_PARTICLE_SHADER));
	}

}
