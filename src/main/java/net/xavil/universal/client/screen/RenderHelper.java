package net.xavil.universal.client.screen;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class RenderHelper {

	public static void renderGrid(VertexConsumer builder, Vec3 focusPos, double gridDiameter, int subcellsPerCell,
			int gridLineCount) {

		var gridCellResolution = gridDiameter / gridLineCount;

		var gridMinX = gridCellResolution * Math.floor(focusPos.x / gridCellResolution);
		var gridMinZ = gridCellResolution * Math.floor(focusPos.z / gridCellResolution);

		float r = 0.1f, g = 0.1f, b = 0.1f, a = 0.3f;

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
