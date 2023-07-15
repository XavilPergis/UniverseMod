package net.xavil.ultraviolet.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xavil.hawklib.Units;
import net.xavil.hawklib.client.gl.shader.ShaderProgram;
import net.xavil.ultraviolet.Mod;

import static net.xavil.hawklib.client.HawkDrawStates.*;
import static net.xavil.ultraviolet.client.UltravioletShaders.*;

import net.xavil.hawklib.client.HawkRendering;
import net.xavil.hawklib.client.camera.CachedCamera;
import net.xavil.hawklib.client.flexible.BufferRenderer;
import net.xavil.hawklib.client.flexible.VertexBuilder;
import net.xavil.hawklib.client.flexible.FlexibleVertexConsumer;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.hawklib.hash.FastHasher;
import net.xavil.hawklib.math.Color;
import net.xavil.hawklib.math.Quat;
import net.xavil.hawklib.math.TransformStack;
import net.xavil.hawklib.math.matrices.Mat4;
import net.xavil.hawklib.math.matrices.Vec3;

public final class PlanetRenderingContext {

	public static final ResourceLocation PLANET_ATLAS_LOCATION = Mod.namespaced("textures/atlas/planets");

	public static final ResourceLocation BASE_ROCKY_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/base_rocky.png");
	public static final ResourceLocation BASE_WATER_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/base_water.png");
	public static final ResourceLocation BASE_GAS_GIANT_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/base_gas_giant.png");
	public static final ResourceLocation FEATURE_CRATERS_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/craters.png");
	public static final ResourceLocation FEATURE_EARTH_LIKE_LOCATION = Mod
			.namespaced("textures/misc/celestialbodies/earth_like.png");

	private static ResourceLocation getBaseLayer(PlanetaryCelestialNode node) {
		final var missing = MissingTextureAtlasSprite.getLocation();
		return switch (node.type) {
			case EARTH_LIKE_WORLD -> BASE_WATER_LOCATION;
			case GAS_GIANT -> BASE_GAS_GIANT_LOCATION;
			case ICE_WORLD -> BASE_WATER_LOCATION;
			case ROCKY_ICE_WORLD -> BASE_WATER_LOCATION;
			case ROCKY_WORLD -> BASE_ROCKY_LOCATION;
			case WATER_WORLD -> BASE_WATER_LOCATION;
			default -> missing;
		};
	}

	public record PointLight(Vec3 pos, Color color, double luminosity) {
		public static PointLight fromStar(StellarCelestialNode node) {
			return new PointLight(node.position, node.getColor(), node.luminosityLsol);
		}
	}

	public final List<PointLight> pointLights = new ArrayList<>();
	private final Minecraft client = Minecraft.getInstance();
	private double celestialTime = 0;
	private Vec3 origin = Vec3.ZERO;
	private int renderedPlanetCount = 0;
	private int renderedStarCount = 0;

	public PlanetRenderingContext() {
	}

	public void begin(double celestialTime) {
		this.celestialTime = celestialTime;
		this.renderedPlanetCount = 0;
		this.renderedStarCount = 0;
	}

	public void end() {
	}

	public void setSystemOrigin(Vec3 origin) {
		this.origin = origin;
	}

	public int getRenderedPlanetCount() {
		return this.renderedPlanetCount;
	}

	public int getRenderedStarCount() {
		return this.renderedStarCount;
	}

	public void render(VertexBuilder builder, CachedCamera<?> camera, CelestialNode node, boolean skip) {
		render(builder, camera, node, new TransformStack(), Color.WHITE, skip);
	}

	public void render(VertexBuilder builder, CachedCamera<?> camera, CelestialNode node, TransformStack tfm,
			Color tintColor, boolean skip) {
		if (node instanceof StellarCelestialNode starNode)
			renderStar(builder, camera, starNode, tfm, tintColor, skip);
		if (node instanceof PlanetaryCelestialNode planetNode)
			renderPlanet(builder, camera, planetNode, tfm, tintColor, skip);
	}

