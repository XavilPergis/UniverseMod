package net.xavil.universal.client.screen.layer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import net.minecraft.client.renderer.GameRenderer;
import net.xavil.universal.client.flexible.BufferRenderer;
import net.xavil.universal.client.flexible.FlexibleBufferBuilder;
import net.xavil.universal.client.screen.UniversalScreen;
import net.xavil.util.math.Color;
import net.xavil.util.math.Vec2i;
import net.xavil.util.math.Vec3;

public class ScreenLayerBackground extends UniversalScreen.Layer2d {

	public Color bottomColor;
	public Color topColor;

	public ScreenLayerBackground(UniversalScreen screen, Color color) {
		this(screen, color, color);
	}

	public ScreenLayerBackground(UniversalScreen screen, Color bottomColor, Color topColor) {
		super(screen);
		this.bottomColor = bottomColor;
		this.topColor = topColor;
	}

	@Override
	public void render(PoseStack poseStack, Vec2i mousePos, float partialTick) {
		RenderSystem.depthMask(false);
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		final var builder = BufferRenderer.immediateBuilder();
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		fillGradient(poseStack.last().pose(), builder,
				0, 0, this.attachedScreen.width, this.attachedScreen.height,
				0, this.bottomColor, this.topColor);
		builder.end();
		builder.draw(GameRenderer.getPositionColorShader());
		RenderSystem.disableBlend();
		RenderSystem.enableTexture();
		RenderSystem.depthMask(true);
	}

	private static void fillGradient(Matrix4f matrix, FlexibleBufferBuilder builder, int x1, int y1, int x2, int y2,
			int z, Color colorA, Color colorB) {
		final var nn = Vec3.from(x1, y1, z).transformBy(matrix);
		final var np = Vec3.from(x1, y2, z).transformBy(matrix);
		final var pn = Vec3.from(x2, y1, z).transformBy(matrix);
		final var pp = Vec3.from(x2, y2, z).transformBy(matrix);
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
