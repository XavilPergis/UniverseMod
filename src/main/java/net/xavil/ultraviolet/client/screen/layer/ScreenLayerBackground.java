package net.xavil.ultraviolet.client.screen.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import net.xavil.hawklib.client.flexible.BufferLayout;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.VertexAttributeConsumer;
import net.xavil.hawklib.client.flexible.PrimitiveType;
import net.xavil.hawklib.client.screen.HawkScreen;
import net.xavil.hawklib.client.screen.HawkScreen.RenderContext;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.VecMath;
import net.xavil.hawklib.math.matrices.interfaces.Mat4Access;

public class ScreenLayerBackground extends HawkScreen.Layer2d {

	public ColorRgba bottomColor;
	public ColorRgba topColor;

	public ScreenLayerBackground(HawkScreen screen, ColorRgba color) {
		this(screen, color, color);
	}

	public ScreenLayerBackground(HawkScreen screen, ColorRgba bottomColor, ColorRgba topColor) {
		super(screen);
		this.bottomColor = bottomColor;
		this.topColor = topColor;
	}

	@Override
	public void render(RenderContext ctx) {
		final var shader = SHADER_VANILLA_POSITION_COLOR.get();
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

		final var builder = BufferRenderer.IMMEDIATE_BUILDER
				.beginGeneric(PrimitiveType.QUAD_DUPLICATED, BufferLayout.POSITION_COLOR);
		fillGradient(ctx.poseStack.last().pose(), builder,
				0, 0, this.attachedScreen.width, this.attachedScreen.height,
				0, this.bottomColor, this.topColor);
		builder.end().draw(shader, DRAW_STATE_DIRECT_ALPHA_BLENDING);
	}

	private static void fillGradient(Matrix4f matrix, VertexAttributeConsumer.Generic builder, int x1, int y1, int x2, int y2,
			int z, ColorRgba colorA, ColorRgba colorB) {
		final var mat = Mat4Access.from(matrix);
		final var nn = VecMath.transformPerspective(mat, new Vec3(x1, y1, z), 1);
		final var np = VecMath.transformPerspective(mat, new Vec3(x1, y2, z), 1);
		final var pn = VecMath.transformPerspective(mat, new Vec3(x2, y1, z), 1);
		final var pp = VecMath.transformPerspective(mat, new Vec3(x2, y2, z), 1);
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