	public void renderStar(VertexBuilder builder, CachedCamera<?> camera, StellarCelestialNode node,
			TransformStack tfm, Color tintColor, boolean skip) {
		var radiusM = Units.m_PER_Rsol * node.radiusRsol;

		final var partialTick = this.client.getFrameTime();

		var nodePosUnits = node.getPosition(partialTick).mul(1e12 / camera.metersPerUnit)
				.add(this.origin.mul(1e12 / camera.metersPerUnit));

		var distanceFromCamera = camera.pos.mul(1e12 / camera.metersPerUnit).distanceTo(nodePosUnits);
		var distanceRatio = radiusM / (distanceFromCamera * camera.metersPerUnit);

		// if (distanceRatio < 0.0001)
		// return;

		tfm.push();

		if (!skip) {
			var radiusUnits = radiusM / camera.metersPerUnit;
			builder.begin(VertexFormat.Mode.QUADS, UltravioletVertexFormats.PLANET_VERTEX_FORMAT);
			addNormSphere(builder, camera, tfm, nodePosUnits.mul(camera.metersPerUnit / 1e12), radiusUnits, tintColor);
			final var shader = getShader(SHADER_STAR);
			shader.setUniform("uStarColor", node.getColor());
			builder.end().draw(shader, DRAW_STATE_NO_CULL);
		}

		tfm.pop();

		// TODO: tint
		// tfm.push();
		// tfm.prependTranslation(this.origin);
		// RenderHelper.renderStarBillboard(builder, camera, tfm, node);
		// tfm.pop();

		this.renderedStarCount += 1;
	}

	private void setupShaderCommon(CachedCamera<?> camera, TransformStack tfm, Vec3 sortOrigin,
			ShaderProgram shader) {

		final int maxLightCount = 4;

		// this could probably be smarter. Merging stars that are close enough compared
		// to the distance of the planet, or prioritizing apparent brightness over just
		// distance.
		final var sortedLights = new ArrayList<>(this.pointLights);
		sortedLights.sort(Comparator.comparingDouble(light -> light.pos.distanceTo(sortOrigin)));

		final var lightCount = Math.min(maxLightCount, sortedLights.size());

		shader.setUniform("uMetersPerUnit", camera.metersPerUnit);
		shader.setUniform("uTime", this.celestialTime);

		for (var i = 0; i < maxLightCount; ++i) {
			shader.setUniform("uLightColor" + i, 0f, 0f, 0f, -1f);
		}

		for (var i = 0; i < lightCount; ++i) {
			final var light = sortedLights.get(i);

			float luminosity = (float) Math.max(light.luminosity, 0.4);
			shader.setUniform("uLightColor" + i, light.color.withA(luminosity));

			var pos = camera.toCameraSpace(light.pos.add(this.origin));
			shader.setUniform("uLightPos" + i, pos.withW(1));
		}

		BufferRenderer.setupCameraUniforms(shader, camera);
	}

	private void setupPlanetShader(PlanetaryCelestialNode node, ShaderProgram shader) {
		shader.setUniform("uPlanetType", switch (node.type) {
			case EARTH_LIKE_WORLD -> 0;
			case GAS_GIANT -> 1;
			case ICE_WORLD -> 2;
			case ROCKY_ICE_WORLD -> 3;
			case ROCKY_WORLD -> 4;
			case WATER_WORLD -> 5;
		});
		shader.setUniform("uRenderingSeed", (float) (FastHasher.hashInt(node.getId()) % 1000000L));
	}

