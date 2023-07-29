package net.xavil.ultraviolet.client.screen;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.ultraviolet.client.StarRenderManager;

public final class BillboardBatcher {
	public final VertexBuilder builder;
	public final int billboardsPerBatch;
	private CachedCamera<?> camera;

	private int current = 0;

	public BillboardBatcher(VertexBuilder builder, int billboardsPerBatch) {
		this.builder = builder;
		this.billboardsPerBatch = billboardsPerBatch;
	}

	public void begin(CachedCamera<?> camera) {
		builder.begin(PrimitiveType.POINT_QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		this.camera = camera;
	}

	public void end() {
		final var shader = getShader(SHADER_STAR_BILLBOARD);
		StarRenderManager.setupStarShader(shader, this.camera);
		builder.end().draw(shader, DRAW_STATE_ADDITIVE_BLENDING);

		this.current = 0;
	}

	public void add(StellarCelestialNode node, Vec3 pos) {
		RenderHelper.addStarPoint(builder, node, pos.mul(1e12 / camera.metersPerUnit).sub(camera.pos));

		this.current += 1;
		if (this.current > this.billboardsPerBatch) {
			end();
			begin(this.camera);
		}
	}
}