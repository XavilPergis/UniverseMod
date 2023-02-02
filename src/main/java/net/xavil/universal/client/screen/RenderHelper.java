package net.xavil.universal.client.screen;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.system.BinaryNode;
import net.xavil.universal.common.universe.system.PlanetNode;
import net.xavil.universal.common.universe.system.StarNode;
import net.xavil.universal.common.universe.system.StarSystemNode;

public final class RenderHelper {

	public static final ResourceLocation STAR_ICON_LOCATION = Mod.namespaced("textures/misc/star_icon.png");
	public static final ResourceLocation SELECTION_CIRCLE_ICON_LOCATION = Mod
			.namespaced("textures/misc/selection_circle.png");

	private static final Minecraft CLIENT = Minecraft.getInstance();

	public static void renderStarBillboard(BufferBuilder builder, OrbitCamera camera, StarSystemNode node, Vec3 center,
			double tmPerUnit, float partialTick) {
		RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
		RenderHelper.addStarBillboard(builder, camera, node, center, tmPerUnit, partialTick);
		builder.end();
		CLIENT.getTextureManager().getTexture(STAR_ICON_LOCATION).setFilter(true, false);
		RenderSystem.setShaderTexture(0, STAR_ICON_LOCATION);
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		RenderSystem.depthMask(false);
		RenderSystem.enableDepthTest();
		BufferUploader.end(builder);
	}

	public static void addStarBillboard(VertexConsumer builder, OrbitCamera camera, StarSystemNode node, Vec3 center,
			double tmPerUnit, float partialTick) {

		var camPos = camera.getPos(partialTick);
		var distanceFromCamera = camPos.distanceTo(center);

		var color = Color.WHITE;
		if (node instanceof StarNode starNode) {
			color = starNode.getColor();
		}

		final double starMinSize = 0.01, starBaseSize = 0.05, starRadiusFactor = 0.5;
		final double otherMinSize = 0.01, otherBaseSize = 0;
		final double brightBillboardSizeFactor = 0.5;

		double d = 0;
		if (node instanceof StarNode starNode) {
			var r = Math.min(100000 * 0.00465047 / tmPerUnit, starNode.radiusRsol);
			// var r = starNode.radiusRsol;
			d = Math.max(starMinSize * distanceFromCamera,
					Math.max(starBaseSize, starRadiusFactor * r));
		} else if (node instanceof PlanetNode planetNode) {
			d = Math.max(otherMinSize * distanceFromCamera, otherBaseSize);
		} else if (!(node instanceof BinaryNode)) {
			d = Math.max(otherMinSize * distanceFromCamera, otherBaseSize);
		}
		RenderHelper.addBillboard(builder, camera, center, d, 0, partialTick, color);
		RenderHelper.addBillboard(builder, camera, center, brightBillboardSizeFactor * d, 0, partialTick, Color.WHITE);
	}

	public static void addBillboard(VertexConsumer builder, OrbitCamera camera, Vec3 center, double scale,
			double zOffset, float partialTick, Color color) {

		var up = camera.getUpVector(partialTick);
		var right = camera.getRightVector(partialTick);
		var backwards = up.cross(right).scale(zOffset);
		var billboardUp = up.scale(scale);
		var billboardRight = right.scale(scale);
		addBillboard(builder, center, billboardUp, billboardRight, backwards,
				color.r(), color.g(), color.b(), color.a());

	}

	public static void addBillboard(VertexConsumer builder, Vec3 center, Vec3 up, Vec3 right, Vec3 forward,
			float r, float g, float b, float a) {
		var qll = center.subtract(up).subtract(right).add(forward);
		var qlh = center.subtract(up).add(right).add(forward);
		var qhl = center.add(up).subtract(right).add(forward);
		var qhh = center.add(up).add(right).add(forward);
		builder.vertex(qhl.x, qhl.y, qhl.z).color(r, g, b, a).uv(1, 0).endVertex();
		builder.vertex(qll.x, qll.y, qll.z).color(r, g, b, a).uv(0, 0).endVertex();
		builder.vertex(qlh.x, qlh.y, qlh.z).color(r, g, b, a).uv(0, 1).endVertex();
		builder.vertex(qhh.x, qhh.y, qhh.z).color(r, g, b, a).uv(1, 1).endVertex();
	}

	public static double getGridScale(OrbitCamera camera, double tmPerUnit, double scaleFactor, float partialTick) {
		var currentThreshold = tmPerUnit;
		var scale = tmPerUnit;
		for (var i = 0; i < 10; ++i) {
			currentThreshold *= scaleFactor;
			if (camera.scale.get(partialTick) > currentThreshold)
				scale = currentThreshold;
		}
		return scale;
	}

	public static void renderGrid(BufferBuilder builder, OrbitCamera camera, double tmPerUnit, double gridUnits,
			int scaleFactor, int gridLineCount, float partialTick) {
		var focusPos = camera.focus.get(partialTick).scale(1 / tmPerUnit);
		var gridScale = getGridScale(camera, gridUnits, scaleFactor, partialTick);
		renderGrid(builder, focusPos, gridScale * gridLineCount, scaleFactor, gridLineCount);
	}

	public static void renderGrid(BufferBuilder builder, Vec3 focusPos, double gridDiameter, int subcellsPerCell,
			int gridLineCount) {
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		addGrid(builder, focusPos, gridDiameter, subcellsPerCell, gridLineCount);
		builder.end();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.lineWidth(1);
		RenderSystem.depthMask(false);
		BufferUploader.end(builder);
	}