	public void renderPlanet(VertexBuilder builder, CachedCamera<?> camera, PlanetaryCelestialNode node,
			TransformStack tfm, Color tintColor, boolean skip) {

		var radiusM = Units.m_PER_Rearth * node.radiusRearth;
		// var radiusM = 200 * Units.METERS_PER_REARTH * node.radiusRearth;
		// final var metersPerUnit = 1 / unitsPerMeter;

		final var partialTick = this.client.getFrameTime();

		var nodePosUnits = node.getPosition(partialTick).mul(1e12 / camera.metersPerUnit)
				.add(this.origin.mul(1e12 / camera.metersPerUnit));

		var distanceFromCamera = camera.pos.mul(1e12 / camera.metersPerUnit).distanceTo(nodePosUnits);
		var distanceRatio = radiusM / (distanceFromCamera * camera.metersPerUnit);

		if (distanceRatio < 0.01) {
			// // TODO: tint
			// tfm.push();
			// tfm.prependTranslation(this.origin);
			// RenderHelper.renderStarBillboard(builder, camera, tfm, node);
			// tfm.pop();
		}

		if (distanceRatio < 0.0001)
			return;

		tfm.push();
		var rotationalSpeed = -2.0 * Math.PI / node.rotationalPeriod;
		tfm.prependRotation(Quat.axisAngle(Vec3.XP, node.obliquityAngle));
		tfm.prependRotation(Quat.axisAngle(Vec3.YP, rotationalSpeed * this.celestialTime));

		setupShaderCommon(camera, tfm, nodePosUnits, getShader(SHADER_PLANET));
		setupShaderCommon(camera, tfm, nodePosUnits, getShader(SHADER_RING));
		setupPlanetShader(node, getShader(SHADER_PLANET));

		if (!skip) {
			var baseTexture = getBaseLayer(node);
			Minecraft.getInstance().getTextureManager().getTexture(baseTexture).setFilter(false, false);

			var radiusUnits = radiusM / camera.metersPerUnit;
			// var radiusUnits = 1000 * 1000;
			// var radiusM = scale * 200 * Units.METERS_PER_REARTH * node.radiusRearth;
			// renderPlanetLayer(builder, camera, baseTexture, tfm, nodePosUnits,
			// radiusUnits, tintColor);
			// if (node.type == PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD) {
			// }
			renderPlanetLayer(builder, camera, BASE_ROCKY_LOCATION, tfm, nodePosUnits.mul(camera.metersPerUnit / 1e12),
					radiusUnits, tintColor);
		}

		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		for (var ring : node.rings()) {
			tfm.push();
			tfm.prependRotation(ring.orbitalPlane.rotationFromReference());
			addRing(builder, camera, tfm, nodePosUnits.mul(camera.metersPerUnit / 1e12), ring.interval.lower(),
					ring.interval.higher(), tintColor);
			tfm.pop();
		}

		final var ringShader = getShader(SHADER_RING);
		builder.end().draw(ringShader, DRAW_STATE_OPAQUE);

		tfm.pop();

		this.renderedPlanetCount += 1;
	}

	private static void renderPlanetLayer(VertexBuilder builder, CachedCamera<?> camera,
			ResourceLocation texture, @Nullable TransformStack tfm, Vec3 center, double radius, Color tintColor) {
		final var planetShader = getShader(SHADER_PLANET);
		// planetShader.setUniformSampler("uSurfaceAlbedo",
		// GlTexture2d.importTexture(texture));
		builder.begin(VertexFormat.Mode.QUADS, UltravioletVertexFormats.PLANET_VERTEX_FORMAT);
		addNormSphere(builder, camera, tfm, center, radius, tintColor);
		builder.end().draw(planetShader, DRAW_STATE_OPAQUE);
	}

	private static void addRing(FlexibleVertexConsumer builder, CachedCamera<?> camera, @Nullable TransformStack tfm,
			Vec3 center, double innerRadius, double outerRadius, Color tintColor) {
		final var pose = tfm == null ? null : tfm.get();
		int segmentCount = 60;
		for (var i = 0; i < segmentCount; ++i) {
			double percentL = i / (double) segmentCount;
			double percentH = (i + 1) / (double) segmentCount;
			double angleL = 2 * Math.PI * percentL;
			double angleH = 2 * Math.PI * percentH;
			double llx = innerRadius * (Units.TERA / camera.metersPerUnit) * Math.cos(angleL);
			double lly = innerRadius * (Units.TERA / camera.metersPerUnit) * Math.sin(angleL);
			double lhx = innerRadius * (Units.TERA / camera.metersPerUnit) * Math.cos(angleH);
			double lhy = innerRadius * (Units.TERA / camera.metersPerUnit) * Math.sin(angleH);
			double hlx = outerRadius * (Units.TERA / camera.metersPerUnit) * Math.cos(angleL);
			double hly = outerRadius * (Units.TERA / camera.metersPerUnit) * Math.sin(angleL);
			double hhx = outerRadius * (Units.TERA / camera.metersPerUnit) * Math.cos(angleH);
			double hhy = outerRadius * (Units.TERA / camera.metersPerUnit) * Math.sin(angleH);

			// clockwise
			ringVertex(builder, camera, pose, center, tintColor, lhx, 1, lhy, (float) percentH, 0);
			ringVertex(builder, camera, pose, center, tintColor, llx, 1, lly, (float) percentL, 0);
			ringVertex(builder, camera, pose, center, tintColor, hlx, 1, hly, (float) percentL, 10);
			ringVertex(builder, camera, pose, center, tintColor, hhx, 1, hhy, (float) percentH, 10);

			// counter-clockwise
			ringVertex(builder, camera, pose, center, tintColor, hhx, -1, hhy, (float) percentH, 10);
			ringVertex(builder, camera, pose, center, tintColor, hlx, -1, hly, (float) percentL, 10);
			ringVertex(builder, camera, pose, center, tintColor, llx, -1, lly, (float) percentL, 0);
			ringVertex(builder, camera, pose, center, tintColor, lhx, -1, lhy, (float) percentH, 0);
		}
	}

