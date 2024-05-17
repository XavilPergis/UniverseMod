package net.xavil.hawklib.client;

import net.xavil.hawklib.client.gl.DrawState;

public class HawkDrawStates {

	protected HawkDrawStates() {
	}

	public static final DrawState DRAW_STATE_DIRECT = new DrawState.Builder()
			.enableDepthTest(false)
			.enableDepthWrite(false)
			.build();

	public static final DrawState DRAW_STATE_DIRECT_ALPHA_BLENDING = new DrawState.Builder()
			.enableDepthTest(false)
			.enableDepthWrite(false)
			.enableAlphaBlending()
			.build();

	public static final DrawState DRAW_STATE_DIRECT_ADDITIVE_BLENDING = new DrawState.Builder()
			.enableDepthTest(false)
			.enableDepthWrite(false)
			.enableAdditiveBlending()
			.build();

	public static final DrawState DRAW_STATE_ADDITIVE_BLENDING = new DrawState.Builder()
			.enableDepthTest()
			.enableDepthWrite(false)
			.enableAdditiveBlending()
			.build();

	public static final DrawState DRAW_STATE_OPAQUE = new DrawState.Builder()
			.enableDepthTest()
			.enableDepthWrite(true)
			.enableBlending(false)
			.enableCulling(true)
			.build();

	public static final DrawState DRAW_STATE_LINES = new DrawState.Builder()
			.enableDepthWrite(true)
			.enableAdditiveBlending()
			.enableDepthTest()
			.build();

}
