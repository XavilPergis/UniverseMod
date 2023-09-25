package net.xavil.ultraviolet.client.screen;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xavil.ultraviolet.Mod;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.hawklib.math.ColorRgba;
import net.xavil.hawklib.math.matrices.Vec3;

public final class RenderHelper {

	public static final ResourceLocation STAR_ICON_LOCATION = Mod.namespaced("textures/misc/star_icon.png");
	public static final ResourceLocation GALAXY_GLOW_LOCATION = Mod.namespaced("textures/misc/galaxyglow.png");
	public static final ResourceLocation SELECTION_CIRCLE_ICON_LOCATION = Mod
			.namespaced("textures/misc/selection_circle.png");

	public static void addLine(FlexibleVertexConsumer builder, CachedCamera camera, Vec3 start, Vec3 end,
			ColorRgba color) {
		addLine(builder, camera, start, end, color, color);
	}

	public static void addLine(FlexibleVertexConsumer builder, Vec3 start, Vec3 end, ColorRgba color) {
		addLine(builder, start, end, color, color);
	}

	public static void addLine(FlexibleVertexConsumer builder, CachedCamera camera, Vec3 start, Vec3 end,
			ColorRgba startColor, ColorRgba endColor) {
		addLine(builder, camera.toCameraSpace(start), camera.toCameraSpace(end), startColor, endColor);
	}

	public static void addLine(FlexibleVertexConsumer builder, Vec3 start, Vec3 end, ColorRgba startColor, ColorRgba endColor) {
		var normal = end.sub(start).normalize();
		builder.vertex(start.x, start.y, start.z)
				.color(startColor.r(), startColor.g(), startColor.b(), startColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
		builder.vertex(end.x, end.y, end.z)
				.color(endColor.r(), endColor.g(), endColor.b(), endColor.a())
				.normal((float) normal.x, (float) normal.y, (float) normal.z)
				.endVertex();
	}

	public static void addCubeSphere(FlexibleVertexConsumer builder, Vec3 center, double radius, int subdivisions) {

		// -X
		double nxlu = 0.00f, nxlv = 0.5f, nxhu = 0.25f, nxhv = 0.25f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lv = (float) Mth.lerp(lpa, nxlv, nxhv);
			var hv = (float) Mth.lerp(hpa, nxlv, nxhv);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lu = (float) Mth.lerp(lpb, nxlu, nxhu);
				var hu = (float) Mth.lerp(hpb, nxlu, nxhu);
				normSphereVertex(builder, center, radius, -1, ha, lb, lu, hv);
				normSphereVertex(builder, center, radius, -1, la, lb, lu, lv);
				normSphereVertex(builder, center, radius, -1, la, hb, hu, lv);
				normSphereVertex(builder, center, radius, -1, ha, hb, hu, hv);
			}
		}