	private static void addNormSphere(VertexBuilder builder, CachedCamera<?> camera,
			@Nullable TransformStack tfm, Vec3 center, double radius, Color tintColor) {

		final var pose = tfm == null ? null : tfm.get();
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
				normSphereVertex(builder, camera, pose, center, tintColor, radius, -1, ha, lb, lu, hv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, -1, la, lb, lu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, -1, la, hb, hu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, -1, ha, hb, hu, hv);
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
				normSphereVertex(builder, camera, pose, center, tintColor, radius, 1, ha, lb, lu, hv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, 1, la, lb, lu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, 1, la, hb, hu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, 1, ha, hb, hu, hv);
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
				normSphereVertex(builder, camera, pose, center, tintColor, radius, ha, -1, lb, hu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, la, -1, lb, lu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, la, -1, hb, lu, hv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, ha, -1, hb, hu, hv);
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
				normSphereVertex(builder, camera, pose, center, tintColor, radius, ha, 1, lb, hu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, la, 1, lb, lu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, la, 1, hb, lu, hv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, ha, 1, hb, hu, hv);
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
				normSphereVertex(builder, camera, pose, center, tintColor, radius, ha, lb, -1, hu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, la, lb, -1, lu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, la, hb, -1, lu, hv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, ha, hb, -1, hu, hv);
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
				normSphereVertex(builder, camera, pose, center, tintColor, radius, ha, lb, 1, hu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, la, lb, 1, lu, lv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, la, hb, 1, lu, hv);
				normSphereVertex(builder, camera, pose, center, tintColor, radius, ha, hb, 1, hu, hv);
			}
		}

	}

	// private static void addCube(VertexConsumer builder, CachedCamera<?> camera,
	// TransformStack tfm, Vec3 center,
	// double radius, Color tintColor) {

	// final double nr = -radius, pr = radius;
	// final var nnn = center.add(nr, nr, nr);
	// final var nnp = center.add(nr, nr, pr);
	// final var npn = center.add(nr, pr, nr);
	// final var npp = center.add(nr, pr, pr);
	// final var pnn = center.add(pr, nr, nr);
	// final var pnp = center.add(pr, nr, pr);
	// final var ppn = center.add(pr, pr, nr);
	// final var ppp = center.add(pr, pr, pr);

	// final var pose = tfm.get();

	// // -X
	// cubeVertex(builder, camera, pose, tintColor, npn.x, npn.y, npn.z, -1, 0, 0,
	// 0.00f, 0.25f);
	// cubeVertex(builder, camera, pose, tintColor, nnn.x, nnn.y, nnn.z, -1, 0, 0,
	// 0.00f, 0.50f);
	// cubeVertex(builder, camera, pose, tintColor, nnp.x, nnp.y, nnp.z, -1, 0, 0,
	// 0.25f, 0.50f);
	// cubeVertex(builder, camera, pose, tintColor, npp.x, npp.y, npp.z, -1, 0, 0,
	// 0.25f, 0.25f);
	// // +X
	// cubeVertex(builder, camera, pose, tintColor, pnn.x, pnn.y, pnn.z, 1, 0, 0,
	// 0.75f, 0.50f);
	// cubeVertex(builder, camera, pose, tintColor, ppn.x, ppn.y, ppn.z, 1, 0, 0,
	// 0.75f, 0.25f);
	// cubeVertex(builder, camera, pose, tintColor, ppp.x, ppp.y, ppp.z, 1, 0, 0,
	// 0.50f, 0.25f);
	// cubeVertex(builder, camera, pose, tintColor, pnp.x, pnp.y, pnp.z, 1, 0, 0,
	// 0.50f, 0.50f);
	// // -Y
	// cubeVertex(builder, camera, pose, tintColor, nnn.x, nnn.y, nnn.z, 0, -1, 0,
	// 0.25f, 0.75f);
	// cubeVertex(builder, camera, pose, tintColor, pnn.x, pnn.y, pnn.z, 0, -1, 0,
	// 0.50f, 0.75f);
	// cubeVertex(builder, camera, pose, tintColor, pnp.x, pnp.y, pnp.z, 0, -1, 0,
	// 0.50f, 0.50f);
	// cubeVertex(builder, camera, pose, tintColor, nnp.x, nnp.y, nnp.z, 0, -1, 0,
	// 0.25f, 0.50f);
	// // +Y
	// cubeVertex(builder, camera, pose, tintColor, ppn.x, ppn.y, ppn.z, 0, 1, 0,
	// 0.50f, 0.00f);
	// cubeVertex(builder, camera, pose, tintColor, npn.x, npn.y, npn.z, 0, 1, 0,
	// 0.25f, 0.00f);
	// cubeVertex(builder, camera, pose, tintColor, npp.x, npp.y, npp.z, 0, 1, 0,
	// 0.25f, 0.25f);
	// cubeVertex(builder, camera, pose, tintColor, ppp.x, ppp.y, ppp.z, 0, 1, 0,
	// 0.50f, 0.25f);
	// // -Z
	// cubeVertex(builder, camera, pose, tintColor, pnn.x, pnn.y, pnn.z, 0, 0, -1,
	// 0.75f, 0.50f);
	// cubeVertex(builder, camera, pose, tintColor, nnn.x, nnn.y, nnn.z, 0, 0, -1,
	// 1.00f, 0.50f);
	// cubeVertex(builder, camera, pose, tintColor, npn.x, npn.y, npn.z, 0, 0, -1,
	// 1.00f, 0.25f);
	// cubeVertex(builder, camera, pose, tintColor, ppn.x, ppn.y, ppn.z, 0, 0, -1,
	// 0.75f, 0.25f);
	// // +Z
	// cubeVertex(builder, camera, pose, tintColor, nnp.x, nnp.y, nnp.z, 0, 0, 1,
	// 0.25f, 0.50f);
	// cubeVertex(builder, camera, pose, tintColor, pnp.x, pnp.y, pnp.z, 0, 0, 1,
	// 0.50f, 0.50f);
	// cubeVertex(builder, camera, pose, tintColor, ppp.x, ppp.y, ppp.z, 0, 0, 1,
	// 0.50f, 0.25f);
	// cubeVertex(builder, camera, pose, tintColor, npp.x, npp.y, npp.z, 0, 0, 1,
	// 0.25f, 0.25f);
	// }

	private static void ringVertex(FlexibleVertexConsumer builder, CachedCamera<?> camera,
			@Nullable Mat4 pose, Vec3 center, Color color, double x, double y, double z, float u, float v) {
		var pos = Vec3.from(x, 0, z);
		pos = pose != null ? pos.transformBy(pose) : pos;
		var norm = y > 0 ? Vec3.YN : Vec3.YP;
		norm = pose != null ? norm.transformBy(pose) : norm;
		norm = norm.normalize();
		var p = camera.toCameraSpace(pos).add(center);
		builder.vertex(p).uv0(u, v).color(color).normal(norm).endVertex();
	}

	private static void normSphereVertex(VertexBuilder builder, CachedCamera<?> camera,
			@Nullable Mat4 pose, Vec3 center, Color color, double radius, double x, double y, double z,
			float u, float v) {
		var pos = Vec3.from(x, y, z);
		var n = pos.normalize();
		n = pose != null ? n.transformBy(pose) : n;
		// var p = camera.toCameraSpace(n.mul(radius));
		var p = camera.toCameraSpace(n.mul(radius)).add(center);
		builder.vertex(p).uv0(u, v).color(color).normal(n).endVertex();
	}

	// private static void cubeVertex(VertexConsumer builder, CachedCamera<?>
	// camera, PoseStack.Pose pose, Color color,
	// double x, double y, double z, double nx, double ny, double nz, float u, float
	// v) {
	// final float r = color.r(), g = color.g(), b = color.b(), a = color.a();
	// builder.vertex(pose.pose(), (float) x, (float) y, (float) z)
	// .uv(u, v)
	// .color(r, g, b, a)
	// .normal(pose.normal(), (float) nx, (float) ny, (float) nz)
	// .endVertex();
	// }

}
