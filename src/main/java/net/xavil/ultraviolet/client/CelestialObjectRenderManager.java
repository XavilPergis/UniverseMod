package net.xavil.ultraviolet.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.collections.impl.Vector;
import net.xavil.hawklib.collections.interfaces.MutableList;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.matrices.Vec3;
import net.xavil.hawklib.math.matrices.interfaces.Vec3Access;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;

public final class CelestialObjectRenderManager {

	private static final Minecraft CLIENT = Minecraft.getInstance();
	private CelestialNode node;

	public CelestialObjectRenderManager(CelestialNode node) {
		this.node = node;
	}

	// public static class Context {
	// 	public record PointLight(Vec3 pos, Color color, double luminosity) {
	// 		public static PointLight fromStar(StellarCelestialNode node) {
	// 			return new PointLight(node.position, node.getColor(), node.luminosityLsol);
	// 		}
	// 	}
	
	// 	public final MutableList<PointLight> pointLights = new Vector<>();
	// 	public double celestialTime = 0;
	// 	public Vec3 origin = Vec3.ZERO;

	// 	public int renderedPlanetCount = 0;
	// 	public int renderedStarCount = 0;
	// }

	// private double nodeRadius_km() {
		
	// }

	// public void render(VertexBuilder builder, CachedCamera<?> camera, Context ctx) {

	// 	final var radiusM = Units.m_PER_Rearth * node.radiusRearth;
	// 	final var partialTick = CLIENT.getFrameTime();

	// 	final var nodePos = node.getPosition(partialTick).add(ctx.origin).mul(1e12 / camera.metersPerUnit);

	// 	final var distanceFromCamera = camera.pos.mul(1e12 / camera.metersPerUnit).distanceTo(nodePos);
	// 	final var distanceRatio = radiusM / (distanceFromCamera * camera.metersPerUnit);

	// 	if (distanceRatio < 0.0001)
	// 		return;

	// 	setupShaderCommon(camera, nodePos, UltravioletShaders.SHADER_PLANET.get());
	// 	setupShaderCommon(camera, nodePos, UltravioletShaders.SHADER_RING.get());
	// 	setupPlanetShader(node, getShader(SHADER_PLANET));
			
	// 	final var modelTfm = new TransformStack();
	// 	modelTfm.prependRotation(Quat.axisAngle(Vec3.XP, node.obliquityAngle));
	// 	modelTfm.prependRotation(Quat.axisAngle(Vec3.YP, -node.rotationalRate * this.celestialTime));

	// 	getShader(SHADER_PLANET).setUniform("uModelMatrix", modelTfm.current());
	// 	getShader(SHADER_RING).setUniform("uModelMatrix", modelTfm.current());

	// 	if (!skip) {
	// 		var baseTexture = getBaseLayer(node);
	// 		Minecraft.getInstance().getTextureManager().getTexture(baseTexture).setFilter(false, false);

	// 		var radiusUnits = 1.0 * radiusM / camera.metersPerUnit;
	// 		// var radiusUnits = 1000 * 1000;
	// 		// var radiusM = scale * 200 * Units.METERS_PER_REARTH * node.radiusRearth;
	// 		// renderPlanetLayer(builder, camera, baseTexture, tfm, nodePos,
	// 		// radiusUnits, tintColor);
	// 		// if (node.type == PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD) {
	// 		// }
	// 		renderPlanetLayer(builder, camera, BASE_ROCKY_LOCATION, nodePos.mul(camera.metersPerUnit / 1e12),
	// 				radiusUnits);
	// 	}

	// 	builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
	// 	for (var ring : node.rings()) {
	// 		// tfm.push();
	// 		modelTfm.prependRotation(ring.orbitalPlane.rotationFromReference());
	// 		addRing(builder, camera, nodePos.mul(camera.metersPerUnit / 1e12), ring.interval.lower(),
	// 				ring.interval.higher());
	// 		// tfm.pop();
	// 	}

	// 	final var ringShader = getShader(SHADER_RING);
	// 	builder.end().draw(ringShader, DRAW_STATE_OPAQUE);

	// 	this.renderedPlanetCount += 1;
	// }

