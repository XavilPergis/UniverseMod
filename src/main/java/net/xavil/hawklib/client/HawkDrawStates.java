package net.xavil.hawklib.client;

import net.xavil.hawklib.client.gl.DrawState;
import net.xavil.hawklib.client.gl.GlState;

public class HawkDrawStates {

	protected HawkDrawStates() {
	}

	public static final DrawState DRAW_STATE_DIRECT = DrawState.builder()
			.enableDepthTest(false)
			.depthMask(false)
			.enableCulling(false)
			.build();
	public static final DrawState DRAW_STATE_DIRECT_ALPHA_BLENDING = DrawState.builder()
			.enableDepthTest(false)
			.depthMask(false)
			.enableCulling(false)
			.enableAlphaBlending()
			.build();
	public static final DrawState DRAW_STATE_DIRECT_ADDITIVE_BLENDING = DrawState.builder()
			.enableDepthTest(false)
			.depthMask(false)
			.enableCulling(false)
			.enableAdditiveBlending()
			.build();

	public static final DrawState DRAW_STATE_LINES = DrawState.builder()
			.depthMask(true)
			.enableCulling(false)
			.enableAdditiveBlending()
			.enableDepthTest(GlState.DepthFunc.LESS)
			.build();

	public static final DrawState DRAW_STATE_ADDITIVE_BLENDING = DrawState.builder()
			.enableDepthTest(GlState.DepthFunc.LESS)
			.depthMask(false)
			.enableCulling(false)
			.enableAdditiveBlending()
			.build();
	public static final DrawState DRAW_STATE_NO_CULL = DrawState.builder()
			.enableDepthTest(GlState.DepthFunc.LESS)
			.depthMask(true)
			.enableCulling(false)
			.build();
	public static final DrawState DRAW_STATE_OPAQUE = DrawState.builder()
			.enableDepthTest(GlState.DepthFunc.LESS)
			.depthMask(true)
			.enableBlending(false)
			.enableCulling(false)
			.build();

}