	public static void addGrid(VertexConsumer builder, Vec3 focusPos, double gridDiameter, int subcellsPerCell,
			int gridLineCount) {

		var gridCellResolution = gridDiameter / gridLineCount;

		var gridMinX = gridCellResolution * Math.floor(focusPos.x / gridCellResolution);
		var gridMinZ = gridCellResolution * Math.floor(focusPos.z / gridCellResolution);

		float r = 0.5f, g = 0.5f, b = 0.5f, a1 = 0.2f, a2 = 0.5f;

		var gridOffset = gridCellResolution * gridLineCount / 2;

		// NOTE: each line needs to be divided into sections, because the lines will
		// become distorted if they are too long.
		// X
		for (var i = 1; i < gridLineCount; ++i) {
			var z = gridMinZ + i * gridCellResolution - gridOffset;
			double lx = gridMinX - gridOffset, hx = lx + gridDiameter;

			var zMark = (int) Math.floor(gridMinZ / gridCellResolution + i - gridLineCount / 2);
			var la = zMark % subcellsPerCell == 0 ? a2 : a1;

			for (var j = 0; j < gridLineCount; ++j) {
				var lt = j / (double) gridLineCount;
				var ht = (j + 1) / (double) gridLineCount;
				builder.vertex(Mth.lerp(lt, lx, hx), focusPos.y, z).color(r, g, b, la).normal(1, 0, 0).endVertex();
				builder.vertex(Mth.lerp(ht, lx, hx), focusPos.y, z).color(r, g, b, la).normal(1, 0, 0).endVertex();
			}
		}
		// Z
		for (var i = 1; i < gridLineCount; ++i) {
			var x = gridMinX + i * gridCellResolution - gridOffset;
			double lz = gridMinZ - gridOffset, hz = lz + gridDiameter;

			var xMark = (int) Math.floor(gridMinX / gridCellResolution + i - gridLineCount / 2);
			var la = xMark % subcellsPerCell == 0 ? a2 : a1;

			for (var j = 0; j < gridLineCount; ++j) {
				var lt = j / (double) gridLineCount;
				var ht = (j + 1) / (double) gridLineCount;
				builder.vertex(x, focusPos.y, Mth.lerp(lt, lz, hz)).color(r, g, b, la).normal(0, 0, 1).endVertex();
				builder.vertex(x, focusPos.y, Mth.lerp(ht, lz, hz)).color(r, g, b, la).normal(0, 0, 1).endVertex();
			}
		}

	}

	public static void renderLine(BufferBuilder builder, Vec3 start, Vec3 end, double lineWidth, Color color) {
		renderLine(builder, start, end, lineWidth, color, color);
	}

	public static void renderLine(BufferBuilder builder, Vec3 start, Vec3 end, double lineWidth, Color startColor,
			Color endColor) {
		RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
		builder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
		var normal = end.subtract(start).normalize();
		builder.vertex(start.x, start.y, start.z)
				.color(startColor.r(), startColor.g(), startColor.b(), startColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		builder.vertex(end.x, end.y, end.z)
				.color(endColor.r(), endColor.g(), endColor.b(), endColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		builder.end();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.depthMask(false);
		RenderSystem.lineWidth((float) lineWidth);
		BufferUploader.end(builder);
	}

	public static void addLine(VertexConsumer builder, Vec3 start, Vec3 end, Color color) {
		addLine(builder, start, end, color, color);
	}

	public static void addLine(VertexConsumer builder, Vec3 start, Vec3 end, Color startColor, Color endColor) {
		var normal = end.subtract(start).normalize();
		builder.vertex(start.x, start.y, start.z)
				.color(startColor.r(), startColor.g(), startColor.b(), startColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		builder.vertex(end.x, end.y, end.z)
				.color(endColor.r(), endColor.g(), endColor.b(), endColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
	}

	public static void addAxisAlignedBox(VertexConsumer builder, Vec3 p0, Vec3 p1, Color color) {
		double lx = p0.x < p1.x ? p0.x : p1.x;
		double ly = p0.y < p1.y ? p0.y : p1.y;
		double lz = p0.z < p1.z ? p0.z : p1.z;
		double hx = p0.x >= p1.x ? p0.x : p1.x;
		double hy = p0.y >= p1.y ? p0.y : p1.y;
		double hz = p0.z >= p1.z ? p0.z : p1.z;

		float r = color.r(), g = color.g(), b = color.b(), a = color.a();

		// X axis
		builder.vertex(lx, ly, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, ly, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(lx, ly, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, ly, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(lx, hy, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, hy, lz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(lx, hy, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		builder.vertex(hx, hy, hz).color(r, g, b, a).normal(1, 0, 0).endVertex();
		// Y axis
		builder.vertex(lx, ly, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(lx, hy, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(lx, ly, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(lx, hy, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, ly, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, hy, lz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, ly, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		builder.vertex(hx, hy, hz).color(r, g, b, a).normal(0, 1, 0).endVertex();
		// Z axis
		builder.vertex(lx, ly, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(lx, ly, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(lx, hy, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(lx, hy, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, ly, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, ly, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, hy, lz).color(r, g, b, a).normal(0, 0, 1).endVertex();
		builder.vertex(hx, hy, hz).color(r, g, b, a).normal(0, 0, 1).endVertex();
	}

}