	private static void addRing(FlexibleVertexConsumer builder, CachedCamera<?> camera,
			Vec3 center, double innerRadius, double outerRadius) {
		int segmentCount = 60;
		for (var i = 0; i < segmentCount; ++i) {
			double percentL = i / (double) segmentCount;
			double percentH = (i + 1) / (double) segmentCount;
			double angleL = 2 * Math.PI * percentL;
			double angleH = 2 * Math.PI * percentH;
			double llx = innerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.cos(angleL);
			double lly = innerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.sin(angleL);
			double lhx = innerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.cos(angleH);
			double lhy = innerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.sin(angleH);
			double hlx = outerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.cos(angleL);
			double hly = outerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.sin(angleL);
			double hhx = outerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.cos(angleH);
			double hhy = outerRadius * (Units.u_PER_Tu / camera.metersPerUnit) * Math.sin(angleH);

			// clockwise
			ringVertex(builder, camera, center, lhx, 1, lhy, (float) percentH, 0);
			ringVertex(builder, camera, center, llx, 1, lly, (float) percentL, 0);
			ringVertex(builder, camera, center, hlx, 1, hly, (float) percentL, 10);
			ringVertex(builder, camera, center, hhx, 1, hhy, (float) percentH, 10);

			// counter-clockwise
			ringVertex(builder, camera, center, hhx, -1, hhy, (float) percentH, 10);
			ringVertex(builder, camera, center, hlx, -1, hly, (float) percentL, 10);
			ringVertex(builder, camera, center, llx, -1, lly, (float) percentL, 0);
			ringVertex(builder, camera, center, lhx, -1, lhy, (float) percentH, 0);
		}
	}

	private static void addNormSphere(FlexibleVertexConsumer builder, CachedCamera<?> camera, Vec3 center, double radius) {
		final int subdivisions = 10;

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
				normSphereVertex(builder, camera, center, radius, -1, ha, lb, lu, hv);
				normSphereVertex(builder, camera, center, radius, -1, la, lb, lu, lv);
				normSphereVertex(builder, camera, center, radius, -1, la, hb, hu, lv);
				normSphereVertex(builder, camera, center, radius, -1, ha, hb, hu, hv);
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
				normSphereVertex(builder, camera, center, radius, 1, ha, lb, lu, hv);
				normSphereVertex(builder, camera, center, radius, 1, la, lb, lu, lv);
				normSphereVertex(builder, camera, center, radius, 1, la, hb, hu, lv);
				normSphereVertex(builder, camera, center, radius, 1, ha, hb, hu, hv);
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
				normSphereVertex(builder, camera, center, radius, ha, -1, lb, hu, lv);
				normSphereVertex(builder, camera, center, radius, la, -1, lb, lu, lv);
				normSphereVertex(builder, camera, center, radius, la, -1, hb, lu, hv);
				normSphereVertex(builder, camera, center, radius, ha, -1, hb, hu, hv);
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
				normSphereVertex(builder, camera, center, radius, ha, 1, lb, hu, lv);
				normSphereVertex(builder, camera, center, radius, la, 1, lb, lu, lv);
				normSphereVertex(builder, camera, center, radius, la, 1, hb, lu, hv);
				normSphereVertex(builder, camera, center, radius, ha, 1, hb, hu, hv);
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
				normSphereVertex(builder, camera, center, radius, ha, lb, -1, hu, lv);
				normSphereVertex(builder, camera, center, radius, la, lb, -1, lu, lv);
				normSphereVertex(builder, camera, center, radius, la, hb, -1, lu, hv);
				normSphereVertex(builder, camera, center, radius, ha, hb, -1, hu, hv);
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
				normSphereVertex(builder, camera, center, radius, ha, lb, 1, hu, lv);
				normSphereVertex(builder, camera, center, radius, la, lb, 1, lu, lv);
				normSphereVertex(builder, camera, center, radius, la, hb, 1, lu, hv);
				normSphereVertex(builder, camera, center, radius, ha, hb, 1, hu, hv);
			}
		}
	}

	private static void ringVertex(FlexibleVertexConsumer builder, CachedCamera<?> camera,
			Vec3Access center, double x, double y, double z, float u, float v) {
		var pos = new Vec3(x, 0, z);
		// pos = pose != null ? pos.transformBy(pose) : pos;
		var norm = y > 0 ? Vec3.YN : Vec3.YP;
		// norm = pose != null ? norm.transformBy(pose) : norm;
		norm = norm.normalize();
		var p = camera.toCameraSpace(pos).add(center);
		builder.vertex(p).uv0(u, v).color(Color.WHITE).normal(norm).endVertex();
	}

	private static void normSphereVertex(FlexibleVertexConsumer builder, CachedCamera<?> camera,
			Vec3Access center, double radius, double x, double y, double z,
			float u, float v) {
		var pos = new Vec3(x, y, z);
		var n = pos.normalize();
		// n = pose != null ? n.transformBy(pose) : n;
		var p = camera.toCameraSpace(n.mul(radius)).add(center);
		builder.vertex(p).uv0(u, v).color(Color.WHITE).normal(n).endVertex();
	}

}
