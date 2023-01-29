package net.xavil.universal.client.screen;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.xavil.universal.Mod;
import net.xavil.universal.common.universe.system.StarNode;

public final class RenderHelper {

	public static final ResourceLocation STAR_ICON_LOCATION = Mod.namespaced("textures/misc/star_icon.png");
	public static final ResourceLocation SELECTION_CIRCLE_ICON_LOCATION = Mod
			.namespaced("textures/misc/selection_circle.png");

	public static void renderBillboard(VertexConsumer builder, OrbitCamera camera, Vec3 center, double scale,
			double zOffset, float partialTick, Color color) {

		var up = camera.getUpVector(partialTick);
		var right = camera.getRightVector(partialTick);
		var backwards = up.cross(right).scale(zOffset);
		var billboardUp = up.scale(scale);
		var billboardRight = right.scale(scale);
		renderBillboard(builder, center, billboardUp, billboardRight, backwards,
				color.r(), color.g(), color.b(), color.a());

	}

	public static void renderBillboard(VertexConsumer builder, Vec3 center, Vec3 up, Vec3 right, Vec3 forward,
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

	public static void renderGrid(VertexConsumer builder, Vec3 focusPos, double gridDiameter, int subcellsPerCell,
			int gridLineCount) {

		var gridCellResolution = gridDiameter / gridLineCount;

		var gridMinX = gridCellResolution * Math.floor(focusPos.x / gridCellResolution);
		var gridMinZ = gridCellResolution * Math.floor(focusPos.z / gridCellResolution);

		float r = 0.1f, g = 0.1f, b = 0.1f, a = 0.2f;

		var gridOffset = gridCellResolution * gridLineCount / 2;

		// NOTE: each line needs to be divided into sections, because the lines will
		// become distorted if they are too long.
		// X
		for (var i = 1; i < gridLineCount; ++i) {
			var z = gridMinZ + i * gridCellResolution - gridOffset;
			double lx = gridMinX - gridOffset, hx = lx + gridDiameter;

			var zMark = (int) Math.floor(gridMinZ / gridCellResolution + i - gridLineCount / 2);
			var la = zMark % subcellsPerCell == 0 ? 0.4f : a;

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
			var la = xMark % subcellsPerCell == 0 ? 0.4f : a;

			for (var j = 0; j < gridLineCount; ++j) {
				var lt = j / (double) gridLineCount;
				var ht = (j + 1) / (double) gridLineCount;
				builder.vertex(x, focusPos.y, Mth.lerp(lt, lz, hz)).color(r, g, b, la).normal(0, 0, 1).endVertex();
				builder.vertex(x, focusPos.y, Mth.lerp(ht, lz, hz)).color(r, g, b, la).normal(0, 0, 1).endVertex();
			}
		}

	}

	public static void renderAxisAlignedBox(VertexConsumer builder, Vec3 p0, Vec3 p1, Color color) {
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