		// +X
		double pxlu = 0.75f, pxlv = 0.50f, pxhu = 0.50f, pxhv = 0.25f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lv = (float) Mth.lerp(lpa, pxlv, pxhv);
			var hv = (float) Mth.lerp(hpa, pxlv, pxhv);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lu = (float) Mth.lerp(lpb, pxlu, pxhu);
				var hu = (float) Mth.lerp(hpb, pxlu, pxhu);
				normSphereVertex(builder, center, radius, 1, ha, lb, lu, hv);
				normSphereVertex(builder, center, radius, 1, ha, hb, hu, hv);
				normSphereVertex(builder, center, radius, 1, la, hb, hu, lv);
				normSphereVertex(builder, center, radius, 1, la, lb, lu, lv);
			}
		}

		// -Y
		double nylu = 0.25f, nylv = 0.75f, nyhu = 0.50f, nyhv = 0.50f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lu = (float) Mth.lerp(lpa, nylu, nyhu);
			var hu = (float) Mth.lerp(hpa, nylu, nyhu);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lv = (float) Mth.lerp(lpb, nylv, nyhv);
				var hv = (float) Mth.lerp(hpb, nylv, nyhv);
				normSphereVertex(builder, center, radius, ha, -1, lb, hu, lv);
				normSphereVertex(builder, center, radius, ha, -1, hb, hu, hv);
				normSphereVertex(builder, center, radius, la, -1, hb, lu, hv);
				normSphereVertex(builder, center, radius, la, -1, lb, lu, lv);
			}
		}

		// +Y
		double pylu = 0.25f, pylv = 0.00f, pyhu = 0.50f, pyhv = 0.25f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lu = (float) Mth.lerp(lpa, pylu, pyhu);
			var hu = (float) Mth.lerp(hpa, pylu, pyhu);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lv = (float) Mth.lerp(lpb, pylv, pyhv);
				var hv = (float) Mth.lerp(hpb, pylv, pyhv);
				normSphereVertex(builder, center, radius, ha, 1, lb, hu, lv);
				normSphereVertex(builder, center, radius, la, 1, lb, lu, lv);
				normSphereVertex(builder, center, radius, la, 1, hb, lu, hv);
				normSphereVertex(builder, center, radius, ha, 1, hb, hu, hv);
			}
		}

		// -Z
		double nzlu = 1.00f, nzlv = 0.50f, nzhu = 0.75f, nzhv = 0.25f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lu = (float) Mth.lerp(lpa, nzlu, nzhu);
			var hu = (float) Mth.lerp(hpa, nzlu, nzhu);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lv = (float) Mth.lerp(lpb, nzlv, nzhv);
				var hv = (float) Mth.lerp(hpb, nzlv, nzhv);
				normSphereVertex(builder, center, radius, ha, lb, -1, hu, lv);
				normSphereVertex(builder, center, radius, la, lb, -1, lu, lv);
				normSphereVertex(builder, center, radius, la, hb, -1, lu, hv);
				normSphereVertex(builder, center, radius, ha, hb, -1, hu, hv);
			}
		}

		// +Z
		double pzlu = 0.25f, pzlv = 0.50f, pzhu = 0.50f, pzhv = 0.25f;
		for (var pa = 0; pa < subdivisions; ++pa) {
			double lpa = (double) pa / (double) subdivisions, hpa = (double) (pa + 1) / (double) subdivisions;
			double la = 2 * lpa - 1, ha = 2 * hpa - 1;
			la = Math.tan(Math.PI / 4 * la);
			ha = Math.tan(Math.PI / 4 * ha);
			var lu = (float) Mth.lerp(lpa, pzlu, pzhu);
			var hu = (float) Mth.lerp(hpa, pzlu, pzhu);
			for (var pb = 0; pb < subdivisions; ++pb) {
				double lpb = (double) pb / (double) subdivisions, hpb = (double) (pb + 1) / (double) subdivisions;
				double lb = 2 * lpb - 1, hb = 2 * hpb - 1;
				lb = Math.tan(Math.PI / 4 * lb);
				hb = Math.tan(Math.PI / 4 * hb);
				var lv = (float) Mth.lerp(lpb, pzlv, pzhv);
				var hv = (float) Mth.lerp(hpb, pzlv, pzhv);
				normSphereVertex(builder, center, radius, ha, lb, 1, hu, lv);
				normSphereVertex(builder, center, radius, ha, hb, 1, hu, hv);
				normSphereVertex(builder, center, radius, la, hb, 1, lu, hv);
				normSphereVertex(builder, center, radius, la, lb, 1, lu, lv);
			}
		}
	}

	private static void normSphereVertex(FlexibleVertexConsumer builder,
			Vec3 center, double radius, double x, double y, double z,
			float u, float v) {
		final var pos = new Vec3(x, y, z);
		final var n = pos.normalize();
		final var p = n.mul(radius).add(center);
		builder.vertex(p).normal(n).uv0(u, v).color(ColorRgba.WHITE).endVertex();
	}

}
