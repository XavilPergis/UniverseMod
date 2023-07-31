package net.xavil.ultraviolet.client.screen.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec2i;
import net.xavil.hawklib.math.matrices.Vec3;

public class ScreenLayerBackground extends HawkScreen.Layer2d {

	public Color bottomColor;
	public Color topColor;

	public ScreenLayerBackground(HawkScreen screen, Color color) {
		this(screen, color, color);
	}

	public ScreenLayerBackground(HawkScreen screen, Color bottomColor, Color topColor) {
		super(screen);
		this.bottomColor = bottomColor;
		this.topColor = topColor;
	}

	@Override
	public void render(PoseStack poseStack, Vec2i mousePos, float partialTick) {
		final var shader = getVanillaShader(SHADER_VANILLA_POSITION_COLOR);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		final var builder = BufferRenderer.IMMEDIATE_BUILDER;
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		fillGradient(poseStack.last().pose(), builder,
				0, 0, this.attachedScreen.width, this.attachedScreen.height,
				0, this.bottomColor, this.topColor);
		builder.end().draw(shader, DRAW_STATE_DIRECT_ALPHA_BLENDING);
	}

	private static void fillGradient(Matrix4f matrix, VertexBuilder builder, int x1, int y1, int x2, int y2,
			int z, Color colorA, Color colorB) {
		final var nn = new Vec3(x1, y1, z).transformBy(matrix);
		final var np = new Vec3(x1, y2, z).transformBy(matrix);
		final var pn = new Vec3(x2, y1, z).transformBy(matrix);
		final var pp = new Vec3(x2, y2, z).transformBy(matrix);
		builder.vertex(pn).color(colorA).endVertex();
		builder.vertex(nn).color(colorA).endVertex();
		builder.vertex(np).color(colorB).endVertex();
		builder.vertex(pp).color(colorB).endVertex();
	}

	@Override
	public boolean clobbersScreen() {
		return this.bottomColor.a() >= 1.0 && this.topColor.a() >= 1.0;
	}

}
