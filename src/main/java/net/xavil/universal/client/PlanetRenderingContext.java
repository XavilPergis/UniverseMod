package net.xavil.universal.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.xavil.universal.Mod;
import net.xavil.universal.client.flexible.FlexibleBufferBuilder;
import net.xavil.universal.client.screen.CachedCamera;
import net.xavil.universal.client.screen.RenderHelper;
import net.xavil.universegen.system.CelestialNode;
import net.xavil.universegen.system.PlanetaryCelestialNode;
import net.xavil.universegen.system.StellarCelestialNode;
import net.xavil.util.FastHasher;
import net.xavil.util.Units;
import net.xavil.util.math.Color;
import net.xavil.util.math.Quat;
import net.xavil.util.math.Vec3;

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
		var missing = MissingTextureAtlasSprite.getLocation();
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
	private final double celestialTime;
	private int renderedPlanetCount = 0;
	private int renderedStarCount = 0;

	public PlanetRenderingContext(double celestialTime) {
		this.celestialTime = celestialTime;
	}

	public int getRenderedPlanetCount() {
		return this.renderedPlanetCount;
	}

	public int getRenderedStarCount() {
		return this.renderedStarCount;
	}

	public void render(FlexibleBufferBuilder builder, CachedCamera<?> camera, CelestialNode node, PoseStack poseStack,
			Color tintColor, boolean skip) {
		if (node instanceof StellarCelestialNode starNode)
			renderStar(builder, camera, starNode, poseStack, tintColor, skip);
		if (node instanceof PlanetaryCelestialNode planetNode)
			renderPlanet(builder, camera, planetNode, poseStack, tintColor, skip);
	}

	public void renderStar(FlexibleBufferBuilder builder, CachedCamera<?> camera, StellarCelestialNode node,
			PoseStack poseStack, Color tintColor, boolean skip) {
		// RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
		// builder.begin(VertexFormat.Mode.QUADS,
		// DefaultVertexFormat.POSITION_COLOR_TEX);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
		// addBillboard(builder, camera, node, center, tmPerUnit, partialTick);
		// double d = RenderHelper.getCelestialBodySize(camera, node);

		poseStack.pushPose();
		// var rotationalSpeed = 2 * Math.PI / node.rotationalPeriod;
		// poseStack.mulPose(Vector3f.YP.rotationDegrees((float) (rotationalSpeed *
		// this.celestialTime)));

		// TODO: tint
		RenderHelper.renderStarBillboard(builder, camera, poseStack, node);

		poseStack.popPose();

		// var aaa = new PoseStack();
		// aaa.last().pose().multiply(RenderSystem.getModelViewMatrix());
		// aaa.last().pose().multiply(poseStack.last().pose());
		// // var aaa = RenderSystem.getModelViewMatrix().copy();
		// // RenderHelper.addBillboard(builder, Vec3.YP, Vec3.XP, node, pos, d);

		// builder.end();
		// this.client.getTextureManager().getTexture(RenderHelper.STAR_ICON_LOCATION).setFilter(true,
		// false);
		// RenderSystem.setShaderTexture(0, RenderHelper.STAR_ICON_LOCATION);
		// RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
		// GlStateManager.DestFactor.ONE);
		// RenderSystem.depthMask(true);
		// RenderSystem.enableDepthTest();
		// BufferUploader.end(builder);

		this.renderedStarCount += 1;
		// var planetNode = new PlanetNode(PlanetNode.Type.EARTH_LIKE_WORLD,
		// node.massYg,
		// Units.METERS_PER_RSOL * node.radiusRsol / Units.METERS_PER_REARTH,
		// node.temperatureK);
		// renderPlanet(planetNode, poseStack, pos, scale, tintColor);
	}

	private void setupShaderCommon(CachedCamera<?> camera, PoseStack poseStack, Vec3 sortOrigin,
			ShaderInstance shader) {

		final int maxLightCount = 4;

		// this could probably be smarter. Merging stars that are close enough compared
		// to the distance of the planet, or prioritizing apparent brightness over just
		// distance.
		final var sortedLights = new ArrayList<>(this.pointLights);
		sortedLights.sort(Comparator.comparingDouble(light -> light.pos.distanceTo(sortOrigin)));

		final var lightCount = Math.min(maxLightCount, sortedLights.size());

		var metersPerUnit = shader.safeGetUniform("MetersPerUnit");
		metersPerUnit.set((float) camera.metersPerUnit);
		var time = shader.safeGetUniform("Time");
		time.set((float) this.celestialTime);

		for (var i = 0; i < maxLightCount; ++i) {
			var lightColor = shader.safeGetUniform("LightColor" + i);
			lightColor.set(new Vector4f(0, 0, 0, -1));
		}

		for (var i = 0; i < lightCount; ++i) {
			var lightColor = shader.safeGetUniform("LightColor" + i);
			var lightPos = shader.safeGetUniform("LightPos" + i);

			final var light = sortedLights.get(i);

			float r = light.color.r(), g = light.color.g(), b = light.color.b();
			float luminosity = (float) Math.max(light.luminosity, 0.4);
			lightColor.set(new Vector4f(r, g, b, luminosity));
			// lightColor.set(new Vector4f(r, g, b, luminosity * 0.2f));

			var pos = camera.toCameraSpace(light.pos);
			var shaderPos = new Vector4f((float) pos.x, (float) pos.y, (float) pos.z, 1);
			// shaderPos.transform(poseStack.last().pose());
			// poseStack
			lightPos.set(shaderPos);
		}

	}

	private void setupPlanetShader(PlanetaryCelestialNode node, ShaderInstance shader) {
		final var isGasGiant = shader.safeGetUniform("IsGasGiant");
		isGasGiant.set(node.type == PlanetaryCelestialNode.Type.GAS_GIANT ? 1 : 0);
		final var renderingSeed = shader.safeGetUniform("RenderingSeed");
		renderingSeed.set((float) (FastHasher.hashInt(node.getId()) % 1000000L));
	}

	public void renderPlanet(FlexibleBufferBuilder builder, CachedCamera<?> camera, PlanetaryCelestialNode node,
			PoseStack poseStack, Color tintColor, boolean skip) {

		poseStack.pushPose();
		var rotationalSpeed = 2.0 * Math.PI / node.rotationalPeriod;
		poseStack.mulPose(Vector3f.YP.rotation((float) (rotationalSpeed * this.celestialTime)));

		var radiusM = Units.m_PER_Rearth * node.radiusRearth;
		// var radiusM = 200 * Units.METERS_PER_REARTH * node.radiusRearth;
		// final var metersPerUnit = 1 / unitsPerMeter;

		var distanceFromCamera = camera.pos.mul(camera.metersPerUnit).distanceTo(node.position.mul(1e12));
		// if (distanceFromCamera > 10000 * radiusM)
		// 	return;

		var nodePosUnits = node.position.mul(1e12 / camera.metersPerUnit);

		setupShaderCommon(camera, poseStack, nodePosUnits, ModRendering.getShader(ModRendering.PLANET_SHADER));
		setupShaderCommon(camera, poseStack, nodePosUnits, ModRendering.getShader(ModRendering.RING_SHADER));
		setupPlanetShader(node, ModRendering.getShader(ModRendering.PLANET_SHADER));

		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.depthMask(true);

		if (!skip) {
			var baseTexture = getBaseLayer(node);
			Minecraft.getInstance().getTextureManager().getTexture(baseTexture).setFilter(false, false);

			var radiusUnits = radiusM / camera.metersPerUnit;
			// var radiusUnits = 1000 * 1000;
			// var radiusM = scale * 200 * Units.METERS_PER_REARTH * node.radiusRearth;
			renderPlanetLayer(builder, camera, baseTexture, poseStack, nodePosUnits, radiusUnits, tintColor);
			if (node.type == PlanetaryCelestialNode.Type.EARTH_LIKE_WORLD) {
				renderPlanetLayer(builder, camera, FEATURE_EARTH_LIKE_LOCATION, poseStack, nodePosUnits, radiusUnits,
						tintColor);
			}
		}

		// RenderSystem.setShader(() -> GameRenderer.getPositionTexColorNormalShader());
		// RenderSystem.setShader(() ->
		// ModRendering.getShader(ModRendering.RING_SHADER));
		builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
		for (var ring : node.rings()) {
			poseStack.pushPose();
			poseStack.mulPose(ring.orbitalPlane.rotationFromReference().toMinecraft());
			addRing(builder.asVanilla(), camera, poseStack, nodePosUnits, ring.interval.lower(), ring.interval.higher(),
					tintColor);
			poseStack.popPose();
		}
		// poseStack.pushPose();
		// // poseStack.mulPose(Quat.axisAngle(Vec3.XP, 3.0 *
		// this.celestialTime).toMinecraft());
		// // poseStack.mulPose(Quat.axisAngle(Vec3.YP, 4.0 *
		// this.celestialTime).toMinecraft());
		// // poseStack.mulPose(Quat.axisAngle(Vec3.ZP, 5.0 *
		// this.celestialTime).toMinecraft());
		// addRing(builder, camera, poseStack, nodePosUnits, 2.0 * node.radiusRearth *
		// (Units.m_PER_Rearth / Units.TERA),
		// 4.0 * node.radiusRearth * (Units.m_PER_Rearth / Units.TERA), tintColor);
		// poseStack.popPose();

		builder.end();
		RenderSystem.setShaderTexture(0, BASE_ROCKY_LOCATION);
		RenderSystem.enableCull();
		builder.draw(ModRendering.getShader(ModRendering.RING_SHADER));
		// RenderSystem.disableDepthTest();
		// BufferUploader.end(builder);

		poseStack.popPose();

		this.renderedPlanetCount += 1;
	}

	private static void renderPlanetLayer(FlexibleBufferBuilder builder, CachedCamera<?> camera,
			ResourceLocation texture,
			PoseStack poseStack, Vec3 center, double radius, Color tintColor) {
		builder.begin(VertexFormat.Mode.QUADS, ModRendering.PLANET_VERTEX_FORMAT);
		addNormSphere(builder, camera, poseStack, center, radius, tintColor);
		builder.end();
		RenderSystem.setShaderTexture(0, texture);
		RenderSystem.disableCull();
		builder.draw(ModRendering.getShader(ModRendering.PLANET_SHADER));
	}

	private static void addRing(VertexConsumer builder, CachedCamera<?> camera, PoseStack poseStack, Vec3 center,
			double innerRadius, double outerRadius, Color tintColor) {
		final var pose = poseStack.last();
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

	private static void addNormSphere(FlexibleBufferBuilder builder, CachedCamera<?> camera, PoseStack poseStack,
			Vec3 center, double radius, Color tintColor) {

		// final var atlas =
		// UniversalTextureManager.INSTANCE.getAtlas(PLANET_ATLAS_LOCATION);

		// atlas.getSprite(BASE_GAS_GIANT_LOCATION);

		poseStack.pushPose();
		// poseStack.translate(center.x, center.y, center.z);
		final var pose = poseStack.last();
		final var subdivisions = 10;

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

		poseStack.popPose();
	}

	private static void addCube(VertexConsumer builder, CachedCamera<?> camera, PoseStack poseStack, Vec3 center,
			double radius, Color tintColor) {

		final double nr = -radius, pr = radius;
		final var nnn = center.add(nr, nr, nr);
		final var nnp = center.add(nr, nr, pr);
		final var npn = center.add(nr, pr, nr);
		final var npp = center.add(nr, pr, pr);
		final var pnn = center.add(pr, nr, nr);
		final var pnp = center.add(pr, nr, pr);
		final var ppn = center.add(pr, pr, nr);
		final var ppp = center.add(pr, pr, pr);

		final var pose = poseStack.last();

		// -X
		cubeVertex(builder, camera, pose, tintColor, npn.x, npn.y, npn.z, -1, 0, 0, 0.00f, 0.25f);
		cubeVertex(builder, camera, pose, tintColor, nnn.x, nnn.y, nnn.z, -1, 0, 0, 0.00f, 0.50f);
		cubeVertex(builder, camera, pose, tintColor, nnp.x, nnp.y, nnp.z, -1, 0, 0, 0.25f, 0.50f);
		cubeVertex(builder, camera, pose, tintColor, npp.x, npp.y, npp.z, -1, 0, 0, 0.25f, 0.25f);
		// +X
		cubeVertex(builder, camera, pose, tintColor, pnn.x, pnn.y, pnn.z, 1, 0, 0, 0.75f, 0.50f);
		cubeVertex(builder, camera, pose, tintColor, ppn.x, ppn.y, ppn.z, 1, 0, 0, 0.75f, 0.25f);
		cubeVertex(builder, camera, pose, tintColor, ppp.x, ppp.y, ppp.z, 1, 0, 0, 0.50f, 0.25f);
		cubeVertex(builder, camera, pose, tintColor, pnp.x, pnp.y, pnp.z, 1, 0, 0, 0.50f, 0.50f);
		// -Y
		cubeVertex(builder, camera, pose, tintColor, nnn.x, nnn.y, nnn.z, 0, -1, 0, 0.25f, 0.75f);
		cubeVertex(builder, camera, pose, tintColor, pnn.x, pnn.y, pnn.z, 0, -1, 0, 0.50f, 0.75f);
		cubeVertex(builder, camera, pose, tintColor, pnp.x, pnp.y, pnp.z, 0, -1, 0, 0.50f, 0.50f);
		cubeVertex(builder, camera, pose, tintColor, nnp.x, nnp.y, nnp.z, 0, -1, 0, 0.25f, 0.50f);
		// +Y
		cubeVertex(builder, camera, pose, tintColor, ppn.x, ppn.y, ppn.z, 0, 1, 0, 0.50f, 0.00f);
		cubeVertex(builder, camera, pose, tintColor, npn.x, npn.y, npn.z, 0, 1, 0, 0.25f, 0.00f);
		cubeVertex(builder, camera, pose, tintColor, npp.x, npp.y, npp.z, 0, 1, 0, 0.25f, 0.25f);
		cubeVertex(builder, camera, pose, tintColor, ppp.x, ppp.y, ppp.z, 0, 1, 0, 0.50f, 0.25f);
		// -Z
		cubeVertex(builder, camera, pose, tintColor, pnn.x, pnn.y, pnn.z, 0, 0, -1, 0.75f, 0.50f);
		cubeVertex(builder, camera, pose, tintColor, nnn.x, nnn.y, nnn.z, 0, 0, -1, 1.00f, 0.50f);
		cubeVertex(builder, camera, pose, tintColor, npn.x, npn.y, npn.z, 0, 0, -1, 1.00f, 0.25f);
		cubeVertex(builder, camera, pose, tintColor, ppn.x, ppn.y, ppn.z, 0, 0, -1, 0.75f, 0.25f);
		// +Z
		cubeVertex(builder, camera, pose, tintColor, nnp.x, nnp.y, nnp.z, 0, 0, 1, 0.25f, 0.50f);
		cubeVertex(builder, camera, pose, tintColor, pnp.x, pnp.y, pnp.z, 0, 0, 1, 0.50f, 0.50f);
		cubeVertex(builder, camera, pose, tintColor, ppp.x, ppp.y, ppp.z, 0, 0, 1, 0.50f, 0.25f);
		cubeVertex(builder, camera, pose, tintColor, npp.x, npp.y, npp.z, 0, 0, 1, 0.25f, 0.25f);
	}

	private static void ringVertex(VertexConsumer builder, CachedCamera<?> camera, PoseStack.Pose pose,
			Vec3 center, Color color, double x, double y, double z, float u, float v) {
		final float r = color.r(), g = color.g(), b = color.b(), a = color.a();
		var pos = Vec3.from(x, 0, z);
		// var n = pos.transformBy(pose);
		// var p = camera.toCameraSpace(n.mul(radius));
		var norm = (y > 0 ? Vec3.YN : Vec3.YP).transformBy(pose).normalize();
		var p = camera.toCameraSpace(pos.transformBy(pose)).add(center);
		builder.vertex((float) p.x, (float) p.y, (float) p.z)
				.uv(u, v)
				.color(r, g, b, a)
				.normal((float) norm.x, (float) norm.y, (float) norm.z)
				.endVertex();
	}

	private static void normSphereVertex(FlexibleBufferBuilder builder, CachedCamera<?> camera, PoseStack.Pose pose,
			Vec3 center, Color color, double radius, double x, double y, double z, float u, float v) {
		var pos = Vec3.from(x, y, z);
		var n = pos.normalize().transformBy(pose);
		// var p = camera.toCameraSpace(n.mul(radius));
		var p = camera.toCameraSpace(n.mul(radius)).add(center);
		builder.vertex(p).uv0(u, v).color(color).normal(n).endVertex();
	}

	private static void cubeVertex(VertexConsumer builder, CachedCamera<?> camera, PoseStack.Pose pose, Color color,
			double x, double y, double z, double nx, double ny, double nz, float u, float v) {
		final float r = color.r(), g = color.g(), b = color.b(), a = color.a();
		builder.vertex(pose.pose(), (float) x, (float) y, (float) z)
				.uv(u, v)
				.color(r, g, b, a)
				.normal(pose.normal(), (float) nx, (float) ny, (float) nz)
				.endVertex();
	}

}
