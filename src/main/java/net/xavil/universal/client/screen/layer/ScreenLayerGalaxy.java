package net.xavil.universal.client.screen.layer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.xavil.universal.client.GalaxyRenderingContext;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.client.camera.CameraConfig;
import net.xavil.universal.client.camera.OrbitCamera;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universal.client.screen.Universal3dScreen;
import net.xavil.universal.common.universe.galaxy.Galaxy;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.Vec3;

public class ScreenLayerGalaxy extends Universal3dScreen.Layer3d {
	private final GalaxyRenderingContext galaxyRenderingContext;
	private final Vec3 originOffset;
	
	public ScreenLayerGalaxy(Universal3dScreen screen, Galaxy galaxy, Vec3 originOffset) {
		// m/ly = 1e12 m/Tm * Tm/ly = m/ly
		super(screen, new CameraConfig(0.01, 1e6));
		this.galaxyRenderingContext = new GalaxyRenderingContext(galaxy);
		this.originOffset = originOffset;
	}
	
	@Override
	public void render3d(OrbitCamera.Cached camera, float partialTick) {
		final var builder = BufferRenderer.immediateBuilder();

		this.galaxyRenderingContext.build();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		this.galaxyRenderingContext.enumerate((pos, size) -> {
			RenderHelper.addBillboard(builder, camera, new PoseStack(),
					pos.sub(this.originOffset).mul(1e12 / camera.metersPerUnit),
					size * (1e12 / camera.metersPerUnit),
					Color.WHITE.withA(0.07));
		});
		builder.end();

		this.client.getTextureManager().getTexture(RenderHelper.GALAXY_GLOW_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.GALAXY_GLOW_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		RenderSystem.enableCull();
		RenderSystem.enableBlend();
		builder.draw(ModRendering.getShader(ModRendering.GALAXY_PARTICLE_SHADER));
	}

}
