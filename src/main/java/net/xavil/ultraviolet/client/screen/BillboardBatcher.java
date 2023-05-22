package net.xavil.ultraviolet.client.screen;

import net.minecraft.client.Minecraft;
import static net.xavil.ultraviolet.client.DrawStates.*;
import static net.xavil.ultraviolet.client.Shaders.*;
import net.xavil.ultraviolet.client.ModRendering;
import net.xavil.ultraviolet.client.camera.CachedCamera;
import net.xavil.ultraviolet.client.flexible.FlexibleBufferBuilder;
import net.xavil.ultraviolet.client.flexible.FlexibleVertexMode;
import net.xavil.ultraviolet.client.gl.texture.GlTexture2d;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.math.matrices.Vec3;

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
		builder.begin(FlexibleVertexMode.POINTS, ModRendering.BILLBOARD_FORMAT);
		// builder.begin(VertexFormat.Mode.QUADS, ModRendering.BILLBOARD_FORMAT);
		// builder.begin(FlexibleVertexMode.POINTS, DefaultVertexFormat.POSITION_COLOR_TEX);
		// builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		this.camera = camera;
	}

	public void end() {
		builder.end();

		final var shader = getShader(SHADER_STAR_BILLBOARD);
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.STAR_ICON_LOCATION));
		builder.draw(shader, DRAW_STATE_ADDITIVE_BLENDING);

		this.current = 0;
	}

	public void add(StellarCelestialNode node, Vec3 pos) {
		// RenderHelper.addBillboard(builder, this.camera, new TransformStack(), node, pos.mul(1e12 / this.camera.metersPerUnit));
		RenderHelper.addBillboard(builder, this.camera, node, pos);

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