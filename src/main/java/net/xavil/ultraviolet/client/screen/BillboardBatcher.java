package net.xavil.ultraviolet.client.screen;

import net.minecraft.client.Minecraft;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.HawkShaders;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.client.gl.texture.GlTexture2d;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.client.UltravioletVertexFormats;

public final class BillboardBatcher {
	private final Minecraft client = Minecraft.getInstance();

	public final VertexBuilder builder;
	public final int billboardsPerBatch;
	private CachedCamera<?> camera;

	private int current = 0;

	public BillboardBatcher(VertexBuilder builder, int billboardsPerBatch) {
		this.builder = builder;
		this.billboardsPerBatch = billboardsPerBatch;
	}

	public void begin(CachedCamera<?> camera) {
		builder.begin(PrimitiveType.POINT_QUADS, UltravioletVertexFormats.BILLBOARD_FORMAT);
		this.camera = camera;
	}

	public void end() {
		final var shader = getShader(SHADER_STAR_BILLBOARD);
		// shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.STAR_ICON_LOCATION));
		shader.setUniformSampler("uBillboardTexture", GlTexture2d.importTexture(RenderHelper.GALAXY_GLOW_LOCATION));
		builder.end().draw(shader, DRAW_STATE_ADDITIVE_BLENDING);

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