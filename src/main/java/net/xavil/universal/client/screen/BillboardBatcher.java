package net.xavil.universal.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.xavil.universal.client.ModRendering;
import net.xavil.universal.client.camera.CachedCamera;
import net.xavil.universal.client.flexible.FlexibleBufferBuilder;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.math.TransformStack;
import net.xavil.util.math.Vec3;

public final class BillboardBatcher {
	private final Minecraft client = Minecraft.getInstance();

	public final FlexibleBufferBuilder builder;
	public final int billboardsPerBatch;
	private CachedCamera<?> camera;

	private int current = 0;

	public BillboardBatcher(FlexibleBufferBuilder builder, int billboardsPerBatch) {
		this.builder = builder;
		this.billboardsPerBatch = billboardsPerBatch;
	}

	public void begin(CachedCamera<?> camera) {
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		this.camera = camera;
	}

	public void end() {
		builder.end();

		this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.disableDepthTest();
		RenderSystem.disableCull();
		RenderSystem.enableBlend();
		builder.draw(ModRendering.getShader(ModRendering.STAR_BILLBOARD_SHADER));

		this.current = 0;
	}

	public void add(StellarCelestialNode node, Vec3 pos) {
		// RenderHelper.addBillboard(builder, this.camera, new TransformStack(), node, pos.mul(1e12 / this.camera.metersPerUnit));
		RenderHelper.addBillboard(builder, this.camera, new TransformStack(), node, pos);

		// var color = Color.WHITE;
		// if (node instanceof StellarCelestialNode starNode) {
		// 	color = starNode.getColor();
		// }
		// RenderHelper.addBillboardWorldspace(builder, camera.pos, camera.up, camera.left,
		// 		pos.mul(1e12 / camera.metersPerUnit), 1, color);


		this.current += 1;
		if (this.current > this.billboardsPerBatch) {
			end();
			begin(this.camera);
		}
	}
}